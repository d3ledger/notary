#!/usr/bin/env bash
CURDIR="$(cd "$(dirname "$0")"; pwd)"
IROHA_HOME="$(dirname $(dirname "${CURDIR}"))"
cmake -H$IROHA_HOME -Bbuild -DSWIG_JAVA=ON -DSHARED_MODEL_DISABLE_COMPATIBILITY=ON;
cmake --build build/ --target irohajava -- -j"$(getconf _NPROCESSORS_ONLN)"
