package net.corda.examples.tokenizedCurrency.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellTokenState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DigitalShellTokenTransfer {

        @InitiatingFlow
        @StartableByRPC
        public static class Initiator extends FlowLogic<SignedTransaction> {
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
            public SignedTransaction call() throws FlowException {
                IdentityService identityService = getServiceHub().getIdentityService();
                Party issuer=identityService.partiesFromName(issuerString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+ issuerString+"party not found"));
                Party receiver=identityService.partiesFromName(receiverString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+receiverString+"party not found"));

                List<StateAndRef<DigitalShellTokenState>> allTokenStateAndRefs =
                        getServiceHub().getVaultService().queryBy(DigitalShellTokenState.class).getStates();

                AtomicInteger totalTokenAvailable = new AtomicInteger();
                List<StateAndRef<DigitalShellTokenState>> inputStateAndRef = new ArrayList<>();
                AtomicInteger change = new AtomicInteger(0);

                List<StateAndRef<DigitalShellTokenState>> tokenStateAndRefs =  allTokenStateAndRefs.stream()
                        .filter(tokenStateStateAndRef -> {
                            //Filter according to issuer and address
                            if(tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer) && tokenStateStateAndRef.getState().getData().getAddress().equals(original_address)){
                                //Filter inputStates for spending
                                if(totalTokenAvailable.get() < amount)
                                    inputStateAndRef.add(tokenStateStateAndRef);

                                //Calculate total tokens available
                                totalTokenAvailable.set(totalTokenAvailable.get() + tokenStateStateAndRef.getState().getData().getAmount());

                                // Determine the change needed to be returned
                                if(change.get() == 0 && totalTokenAvailable.get() > amount){
                                    change.set(totalTokenAvailable.get() - amount);
                                }
                                //keep Address to use

                                return true;
                            }
                            return false;
                        }).collect(Collectors.toList());

                // Validate if there is sufficient tokens to spend
                if(totalTokenAvailable.get() < amount){
                    throw new FlowException("Insufficient balance");
                }

                DigitalShellTokenState outputState = new DigitalShellTokenState( issuer, receiver, amount, address);

                TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                        .getNotaryIdentities().get(0))
                        .addOutputState(outputState)
                        .addCommand(new TokenContract.Commands.Transfer(), ImmutableList.of(getOurIdentity().getOwningKey()));
                inputStateAndRef.forEach(txBuilder::addInputState);

                if(change.get() > 0){
                    DigitalShellTokenState changeState = new DigitalShellTokenState(issuer, getOurIdentity(), change.get(), original_address);
                    txBuilder.addOutputState(changeState);
                }

                txBuilder.verify(getServiceHub());

                SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

                // Updated Token State to be send to issuer and receiver
                FlowSession issuerSession = initiateFlow(issuer);
                FlowSession receiverSession = initiateFlow(receiver);

                if(receiver.equals(getOurIdentity())){
                    return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession)));
                }else {

                return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession, receiverSession)));

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
