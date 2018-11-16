#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. $BIN_DIR/load-env.sh

yarn jar $STRESSO_SHADED_JAR stresso.trie.Load $FLUO_CONN $FLUO_APP_NAME $@
