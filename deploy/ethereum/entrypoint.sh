#!/bin/sh
NODE=${NODE:-0}

/parity/parity account import --chain ropsten /eth/keys

/parity/parity account --chain ropsten list

/parity/parity --config /eth/config/node${NODE}.toml
