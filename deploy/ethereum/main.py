import json
from pprint import pprint
import requests
import time
import os

try:
    NODE_0 = os.environ['NODE_0']
except KeyError:
    NODE_0 = "node0"
try:
    NODE_1 = os.environ['NODE_1']
except KeyError:
    NODE_1 = "node1"
try:
    PORT = os.environ['PORT']
except KeyError:
    PORT = 8545
HEADERS = {"Content-Type": "application/json"}

template = dict(
    jsonrpc="2.0",
    method=None,
    params=[],
    id=1
)

def get_accounts(node):
    URL = "http://{}:{}".format(node, PORT)
    rpc = template.copy()
    rpc["method"] = "eth_accounts"
    r = requests.post(URL, data=json.dumps(rpc), headers=HEADERS)
    data = json.loads(r.text)
    return data["result"]

def get_enode(node):
    URL = "http://{}:{}".format(node, PORT)
    rpc = template.copy()
    rpc["method"] = "parity_enode"
    r = requests.post(URL, data=json.dumps(rpc), headers=HEADERS)
    data = json.loads(r.text)

    return data["result"]

def add_peer(node, enode):
    URL = "http://{}:{}".format(node, PORT)
    rpc = template.copy()
    rpc["method"] = "parity_addReservedPeer"
    rpc["params"] = [enode]
    r = requests.post(URL, data=json.dumps(rpc), headers=HEADERS)
    data = json.loads(r.text)
    return data["result"]

def get_peers(node):
    URL = "http://{}:{}".format(node, PORT)
    rpc = template.copy()
    rpc["method"] = "parity_netPeers"
    # print(rpc)
    r = requests.post(URL, data=json.dumps(rpc), headers=HEADERS)
    data = json.loads(r.text)
    if "error" in data.keys():
        pprint(data)

    return data["result"]


time.sleep(8)
print(NODE_0, NODE_1, PORT)
print(get_accounts(NODE_0))
print(get_enode(NODE_0))
print(get_enode(NODE_1))

enode0 = get_enode(NODE_0)

print(add_peer(NODE_0, enode0))

for i in range(10):
    conn = get_peers(NODE_0)["connected"]
    pprint("Connected peers: {}".format(conn))
    if(conn > 0):
        break
    time.sleep(2)

print("Two peers were connected")

