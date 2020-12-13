package net.corda.DigitalShell.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import net.corda.examples.tokenizedCurrency.states.DigitalShellTokenState;

public class DigitalShellTokenRedeem {
    @InitiatingFlow
    @StartableByRPC
    public static class RedeemDigitalShellTokenFlow extends FlowLogic<SignedTransaction> {
        private int amount;
        private String address;
        // amount property of a Currency can change hence we are considering Currency as a evolvable asset

        public RedeemDigitalShellTokenFlow(int amount , String address) {
        this.amount = amount;
        this.address = address;
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
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary1,L=Guangzhou,C=CN")); // METHOD 2
            final Party bank = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Bank,L=Guangzhou,C=CN")); // METHOD 2

            DigitalShellTokenState digitalShellTokenState = new DigitalShellTokenState(bank,bank, amount, address);

            //wrap it with transaction state specifying the notary
//            TransactionState<DigitalShellTokenState> transactionState = new TransactionState<>(digitalShellTokenState, notary);
            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                    .getNotaryIdentities().get(0))
                    .addOutputState(digitalShellTokenState)
                    .addCommand(new TokenContract.Commands.Redeem(), ImmutableList.of(getOurIdentity().getOwningKey()));

            txBuilder.verify(getServiceHub());

            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession receiverSession = initiateFlow(bank);
            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(receiverSession)));
        }
    }


    @InitiatedBy(RedeemDigitalShellTokenFlow.class)
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
