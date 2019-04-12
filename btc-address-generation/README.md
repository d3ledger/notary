## Bitcoin address generation service
Bitcoin Address Generation service(or simply `btc-address-generation`) is used to create Bitcoin MultiSig addresses that may be registered by D3 clients later. The service is also responsible for creating change addresses (addresses that store changes). 

### Glossary
 Free address - Bitcoin MultiSig address that may be registered by any D3 client.
 
 Change address - Bitcoin MultiSig address that is used by withdrawal service to store changes.
 
### Simplified flow
1) The service waits for a special command that triggers the address generation process. Given the fact, that this process takes approximately 10-15 seconds, free and change addresses are generated on service start. Free addresses are also generated on D3 client registration because one registration takes exactly one MultiSig address. Addresses may be generated using `gradlew generateBtcFreeAddress` and `gradlew generateBtcChangeAddress` to create regular and change address respectively. Attention must be paid to the fact that these commands don't create addresses, they trigger address generation. Calling these commands won't make any effect if the service is not up and running.
 2) Once this command appears, the service starts key pair creation and saves it in `keys.d3.wallet` file and Iroha(only public keys go to Iroha).  
3) Then, the service waits until enough key pairs are collected in Iroha. All the Notaries must create a key pair. If enough key pairs are created, the service generates a new Bitcoin MultiSig address using public keys created by all the Notaries. This MultiSig address is also saved in `keys.d3.wallet` file and Iroha. 

### Configuration overview (address_generation.properties)

* `btc-address-generation.mstRegistrationAccount` - Iroha account that is responsible for Bitcoin MultiSig addresses creation. The account must be multisignature.
* `btc-address-generation.btcKeysWalletPath` - path to wallet file where key pairs will be stored.
* `btc-address-generation.notaryAccount` -  Iroha account that is responsible for Bitcoin MultiSig addresses storage. Addresses are stored in this account details. Probably, this is not a very good candidate for that purpose.
* `btc-address-generation.changeAddressesStorageAccount` -  Iroha account that is responsible for change addresses storage. Addresses are stored in this account details.
* `btc-address-generation.healthCheckPort` - port of health check endpoint. A health check is available on `http://host:healthCheckPort/health`. This service checks if `btc-address-generation` is able to listen to Iroha blocks.
* `btc-address-generation.threshold` - a number of MultiSig addresses that must be created in advance.
* `btc-address-generation.nodeId` - identifier of Notary node. This identifier must correlate to an identifier that is set in `btc-registration` configuration file on the same node. This value must be different on different nodes. We use this identifier as a mechanism of synchronization issues resistance.

### How to deploy
1) Create `keys.d3.wallet` and set a path to the wallet in the configuration file. This file must be different on other nodes. Having the same file on different nodes causes duplicate key pairs creation.
2) Run the service using `gradlew runBtcAddressGeneration` command.
