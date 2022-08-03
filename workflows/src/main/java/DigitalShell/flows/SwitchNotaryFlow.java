package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import jdk.nashorn.internal.ir.annotations.Ignore;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.tokenizedCurrency.states.AddressState;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;
import util.IdentityManager;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.getField;
import static util.IdentityManager.getParty;

@InitiatingFlow
@StartableByRPC
public class SwitchNotaryFlow extends FlowLogic<String> {
    private String issuerString;
    private String address;
    private int notaryInt;

    public SwitchNotaryFlow(String issuerString, String address, int notaryInt) {
        this.issuerString = issuerString;
        this.address = address;
        this.notaryInt = notaryInt;
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
        Party newNotary=getServiceHub().getNetworkMapCache().getNotaryIdentities().get(notaryInt);
        Party issuer = getParty(identityService, issuerString);

/** Since we need to find a token for notary change, we better firstly merge all tokens
 * together into one token that belongs to  one notary.
 * But for experiment use, we only change one token to one notary.
 **/
//        subFlow(new DigitalShellMerger.MergeDigitalShellFlow(issuer, address));

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
        Party notary = digitalShellQueryableStateStateAndRef.getState().getNotary();
        if (notary == newNotary){
            subFlow(new NotaryChangeFlow<DigitalShellQueryableState>(digitalShellQueryableStateStateAndRef, newNotary, AbstractStateReplacementFlow.Instigator.Companion.tracker()));
        }

        //for experiments
//        subFlow(new DigitalShellTokenTransfer.Initiator(issuerString, "1", "Bank", address, address, "Coffee"));
        return "Notary Switched Successfully";
    }

    /*get party from name*/

}
