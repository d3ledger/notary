#!/bin/sh
##
## Copyright D3 Ledger, Inc. All Rights Reserved.
## SPDX-License-Identifier: Apache-2.0
##

NODE=${NODE:-0}

/parity/parity account import --chain ropsten /eth/keys

/parity/parity account --chain ropsten list

/parity/parity --config /eth/config/node${NODE}.toml
