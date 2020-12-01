package net.corda.examples.tokenizedCurrency.flows.entity;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class MyToken {
    private long amount;
    private String Issuer;

    public MyToken(long amount, String issuer) {
        this.amount = amount;
        Issuer = issuer;
    }

    public long getAmount() {
        return amount;
    }

    public String getIssuer() {
        return Issuer;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void setIssuer(String issuer) {
        Issuer = issuer;
    }
}
