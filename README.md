# notary
Backend code for a D3 notary

[![CircleCI](https://circleci.com/gh/d3ledger/notary.svg?style=svg)](https://circleci.com/gh/d3ledger/notary)

## How to run
1) clone project `master` branch
2) launch Iroha and Postgres in docker with `docker-compose -f deploy/docker-compose-dev.yml up`
3) Provide ethereum password `src/main/resources/eth/ethereum_password.properties`
4) Run notary service `gradle runEthNotary -Pprofile=mainnet`
5) Run registration service `gradle runEthRegistration -Pprofile=mainnet`
6) Run  withdrawal service `gradle runWithdrawal -Pprofile=mainnet`
6) Deploy relay (one relay per one client registration) `gradle runDeployRelay -Pprofile=mainnet`. Ensure relay is deployed on etherscan.io

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
