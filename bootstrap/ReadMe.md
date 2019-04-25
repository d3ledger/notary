# Bootstrap service

## API documentation
* SwaggerUI - 'http://localhost:8080/swagger-ui.html' In SwaggerUi it is also possible to make test calls through browser
* Json representation - 'http://localhost:8080/v2/api-docs' In Firefox json is more readable, so try to use it.


## Description
Service provides possibilities to automate DevOps activities.

It is usable for test environments and production. Only difference is that for test environments we 
may generate blockchain credentials using this service but for production owners of private keys 
should provide public keys for us.

Functionality:
* Creation of Iroha credentials (private and public keys)
* Creation of Iroha Genesis Block 

## Startup Configuration
You should provide environment parameter `btc.network` with one of three values: `TestNet3, MainNet, RegTest`
Else `btc.network` parameter will be taken from application.properties file

## How to develop
To Add generation of genesis block for new project and/or environment Create a class which should 
implement `GenesisInterface`

* `fun getProject(): String` - should provide name of project
* `fun getEnvironment(): String` - should provide name of environment eg 'local_my', 'test', 'prod'
* `fun createGenesisBlock(accounts:List<IrohaAccountDto>, peers:List<Peer>, blockVersion:String = "1"): String`
This function should return string json representatin of genesis block. It takes Iroha peers and accounts as arguments from API request.
* `fun getAccountsNeeded(): List<AccountPrototype>` - This function should return Accounts which have to present in genesis block to generate credentials for.

Example realisation for D3: `genesis.d3.D3TestGenesisFactory`

## Ethereum endpoint

### Deploy smart contracts
There are function to deploy all smart contracts by one API call: 'eth/deploy/D3/smartContracts' and functions to deploy initial ethereum contracts separately.
Only owner can call contracts later.
The Sequence to call smart contract deployment functions is following:
1 - 'eth/deploy/D3/relayRegistry' - then take resulting contract address as an argument for the next function
2 - 'eth/deploy/D3/masterContract' - then take resulting smart contract address as an argument to the next function
3 - 'eth/deploy/D3/relayImplementation'
