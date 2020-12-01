# fungible Currency token CorDapp

This CorDapp serves as an application to create, issue, redeem, and move [Fungible](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) tokens in Corda utilizing the Token SDK. We try to build a scenario in canteen payment with IoT and digital currency to explore the future possibility in CBDC and IoT devices. 

# Tip: this cordapp is for development use, It's not safe for porduction now
we'll consider it in the futrue.


## Update Log 

#### 10.27
We do some adjustment on the basis of original corda templete to change topic to "currency".

We create four nodes here: Bank, CustomerA, CustomerB, Notary, which functions properly.

We modify the "FungibleCurrencyTokenState": 

a. change implementation from EvolvableTokenType to TokenType; 

b. delete "maintainer" and add "Tokenidentifier".

We merge the "CreateCurrencyTokenFlow" and "IssueCurrencyTokenFlow" into one flow with some bugs to be updated.

#### 10.28
a. From TokenType back to EvolvableTokenType, Now TokenType version is separated to project 
Canteen-IoT-Token-FixedType.

b. Delete useless property in TokenState

c. Constrain only bank can create Token.

d. update getBalance.

#### 10.30
a. add usage docs

#### 11.5
a. complete Move Token and get balance API

b. other common API like whoIam,otherPeers,flow,etc.

### 11.6
a. getTotalVolumesAndSale API

### 11.7 
a. design goods state, contract and transaction flow. It hasn't been completed. There are still a few bugs


### 11.8~11.10
a. android APP development.

b. Modify other details

======== Usage is below =========




## Usage （in Windows）

### clone project using git
```
git clone git@github.com:CBDC-IoT/Canteen-IoT-Token.git
cd Canteen-IoT-Token
```

### deploy nodes

Open a terminal，then go to the project root directory and run following code. 
It will take about ten minutes at first time building.
```
gradlew.bat deployNodes
```
Then type: (to run the nodes), then it will open four new terminal.
```
/build/nodes/runnodes.bat
```

### Interacting with the nodes

#### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.

    Tue July 09 11:58:13 GMT 2019>>>

You can use this shell to interact with your node.


Create Currency on the ledger using Bank's terminal. If you try to use other terminal, 
A Exception will be thrown with the message like "Only bank can issue money" 


symbol参数表示Token的含义，
例如symbol可以设置为：RMB,House,Food.  只需要运行一次，之后即可多次issue。
(这段命令代表创造了一种新类型的Token，之后便可以issue这种Token.)

    flow start CreateCurrencyTokenFlow symbol: CanteenToken

This will create a linear state of type CurrencyTokenState in Bank's vault

Now, Bank will issue some tokens to CustomerA. run below command via Bank's terminal.

    flow start IssueCurrencyTokenFlow symbol: CanteenToken, quantity: 50, holder: CustomerA

Now at CostomerA's terminal, we can query the tokens by running:

    flow start GetTokenBalance symbol: CanteenToken

Since CostomerA now has 50 tokens, Move tokens to CostomerB from CostomerA's terminal

    flow start MoveCurrencyTokenFlow symbol: CanteenToken, holder: Canteen, quantity: 23

You can now view the number of Tokens held by both the CustomerA and B 
by executing the following Query flow in their respective terminals.

    flow start GetTokenBalance symbol: CanteenToken

Now try to redeem Token 10 Token from CostomerA's terminal
    
    flow start RedeemCurrencyFungibleTokenFlow symbol: CanteenToken, issuer: Bank, quantity: 10
    
at CostomerA's terminal, we can query the tokens by running:

    flow start GetTokenBalance symbol: CanteenToken
    
    
## Web Usage (in Windows)

First run Webserver in two different terminal of root directory. 
By default, it connects to the node with RPC address `localhost:10009` with 
the username `user1` and the password `test`, and serves the webserver on port `localhost:10050`.

Default args '--server.port=10050', '--config.rpc.host=localhost', '--config.rpc.port=10009', '--config.rpc.username=user1', '--config.rpc.password=test'


    gradlew.bat runTemplateServer
    gradlew.bat runTemplateServer1

###API for CustomerA


getBalacne API is served on:

    http://localhost:10051/getBalance?symbol=CantenToken
    
MoveToken API is served on:

    http://localhost:10051/moveToken?quantity=2&holder=Canteen&symbol=CanteenToken

###API for Canteen 

Get sales volume and gross profit

    http://localhost:10050/getTotalVolumesAndSales


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



