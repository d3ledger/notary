## Bitcoin withdrawal service
Bitcoin Withdrawal service(or simply `btc-withdrawal`) is a service dedicated to creating and signing Bitcoin withdrawal transactions.

### Simplified flow
1) The service listens to transfer commands in Iroha blockchain using `chain-adapter` in order to achieve high stability while handling blocks.
2) If transfer command is valid(destination address is whitelisted and has base58 format), then the service tries to establish 'withdrawal consensus' among all the Notaries. They must decide what UTXO may be spent according to Bitcoin blockchain state.
3) Once 'withdrawal consensus' is established, the service starts creating Bitcoin withdrawal transaction.
4) The next step is - signing. All the Notaries must sign a newly created transaction.
5) If all the Notaries signed Bitcoin withdrawal transaction properly, it will be sent to the Bitcoin network.

Assets will be rolled back to the initiator of transfer in case of error or failure.    
### Configuration overview (withdrawal.properties)
* `btc-withdrawal.withdrawalCredential` - D3 clients send assets to this account in order to execute withdrawal. The service reacts to the account deposits and starts the withdrawal process. The account is also used to perform rollbacks. This account must be multisignature.
* `btc-withdrawal.signatureCollectorCredential` - this account is used to collect Bitcoin withdrawal transaction signatures. The account creates other accounts named after transaction hashes in `btcSignCollect` domain and saves signatures in it.
* `btc-withdrawal.btcConsensusCredential` - this account is used to create 'withdrawal consensus'. It creates other accounts named after withdrawal operation `hashCode()` in `btcConsensus` domain and saves consensus information in it.
* `btc-withdrawal.registrationCredential` - the account that was used to register D3 clients in the Bitcoin network. This account is needed to get clients whitelists.
* `btc-withdrawal.changeAddressesStorageAccount` - Iroha account that we use to store Bitcoin change address 
* `btc-withdrawal.irohaBlockQueue` - RabbitMQ queue name that `chain-adapter` uses to deliver Iroha blocks
* `btc-withdrawal.btcTransfersWalletPath` - a path to wallet file where UTXOs from `btc-deposit` service are stored. 
* `btc-withdrawal.btcKeysWalletPath` - a path to wallet file full of Bitcoin MultiSig addresses private keys. The wallet is used to sign Bitcoin withdrawal transactions.
* `btc-withdrawal.healthCheckPort` - port of health check endpoint. A health check is available on `http://host:healthCheckPort/health`. This service checks if `btc-withdrawal` is connected to one Bitcoin peer at least. 
* `btc-withdrawal.mstRegistrationAccount` - an account that creates all the Bitcoin MultiSig addresses in D3. Used to get Bitcoin change address.
* `btc-withdrawal.bitcoin.blockStoragePath` - a path to Bitcoin blockchain storage.  Only headers are stored. Typically this folder consumes very little amount of disk space (approximately 50-60mb on MainNet).
* `btc-withdrawal.bitcoin.confidenceLevel` - the minimum depth of deposit transaction in Bitcoin blockchain to be considered as available to spend.
* `btc-withdrawal.bitcoin.hosts` - a list of Bitcoin full node hosts. Hosts are separated by a comma(`,`) symbol. These hosts are used for Bitcoin withdrawal transactions broadcasting. 

### How to deploy

Typically, the service runs as a part of the `btc-dw-bridge`. But this guide may be helpful anyway.
1) Create `transfers.d3.wallet` and set a path to the wallet in the configuration file. Nodes must have the same transfer wallet. The path to the wallet must be the same as in `btc-deposit` configuration file.
2) Create Bitcoin blockchain folder and set a path to the storage in the configuration file.
3) Set a list of Bitcoin full node hosts in the configuration file. Hosts may be taken from https://bitnodes.earn.com if no D3 controlled node was deployed yet.
4) Run service with `gradlew runBtcWithdrawal` command. Wait until the service stops downloading  Bitcoin blockchain headers. If you work with MainNet it may take a long time(1-2 days). Once blockchain data is downloaded, it can be used by other nodes in order to skip this time-consuming step. Just copy the contents of `btc-withdrawal.bitcoin.blockStoragePath`.
