# notary
Backend code for a D3 notary

[![CircleCI](https://circleci.com/gh/d3ledger/notary.svg?style=svg)](https://circleci.com/gh/d3ledger/notary)


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
