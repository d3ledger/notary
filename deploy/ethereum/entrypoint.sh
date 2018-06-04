NODE="$1"
echo ${NODE}
/parity/parity  \
        account import --chain /eth/genesis.json --keys-path /tmp/parity/node${NODE}/keys /eth/keys

/parity/parity account --chain /eth/genesis.json list

/parity/parity \
        --config /eth/config/node${NODE}.toml
