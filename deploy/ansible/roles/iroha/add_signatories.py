import base64
import json
import os
import sys

os.chdir("files")


def hex_to_b64(hex_string):
    hex_string = base64.b64encode(bytearray.fromhex(hex_string))
    return hex_string.decode('utf-8')


def mfilter(cmd):
    if not ("createAccount" in cmd.keys()):
        return True
    if cmd["createAccount"]["accountName"] == "notary" and cmd["createAccount"]["domainId"] == "notary":
        return False
    return True


def add_signatory(key):
    genesis_dict['payload']['transactions'][0]['payload']['reducedPayload']['commands'].append({
        "addSignatory": {
            "accountId": "notary@notary",
            "publicKey": key
        }
    })


def readkey(filename):
    return open(filename).read()


keys = sys.argv[1:]

genesis_dict = json.loads(open("genesis.block", "r").read())

for cmd in genesis_dict['payload']['transactions'][0]['payload']['reducedPayload']['commands']:
    if not mfilter(cmd):
        cmd["createAccount"]["publicKey"] = hex_to_b64(readkey(keys[0]))

for key in keys[1:]:
    add_signatory(hex_to_b64(readkey(key)))

with open("genesis.block", 'w') as genesis_json:
    json.dump(genesis_dict, genesis_json, indent=4)
