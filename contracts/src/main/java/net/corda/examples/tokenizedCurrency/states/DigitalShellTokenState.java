package net.corda.examples.tokenizedCurrency.states;

import com.google.common.collect.ImmutableList;
import cordaCode.core.contracts.BelongsToContract;
import cordaCode.core.contracts.ContractState;
import cordaCode.core.identity.AbstractParty;
import cordaCode.core.identity.Party;
import net.corda.examples.tokenizedCurrency.contracts.TokenContract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
@BelongsToContract(TokenContract.class)
public class DigitalShellTokenState implements ContractState {
    private Party issuer;
    private Party owner;
    private int amount;
    private String address;

    public DigitalShellTokenState(Party issuer, Party owner, int amount, String address) {
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

    public int getAmount() {
        return amount;
    }

    public String getAddress() {
        return address;
    }
}
