package net.corda.examples.tokenizedCurrency.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
public class CurrencyTokenStateContract extends EvolvableTokenContract implements Contract {
    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands{}
        class Transfer extends TypeOnlyCommandData implements Commands{}
        class Settle extends TypeOnlyCommandData implements Commands{}
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        FungibleCurrencyTokenState outputState = (FungibleCurrencyTokenState) tx.getOutput(0);
        if(!(tx.getCommand(0).getSigners().contains(outputState.getMaintainer().getOwningKey())))
            throw new IllegalArgumentException("Maintainer Signature Required");
        if(!outputState.getMaintainer().getName().equals(CordaX500Name.parse("O=Bank,L=Guangzhou,C=CN")))
            throw new IllegalArgumentException("your indentify is"+outputState.getMaintainer().getName()+",but Only bank can issue money");
        if(outputState.getAddress().equals("")) {
            throw new IllegalArgumentException("You have to choose a target address.");
        }
    }


    @Override
    public void additionalCreateChecks(LedgerTransaction tx) {
        // add additional create checks here
//        FungibleCurrencyTokenState outputState = (FungibleCurrencyTokenState) tx.getOutput(0);
//        if(outputState.getMaintainer().getName()!= CordaX500Name.parse("O=Bank,L=London,C=GB"))
//            throw new IllegalArgumentException("Only bank can issue money");
    }


    @Override
    public void additionalUpdateChecks(LedgerTransaction tx) {
        // add additional update checks here
    }
}
