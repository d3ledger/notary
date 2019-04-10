# D3 services deployment
The role for deployment of D3 services along with Iroha.

## Background
This Ansible role allows to deploy a set of services that form a D3 Node. A set of nodes forms a D3 Network. Each Node is a separate entity capable of validating transactions in the Network, applying some business logic to them, and processing withdrawals and deposits from external providers (such as Bitcoin or Ethereum). Each service on the Node runs inside a Docker container.

To better understand the terminology and workflow, it is recommended to first get familiar with [architecture of the system](https://soramitsu.atlassian.net/wiki/spaces/D3/pages/451674124/Architectural+notes)

## Overview
The role is logically split into several files (see `tasks` directory):
  - `iroha.yml`. Generates Iroha keys for accounts required for a correct work of the Node along with Genesis Block. Makes use of [external Iroha role](https://github.com/hyperledger/iroha/tree/master/deploy/ansible/roles/iroha-docker)
  - `ethereum.yml`. Generates addresses (wallets) in Ethereum and deploys smart contracts
  - `bitcoin.yml`. Generates addresses in Bitcoin and creates wallets for services
  - `main.yml`. Prepares the remote host: downloads config files, creates required paths, runs services.
  
The role relies on so-called [Bootstrap service](https://github.com/d3ledger/notary/tree/master/bootstrap). This is a special helper service that runs locally and exposes several REST API endpoints. Tasks in the role then call methods of the API to perform certain operations (such as generation of Iroha keys, deploying smart contracts in Ethereum, etc). See the [README.md file](https://github.com/d3ledger/notary/blob/master/bootstrap/ReadMe.md) in Bootstrap service repository.

In order for services to function properly they need a set of addresses of smart contracts in Ethereum. The role deploys them automatically but it requires depositing a Genesis wallet beforehand. By the time of writing, the lower fee limit for deployment is ~7USD.

The role stores some files locally to persist the state between runs:
  - `bootstrap_api_smart_contracts_credentialspath` variable sets a local path to Genesis wallet. Genesis wallet is just an ordinary Ethereum wallet that is used to deploy smart contract during the play. We persist it locally to avoid generating a new one on each play since you need to deposit it upon the creation to be able to deploy smart contracts. Delete the file on the path if you want to generate a new Genesis wallet.
  - `bootstrap_api_smart_contracts_addresspath` variable sets a local path to a text file with addresses of already deployed smart contracts. The role checks whether the file exist on a file system and either deploys smart contracts using Genesis wallet if it does not or tries to read addresses from it to put them into configuration files on hosts in the play. Deleting the file on the path or setting `bootstrap_force_smart_contract_deployment` variable to `true` will force redeployment of the smart contracts.

## Quick Start
1. Clone `d3ledger/notary` repository. It contains bootstrap service that is required for the role:
```
$ git clone --depth 1 https://github.com/d3ledger/notary.git
```
2. Run bootstrap service with a local mount (to persist the state between the runs. See above.) inside a Docker container:
```
$ docker run -it -w /opt/bootstrap -v $(pwd)/notary:/opt/bootstrap -v /opt/bootstrap/state/:/opt/bootstrap/state --entrypoint bash -p 127.0.0.1:8090:8080 gradle:4.10
$ BTC_NETWORK=MainNet ./gradlew runBootstrap
```
See [Bootstrap service README](https://github.com/d3ledger/notary/blob/master/bootstrap/ReadMe.md) for possible values of `BTC_NETWORK`.

3. Create a playbook
```
- hosts: replace.with.your.host.tld
  roles:
    - role: iroha-docker-clean
      vars:
        iroha_init_vars: true
        iroha_generate_init_configs: false 
        iroha_update_runtime_configs: false
      tags: ["iroha-init"]

    - role: bootstrap
      tags: ["bootstrap"]

    - role: iroha-docker-clean
      vars:
        iroha_init_vars: false
        iroha_generate_init_configs: true
        iroha_update_runtime_configs: true
      tags: ["iroha-deploy"]

```
> TODO: replace `iroha-docker-clean` with `iroha-docker` as soon as the changes will propagate to mainline of Iroha repository.

Put this playbook in `playbooks/d3.yml`. You may want to override some default parameters on either host or group level. See Ansible manual for instructions.

4. Run the playbook
```
$ ansible-playbook playbooks/d3.yml
```
## Examples
All the tasks in the role are tagged. You can run only the parts of the role you need. Below are some examples of running a playbook with different tags. Run tasks that are only related to configuration of:

- **Iroha**
```
$ ansible-playbook playbooks/d3.yml --tags iroha-init-vars,iroha-config-gen,iroha-deploy,iroha
```

- **Ethereum**
```
$ ansible-playbook playbooks/d3.yml --tags ethereum
```

- **Bitcoin**
```
$ ansible-playbook playbooks/d3.yml --tags bitcoin
```

See Iroha role for possible tags.

## Externalized Configuration
Since services can be run in a different environments they require different configuration parameters. Each service has one or multiple configuration files (https://github.com/d3ledger/notary/tree/master/configs). Parameters can be overridden by environment variables. There are a bunch of files in `templates` directory with names starting with `.env-`. They override some default parameters and used as an [externalized configuration](https://docs.docker.com/compose/env-file/) for a Docker container. Override files are named after the service (e.g., `.env-chain-adapter`). Parameters in override files are set with Ansible variables defined in `defaults/main.yml`. Ansible variables themselves can be set in a variety of ways. See Ansible documentation about [variable precedence](https://docs.ansible.com/ansible/latest/user_guide/playbooks_variables.html#variable-precedence-where-should-i-put-a-variable).

`templates/.env` override file is not related to any particular service. This file is always read by a Docker Compose during a startup and can set variables defined in `docker-compose.yml` file itself.