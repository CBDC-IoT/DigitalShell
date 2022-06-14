package com.template.webserver;

import Bean.MyTransaction;
import DigitalShell.flows.*;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.messaging.StateMachineTransactionMapping;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.WireTransaction;
import net.corda.examples.tokenizedCurrency.contracts.QueryableTokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
//@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @GetMapping(value =  "/moveToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> MoveCurrencyTokenFlow(@RequestParam(value = "issuer") String issuer,
                                              @RequestParam(value = "amount") String amount,
                                              @RequestParam(value = "receiver") String receiver,
                                                        @RequestParam(value = "originalAddress") String originalAddress,
                                                        @RequestParam(value = "address") String address,
                                                        @RequestParam(value = "item") Optional<String> itemString) {
        String item = itemString.orElse("Transfer");
        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenTransfer.Initiator.class,issuer, amount, receiver, originalAddress, address, item).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(""+ amount + " DigitalShell has been transferred to "+receiver+" from the address of " + address + " for " + item +".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value =  "/issueToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> createCurrencyTokenFlow(@RequestParam(value = "amount") String amount,
                                                          @RequestParam(value = "receiver") String receiver,
                                                          @RequestParam(value = "address") String address,
                                                          @RequestParam(value = "notary") Optional<Integer> notary){
        Integer notaryInt = notary.orElse(0);
        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenCreateAndIssue.CreateDigitalShellTokenFlow.class,amount, receiver, address, notaryInt).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body("" + amount + " E-HKD has been issued to "+ address + ".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value =  "/redeemToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> redeemCurrencyTokenFlow(@RequestParam(value = "issuer") String issuer,
                                                          @RequestParam(value = "amount") String amount,
                                                          @RequestParam(value = "address") String address){

        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenRedeem.RedeemDigitalShellTokenFlow.class, issuer, amount , address).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(" Token has been redeemed to "+ address + ".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value =  "/queryToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> queryCurrencyTokenFlow(@RequestParam(value = "issuer") String issuer,
                                                          @RequestParam(value = "address") String address){

        try {
            BigDecimal balance = proxy.startTrackedFlowDynamic(DigitalShellQuery.DigitalShellQueryFlow.class, issuer, address).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body("The balance of "+ address + " is " + balance.toString() + ".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value =  "/notaryChange/{issuer}/{address}/{newNotary}" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> notaryChangeFlow(@PathVariable(value = "issuer") String issuer,
                                                          @PathVariable(value = "address") String address,
                                                          @PathVariable(value = "newNotary") String notary){

        try {
            proxy.startTrackedFlowDynamic(SwitchNotaryFlow.class,issuer, address, notary).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(" Token has been issued to "+ address + ".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    /*
    * Get Transaction History for front-end presentation
    * */
    @GetMapping(value = "/getTransactionHistory", produces = APPLICATION_JSON_VALUE)
    private List<MyTransaction> getTransactionHistroy() {
        List<MyTransaction> list = new ArrayList<>();
        List<StateMachineTransactionMapping> stateMachineTransactionMappings = proxy.stateMachineRecordedTransactionMappingSnapshot();
        for (StateMachineTransactionMapping stateMachineTransactionMapping : stateMachineTransactionMappings) {
            SecureHash secureHash = stateMachineTransactionMapping.component2();
            WireTransaction tx = proxy.internalFindVerifiedTransaction(secureHash).getTx();

//            CoreTransaction coreTransaction = proxy.internalFindVerifiedTransaction(SecureHash.parse(txid)).getCoreTransaction();
            boolean equalsIssue = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellIssue.class);
            boolean equalsMove = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellTransfer.class);
            boolean equalsRedeem = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.Redeem.class);

            System.out.println(equalsIssue);
            Iterable<String> items=proxy.getVaultTransactionNotes(secureHash);
            String item=items.iterator().hasNext()?items.iterator().next():"no item recorded";
            if (equalsIssue) {
                DigitalShellQueryableState output = (DigitalShellQueryableState) tx.getOutput(0);
                String organisation = Objects.requireNonNull(output.getOwner().nameOrNull()).getOrganisation();
                MyTransaction myTransaction = new MyTransaction(secureHash.toString(),"Issue", getTime(secureHash.toString()), output.getAmount(), organisation,"");
                list.add(myTransaction);
            }
            if (equalsMove) {
                DigitalShellQueryableState output = (DigitalShellQueryableState) tx.getOutput(0);
//                System.out.println(output1.getIssuedTokenType().getTokenType());
                MyTransaction myTransaction=null;
                String organisation = Objects.requireNonNull(output.getOwner().nameOrNull()).getOrganisation();
                if(output.getOwner().toString().equals(me.toString())){
                    myTransaction = new MyTransaction(secureHash.toString(),"received", getTime(secureHash.toString()), output.getAmount(), organisation, item);
                }
                else {
                    myTransaction = new MyTransaction(secureHash.toString(),"Consumed", getTime(secureHash.toString()), output.getAmount(), organisation, item);
                }
                list.add(myTransaction);
            }
        }
        Collections.reverse(list);
        return list;
    }

    private String getTime(String txid){
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        List<Vault.StateMetadata> statesMetadata = proxy.vaultQueryByCriteria(generalCriteria, DigitalShellQueryableState.class).getStatesMetadata();
        for (Vault.StateMetadata state:statesMetadata){
            if (state.getRef().getTxhash().equals(SecureHash.parse(txid))){
                ZoneId ShangHai = ZoneId.of("Asia/Shanghai");
                Instant recordedTime = state.getRecordedTime();

                DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

                String time2=DATE_TIME_FORMATTER.format(recordedTime);
                return time2;
            }
        }
        return "no record";
    }


    /*
    * Find new Transactions
    *
    * */
    @GetMapping(value = "/getTransactionUpdate", produces = APPLICATION_JSON_VALUE)
    private List<MyTransaction> getTransactionUpdate(  @RequestParam(value = "txid") String txid) {
        boolean alreadyUpdated = true;
        List<MyTransaction> list = new ArrayList<>();
        List<StateMachineTransactionMapping> stateMachineTransactionMappings = proxy.stateMachineRecordedTransactionMappingSnapshot();
        String lastTxId= stateMachineTransactionMappings.get(stateMachineTransactionMappings.size()-1).component2().toString();
        if(lastTxId.equals(txid)){
            return list;
        }

        for (StateMachineTransactionMapping stateMachineTransactionMapping : stateMachineTransactionMappings) {
            SecureHash secureHash = stateMachineTransactionMapping.component2();
            if (!alreadyUpdated) {
                WireTransaction tx = proxy.internalFindVerifiedTransaction(secureHash).getTx();
                boolean equalsIssue = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellIssue.class);
                boolean equalsMove = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellTransfer.class);
                boolean equalsRedeem = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.Redeem.class);
                Iterable<String> foods = proxy.getVaultTransactionNotes(secureHash);
                String food = foods.iterator().hasNext() ? foods.iterator().next() : "no food recorded";
                if (equalsIssue) {
                    ContractState output = tx.getOutput(0);
                    DigitalShellQueryableState output1 = (DigitalShellQueryableState) output;
                    String organisation = Objects.requireNonNull(output1.getOwner().nameOrNull()).getOrganisation();
                    MyTransaction myTransaction = new MyTransaction(secureHash.toString(), "Issue", getTime(secureHash.toString()), output1.getAmount(), organisation, "");
                    list.add(myTransaction);
                }
                if (equalsMove) {
                    ContractState output = tx.getOutput(0);
                    DigitalShellQueryableState output1 = (DigitalShellQueryableState) output;
                    MyTransaction myTransaction = null;
                    String organisation = Objects.requireNonNull(output1.getOwner().nameOrNull()).getOrganisation();
                    if (output1.getOwner().toString().equals(me.toString())) {
                        myTransaction = new MyTransaction(secureHash.toString(), "received", getTime(secureHash.toString()), output1.getAmount(), organisation, food);
                    } else {
                        myTransaction = new MyTransaction(secureHash.toString(), "Consumed", getTime(secureHash.toString()), output1.getAmount(), organisation, food);
                    }
                    list.add(myTransaction);
                }
            }
            if(txid.equals(secureHash.toString())){
                alreadyUpdated=false;
            }
        }
        Collections.reverse(list);
        return list;
    }


}
