package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DigitalShellQuery {


    @InitiatingFlow
    @StartableByRPC
    public static class DigitalShellQueryFlow extends FlowLogic<BigDecimal> {
        private String issuerString;
        private String address;
        // amount property of a Currency can change hence we are considering Currency as a evolvable asset

        public DigitalShellQueryFlow(String issuer, String address) {
            this.issuerString = issuer;
            this.address = address;
        }

        @Override
        @Suspendable
        public BigDecimal call() throws FlowException {
            IdentityService identityService = getServiceHub().getIdentityService();

            Party issuer = getParty(identityService, issuerString);



            AtomicReference<BigDecimal> totalTokenAvailable = new AtomicReference<>();
            HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> partyArrayListHashMap = getPartyArrayListHashMap(totalTokenAvailable, issuer, 100);

            Collection<ArrayList<StateAndRef<DigitalShellQueryableState>>> values = partyArrayListHashMap.values();

            BigDecimal totalBalance = new BigDecimal(0);
            for(ArrayList<StateAndRef<DigitalShellQueryableState>> i: values){
                for(int k = 0; k < i.size(); k++ ){
                    totalBalance.add(i.get(k).getState().getData().getAmount());
                }
            }

            return totalBalance;

        }


        /*find all needed State*/
        @NotNull
        private HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> getPartyArrayListHashMap(AtomicReference<BigDecimal> totalTokenAvailable, Party issuer, int pagesize) throws FlowException {

            int pageSize = pagesize;
            int pageNumber = 1;
            long totalStatesAvailable;
            HashMap<Party, ArrayList<StateAndRef<DigitalShellQueryableState>>> map = new HashMap<>();

            LoggerFactory.getLogger(DigitalShellMerger.class).info("Flag0");
            do {
//                    System.out.println("Querying" + pageNumber);
                PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
                Vault.Page<DigitalShellQueryableState> results =
                        getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, pageSpec);
                totalStatesAvailable = results.getTotalStatesAvailable();
                List<StateAndRef<DigitalShellQueryableState>> states = results.getStates();
                List<StateAndRef<DigitalShellQueryableState>> tokenStateAndRefs = states.stream().filter(tokenStateStateAndRef -> {
                    //Filter according to issuer and address
                    if (tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer) && tokenStateStateAndRef.getState().getData().getAddress().equals(address)) {

                        if (map.get(tokenStateStateAndRef.getState().getNotary()) != null) {
                            ArrayList<StateAndRef<DigitalShellQueryableState>> stateAndRefs = map.get(tokenStateStateAndRef.getState().getNotary());
                            stateAndRefs.add(tokenStateStateAndRef);
                            map.put(tokenStateStateAndRef.getState().getNotary(), stateAndRefs);
                        } else {
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
            } while ((pageSize * (pageNumber - 1) <= totalStatesAvailable));

            return map;
        }

        /*get party from name*/
        private Party getParty(IdentityService identityService, String name) {
            return identityService.partiesFromName(name,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+ issuerString +"party not found"));
        }
    }
}