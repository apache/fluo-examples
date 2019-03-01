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
PC_HOME=$( cd "$( dirname "$BIN_DIR" )" && pwd )

# stop if any command fails
set -e

if [ "$#" -ne 1 ]; then
  echo "Usage : $0 <TXT FILES DIR>"
  exit 
fi

#set the following to a directory containing text files
TXT_DIR=$1
if [ ! -d $TXT_DIR ]; then
  echo "Document directory $TXT_DIR does not exist" 
  exit 1
fi

#ensure $FLUO_HOME is set
if [ -z "$FLUO_HOME" ]; then
  echo '$FLUO_HOME must be set!'
  exit 1
fi

#Set application name.  $FLUO_APP_NAME is set by fluo-dev and zetten
APP=${FLUO_APP_NAME:-phrasecount}

#derived variables
APP_PROPS=$FLUO_HOME/apps/$APP/conf/fluo.properties

if [ ! -f $FLUO_HOME/conf/fluo.properties ]; then
  echo "Fluo is not configured, exiting."
  exit 1
fi

#remove application if it exists
if [ -d $FLUO_HOME/apps/$APP ]; then
  echo "Restarting '$APP' application.  Errors may be printed if it's not running..."
  $FLUO_HOME/bin/fluo kill $APP || true
  rm -rf $FLUO_HOME/apps/$APP
fi

#create new application dir
$FLUO_HOME/bin/fluo new $APP

#copy phrasecount jars to Fluo application lib dir
$PC_HOME/bin/copy-jars.sh $FLUO_HOME $PC_HOME

#Create export table and output Fluo configuration
$FLUO_HOME/bin/fluo exec $APP phrasecount.cmd.Setup $APP_PROPS pcExport >> $APP_PROPS

$FLUO_HOME/bin/fluo init $APP -f
$FLUO_HOME/bin/fluo exec $APP org.apache.fluo.recipes.accumulo.cmds.OptimizeTable
$FLUO_HOME/bin/fluo start $APP
$FLUO_HOME/bin/fluo info $APP

#Load data
$FLUO_HOME/bin/fluo exec $APP phrasecount.cmd.Load $APP_PROPS $TXT_DIR

#wait for all notifications to be processed.
$FLUO_HOME/bin/fluo wait $APP

#print phrase counts
$FLUO_HOME/bin/fluo exec $APP phrasecount.cmd.Print $APP_PROPS pcExport

$FLUO_HOME/bin/fluo stop $APP

