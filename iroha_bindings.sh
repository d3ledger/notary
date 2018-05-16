#!/bin/sh
# This file generates Iroha bindings based on vendor version of Iroha


# change dir to script location
cd "$(dirname "$0")"

build="iroha_bindings/$1"
echo "iroha target dir" ${build}

# clear previous bindings
rm -rf ${build} && mkdir ${build}

bindings_dir=vendor/iroha/example/java

# generate iroha bindings
sh ${bindings_dir}/build_library.sh

# move generate files to repository
cp -R ${bindings_dir}/dist/iroha-bindings-example.jar ${build}/iroha-bindings-example.jar
cp -R ${bindings_dir}/dist/libirohajava.jnilib ${build}/libirohajava.jnilib

# move protoc library to bindings
unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then
    cp vendor/iroha/external/src/google_protobuf-build/libprotobuf.so dist/
fi
