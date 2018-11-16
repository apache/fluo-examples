#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. $BIN_DIR/load-env.sh

if [ "$#" -ne 3 ]; then
    echo "Usage : $0 <input dir> <work dir> <num reducers>"
    exit 1
fi

# Set this to avoid Hadoop's old version of guava.
export HADOOP_USE_CLIENT_CLASSLOADER=true
yarn jar $STRESSO_SHADED_JAR stresso.trie.Init -Dmapreduce.job.reduces=$3 $FLUO_CONN $FLUO_APP_NAME $1 $2
