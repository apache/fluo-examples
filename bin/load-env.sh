if [ ! -f $BIN_DIR/../conf/env.sh ] 
then
  . $BIN_DIR/../conf/env.sh.example
else
  . $BIN_DIR/../conf/env.sh
fi

# verify fluo configuration
if [ ! -d "$FLUO_HOME" ]; then
  echo "Problem with FLUO_HOME : $FLUO_HOME"
  exit 1
fi
FLUO_CMD=$FLUO_HOME/bin/fluo
if [ -z "$FLUO_APP_NAME" ]; then
  echo "FLUO_APP_NAME is not set!" 
  exit 1
fi
FLUO_APP_LIB=$FLUO_HOME/apps/$FLUO_APP_NAME/lib
FLUO_PROPS=$FLUO_HOME/apps/$FLUO_APP_NAME/conf/fluo.properties
if [ ! -f "$FLUO_PROPS" ] && [ -z "$SKIP_FLUO_PROPS_CHECK" ]; then
  echo "Fluo properties file not found : $FLUO_PROPS" 
  exit 1
fi

STRESSO_VERSION=0.0.1-SNAPSHOT
STRESSO_JAR=$BIN_DIR/../target/stresso-$STRESSO_VERSION.jar
STRESSO_SHADED_JAR=$BIN_DIR/../target/stresso-$STRESSO_VERSION-shaded.jar
if [ ! -f "$STRESSO_JAR" ] && [ -z "$SKIP_JAR_CHECKS" ]; then
  echo "Stresso jar not found : $STRESSO_JAR" 
  exit 1;
fi
if [ ! -f "$STRESSO_SHADED_JAR" ] && [ -z "$SKIP_JAR_CHECKS" ]; then
  echo "Stresso shaded jar not found : $STRESSO_SHADED_JAR" 
  exit 1;
fi

command -v yarn >/dev/null 2>&1 || { echo >&2 "I require yarn but it's not installed.  Aborting."; exit 1; }
command -v hadoop >/dev/null 2>&1 || { echo >&2 "I require hadoop but it's not installed.  Aborting."; exit 1; }

if [[ "$OSTYPE" == "darwin"* ]]; then
  export SED="sed -i .bak"
else
  export SED="sed -i"
fi
