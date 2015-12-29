#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. $BIN_DIR/load-env.sh

yarn jar $STRESS_JAR io.fluo.stress.trie.CompactLL $FLUO_PROPS $@

