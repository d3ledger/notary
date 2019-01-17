If you want to run a full node, with all the services:

`PROFILE=testnet TAG=debug docker-compose -f deploy/docker-compose-base.yml -f deploy/docker-compose-full.yml -f deploy/docker-compose-single.yml up`


To run only the services that have to be deployed on each node of the network

`PROFILE=testnet TAG=debug docker-compose -f deploy/docker-compose-base.yml -f deploy/docker-compose-full.yml up`


To run only the services that have to deployed only once per network:

`PROFILE=testnet TAG=debug docker-compose -f deploy/docker-compose-base.yml -f deploy/docker-compose-single.yml up`