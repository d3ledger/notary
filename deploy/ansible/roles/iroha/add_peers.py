#!/usr/env/python3
import base64
import csv
import json
import sys
from pprint import pprint

'''
host;port;priv_key_b64_encoded;pub_key_b64_encoded
'''


class Peer:
    def __init__(self, host, port, priv_key, pub_key):
        self.host = host
        self.port = port
        self.priv_key = priv_key
        self.pub_key = pub_key

    def __str__(self):
        return "{}\n{}\n{}\n{}\n".format(
            self.host,
            self.port,
            self.priv_key,
            self.pub_key
        )


def parse_peers(peers_csv_fp):
    peers = []
    with open(peers_csv_fp) as csvfile:
        peersreader = csv.reader(csvfile, delimiter=';')
        for peer in peersreader:
            peers.append(Peer(peer[0], peer[1], peer[3], peer[2]))
    return peers


def genesis_add_peers(peers_list, genesis_block_fp):
    genesis_dict = json.loads(open(genesis_block_fp, "r").read())
    pprint(genesis_dict)
    for p in peers_list:
        p_add_command = {
            "addPeer": {"peer": {"address": "%s:%s" % (p.host, '10001'), "peerKey": hex_to_b64(p.pub_key)}}}
        genesis_dict['payload']['transactions'][0]['payload']['reducedPayload']['commands'].append(p_add_command)

    with open(genesis_block_fp, 'w') as genesis_json:
        json.dump(genesis_dict, genesis_json, sort_keys=True, indent=4)
        # genesis_json.truncate()


def hex_to_b64(hex_string):
    hex_string = base64.b64encode(bytearray.fromhex(hex_string))
    return hex_string.decode('utf-8')


def make_keys(peers):
    for i, p in enumerate(peers):
        with open('node%s.priv' % i, 'w+') as priv_key_file:
            priv_key_file.write(p.priv_key)
        with open('node%s.pub' % i, 'w+') as pub_key_file:
            pub_key_file.write(p.pub_key)


if __name__ == "__main__":
    peers_csv = sys.argv[1]
    peers = parse_peers(peers_csv)
    genesis_add_peers(peers, "genesis.block")
    make_keys(peers)
