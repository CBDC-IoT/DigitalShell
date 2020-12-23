package net.corda.examples.tokenizedCurrency.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
@BelongsToContract(TokenContract.class)
public class DigitalShellTokenState implements ContractState {
    private Party issuer;
    private Party owner;
    private BigDecimal amount;
    private String address;

    public DigitalShellTokenState(Party issuer, Party owner, BigDecimal amount, String address) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.address = address;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(issuer, owner);
    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getOwner() {
        return owner;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAddress() {
        return address;
    }
}
