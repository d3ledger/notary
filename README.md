# notary
Backend code for a D3 notary

[![CircleCI](https://circleci.com/gh/d3ledger/notary.svg?style=svg)](https://circleci.com/gh/d3ledger/notary)

## How to run notary application and services in Ethereum main net
1) clone project `master` branch
2) launch Iroha and Postgres in docker with `docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.dev.yml up`
3) Provide ethereum passwords `src/main/resources/eth/ethereum_password.properties` (ask someone from maintainers team about the format)
4) Run notary service `gradle runEthNotary -Pprofile=mainnet`
5) Run registration service `gradle runEthRegistration -Pprofile=mainnet`
6) Run withdrawal service `gradle runWithdrawal -Pprofile=mainnet`
6) Deploy relay smart contract (one relay per one client registration) `gradle runDeployRelay -Pprofile=mainnet`. Ensure relay is deployed on etherscan.io

Great! So now you can move on and connect frontend application (check back-office repo in d3ledger)

## Ethereum passwords
Passwords for Ethereum network may be set in 3 different ways:

1) Using `eth/ethereum_password.properties` file.
2) Using environment variables(`ETH_CREDENTIALS_PASSWORD`, `ETH_NODE_LOGIN` and `ETH_NODE_PASSWORD`).
3) Using command line arguments. For example `gradle runEthNotary -PcredentialsPassword=test -PnodeLogin=login -PnodePassword=password`

Configurations have the following priority:

Command line args > Environment variables > Properties file

## Testing
`gradle test` for unit tests

`gradle integrationTest` for integation tests


## Troubleshooting

1. Services cannot be lauched due to the issue with protobuf. Solution for linux — use 3.5.1 version. Solution for mac — manually put old version in Cellar folder for 3.5.1 version (ask someone from the team), and link it with `brew switch protobuf 3.5.1`. 

2. Services cannot resolve their hostnames. Solution — add following lines to /etc/hosts file:
```
127.0.0.1 d3-iroha
127.0.0.1 d3-iroha-postgres
127.0.0.1 d3-notary
127.0.0.1 d3-eth-node0
```
