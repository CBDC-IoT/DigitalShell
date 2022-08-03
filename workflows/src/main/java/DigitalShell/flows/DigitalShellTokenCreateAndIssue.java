package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.tokenizedCurrency.contracts.QueryableTokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellQueryableState;

import java.math.BigDecimal;

public class DigitalShellTokenCreateAndIssue {
    @InitiatingFlow
    @StartableByRPC
    public static class CreateDigitalShellTokenFlow extends FlowLogic<SignedTransaction> {
        private BigDecimal amount;
        private String address;
        private String receiverString;
        private int notaryInt;

        private final ProgressTracker.Step INITITATING_TRANSACTION = new ProgressTracker.Step("Initiating Issuance Transaction"){
            @Override
            public ProgressTracker childProgressTracker() {
                return AbstractStateReplacementFlow.Instigator.Companion.tracker();
            }
        };

        public CreateDigitalShellTokenFlow(String amount, String receiver, String address, int notary) {
        this.amount = new BigDecimal(amount);
        this.address = address;
        this.receiverString = receiver;
        this.notaryInt = notary;
        }

        private final ProgressTracker progressTracker = new ProgressTracker(
                INITITATING_TRANSACTION
        );

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            /** Obtain a reference to the notary of a DigitalShellQueryableState.
             *  METHOD 1: Choose the first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *  * - For production you always want to use Method 2 as it guarantees the expected notary.
             */
//            METHOD 1
//            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            IdentityService identityService = getServiceHub().getIdentityService();
            Party owner=identityService.partiesFromName(receiverString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+receiverString+"party not found"));
            DigitalShellQueryableState digitalShellTokenState = new DigitalShellQueryableState( getOurIdentity(),owner, amount, address);

            progressTracker.setCurrentStep(INITITATING_TRANSACTION);
            //Method 2
            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                    .getNotaryIdentities().get(notaryInt))
                    .addOutputState(digitalShellTokenState)
                    .addCommand(new QueryableTokenContract.Commands.ShellIssue(), ImmutableList.of(getOurIdentity().getOwningKey()));

            txBuilder.verify(getServiceHub());

            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession receiverSession = initiateFlow(owner);

            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(receiverSession)));
        }
    }


    @InitiatedBy(CreateDigitalShellTokenFlow.class)
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
