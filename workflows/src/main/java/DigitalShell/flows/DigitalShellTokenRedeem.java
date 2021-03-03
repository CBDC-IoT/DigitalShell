package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellTokenState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DigitalShellTokenRedeem {

    @InitiatingFlow
    @StartableByRPC
    public static class RedeemDigitalShellTokenFlow extends FlowLogic<SignedTransaction> {
        private String issuerString;
        private BigDecimal amount;
        private String original_address;
        // amount property of a Currency can change hence we are considering Currency as a evolvable asset

        public RedeemDigitalShellTokenFlow(String issuer, String amount , String address) {
            this.issuerString = issuer;
            this.amount = new BigDecimal(amount);
            this.original_address = address;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
//            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            IdentityService identityService = getServiceHub().getIdentityService();
            Party issuer=identityService.partiesFromName(issuerString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+ issuerString+"party not found"));

            AtomicDouble change = new AtomicDouble(0);

            HashMap<Party, ArrayList<StateAndRef<DigitalShellTokenState>>> map = getPartyArrayListHashMap(issuer,original_address, amount, change, 400);

            /*
             * How to choose Notary here
             * */

            SignedTransaction signedTransaction = null;

            TransactionBuilder txBuilder = getTransactionBuilder(map);

            //output
            DigitalShellTokenState outputState = new DigitalShellTokenState( issuer, issuer, amount, "Bank");

            txBuilder.addOutputState(outputState).addCommand(new TokenContract.Commands.Transfer(), ImmutableList.of(getOurIdentity().getOwningKey()));

            //judge change
            if(change.doubleValue() > 0){
                DigitalShellTokenState changeState = new DigitalShellTokenState(issuer, getOurIdentity(), new BigDecimal(change.get()), original_address);
                txBuilder.addOutputState(changeState);
            }

            signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession receiverSession = initiateFlow(issuer);
            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(receiverSession)));
        }



        @NotNull
        private HashMap<Party, ArrayList<StateAndRef<DigitalShellTokenState>>> getPartyArrayListHashMap(Party issuer, String original_address, BigDecimal amount, AtomicDouble change, int pagesize) throws FlowException {
            AtomicReference<BigDecimal> totalTokenAvailable = new AtomicReference<>(new BigDecimal(0));

            AtomicBoolean getEnoughMoney= new AtomicBoolean(false);

            int pageSize = pagesize;
            int pageNumber = 1;
            long totalStatesAvailable;
            HashMap<Party, ArrayList<StateAndRef<DigitalShellTokenState>>> map = new HashMap<>();

            LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info("Flag 0");
            do {
//                    System.out.println("Querying" + pageNumber);
                PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
                Vault.Page<DigitalShellTokenState> results =
                        getServiceHub().getVaultService().queryBy(DigitalShellTokenState.class, pageSpec);
                totalStatesAvailable = results.getTotalStatesAvailable();
                List<StateAndRef<DigitalShellTokenState>> states = results.getStates();
                List<StateAndRef<DigitalShellTokenState>> tokenStateAndRefs = states.stream().filter(tokenStateStateAndRef -> {
                    //Filter according to issuer and address
                    if (tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer) && tokenStateStateAndRef.getState().getData().getAddress().equals(original_address)) {

                        if (totalTokenAvailable.get().doubleValue() < amount.doubleValue()) {
                            if (map.get(tokenStateStateAndRef.getState().getNotary()) != null) {
                                ArrayList<StateAndRef<DigitalShellTokenState>> stateAndRefs = map.get(tokenStateStateAndRef.getState().getNotary());
                                stateAndRefs.add(tokenStateStateAndRef);
                                map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                            } else {
                                ArrayList<StateAndRef<DigitalShellTokenState>> stateAndRefs = new ArrayList<>();
                                stateAndRefs.add(tokenStateStateAndRef);
                                map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);

                            }
                            double v = totalTokenAvailable.get().doubleValue();
                            //Calculate total tokens available
                            totalTokenAvailable.set(tokenStateStateAndRef.getState().getData().getAmount().add(BigDecimal.valueOf(v)));
                        }

                        // Determine the change needed to be returned
                        if (change.get() == 0 && totalTokenAvailable.get().doubleValue() >= amount.doubleValue()) {
                            change.set(totalTokenAvailable.get().doubleValue() - amount.doubleValue());
                            getEnoughMoney.set(true);
                        }
                        //keep Address to use

                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
                pageNumber++;
            } while (( pageSize * ( pageNumber - 1 ) <= totalStatesAvailable ) && !getEnoughMoney.get());


            // Validate if there is sufficient tokens to spend
            if(totalTokenAvailable.get().doubleValue() < amount.doubleValue()){
                throw new FlowException("Insufficient balance");
            }


            return map;
        }

        /*put state into transactionbuilder*/
        @NotNull
        @Suspendable
        private TransactionBuilder getTransactionBuilder(HashMap<Party, ArrayList<StateAndRef<DigitalShellTokenState>>> map) throws FlowException {

            //judge num of notary and add inputState
            Party hotNotary = null;
            if(map.keySet().size() == 1){
                for(Party notary: map.keySet()){
                    hotNotary = notary;

                }

            }else {
                //get the max transaction list
                int size = -1;
                for(Party notary: map.keySet()){
                    int sizeTemp = map.get(notary).size();
                    if (sizeTemp > size){
                        size = sizeTemp;
                        hotNotary = notary;
                    }
                }
            }
            TransactionBuilder txBuilder = new TransactionBuilder(hotNotary);

            //notary change
            for(Party notary: map.keySet()){
                if(notary == hotNotary){
                    for (StateAndRef stateAndRef:map.get(notary)) {
                        txBuilder.addInputState(stateAndRef);
                    }
                }else{
                    for (StateAndRef stateAndRef:map.get(notary)){
                        StateAndRef newStateAndRef = (StateAndRef) subFlow(new NotaryChangeFlow(stateAndRef, hotNotary, AbstractStateReplacementFlow.Instigator.Companion.tracker()));
                        txBuilder.addInputState(newStateAndRef);
                    }
                }
            }
            return txBuilder;
        }
    }


    @InitiatedBy(RedeemDigitalShellTokenFlow.class)
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
