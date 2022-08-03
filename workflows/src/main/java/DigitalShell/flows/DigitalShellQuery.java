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
import net.corda.core.node.services.vault.*;
import net.corda.examples.tokenizedCurrency.states.AddressState;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.getField;
import static util.IdentityManager.getParty;

public class DigitalShellQuery {


    @InitiatingFlow
    @StartableByRPC
    public static class DigitalShellQueryFlow extends FlowLogic<BigDecimal> {
        private String issuerString;
        private String address;

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
            BigDecimal totalbalance = null;
            try {
                totalbalance = getTotalBalance(issuer, address);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            return totalbalance;
        }

        @NotNull
        private BigDecimal getTotalBalance(Party issuer,  String original_address) throws FlowException, NoSuchFieldException {
            AtomicReference<BigDecimal> totalTokenAvailable = new AtomicReference<BigDecimal>(new BigDecimal(0));

            AtomicBoolean getEnoughMoney= new AtomicBoolean(false);

            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

            FieldInfo attributeAddress = getField("address", AddressState.class);
            FieldInfo attributeIssuer = getField("issuer", AddressState.class);

            CriteriaExpression addressIndex = Builder.equal(attributeAddress, original_address.trim());
            CriteriaExpression issuerIndex = Builder.equal(attributeIssuer, issuer);

            QueryCriteria customCriteria = new QueryCriteria.VaultCustomQueryCriteria(addressIndex);
            QueryCriteria customCriteria2 = new QueryCriteria.VaultCustomQueryCriteria(issuerIndex);

            QueryCriteria criteria = generalCriteria.and(customCriteria);
            QueryCriteria criteria2 = criteria.and(customCriteria2);

            PageSpecification pageSpec = new PageSpecification(1, 50);

            Vault.Page<DigitalShellQueryableState> digitalShellQueryableStatePage = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, criteria2, pageSpec);

            List<StateAndRef<DigitalShellQueryableState>> states = digitalShellQueryableStatePage.getStates();
            List<StateAndRef<DigitalShellQueryableState>> tokenStateAndRefs =  states.stream().filter(tokenStateStateAndRef -> {

                System.out.println(totalTokenAvailable);
                //Calculate total tokens available
                totalTokenAvailable.set(totalTokenAvailable.get().add(tokenStateStateAndRef.getState().getData().getAmount()));
                return true;

            }).collect(Collectors.toList());
            BigDecimal bigDecimal = totalTokenAvailable.get();
            return bigDecimal;
        }
    }
}