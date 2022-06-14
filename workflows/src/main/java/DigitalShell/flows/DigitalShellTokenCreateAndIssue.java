package DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
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
        // amount property of a Currency can change hence we are considering Currency as a evolvable asset

        public CreateDigitalShellTokenFlow(String amount, String receiver, String address, int notary) {
        this.amount = new BigDecimal(amount);
        this.address = address;
        this.receiverString = receiver;
        this.notaryInt = notary;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to a notaryDigitalShellQueryableState we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
//            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            IdentityService identityService = getServiceHub().getIdentityService();
            Party owner=identityService.partiesFromName(receiverString,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(""+receiverString+"party not found"));
            DigitalShellQueryableState digitalShellTokenState = new DigitalShellQueryableState( getOurIdentity(),owner, amount, address);

            //wrap it with transaction state specifying the notary
//            TransactionState<DigitalShellTokenState> transactionState = new TransactionState<>(digitalShellTokenState, notary);



            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                    .getNotaryIdentities().get(notaryInt))
                    .addOutputState(digitalShellTokenState)
                    .addCommand(new QueryableTokenContract.Commands.ShellIssue(), ImmutableList.of(getOurIdentity().getOwningKey()));

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
