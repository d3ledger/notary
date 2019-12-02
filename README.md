# notary
Backend code for a D3 notary

D3 consists of 3 parts. Common services should be run obligatory and rest can be run optionally:
1) Common services
2) Ethereum subsystem

## Common services
1) Clone project `master` branch
2) Launch Iroha and Postgres in docker with `docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.dev.yml up`
3) Run registration service `PROFILE=mainnet ./gradlew runRegistration`
4) (Optional) Run exchanger service `PROFILE=mainnet ./gradlew runExchanger`

Now you can register clients and launch subsystems.

## Testing
`./gradlew test` for unit tests

`./gradlew integrationTest` for integation tests

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
