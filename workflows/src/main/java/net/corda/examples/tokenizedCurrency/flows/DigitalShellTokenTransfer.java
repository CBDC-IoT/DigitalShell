package net.corda.examples.tokenizedCurrency.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellTokenState;
import org.slf4j.LoggerFactory;
import util.MACRO_TIME_MANG;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DigitalShellTokenTransfer {

        @InitiatingFlow
        @StartableByRPC
        public static class Initiator extends FlowLogic<String> {
            private final String issuerString;
            private final int amount;
            private final String receiverString;
            private final String address;
            private final String original_address;

            public Initiator(String issuer, int amount, String receiver, String originalAddress, String address) {
                this.issuerString  = issuer;
                this.amount = amount;
                this.receiverString = receiver;
                this.address = address;
                this.original_address = originalAddress;
            }

            @Override
            @Suspendable
            public String call() throws FlowException {
                MACRO_TIME_MANG time_manager = new MACRO_TIME_MANG();
                time_manager.start();
                IdentityService identityService = getServiceHub().getIdentityService();
                Party issuer=identityService.partiesFromName(issuerString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+ issuerString+"party not found"));
                Party receiver=identityService.partiesFromName(receiverString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+receiverString+"party not found"));

                final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary2,L=Guangzhou,C=CN")); // METHOD 2
                final Party originalNotary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary1,L=Guangzhou,C=CN")); // METHOD 2
                time_manager.cut("1");
//                List<StateAndRef<DigitalShellTokenState>> allTokenStateAndRefs =
//                        getServiceHub().getVaultService().queryBy(DigitalShellTokenState.class).getStates();

                AtomicInteger totalTokenAvailable = new AtomicInteger();
                List<StateAndRef<DigitalShellTokenState>> inputStateAndRef = new ArrayList<>();
                AtomicInteger change = new AtomicInteger(0);

                time_manager.cut("2");

                AtomicBoolean getEnoughMoney= new AtomicBoolean(false);
                int pageSize=5;
                int pageNumber=1;
                long totalStatesAvailable;
                do {
//                    System.out.println("Querying" + pageNumber);
                    PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
                    Vault.Page<DigitalShellTokenState> results =
                            getServiceHub().getVaultService().queryBy(DigitalShellTokenState.class, pageSpec);
                    totalStatesAvailable = results.getTotalStatesAvailable();
                    List<StateAndRef<DigitalShellTokenState>> states = results.getStates();
                    List<StateAndRef<DigitalShellTokenState>> tokenStateAndRefs =  states.stream().filter(tokenStateStateAndRef -> {
                            //Filter according to issuer and address
                            if(tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer) && tokenStateStateAndRef.getState().getData().getAddress().equals(original_address)){

                                //Filter inputStates for spending
//                                try {
//                                    System.out.println(originalNotary);
//                                    System.out.println(notary);
//                                    subFlow(new SwitchNotaryFlow(tokenStateStateAndRef,notary));
//                                } catch (FlowException e) {
//                                    e.printStackTrace();
//                                }

                                if(totalTokenAvailable.get() < amount) {
                                    inputStateAndRef.add(tokenStateStateAndRef);

                                    //Calculate total tokens available
                                    totalTokenAvailable.set(totalTokenAvailable.get() + tokenStateStateAndRef.getState().getData().getAmount());
                                }

                                // Determine the change needed to be returned
                                if(change.get() == 0 && totalTokenAvailable.get() >= amount){
                                    change.set(totalTokenAvailable.get() - amount);
                                    getEnoughMoney.set(true);
                                }
                                //keep Address to use

                                return true;
                            }
                            return false;
                        }).collect(Collectors.toList());
                    pageNumber++;
                }while ((pageSize * (pageNumber - 1) <= totalStatesAvailable) && !getEnoughMoney.get());


                time_manager.cut("3");

                // Validate if there is sufficient tokens to spend
                if(totalTokenAvailable.get() < amount){
                    throw new FlowException("Insufficient balance");
                }
//                time_manager.cut("4");
                DigitalShellTokenState outputState = new DigitalShellTokenState( issuer, receiver, amount, address);

//                time_manager.cut("5");


                /*
                * How to choose Notary here
                * */










                /*
                 * How to choose Notary here
                 * */


                TransactionBuilder txBuilder = new TransactionBuilder(originalNotary)
                        .addOutputState(outputState)
                        .addCommand(new TokenContract.Commands.Transfer(), ImmutableList.of(getOurIdentity().getOwningKey()));

                inputStateAndRef.forEach(txBuilder::addInputState);
//                time_manager.cut("6");
                if(change.get() > 0){
                    DigitalShellTokenState changeState = new DigitalShellTokenState(issuer, getOurIdentity(), change.get(), original_address);
                    txBuilder.addOutputState(changeState);
                }

                txBuilder.verify(getServiceHub());
                time_manager.cut("7");
                SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

                // Updated Token State to be send to issuer and receiver
                FlowSession issuerSession = initiateFlow(issuer);
                FlowSession receiverSession = initiateFlow(receiver);
                time_manager.cut("8");


                if(receiver.equals(getOurIdentity())){
                    subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession)));
                    time_manager.cut("9");
                    time_manager.result();
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info("SiYuan1");
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info(time_manager.result());
//                    subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession)))
                    return "Success";
                }else {
//                    FinalityFlow finalityFlow = new FinalityFlow(signedTransaction,ImmutableList.of(issuerSession, receiverSession));
                    time_manager.cut("9");
                    subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession, receiverSession)));
                    time_manager.cut("10");
                    System.out.println(time_manager.result());
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info("SiYuan2");
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info(time_manager.result());
//                    subFlow(finalityFlow);
                    return "Success";
            }}
        }

        @InitiatedBy(Initiator.class)
        public static class Responder extends FlowLogic<SignedTransaction>{

            private FlowSession otherPartySession;

            public Responder(FlowSession otherPartySession) {
                this.otherPartySession = otherPartySession;
            }

            @Override
            @Suspendable
            public SignedTransaction call() throws FlowException {
                return subFlow(new ReceiveFinalityFlow(otherPartySession));
            }
        }

}
