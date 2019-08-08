#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
SKIP_JAR_CHECKS="true"

. $BIN_DIR/load-env.sh

unset SKIP_JAR_CHECKS

cd $BIN_DIR/..

# build Stresso using the versions of Fluo and Accumulo running on the system
mvn clean package -Dfluo.version=$FLUO_VERSION -Daccumulo.version=$ACCUMULO_VERSION -DskipTests

mkdir -p lib

# populate lib dir used by fluo init
rm -f lib/*
cp target/stresso-0.0.1-SNAPSHOT.jar ./lib/
mvn dependency:copy-dependencies  -DincludeArtifactIds=fluo-recipes-core,commons-collections -DoutputDirectory=./lib
