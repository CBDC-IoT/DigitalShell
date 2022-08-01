package com.template.bean;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class movedToken {
    String issuer;
    String amount;
    String receiver;
    String originalAddress;
    String address;
    String itemString;

    public movedToken(String issuer, String amount, String receiver, String originalAddress, String address) {
        this.issuer = issuer;
        this.amount = amount;
        this.receiver = receiver;
        this.originalAddress = originalAddress;
        this.address = address;
        this.itemString = "Null";
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAmount() {
        return amount;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getOriginalAddress() {
        return originalAddress;
    }

    public String getAddress() {
        return address;
    }

    public String getItemString() {
        return itemString;
    }
}
