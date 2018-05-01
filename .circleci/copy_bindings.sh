#!/bin/sh
# Copy protolib to local binaries on CI

cd "$(dirname "$0")"

sudo cp ../iroha_bindings/linux/libprotobuf.so /usr/local/lib/
