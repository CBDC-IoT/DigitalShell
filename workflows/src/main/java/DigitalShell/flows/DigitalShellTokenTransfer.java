package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.QueryableTokenContract;
import net.corda.examples.tokenizedCurrency.states.AddressState;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import util.MACRO_TIME_MANG;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.getField;

public class DigitalShellTokenTransfer {

        @InitiatingFlow
        @StartableByRPC
        public static class Initiator extends FlowLogic<String> {
            private final String issuerString;
            private final BigDecimal amount;
            private final String receiverString;
            private final String address;
            private final String original_address;

            public Initiator(String issuer, String amount, String receiver, String originalAddress, String address) {
                this.issuerString  = issuer;
                this.amount = new BigDecimal(amount);
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

                Party issuer = getParty(identityService, issuerString);

                Party receiver=getParty(identityService, receiverString);

                AtomicReference<BigDecimal> change = new AtomicReference<BigDecimal>(new BigDecimal(0));

                time_manager.cut("2");

                HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map = null;
                try {
                    map = getPartyArrayListHashMap(issuer, change, 300);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }

                /*
                * How to choose Notary here
                * */

                time_manager.cut("4");

                SignedTransaction signedTransaction = null;

                TransactionBuilder txBuilder = getTransactionBuilder(map);

                //output
                DigitalShellQueryableState outputState = new DigitalShellQueryableState( issuer, receiver, amount, address);

                txBuilder.addOutputState(outputState).addCommand(new QueryableTokenContract.Commands.ShellTransfer(), ImmutableList.of(getOurIdentity().getOwningKey()));

                if(change.get().compareTo(BigDecimal.ZERO) == 1){//>0
                    DigitalShellQueryableState changeState = new DigitalShellQueryableState(issuer, getOurIdentity(), change.get(), original_address);
                    txBuilder.addOutputState(changeState);
                }

                signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

                txBuilder.verify(getServiceHub());

                // Updated Token State to be send to issuer and receiver
                FlowSession issuerSession = initiateFlow(issuer);
                FlowSession receiverSession = initiateFlow(receiver);


                if(receiver.equals(getOurIdentity())){
                    subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession)));
                    time_manager.cut("9");
                    System.out.println(time_manager.result());
                    time_manager.result();
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info("SiYuan1");
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info(time_manager.result());

                }else {
//                    FinalityFlow finalityFlow = new FinalityFlow(signedTransaction,ImmutableList.of(issuerSession, receiverSession));
                    subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession, receiverSession)));
                    time_manager.cut("9");
                    System.out.println(time_manager.result());
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info("SiYuan2");
                    LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info(time_manager.result());

            }
                return "Success";
            }

            /*get party from name*/
            private Party getParty(IdentityService identityService, String name) {
                return identityService.partiesFromName(name,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+ issuerString+"party not found"));
            }



            /*find all needed State*/
            @NotNull
            private HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> getPartyArrayListHashMap(Party issuer, AtomicReference<BigDecimal> change, int pagesize) throws FlowException, NoSuchFieldException {
                AtomicReference<BigDecimal> totalTokenAvailable = new AtomicReference<BigDecimal>(new BigDecimal(0));

                AtomicBoolean getEnoughMoney= new AtomicBoolean(false);

                QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

                FieldInfo attributeAddress = getField("address", AddressState.class);

                CriteriaExpression addressIndex = Builder.equal(attributeAddress, original_address.trim());

                QueryCriteria customCriteria = new QueryCriteria.VaultCustomQueryCriteria(addressIndex);

                QueryCriteria criteria = generalCriteria.and(customCriteria);


                PageSpecification pageSpec = new PageSpecification(1, 50);

                Vault.Page<DigitalShellQueryableState> digitalShellQueryableStatePage = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, criteria, pageSpec);


                long totalStatesAvailable1 = digitalShellQueryableStatePage.getTotalStatesAvailable();

                System.out.println("TotalAvailable" + totalStatesAvailable1);

//                Vault.Page<DigitalShellQueryableState> generalResult = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, generalCriteria, pageSpec);
//
//                System.out.println("generalResult" + generalResult.getTotalStatesAvailable());
//
//
//                Vault.Page<DigitalShellQueryableState> result = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, pageSpec);
//                System.out.println("generalResult" + result.getTotalStatesAvailable());
//
//                Vault.Page<DigitalShellQueryableState> customresult = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, customCriteria, pageSpec);
//                System.out.println("customresult" + customresult.getTotalStatesAvailable());
//
//
//                Vault.Page<DigitalShellQueryableState> digitalShellQueryableStatePage1 = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, pageSpec);
//                System.out.println("aaaa" + digitalShellQueryableStatePage1.getTotalStatesAvailable());
//                System.out.println("aaaa" + digitalShellQueryableStatePage1.getStates().size());


