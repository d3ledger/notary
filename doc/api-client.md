# D3ledger API

D3ledger is built on top of Iroha chain and every action is done via Iroha
account and signed by Iroha key. Every D3 client has an account in Iroha
and should use API provided by Iroha. Iroha provides a set of libraries on
different languages to communicate with Iroha (see 
[Iroha docs](https://iroha.readthedocs.io/en/latest/api/index.html)).

## Registration

A registration of the client is responsibility of D3 service.
That's why D3 services provide registration of new client via HTTP POST request. 

### Register

    POST /users

* **Parameters:**

    * ***name*** - client name
    * ***domain*** - client domain
    * ***pubkey*** - Iroha public key of client. Client should generate keypair herself

* **Successfull Response:**

  * **Code:** `200` <br />
    **Content:** `:clientId`
    
    clientId - client id in Iroha
    
* **Example:**
  
      curl -v -F "name=myname" \
      -F "pubkey=e48e003991142b90a3569d6804738c69296f339216166a3e6d20d6380afb25b1" \
      -F "domain=d3" \
      http://localhost:8085/users
      
* **Example response:**
    
    **Code:** `200` <br />
    
    **Content:** `myname@d3`
              
* **Example error response:**
    
    **Code:** `500` <br />
    
    **Content:** `Response has been failed. java.lang.Exception: Tx 9fd23b3372dd772ca17b400d3abecf2160d1f6d8e0afc7d7b2f5c4de956051fc failed. CreateAccount`
      

### Get free addresses number

Get number of free addresses or how many clients can be registered right now.

    GET /free-addresses/number

* **Successfull Response:**

  * **Code:** `200` <br />
    **Content:** `:number`
    
    number - the number of free addresses
    
* **Example:**
  
      curl http://localhost:8083/free-addresses/number

## Iroha API

Iroha API is provided via gRPC. Iroha provides libraries for the following languages:
- Java
- Objective-C
- Swift
- Python
- NodeJS

Libraries help build and send protobuf objects. Here is an example in Kotlin that
uses Iroha Java library:

    // build transaction object
    var unsignedTx = ModelTransactionBuilder()
        .creatorAccountId(transaction.creator)
        .createdTime(transaction.createdTime)
        .transferAsset(
            cmd.srcAccountId,
            cmd.destAccountId,
            cmd.assetId,
            cmd.description,
            cmd.amount
        )
        .build()
                
    // sign transaction
    var tx = unsignedTx.signAndAddSignature(keys)
        .finish()
        
    // serialize to protobuf 
    val blob = tx.blob()
    val bs = blob.toByteArray()
    val protoTx = Transaction.parseFrom(bs)
    
    // get gRPC stub endpoint
    val toriiStub = CommandServiceGrpc.newBlockingStub(channel)
    
    // and send
    toriiStub.torii(protoTx)
    
You can find more examples on [Iroha documentation](https://iroha.readthedocs.io/en/latest/overview.html)
page.

### Get balance

Client balance can be queried with Iroha query 
[GetAccountAssets](https://iroha.readthedocs.io/en/latest/api/queries.html#get-account-assets).

**Request schema:**

    message GetAccountAssets {
        string account_id = 1;
    }
    
**Response schema:**

    message AccountAssetResponse {
        repeated AccountAsset acct_assets = 1;
    }
    
    message AccountAsset {
        string asset_id = 1;
        string account_id = 2;
        Amount balance = 3;
    }

### Get Ethereum relay address

Client Ethereum relay address can be queried with 
[GetAccountDetail](https://iroha.readthedocs.io/en/latest/api/queries.html#get-account-detail)
on client account. Relay address is accessed by key `ethereum_wallet`, detail
setter `eth_registration_service@notary`.

**Request schema:**

    message GetAccountDetail {
        oneof opt_account_id {
            string account_id = 1;
        }
        oneof opt_key {
            string key = 2;
        }
        oneof opt_writer {
            string writer = 3;
        }
    }

**Response schema:**

    message AccountDetailResponse {
        string detail = 1;
    }
    
**Response example:**

    {
        "eth_registration_service@notary": {
            "ethereum_wallet": "0x6826d84158e516f631bBf14586a9BE7e255b2D23"
        }
    }

### Get whitelist

Client Ethereum whitelist can be queried with 
[GetAccountDetail](https://iroha.readthedocs.io/en/latest/api/queries.html#get-account-detail)
on client account. Whitelist is accessed by key `eth_whitelist`, detail
setter `eth_registration_service@notary`.

**Request schema:**

    message GetAccountDetail {
        oneof opt_account_id {
            string account_id = 1;
        }
        oneof opt_key {
            string key = 2;
        }
        oneof opt_writer {
            string writer = 3;
        }
    }

**Response schema:**

    message AccountDetailResponse {
        string detail = 1;
    }
    
**Response example:**

    {
        "eth_registration_service@notary": {
            "eth_whitelist": "0x6826d84158e516f631bBf14586a9BE7e255b2D23"
        }
    }

### Get asset transactions

Get all transactions associated with given account and asset. See
[GetAccountAssetTransactions](https://iroha.readthedocs.io/en/latest/api/queries.html#get-account-asset-transactions).

**Request schema:**

    message GetAccountAssetTransactions {
        string account_id = 1;
        string asset_id = 2;
    }

**Response schema:**

    message TransactionsResponse {
        repeated Transaction transactions = 1;
    }

### Get transactions

Get information about transactions based on their hashes. See
[GetTransactions](https://iroha.readthedocs.io/en/latest/api/queries.html#get-transactions).

**Request schema:**

    message GetTransactions {
        repeated bytes tx_hashes = 1;
    }
   
**Response schema:**
 
    message TransactionsResponse {
        repeated Transaction transactions = 1;
    }
    
### Get pending transactions

Retrieve a list of pending (not dully signed) multisignature transactions or
batches of transactions issued by account of query creator. See
[GetPeningTransactions](https://iroha.readthedocs.io/en/latest/api/queries.html#get-pending-transactions).

**Request schema:**

    message GetPendingTransactions {
    }
   
**Response schema:**
 
    message TransactionsResponse {
        repeated Transaction transactions = 1;
    }

### Withdraw

To initiate a process of withdrawal a client should send transfer transaction
to `notary@notary` account. In `description` the client should specify withdrawal
address. See
[TransferAsset](https://iroha.readthedocs.io/en/latest/api/commands.html#transfer-asset).

**Schema:**

    message TransferAsset {
        string src_account_id = 1;
        string dest_account_id = 2;
        string asset_id = 3;
        string description = 4;
        Amount amount = 5;
    }
    
| Field            | Description               |
| ---              | ---                       |    
| **src_account**  | client account            |
| **dest_account** | `notary@notary`           |
| **asset_id**     | asset to withdraw         |
| **description**  | address where to withdraw |
| **amount**       | amount to withdraw        |

### Transfer

Transfer assets to another client. Description field contains any text message
for the client. See
[TransferAsset](https://iroha.readthedocs.io/en/latest/api/commands.html#transfer-asset).

**Schema:**

    message TransferAsset {
        string src_account_id = 1;
        string dest_account_id = 2;
        string asset_id = 3;
        string description = 4;
        Amount amount = 5;
    }
    
| Field            | Description       |
| ---              | ---               |    
| **src_account**  | sender account    |
| **dest_account** | client to send to |
| **asset_id**     | asset to send     |
| **description**  | message           |
| **amount**       | amount to send    |

### Settlement

For a settlement the client should send an atomic
[batch transaction](https://iroha.readthedocs.io/en/latest/core_concepts/glossary.html?highlight=batch#atomic-batch)
with two transfer transactions. The first transaction is a transfer for counterparty
signed by the user and the second is a transfer from counterparty without the signature.
The counterparty should sign it to accept the settlement. If the batch transaction
is not signed in 24 hours, it is removed from the list of pending transactions
(cancelled). 

**Schema:**

    message Transaction {
      message Payload {
        message BatchMeta{
          enum BatchType{
            ATOMIC = 0;
            ORDERED = 1;
          }
          BatchType type = 1;
          // array of reduced hashes of all txs from the batch
          repeated bytes reduced_hashes = 2;
        }
        message ReducedPayload{
          repeated Command commands = 1;
          string creator_account_id = 2;
          uint64 created_time = 3;
          uint32 quorum = 4;
        }
        // transcation fields
        ReducedPayload reduced_payload = 1;
        // batch meta fields if tx belong to any batch
        oneof optional_batch_meta{
          BatchMeta batch = 5;
        }
      }
    
      Payload payload = 1;
      repeated Signature signatures = 2;
    }

### Accept settlement

To accept the settlement the client should get pending settlement transaction and
sign it in
[batch transaction](https://iroha.readthedocs.io/en/latest/core_concepts/glossary.html?highlight=batch#atomic-batch)
with his private key and send it again.

### Cancel settlement

To cancel the settlement the client should get pending settlement transaction and
sign it in
[batch transaction](https://iroha.readthedocs.io/en/latest/core_concepts/glossary.html?highlight=batch#atomic-batch)
with any invalid key and send it again to remove the transaction from Iroha list
of pending transactions.

### Reject settlement

To reject the settlement the client should get pending settlement transaction and sign it in
[batch transaction](https://iroha.readthedocs.io/en/latest/core_concepts/glossary.html?highlight=batch#atomic-batch)
with any invalid key and send it again to remove the transaction from Iroha list
of pending transactions.
