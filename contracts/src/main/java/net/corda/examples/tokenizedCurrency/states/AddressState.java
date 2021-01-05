package net.corda.examples.tokenizedCurrency.states;

import net.corda.core.identity.Party;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * JPA Entity for saving insurance details to the database table
 */
@Entity
@Table(name = "DIGITAL_SHELL")
public class AddressState extends PersistentState implements Serializable {
    @Column private final Party issuer;
    @Column private final Party owner;
    @Column private final BigDecimal amount;
    @Column(name = "address") private final String address;



    /**
     * Default constructor required by Hibernate
     */
    public AddressState() {
        this.issuer = null;
        this.owner = null;
        this.amount = null;
        this.address = null;
    }

    public AddressState(Party issuer, Party owner, BigDecimal amount, String address) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

}
