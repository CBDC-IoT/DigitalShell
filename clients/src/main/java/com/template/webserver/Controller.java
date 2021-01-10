package com.template.webserver;

import DigitalShell.flows.DigitalShellTokenCreateAndIssue;
import DigitalShell.flows.DigitalShellTokenTransfer;
import com.template.webserver.Service.VendingMachineService;
import net.corda.core.messaging.CordaRPCOps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                                                        @RequestParam(value = "address") String address) {

        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenTransfer.Initiator.class,issuer, amount, receiver, originalAddress, address).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(""+ amount + "DigitalShell has been transferred to "+receiver+" with address of " + address +".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    @GetMapping(value =  "/createToken" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> createCurrencyTokenFlow(@RequestParam(value = "amount") String amount,
                                                          @RequestParam(value = "receiver") String receiver,
                                                          @RequestParam(value = "address") String address,
                                                          @RequestParam(value = "notary") int notary){

        try {
            proxy.startTrackedFlowDynamic(DigitalShellTokenCreateAndIssue.CreateDigitalShellTokenFlow.class,amount, receiver, address, notary).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.OK).body(" Token has been issued to "+ address + ".");
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
}
