package net.corda.examples.tokenizedCurrency.flows.entity;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow;
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

public class AddressUpdateFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<String> {

        private final String address;

        public Initiator(String address) {
            this.address = address;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            // Retrieved the unconsumed DigitalShellState from the vault
            StateAndRef<FungibleCurrencyTokenState> DigitalShellStateRef = CustomQuery.queryDigitalShell(address, getServiceHub());
            FungibleCurrencyTokenState DigitalShell = DigitalShellStateRef.getState().getData();

            // Form the output state here with a address to be announced
            FungibleCurrencyTokenState outputState = new FungibleCurrencyTokenState(
                    DigitalShell.getMaintainer(),
                    DigitalShell.getUniqueIdentifier(),
                    DigitalShell.getFractionDigits(),
                    DigitalShell.getSymbol(),
                    address);

            // Update the DigitalShell state
            SignedTransaction stx = subFlow(new UpdateEvolvableTokenFlow(DigitalShellStateRef, outputState, ImmutableList.of()));
            return "\nDigitalShell " + DigitalShell.getAddress() + " has to " + this.address + ". " + stx.getId();
        }
    }

    @InitiatedBy(AddressUpdateFlow.Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        private FlowSession counterSession;

        public Responder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // To implement the responder flow, simply call the subflow of UpdateEvolvableTokenFlowHandler
            subFlow(new UpdateEvolvableTokenFlowHandler(counterSession));
            return null;
        }
    }

//We could also include observer in this step.
}
