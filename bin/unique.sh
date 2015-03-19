#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. $BIN_DIR/load-env.sh

if [ "$#" -lt 2 ]; then
    echo "Usage : $0 <num reducers> <input dir>{ <input dir>}"
    exit 1
fi


LJO="-libjars $LIB_JARS"
yarn jar $STRESS_JAR io.fluo.stress.trie.Unique -Dmapreduce.job.reduces=$1 $LJO ${@:2}

