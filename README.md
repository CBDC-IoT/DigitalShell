# DigitalShell Project

We rebuild previous IoT-Token project to provide more fruitful functions.

### Test API:

flow start CreateDigitalShellTokenFlow amount: 10000, receiver: CustomerA, address: ABC

flow start DigitalShellTokenTransfer issuer: Bank, amount: 10, receiver: Canteen, originalAddress: ABC, address: BB

http://161.189.108.227:10051/moveToken?issuer=Bank&amount=10&receiver=Canteen&originalAddress=ABC&address=D

