# DigitalShell Project

This CorDapp serves as an application to create, issue, redeem, and move [Fungible](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) tokens in Corda utilizing the Token SDK. We try to build a scenario in canteen payment with IoT and digital currency to explore the future possibility in CBDC and IoT devices. 

We rebuild previous IoT-Token project to provide more fruitful functions.

### Other API

    http://localhost:10051/me
    http://localhost:10051/peers
    http://localhost:10051/notaries
    http://localhost:10051/flows
    http://localhost:10051/states

# UI Application 
Before you start these project, you should run API above.  


## We developed the web front end of user's wallet APP (Vue project). 

    https://github.com/CBDC-IoT/Canteen-CBDC-wallet

## Canteen background management system (Vue project).

    https://github.com/CBDC-IoT/Canteen-CBDC-Backend

## We developed the android front end of user's wallet APP (android studio project). It's still being updated. 

    https://github.com/CBDC-IoT/Android-wallet


## Future Work
Design Goods State and Contract,because Currency 
State hasn't price attribute.

Design flow that can move money and goods 
in one transaction in order to atomicity. And Canteen 
can automatically check whether customer pays enough money in flow logic.



