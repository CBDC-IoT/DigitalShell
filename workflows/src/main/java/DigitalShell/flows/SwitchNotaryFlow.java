package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.tokenizedCurrency.states.AddressState;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.getField;

@InitiatingFlow
@StartableByRPC
public class SwitchNotaryFlow extends FlowLogic<String> {
    private String issuerString;
    private String address;
    private String newNotaryString;

    public SwitchNotaryFlow(String issuer, String address, String newNotary) {
        this.issuerString = issuer;
        this.address = address;
        this.newNotaryString = newNotary;
    }

    private final ProgressTracker.Step QUERYING_VAULT = new ProgressTracker.Step("Fetching StateStateAndRef from node's vault.");
    private final ProgressTracker.Step INITITATING_TRANSACTION = new ProgressTracker.Step("Initiating Notary Change Transaction"){
        @Override
        public ProgressTracker childProgressTracker() {
            return AbstractStateReplacementFlow.Instigator.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            QUERYING_VAULT,
            INITITATING_TRANSACTION
    );

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        progressTracker.setCurrentStep(QUERYING_VAULT);
        progressTracker.setCurrentStep(INITITATING_TRANSACTION);
        IdentityService identityService = getServiceHub().getIdentityService();
        Party newNotary=identityService.partiesFromName(newNotaryString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+newNotaryString+"party not found"));


        subFlow(new DigitalShellMerger.MergeDigitalShellFlow(issuerString, address));

        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        FieldInfo attributeAddress = null;
        try {
            attributeAddress = getField("address", AddressState.class);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        CriteriaExpression addressIndex = Builder.equal(attributeAddress, address.trim());

        QueryCriteria customCriteria = new QueryCriteria.VaultCustomQueryCriteria(addressIndex);

        QueryCriteria criteria = generalCriteria.and(customCriteria);

        PageSpecification pageSpec = new PageSpecification(1, 50);

        Vault.Page<DigitalShellQueryableState> digitalShellQueryableStatePage = getServiceHub().getVaultService().queryBy(DigitalShellQueryableState.class, criteria, pageSpec);

        StateAndRef<DigitalShellQueryableState> digitalShellQueryableStateStateAndRef = digitalShellQueryableStatePage.getStates().get(0);

        subFlow(new NotaryChangeFlow<DigitalShellQueryableState>(digitalShellQueryableStateStateAndRef, newNotary, AbstractStateReplacementFlow.Instigator.Companion.tracker()));

        return "Notary Switched Successfully";
    }


}
