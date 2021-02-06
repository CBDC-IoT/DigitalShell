package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.QueryableTokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import util.MACRO_TIME_MANG;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DigitalShellMerger {

        @InitiatingFlow
        @StartableByRPC
        public static class MergeDigitalShellFlow extends FlowLogic<String> {
            private final String partyString;
            private final String original_address;

            public MergeDigitalShellFlow(String party, String originalAddress) {
                this.partyString  = party;
                this.original_address = originalAddress;
            }

            @Override
            @Suspendable
            public String call() throws FlowException {

                MACRO_TIME_MANG time_manager = new MACRO_TIME_MANG();
                time_manager.start();

                IdentityService identityService = getServiceHub().getIdentityService();

                Party issuer = getParty(identityService, partyString);

                time_manager.cut("2");

                AtomicReference<BigDecimal> totalTokenAvailable = new AtomicReference<BigDecimal>(new BigDecimal(0));

                HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map = getPartyArrayListHashMap(totalTokenAvailable, issuer, 300);

                /*
                * How to choose Notary here
                * */

                time_manager.cut("4");

                SignedTransaction signedTransaction = null;

                TransactionBuilder txBuilder = getTransactionBuilder(map);

                //output
                DigitalShellQueryableState outputState = new DigitalShellQueryableState( issuer, getOurIdentity(), totalTokenAvailable.get(), original_address);

                txBuilder.addOutputState(outputState).addCommand(new QueryableTokenContract.Commands.ShellTransfer(), ImmutableList.of(getOurIdentity().getOwningKey()));

                signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

                txBuilder.verify(getServiceHub());

                // Updated Token State to be send to issuer and receiver
//                FlowSession issuerSession = initiateFlow(issuer);


                subFlow(new FinalityFlow(signedTransaction, ImmutableList.of()));
                time_manager.cut("9");
                System.out.println(time_manager.result());
                time_manager.result();
                LoggerFactory.getLogger(DigitalShellMerger.class).info("SiYuan1");
                LoggerFactory.getLogger(DigitalShellMerger.class).info(time_manager.result());

                return "Success";
            }

            /*get party from name*/
            private Party getParty(IdentityService identityService, String name) {
                return identityService.partiesFromName(name,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+ partyString+"party not found"));
            }

            /*find all needed State*/
            @NotNull
            private HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> getPartyArrayListHashMap( AtomicReference<BigDecimal> totalTokenAvailable, Party issuer, int pagesize) throws FlowException {

                int pageSize = pagesize;
                int pageNumber = 1;
                long totalStatesAvailable;
                HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map = new HashMap<>();

                LoggerFactory.getLogger(DigitalShellMerger.class).info("SiYuan0");
                do {
//                    System.out.println("Querying" + pageNumber);
                    PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
                    Vault.Page<DigitalShellQueryableState> results =
                            getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, pageSpec);
                    totalStatesAvailable = results.getTotalStatesAvailable();
                    List<StateAndRef<DigitalShellQueryableState>> states = results.getStates();
                    List<StateAndRef<DigitalShellQueryableState>> tokenStateAndRefs =  states.stream().filter(tokenStateStateAndRef -> {
                            //Filter according to issuer and address
                            if(tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer) && tokenStateStateAndRef.getState().getData().getAddress().equals(original_address)){

                                    if(map.get(tokenStateStateAndRef.getState().getNotary())!= null) {
                                        ArrayList<StateAndRef<DigitalShellQueryableState>> stateAndRefs = map.get(tokenStateStateAndRef.getState().getNotary());
                                        stateAndRefs.add(tokenStateStateAndRef);
                                        map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                                    }else{
                                        ArrayList<StateAndRef<DigitalShellQueryableState>> stateAndRefs = new ArrayList<>();
                                        stateAndRefs.add(tokenStateStateAndRef);
                                        map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                                    }
                                totalTokenAvailable.set(totalTokenAvailable.get().add(tokenStateStateAndRef.getState().getData().getAmount()));
                                return true;
                            }
                            return false;
                        }).collect(Collectors.toList());
                    pageNumber++;
                }while ((pageSize * (pageNumber - 1) <= totalStatesAvailable));

                return map;
            }

            /*put state into transactionbuilder*/
            @NotNull
            @Suspendable
            private TransactionBuilder getTransactionBuilder(HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map) throws FlowException {

                //judge num of notary and add inputState
                Party hotNotary = null;
                if(map.keySet().size() == 1){
                    for(Party notary: map.keySet()){
                        hotNotary = notary;

                    }

                }else if(map.keySet().size() == 0){
                    throw new IllegalStateException("You do not have any money");
                }
                else {
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



        @InitiatedBy(MergeDigitalShellFlow.class)
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

