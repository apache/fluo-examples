#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. $BIN_DIR/load-env.sh

if [ "$#" -lt 2 ]; then
    echo "Usage : $0 <num reducers> <input dir>{ <input dir>}"
    exit 1
fi

yarn jar $STRESSO_JAR stresso.trie.Unique -Dmapreduce.job.reduces=$1 ${@:2}
