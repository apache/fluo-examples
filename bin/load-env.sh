if [ ! -f $BIN_DIR/../conf/env.sh ] 
then
  . $BIN_DIR/../conf/env.sh.example
else
  . $BIN_DIR/../conf/env.sh
fi

if [ ! -d "$FLUO_HOME" ]
then
  echo "Problem with FLUO_HOME : $FLUO_HOME"
  exit 1
fi

if [ ! -f "$FLUO_PROPS" ]
then
  echo "Fluo properties file not found : $FLUO_PROPS" 
  exit 1;
fi

if [ ! -f "$STRESS_JAR" ] && [ -z "$SKIP_STRESS_JAR_CHECK" ]
then
  echo "Stress jar not found : $STRESS_JAR" 
  exit 1;
fi

command -v yarn >/dev/null 2>&1 || { echo >&2 "I require yarn but it's not installed.  Aborting."; exit 1; }
command -v hadoop >/dev/null 2>&1 || { echo >&2 "I require hadoop but it's not installed.  Aborting."; exit 1; }

if [[ "$OSTYPE" == "darwin"* ]]; then
  export SED="sed -i .bak"
else
  export SED="sed -i"
fi
