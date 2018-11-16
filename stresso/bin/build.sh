#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
SKIP_JAR_CHECKS="true"

. $BIN_DIR/load-env.sh

unset SKIP_JAR_CHECKS

cd $BIN_DIR/..

# build Stresso using the versions of Fluo and Accumulo running on the system
mvn clean package -Dfluo.version=$FLUO_VERSION -Daccumulo.version=$ACCUMULO_VERSION -DskipTests

# populate lib dir used by fluo init
rm lib/*
cp target/stresso-0.0.1-SNAPSHOT.jar ./lib/
mvn dependency:copy-dependencies  -DincludeArtifactIds=fluo-recipes-core -DoutputDirectory=./lib
