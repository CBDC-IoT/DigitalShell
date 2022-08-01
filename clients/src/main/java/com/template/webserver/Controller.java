package com.template.webserver;

import DigitalShell.flows.*;
import com.template.bean.*;
import kotlin.Pair;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
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
import java.util.concurrent.ConcurrentHashMap;

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

    @PostMapping(value =  "/moveToken" , consumes = APPLICATION_JSON_VALUE, produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> MoveCurrencyTokenFlow(@RequestBody movedToken movedToken) {
        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenTransfer.Initiator.class,movedToken.getIssuer(), movedToken.getAmount(), movedToken.getReceiver(),
                    movedToken.getOriginalAddress(), movedToken.getAddress(), movedToken.getItemString()).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(""+ movedToken.getAmount() + " DigitalShell has been transferred to "+movedToken.getReceiver() +" from the address of " +
                    movedToken.getAddress() + " for " + movedToken.getItemString() +".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value="/issueToken", consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createCurrencyTokenFlow(@RequestBody IssuedToken issuedToken){
        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenCreateAndIssue.CreateDigitalShellTokenFlow.class,issuedToken.getAmount(), issuedToken.getReceiver(), issuedToken.getAddress(), issuedToken.getNotary()).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body("" + issuedToken.getAmount() + " E-HKD has been issued to "+ issuedToken.getAddress() + ".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value =  "/redeemToken" , consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE )
    public ResponseEntity<String> redeemCurrencyTokenFlow(@RequestBody redeemedToken redeemedToken){

        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenRedeem.RedeemDigitalShellTokenFlow.class,
                    redeemedToken.getIssuer(), redeemedToken.getAmount(), redeemedToken.getAddress()).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(redeemedToken.getAmount() + " E-HKD has been redeemed to Bank.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value =  "/queryToken" , consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> queryCurrencyTokenFlow(@RequestBody queriedToken queriedToken){
        try {
            BigDecimal balance = proxy.startTrackedFlowDynamic(DigitalShellQuery.DigitalShellQueryFlow.class,
                    queriedToken.getIssuer(), queriedToken.getAddress()).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(balance.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value =  "/notaryChange" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> notaryChangeFlow(@RequestParam(value = "issuer") String issuer,
                                                   @RequestParam(value = "address") String address,
                                                   @RequestParam(value = "notary") Optional<Integer> notary){
        Integer notaryInt = notary.orElse(0);
        try {
            proxy.startTrackedFlowDynamic(SwitchNotaryFlow.class, issuer, address, notaryInt).getReturnValue().get();
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

//          CoreTransaction coreTransaction = proxy.internalFindVerifiedTransaction(SecureHash.parse(txid)).getCoreTransaction();
            boolean equalsIssue = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellIssue.class);
            boolean equalsMove = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellTransfer.class);
            boolean equalsRedeem = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellRedeem.class);

            Iterable<String> items=proxy.getVaultTransactionNotes(secureHash);
            String item=items.iterator().hasNext()?items.iterator().next():"no item recorded";

            if (equalsIssue) {
                DigitalShellQueryableState output = (DigitalShellQueryableState) tx.getOutput(0);
                String payeeNode = Objects.requireNonNull(output.getOwner().nameOrNull()).getOrganisation();
                String payeeAddress = Objects.requireNonNull(output.getAddress());
                MyTransaction myTransaction = new MyTransaction(secureHash.toString(),"Issued", getTime(secureHash.toString()), output.getAmount(), "Bank" , "Bank" ,payeeNode, payeeAddress,"");
                list.add(myTransaction);
            }

            if (equalsMove) {
                DigitalShellQueryableState output = (DigitalShellQueryableState) tx.getOutput(0);

                /*get input information*/
                StateRef stateRef = tx.getInputs().get(0);
                WireTransaction input_tx = proxy.internalFindVerifiedTransaction(stateRef.getTxhash()).getTx();
                DigitalShellQueryableState output1 = (DigitalShellQueryableState) input_tx.getOutput(stateRef.getIndex());
                String payerAddress = output1.getAddress();
                String payerNode = Objects.requireNonNull(output1.getOwner().nameOrNull().getOrganisation());

                MyTransaction myTransaction = null;
                String payeeNode = Objects.requireNonNull(output.getOwner().nameOrNull()).getOrganisation();
                String payeeAddress = Objects.requireNonNull(output.getAddress());
                if(output.getOwner().toString().equals(me.toString())){
                    myTransaction = new MyTransaction(secureHash.toString(),"Received", getTime(secureHash.toString()), output.getAmount(), payerNode, payerAddress, payeeNode, payeeAddress, item);
                }
                else {
                    myTransaction = new MyTransaction(secureHash.toString(),"Consumed", getTime(secureHash.toString()), output.getAmount(), payerNode, payerAddress, payeeNode, payeeAddress, item);
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
                boolean equalsRedeem = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellRedeem.class);
                Iterable<String> foods = proxy.getVaultTransactionNotes(secureHash);
                String item = foods.iterator().hasNext() ? foods.iterator().next() : "no food recorded";
                if (equalsIssue) {
                    DigitalShellQueryableState output = (DigitalShellQueryableState) tx.getOutput(0);
                    String payeeNode = Objects.requireNonNull(output.getOwner().nameOrNull()).getOrganisation();
                    String payeeAddress = Objects.requireNonNull(output.getAddress());
                    System.out.println(payeeAddress);
                    MyTransaction myTransaction = new MyTransaction(secureHash.toString(), "Issue",  getTime(secureHash.toString()), output.getAmount(), "Bank", "Bank", payeeNode, payeeAddress, item);
                    list.add(myTransaction);
                }

                if (equalsMove) {
                    DigitalShellQueryableState output = (DigitalShellQueryableState) tx.getOutput(0);;

                    /*get input informaion*/
                    StateRef stateRef = tx.getInputs().get(0);
                    WireTransaction input_tx = proxy.internalFindVerifiedTransaction(stateRef.getTxhash()).getTx();
                    DigitalShellQueryableState output1 = (DigitalShellQueryableState) input_tx.getOutput(stateRef.getIndex());
                    String payerAddress = output1.getAddress();
                    String payerNode = Objects.requireNonNull(output1.getOwner().nameOrNull().getOrganisation());

                    MyTransaction myTransaction = null;
                    String payeeNode = Objects.requireNonNull(output1.getOwner().nameOrNull()).getOrganisation();
                    String payeeAddress = Objects.requireNonNull(output1.getAddress());
                    if (output1.getOwner().toString().equals(me.toString())) {
                        myTransaction = new MyTransaction(secureHash.toString(),"Received", getTime(secureHash.toString()), output.getAmount(), payerNode, payerAddress, payeeNode, payeeAddress, item);
                    } else {
                        myTransaction = new MyTransaction(secureHash.toString(), "Consumed", getTime(secureHash.toString()), output.getAmount(), payerNode, payerAddress, payeeNode, payeeAddress, item);
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

    /**
    * This method is only for demo use. The Canteen can get transaction volumes and sales from this API.
    * We need to implement this API in Canteen end in the future.
    * */
    @GetMapping(value = "/getTotalVolumesAndSales", produces = APPLICATION_JSON_VALUE)
    private Map<String, Double> getTotalVolumesAndSales() {
        int count=0;
        double sales=0;
        List<StateMachineTransactionMapping> stateMachineTransactionMappings = proxy.stateMachineRecordedTransactionMappingSnapshot();
        for (StateMachineTransactionMapping stateMachineTransactionMapping : stateMachineTransactionMappings) {
            SecureHash secureHash = stateMachineTransactionMapping.component2();
            WireTransaction tx = proxy.internalFindVerifiedTransaction(secureHash).getTx();
            boolean equalsMove = tx.getCommands().get(0).getValue().getClass().equals(QueryableTokenContract.Commands.ShellTransfer.class);
            if (equalsMove) {
                ContractState output = tx.getOutput(0);
                DigitalShellQueryableState output1 = (DigitalShellQueryableState) output;
                AbstractParty holder = output1.getOwner();
                holder.nameOrNull();

                if(holder.nameOrNull().equals(me)){
                    count++;
                    sales+= output1.getAmount().doubleValue();
                }
            }
        }
        Map<String,Double> map = new ConcurrentHashMap<>();
        map.put("totalSales",sales);
        map.put("totalVolumes",(double)count);
        Pair<Integer, Double> pair = new Pair<>(count, sales);
        return map;
    }

}
