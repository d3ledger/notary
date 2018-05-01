#!/bin/sh
# This file generates Iroha bindings based on vendor version of Iroha


# change dir to script location
cd "$(dirname "$0")"

build=iroha_bindings

# clear previous bindings
rm -rf ${build} && mkdir ${build}

bindings_dir=vendor/iroha/example/java

# generate iroha bindings
sh ${bindings_dir}/build_library.sh

# move generate files to repository
cp -R ${bindings_dir}/dist/* ${build}
