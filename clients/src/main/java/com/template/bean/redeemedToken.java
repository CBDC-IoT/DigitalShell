package com.template.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class redeemedToken {
    String issuer;
    String amount;
    String address;

    public String getIssuer() {
        return issuer;
    }

    public String getAmount() {
        return amount;
    }

    public String getAddress() {
        return address;
    }
}
