# DigitalShell Project

DigitalShell is a CBDC project for providing basic currency functionalities, such as create, issue, transfer and redeem. The project is built on the Corda framework and proposes CBDC-targeted improvements. The project provides Sharding services in terms of notary node.


### Installation
---
We encourage installing this project by directly cloning into the local. The following command downloads the project:
```
git clone git@github.com:CBDC-IoT/DigitalShell.git
```

### Run your First Network
Now the project is downloaded, it's time to begin working with Digitalshell.

You can run deployNodes in the build.gradle to generate the network and nodes. Then you can go to the project file and build/node and run runnodes.bat.

#### Then you can run the following workflows to create DigitalShell and transfer DigitalShell

flow start CreateDigitalShellTokenFlow amount: 10000, receiver: CustomerA, address: ABC

flow start DigitalShellTokenTransfer issuer: Bank, amount: 10, receiver: Canteen, originalAddress: ABC, address: BB

#### You can also run client service which will connect to the corda nodes and provide open APIs.
You can go to DigitalShell/clients/build.gradle to run diverse servers. You can config the connection point based on your preference.

Then you can send your request by the internet, for example,
```
http://localhost:10051/moveToken?issuer=Bank&amount=10&receiver=Canteen&originalAddress=ABC&address=D
```


### H2 Configuration