//                String nativeQuery = "SELECT v.transaction_id, v.output_index FROM DIGITAL_SHELL v WHERE v.address =' " + original_address + "'";
//
//                Connection connection = getServiceHub().jdbcSession();
//
//                try {
//                    ResultSet resultSet = connection.prepareStatement(nativeQuery).executeQuery();
//                    System.out.println("resultSet" + resultSet.getFetchSize());
//
//
//                    String string = resultSet.getString(1);
//                    System.out.println("TxId:" + string);
//
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }

                HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map = new HashMap<>();


                List<StateAndRef<DigitalShellQueryableState>> states = digitalShellQueryableStatePage.getStates();
                List<StateAndRef<DigitalShellQueryableState>> tokenStateAndRefs =  states.stream().filter(tokenStateStateAndRef -> {
                    //Filter according to issuer and address
                    if(tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer)){

                        if(totalTokenAvailable.get().compareTo(amount)== -1) {// <
                            if(map.get(tokenStateStateAndRef.getState().getNotary())!= null) {
                                ArrayList<StateAndRef<DigitalShellQueryableState>> stateAndRefs = map.get(tokenStateStateAndRef.getState().getNotary());
                                stateAndRefs.add(tokenStateStateAndRef);
                                map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                            }else{
                                ArrayList<StateAndRef<DigitalShellQueryableState>> stateAndRefs = new ArrayList<>();
                                stateAndRefs.add(tokenStateStateAndRef);
                                map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                            }

                            //Calculate total tokens available
                            totalTokenAvailable.set(totalTokenAvailable.get().add( tokenStateStateAndRef.getState().getData().getAmount()));
                        }

                        // Determine the change needed to be returned
                        if(change.get().equals(new BigDecimal(0)) && totalTokenAvailable.get().compareTo(amount)!= -1 ){//>=
                            change.set(totalTokenAvailable.get().subtract( amount) );
                            getEnoughMoney.set(true);
                            System.out.println(change.get().toString());
                        }
                        //keep Address to use

                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());

                if(totalTokenAvailable.get().compareTo(amount)==-1){//<
                    throw new FlowException("Insufficient balance");
                }
//                int pageSize = pagesize;
//                int pageNumber = 1;
//                long totalStatesAvailable;
//                HashMap<Party, ArrayList<StateAndRef<DigitalShellTokenState>>> map = new HashMap<>();
//                LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info("SiYuan0");
//                do {
////                    System.out.println("Querying" + pageNumber);
////                    PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
//                    Vault.Page<DigitalShellTokenState> results =
//                            getServiceHub().getVaultService().queryBy(DigitalShellTokenState.class, pageSpec);
//                    totalStatesAvailable = results.getTotalStatesAvailable();
//                    List<StateAndRef<DigitalShellTokenState>> states = results.getStates();
//                    List<StateAndRef<DigitalShellTokenState>> tokenStateAndRefs =  states.stream().filter(tokenStateStateAndRef -> {
//                            //Filter according to issuer and address
//                            if(tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer) && tokenStateStateAndRef.getState().getData().getAddress().equals(original_address)){
//
//                                if(totalTokenAvailable.get().compareTo(amount)== -1) {// <
//                                    if(map.get(tokenStateStateAndRef.getState().getNotary())!= null) {
//                                        ArrayList<StateAndRef<DigitalShellTokenState>> stateAndRefs = map.get(tokenStateStateAndRef.getState().getNotary());
//                                        stateAndRefs.add(tokenStateStateAndRef);
//                                        map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
//                                    }else{
//                                        ArrayList<StateAndRef<DigitalShellTokenState>> stateAndRefs = new ArrayList<>();
//                                        stateAndRefs.add(tokenStateStateAndRef);
//                                        map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
//                                    }
//
//                                    //Calculate total tokens available
//                                    totalTokenAvailable.set(totalTokenAvailable.get().add( tokenStateStateAndRef.getState().getData().getAmount()));
//                                }
//
//                                // Determine the change needed to be returned
//                                if(change.get().equals(new BigDecimal(0)) && totalTokenAvailable.get().compareTo(amount)!= -1 ){//>=
//                                    change.set(totalTokenAvailable.get().subtract( amount) );
//                                    getEnoughMoney.set(true);
//                                    System.out.println(change.get().toString());
//                                }
//                                //keep Address to use
//
//                                return true;
//                            }
//                            return false;
//                        }).collect(Collectors.toList());
//                    pageNumber++;
//                }while ((pageSize * (pageNumber - 1) <= totalStatesAvailable) && !getEnoughMoney.get());
//
//                System.out.println(totalStatesAvailable);
//                // Validate if there is sufficient tokens to spend
//                if(totalTokenAvailable.get().compareTo(amount)==-1){//<
//                    throw new FlowException("Insufficient balance");
//                }
                return map;
            }

            /*put state into transactionbuilder*/
            @NotNull
            @Suspendable
            private TransactionBuilder getTransactionBuilder(HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map) throws FlowException {


                LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info("map");
                LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info(map.toString());
                LoggerFactory.getLogger(DigitalShellTokenTransfer.class).info(String.valueOf(map.size()));

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

