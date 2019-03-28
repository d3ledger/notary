
## Bitcoin deposit service
Bitcoin Deposit service(or simply `btc-deposit`) is here to listen to Bitcoin blockchain transactions and increase clients balances in Iroha blockchain. 

### Simplified flow
1) The service creates Bitcoin blockchain listeners that listen to transactions where coins were sent to our clients. The listener will wait until the transaction of interest hits at least 6 blocks. Once it happens, an Iroha transaction is created. 
2) Then it starts Bitcoin blockchain downloading process (only headers are stored on the disk)
3) After that, the service increases client's balance in Iroha blockchain if needed. This 'increase' works in a multisignature fashion. So every node must create the same Iroha transaction. Bitcoin block time is used as a source of time for the Iroha 'increase balance' transaction. Doing that we can guarantee that every node will use the same time.

### Configuration overview (deposit.properties)
* `btc-deposit.registrationAccount` - this account stores registered Bitcoin addresses associated with D3 clients. This information is used to check if a Bitcoin transaction is related to our clients.

* `btc-deposit.healthCheckPort` - port of health check endpoint. A health check is available on `http://host:healthCheckPort/health`. This service checks if `btc-deposit` is connected to one Bitcoin peer at least.

* `btc-deposit.btcTransferWalletPath` - a path of wallet file where deposit transactions are stored. We need this wallet to use deposit transactions as UTXO(Unspent Transaction Output) in the withdrawal service.

* `btc-deposit.bitcoin.blockStoragePath` - a path to Bitcoin blockchain storage.  Only headers are stored. Typically this folder consumes very little amount of disk space (approximately 50-60mb on MainNet).

* `btc-deposit.bitcoin.confidenceLevel` - the minimum depth of deposit transaction in Bitcoin blockchain to be considered as available to spend.

* `btc-deposit.bitcoin.hosts` - a list of Bitcoin full node hosts. Hosts are separated by a comma(`,`) symbol. These hosts are used as a source of Bitcoin blockchain. 

* `btc-deposit.notaryCredential` - credentials of the Notary account. This account is used to create 'increase balance'  transactions in Iroha. Must be a  multisignature one.

### How to deploy

Typically, the service runs as a part of the `btc-dw-bridge`. But this guide may be helpful anyway.
1) Create `transfers.d3.wallet` and set a path to the wallet in the configuration file. Nodes must have the same transfer wallet. The path to the wallet must be the same as in the `btc-withdrawal` configuration file.
2) Create a Bitcoin blockchain folder and set a path to the storage in the configuration file.
3) Set a list of Bitcoin full node hosts in the configuration file. Hosts may be taken from https://bitnodes.earn.com if no D3 controlled node was deployed yet.
4)  Run the service with the `gradlew runBtcDeposit` command. Wait until the service stops downloading  Bitcoin blockchain headers. If you work with MainNet it may take a long time (1-2 days). Once blockchain data is downloaded, it can be used by other nodes in order to skip this time-consuming step. Just copy the contents of `btc-deposit.bitcoin.blockStoragePath`.
