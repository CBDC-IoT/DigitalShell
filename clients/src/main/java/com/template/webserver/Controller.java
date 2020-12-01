package com.template.webserver;
import com.r3.corda.lib.tokens.contracts.commands.*;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import kotlin.Pair;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.messaging.StateMachineTransactionMapping;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.WireTransaction;
import net.corda.examples.tokenizedCurrency.flows.DigitalShellTokenCreateAndIssue;
import net.corda.examples.tokenizedCurrency.flows.DigitalShellTokenTransfer;
import net.corda.examples.tokenizedCurrency.flows.entity.MyToken;
import net.corda.examples.tokenizedCurrency.flows.entity.MyTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);
    private final CordaX500Name me;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @GetMapping(value =  "/moveToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> MoveCurrencyTokenFlow(@RequestParam(value = "issuer") String issuer,
                                              @RequestParam(value = "amount") int amount,
                                              @RequestParam(value = "receiver") String receiver,
                                                        @RequestParam(value = "originalAddress") String originalAddress,
                                                        @RequestParam(value = "address") String address) {

        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenTransfer.Initiator.class, issuer, amount, receiver, originalAddress, address).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(""+ amount + "DigitalShell has been transferred to "+receiver+" with address of " + address +".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    @GetMapping(value =  "/createToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> createCurrencyTokenFlow(@RequestParam(value = "amount") int amount,
                                                          @RequestParam(value = "receiver") String receiver,
                                                          @RequestParam(value = "address") String address){

        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenCreateAndIssue.CreateDigitalShellTokenFlow.class,amount, receiver, address).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(" Token has been issued to .");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


}
