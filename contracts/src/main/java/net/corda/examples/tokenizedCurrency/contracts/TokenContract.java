package net.corda.examples.tokenizedCurrency.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.tokenizedCurrency.states.DigitalShellTokenState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        if(tx.getCommands().size() !=1)
            throw new IllegalArgumentException("One Command Expected");

        if(tx.getCommand(0).getValue() instanceof Commands.Issue)
            verifyIssue(tx);
        else if(tx.getCommand(0).getValue() instanceof Commands.Transfer)
            verifyTransfer(tx);
        else
            throw new IllegalArgumentException("Unsupported Command");

    }

    private void verifyIssue(LedgerTransaction tx){
        if(tx.getInputs().size() != 0)
            throw new IllegalArgumentException("Zero Inputs Expected");

        if(tx.getOutputs().size() != 1)
            throw new IllegalArgumentException("One Output Expected");

        if(!(tx.getOutput(0) instanceof DigitalShellTokenState))
            throw new IllegalArgumentException("Output of type TokenState Expected");

        DigitalShellTokenState tokenState = (DigitalShellTokenState)tx.getOutput(0);
        if(tokenState.getAmount().doubleValue() < 0)
            throw new IllegalArgumentException("Positive amount expected");

        if(!(tx.getCommand(0).getSigners()
                .contains(tokenState.getIssuer().getOwningKey())))
            throw new IllegalArgumentException("Issuer must sign");

        if (tokenState.getAddress().equals("")){
            throw new IllegalArgumentException("Address expected");
        }
    }

    private void verifyTransfer(LedgerTransaction tx){
        // Inputs must be greater than zero
        if(tx.getInputs().size() < 1)
            throw new IllegalArgumentException("More than 0 inputs expected");

        // Output must be equal to either 1 or 2
        if(!(tx.getOutputs().size() == 1 || tx.getOutputs().size() == 2))
            throw new IllegalArgumentException("Output count must either be one or two");

        // Input amount must be equal to output amount
        AtomicReference<BigDecimal> inputSum = new AtomicReference<BigDecimal>(new BigDecimal(0));

        tx.getInputs().forEach(contractStateStateAndRef -> {
            TransactionState<ContractState> transactionState = contractStateStateAndRef.getState();
            ContractState data = transactionState.getData();
            DigitalShellTokenState inputState = (DigitalShellTokenState)data;
            BigDecimal amount = inputState.getAmount();
            BigDecimal tempSum = inputSum.get();
            BigDecimal add = amount.add(tempSum);
            inputSum.set(add);
        });

        AtomicReference<BigDecimal> outputSum = new AtomicReference<BigDecimal>(new BigDecimal(0));
        tx.getOutputs().forEach(contractStateTransactionState -> {
            outputSum.set(outputSum.get().add(((DigitalShellTokenState)contractStateTransactionState.getData()).getAmount()));
        });

        if(inputSum.get().compareTo(outputSum.get())!=0)
            throw new IllegalArgumentException("Incorrect Spending");

        // Owner must sign
        if(!(tx.getCommand(0).getSigners().contains(((DigitalShellTokenState)tx.getInput(0)).getOwner().getOwningKey())))
            throw new IllegalArgumentException("Owner must Sign");
    }

    public interface Commands extends CommandData {
        class Issue implements Commands { }
        class Transfer implements Commands { }
        class Redeem implements Commands { }
    }
}