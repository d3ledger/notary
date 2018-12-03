# Using ansible for D3 deployment
There are 2 deployment scenarios supported which use different inventory files.

1. Deploy on remote Ubuntu servers
2. Deploy locally on `docker-machine` network.


## 1 Remote deploy
### 1.1 Inventory file
```
[ubuntu]
node0 ansible_host=212.47.240.224 
node1 ansible_host=51.15.231.25 
node2 ansible_host=163.172.187.40 

[ubuntu:vars]
ansible_ssh_user=root

```

As you can see, basic host field in group contains `hostname`, and `ansible_host <ip>`.
Then there is a section with variables for this group, in this case we have only one, which tells ansible as which user to login.


### 1.2 How to launch

After `inventory.list` inventory file is configured one could launch the playbook.

```
PROFILE=testnet TAG=develop ansible-playbook -i inventory.list main.yml --key-file "~/.ssh/<key>" --extra-vars "docker_password=<nexus_password>" 
```
, where you should specify profile that you want to run, docker image tag and your SSH key and the password for accessing our Docker Hub on nexus.



## 2 Local deploy
If you don't have any servers, or you want to deploy everything on you machine you can use this scenario.
You should have installed and run [docker-machine](https://docs.docker.com/machine/overview/#where-to-go-next).


### 1.1 Inventory file
```
[docker]
vm0 ansible_host=192.168.99.101 ansible_ssh_private_key_file=<key1>
vm1 ansible_host=192.168.99.102 ansible_ssh_private_key_file=<key2>
vm2 ansible_host=192.168.99.103 ansible_ssh_private_key_file=<key3>

[docker:vars]
ansible_ssh_user=docker
ansible_python_interpreter=/usr/bin/env python

```

As you can see, basic host field in group contains `hostname`, `ansible_host <ip>`, and `<key>` which has to be provided for each host.
Then there is a section with variables for this group, in this case we have only one, which tells ansible as which user to login, and which interpreter to use (mandatory for docker-machine).


### 1.2 How to launch

After `inventory_local.list` inventory file is configured one could launch the playbook.

```
PROFILE=testnet TAG=develop ansible-playbook -i inventory_local.list main.yml  --extra-vars "docker_password=<nexus_password>" 
```
, where you should specify profile that you want to run, docker image tag and the password for accessing our Docker Hub on nexus.
Here you don't need to tell the SSH key to use, because it is written in inventory file.

