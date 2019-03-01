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

#This script will copy the phrase count jar and its dependencies to the Fluo
#application lib dir


if [ "$#" -ne 2 ]; then
  echo "Usage : $0 <FLUO HOME> <PHRASECOUNT_HOME>"
  exit 
fi

FLUO_HOME=$1
PC_HOME=$2

PC_JAR=$PC_HOME/target/phrasecount-0.0.1-SNAPSHOT.jar

#build and copy phrasecount jar
(cd $PC_HOME; mvn package -DskipTests)

FLUO_APP_LIB=$FLUO_HOME/apps/phrasecount/lib/

cp $PC_JAR $FLUO_APP_LIB
(cd $PC_HOME; mvn dependency:copy-dependencies -DoutputDirectory=$FLUO_APP_LIB)

