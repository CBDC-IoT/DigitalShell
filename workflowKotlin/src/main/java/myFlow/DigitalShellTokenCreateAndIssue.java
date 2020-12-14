package myFlow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import cordaCode.core.flows.*;
import cordaCode.core.flows.FlowException;
import cordaCode.core.flows.FlowSession;
import cordaCode.core.flows.StartableByRPC;
import cordaCode.core.identity.Party;
import cordaCode.core.node.services.IdentityService;
import cordaCode.core.transactions.SignedTransaction;
import cordaCode.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellTokenState;

public class DigitalShellTokenCreateAndIssue {
    @InitiatingFlow
    @StartableByRPC
    public static class CreateDigitalShellTokenFlow extends FlowLogic<SignedTransaction> {
        private int amount;
        private String address;
        private String receiverString;
        // amount property of a Currency can change hence we are considering Currency as a evolvable asset

        public CreateDigitalShellTokenFlow(int amount, String receiver, String address) {
        this.amount = amount;
        this.address = address;
        this.receiverString = receiver;
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
            Party owner=identityService.partiesFromName(receiverString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+receiverString+"party not found"));
            DigitalShellTokenState digitalShellTokenState = new DigitalShellTokenState( getOurIdentity(),owner, amount, address);

            //wrap it with transaction state specifying the notary
//            TransactionState<DigitalShellTokenState> transactionState = new TransactionState<>(digitalShellTokenState, notary);
            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                    .getNotaryIdentities().get(0))
                    .addOutputState(digitalShellTokenState)
                    .addCommand(new TokenContract.Commands.Issue(), ImmutableList.of(getOurIdentity().getOwningKey()));

            txBuilder.verify(getServiceHub());

            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession receiverSession = initiateFlow(owner);
            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
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
