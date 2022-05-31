package net.corda.examples.tokenizedCurrency.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.examples.tokenizedCurrency.contracts.QueryableTokenContract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
@BelongsToContract(QueryableTokenContract.class)
public class DigitalShellQueryableState implements QueryableState {

    private final Party issuer;
    private final Party owner;
    private final BigDecimal amount;
    private final String address;

    public DigitalShellQueryableState(Party issuer, Party owner, BigDecimal amount, String address) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.address = address;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(owner);
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
    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {

        if (schema instanceof DigitalShellShemaV1) {
            return new AddressState(issuer, owner, amount, address);
        } else {
            throw new IllegalArgumentException("Unrecognised schema");
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new DigitalShellShemaV1());
    }
}
