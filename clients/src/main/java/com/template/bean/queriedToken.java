package com.template.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class queriedToken {
    String issuer;
    String address;

    public String getIssuer() {
        return issuer;
    }

    public String getAddress() {
        return address;
    }
}
