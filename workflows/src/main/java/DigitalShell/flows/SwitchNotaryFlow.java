package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import cordaCode.core.contracts.StateAndRef;
import cordaCode.core.flows.*;
import cordaCode.core.identity.Party;
import cordaCode.core.utilities.ProgressTracker;
import net.corda.examples.tokenizedCurrency.states.DigitalShellTokenState;

@InitiatingFlow
@StartableByRPC
public class SwitchNotaryFlow extends FlowLogic<String> {

    private StateAndRef stateAndRef;
    private Party newNotary;

    public SwitchNotaryFlow(StateAndRef stateAndRef, Party newNotary) {
        this.stateAndRef = stateAndRef;
        this.newNotary = newNotary;
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
        NotaryChangeFlow<DigitalShellTokenState> digitalShellTokenStateNotaryChangeFlow = new NotaryChangeFlow<DigitalShellTokenState>(stateAndRef, newNotary, AbstractStateReplacementFlow.Instigator.Companion.tracker());

        return "Notary Switched Successfully";
    }
}
