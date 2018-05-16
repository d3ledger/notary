import json
from pprint import pprint
import requests
import time

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

print(get_accounts("node0"))
print(get_enode("node0"))
print(get_enode("node1"))

enode0 = get_enode("node0")

print(add_peer("node1", enode0))

for i in range(10):
    conn = get_peers("node0")["connected"]
    pprint("Connected peers: {}".format(conn))
    if(conn > 0):
        break
    time.sleep(2)

print("Two peers were connected")