# notary
Backend code for a D3 notary

D3 consists of 3 parts. Common services should be run obligatory and rest can be run optionally:
1) Common services
2) Ethereum subsystem
3) Bitcoin subsystem

## Common services
1) Clone project `master` branch
2) Launch Iroha and Postgres in docker with `docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.dev.yml up`
3) Run registration service `PROFILE=mainnet ./gradlew runRegistration`

Now you can register clients and launch subsystems.

## How to run notary application and services in Ethereum main net
1) Run common services
2) Provide ethereum passwords `configs/eth/ethereum_password_mainnet.properties` (ask someone from maintainers team about the format)
3) Deploy Ethereum master contract and relay registry contract, provide notary ethereum accounts `./gradlew runPreDeployEthereum --args="0x6826d84158e516f631bbf14586a9be7e255b2d23"` 
4) Run deposit service `PROFILE=mainnet ./gradlew runEthDeposit`
5) Run registration service `PROFILE=mainnet ./gradlew runEthRegistration`
6) Run withdrawal service `PROFILE=mainnet ./gradlew runWithdrawal`
7) Deploy relay smart contract (one relay per one client registration) `PROFILE=mainnet ./gradlew runDeployRelay`. Ensure relay is deployed on etherscan.io

Great! So now you can move on and connect frontend application (check back-office repo in d3ledger)

## Ethereum passwords
Passwords for Ethereum network may be set in 3 different ways:

1) Using `eth/ethereum_password.properties` file.
2) Using environment variables(`ETH_CREDENTIALS_PASSWORD`, `ETH_NODE_LOGIN` and `ETH_NODE_PASSWORD`).
3) Using command line arguments. For example `./gradlew runEthDeposit -PcredentialsPassword=test -PnodeLogin=login -PnodePassword=password`

Configurations have the following priority:

Command line args > Environment variables > Properties file

## How to run notary application and services in Bitcoin main net
1) Run common services
2) Create `.wallet` file (ask maintainers how to do that) and put it to desired location
3) Run address generation process using `PROFILE=mainnet ./gradlew runBtcAddressGeneration`
4) Create change address by running `./gradlew generateBtcChangeAddress`
5) Create few free addresses(addresses that may be registered by clients lately) `./gradlew generateBtcFreeAddress`
6) Run registration service `PROFILE=mainnet ./gradlew runBtcRegistration`
7) Run notary service `PROFILE=mainnet ./gradlew runBtcDepositWithdrawal`

## How to run notification services
1) Create SMTP configuration file located at `configs/smtp.properties`(see test example `configs/smtp_test.properties`). This file contains SMTP server credentials.
2) Create Push API configuration file located at `configs/push.properties`(see test example `configs/push_test.properties`). This file contains VAPID keys. You can generate keys by yourself using [webpush-libs tutorial](https://github.com/web-push-libs/webpush-java/wiki/VAPID).
3) Run gralde command `./gradlew runNotifications`

## AWS SES Integration
SMTP configuration file `configs/smtp.properties` must be modified in order to be able to send email notifications via AWS SES. 
```
smtp.host=email-smtp.eu-west-1.amazonaws.com
smtp.port=25
smtp.userName=ask maintainers
smtp.password=ask maintainers
```
## Testing
`./gradlew test` for unit tests

`./gradlew integrationTest` for integation tests

## Testing Bitcoin
`./gradlew btcSendToAddress -Paddress=<address> -PamountBtc=<amount>` — sends `<amount>` BTC to `<address>` in Bitcoin regtest network

`./gradlew btcGenerateBlocks -Pblocks=<blocks>` — generates `<blocks>` in Bitcoin regtest network

## Troubleshooting

1. Services cannot be lauched due to the issue with protobuf. Solution for linux — use 3.5.1 version. Solution for mac — manually put old version in Cellar folder for 3.5.1 version (ask someone from the team), and link it with `brew switch protobuf 3.5.1`. 

2. Services cannot resolve their hostnames. Solution — add following lines to /etc/hosts file:
```
127.0.0.1 d3-iroha
127.0.0.1 d3-iroha-postgres
127.0.0.1 d3-notary
127.0.0.1 d3-eth-node0
127.0.0.1 d3-btc-node0
127.0.0.1 d3-rmq
127.0.0.1 d3-brvs
127.0.0.1 d3-brvs-mongodb
```
