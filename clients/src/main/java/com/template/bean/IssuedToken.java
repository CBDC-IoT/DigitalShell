package com.template.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssuedToken {
    String amount;
    String receiver;
    String address;
    int notary;

    public IssuedToken(String receiver, String address, String amount) {
        this.amount = amount;
        this.receiver = receiver;
        this.address = address;
        this.notary = 0;
    }

    public String getAmount() {
        return amount;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getAddress() {
        return address;
    }

    public int getNotary() {
        return notary;
    }

}
