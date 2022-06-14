package com.template.webserver;

import DigitalShell.flows.*;
import com.template.webserver.Service.VendingMachineService;
import net.corda.core.messaging.CordaRPCOps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
//@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;

    @Autowired
    VendingMachineService vendingMachineService;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;

    }

    @GetMapping(value =  "/moveToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> MoveCurrencyTokenFlow(@RequestParam(value = "issuer") String issuer,
                                              @RequestParam(value = "amount") String amount,
                                              @RequestParam(value = "receiver") String receiver,
                                                        @RequestParam(value = "originalAddress") String originalAddress,
                                                        @RequestParam(value = "address") String address,
                                                        @RequestParam(value = "item") String item) {

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


    @GetMapping(value =  "/PickFood" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> MoveCurrencyTokenFlow(@RequestParam(value = "receiver") String receiver,
                                                        @RequestParam(value = "originalAddress") String originalAddress,
                                                        @RequestParam(value = "address") String address,
                                                        @RequestParam(value = "foodName") String foodName) {

        try {
            Integer amount = vendingMachineService.getFoodPrice(foodName);
            proxy.startTrackedFlowDynamic(DigitalShellTokenTransfer.Initiator.class,"Bank", amount, receiver, originalAddress, address).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(""+ amount + "DigitalShell has been transferred to " + receiver + " with address of " + address +".");
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
}
