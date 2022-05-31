package com.template.webserver.Service;

/*
* 1. Collect User's signature.
* 2. Verify user's signature.
* 3. send user's signature to corda node.
* */
public class MoveTokenService {
    private String url;
    private String signature;

    /*Receive url of Tx*/
    public void receiveTransaction(String url){
        this.url = url;

    }

    /*Rec*/
    public void verifySignature(String signature){
        this.signature = signature;
    }
}
