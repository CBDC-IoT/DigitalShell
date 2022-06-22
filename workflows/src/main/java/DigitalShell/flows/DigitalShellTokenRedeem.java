package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.QueryableTokenContract;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import net.corda.examples.tokenizedCurrency.states.AddressState;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;
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

import static net.corda.core.node.services.vault.QueryCriteriaUtils.getField;

public class DigitalShellTokenRedeem {

    @InitiatingFlow
    @StartableByRPC
    public static class RedeemDigitalShellTokenFlow extends FlowLogic<String> {
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
        public String call() throws FlowException {
            /*Time manager used for performance test*/
            System.out.println("Redeem - Start");
            IdentityService identityService = getServiceHub().getIdentityService();

            Party issuer = getParty(identityService, issuerString);

            Party receiver=getParty(identityService, "Bank");

            TransactionBuilder txBuilder = getTransactionBuilder(issuer, receiver, amount, "Bank", original_address);

            SignedTransaction signedTransaction;

            signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

            // Updated Token State to be send to issuer and receiver
//                FlowSession issuerSession = initiateFlow(issuer);
            FlowSession receiverSession = initiateFlow(receiver);
            if(receiver.equals(getOurIdentity())){
                subFlow(new FinalityFlow(signedTransaction, ImmutableList.of()));
            }else {
                subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(receiverSession)));

            }
            return "Success";
        }

        @Suspendable
        private TransactionBuilder getTransactionBuilder(Party issuer, Party receiver, BigDecimal amount, String address, String original_address) throws FlowException {
            AtomicReference<BigDecimal> change = new AtomicReference<BigDecimal>(new BigDecimal(0));

            HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map = null;
            try {
                map = getPartyArrayListHashMap(issuer, change, original_address);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

            /*
             * How to choose Notary here
             * */
            TransactionBuilder txBuilder = mapAnalysisTransactionBuilder(map);

            //output
            DigitalShellQueryableState outputState = new DigitalShellQueryableState( issuer, receiver, amount, address);

            txBuilder.addOutputState(outputState).addCommand(new QueryableTokenContract.Commands.ShellRedeem(), ImmutableList.of(getOurIdentity().getOwningKey()));

            if(change.get().compareTo(BigDecimal.ZERO) == 1){//>0
                DigitalShellQueryableState changeState = new DigitalShellQueryableState(issuer, getOurIdentity(), change.get(), original_address);
                txBuilder.addOutputState(changeState);
            }

            txBuilder.verify(getServiceHub());
            return txBuilder;
        }

        /*get party from name*/
        private Party getParty(IdentityService identityService, String name) {
            return identityService.partiesFromName(name,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+ issuerString+"party not found"));
        }

        /*find all needed State*/
        @NotNull
        private HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> getPartyArrayListHashMap(Party issuer, AtomicReference<BigDecimal> change, String original_address) throws FlowException, NoSuchFieldException {
            AtomicReference<BigDecimal> totalTokenAvailable = new AtomicReference<BigDecimal>(new BigDecimal(0));

            AtomicBoolean getEnoughMoney= new AtomicBoolean(false);

            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

            FieldInfo attributeAddress = getField("address", AddressState.class);

            CriteriaExpression addressIndex = Builder.equal(attributeAddress, original_address.trim());

            QueryCriteria customCriteria = new QueryCriteria.VaultCustomQueryCriteria(addressIndex);

            QueryCriteria criteria = generalCriteria.and(customCriteria);

            PageSpecification pageSpec = new PageSpecification(1, 50);

            Vault.Page<DigitalShellQueryableState> digitalShellQueryableStatePage = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, criteria, pageSpec);

            System.out.println("Redeem - E-HKD search");

            HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map = new HashMap<>();

            List<StateAndRef<DigitalShellQueryableState>> states = digitalShellQueryableStatePage.getStates();
            List<StateAndRef<DigitalShellQueryableState>> tokenStateAndRefs =  states.stream().filter(tokenStateStateAndRef -> {
                if (getEnoughMoney.get() != true || totalTokenAvailable.get().compareTo(amount) == 0) {// totalToeknAvailable < Amount
                    if (map.get(tokenStateStateAndRef.getState().getNotary()) != null) {
                        ArrayList<StateAndRef<DigitalShellQueryableState>> stateAndRefs = map.get(tokenStateStateAndRef.getState().getNotary());
                        stateAndRefs.add(tokenStateStateAndRef);
                        map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                    } else {
                        ArrayList<StateAndRef<DigitalShellQueryableState>> stateAndRefs = new ArrayList<>();
                        stateAndRefs.add(tokenStateStateAndRef);
                        map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                    }
                    System.out.println(totalTokenAvailable);
                    //Calculate total tokens available
                    totalTokenAvailable.set(totalTokenAvailable.get().add(tokenStateStateAndRef.getState().getData().getAmount()));
                }

                // Determine the change needed to be returned
                if (change.get().equals(new BigDecimal(0)) && totalTokenAvailable.get().compareTo(amount) != -1) {//>=
                    change.set(totalTokenAvailable.get().subtract(amount));
                    getEnoughMoney.set(true);
                    System.out.println(change.get().toString());
                }
                //keep Address to use
                return true;
            }).collect(Collectors.toList());

            if(totalTokenAvailable.get().compareTo(amount)==-1){//<
                throw new FlowException("Insufficient balance");
            }

            return map;
        }

        /*put state into transactionbuilder*/
        @NotNull
        @Suspendable
        private TransactionBuilder mapAnalysisTransactionBuilder(HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map) throws FlowException {

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
            SignedTransaction signedTransaction =subFlow(new ReceiveFinalityFlow(otherPartySession));
            return signedTransaction;
        }
    }

}
