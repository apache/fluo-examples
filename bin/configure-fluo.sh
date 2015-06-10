#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
SKIP_STRESS_JAR_CHECK="true"
SKIP_FLUO_PROPS_CHECK="true"

. $BIN_DIR/load-env.sh

if [ "$BUILD" = "true" ]
then
  #build fluo-stress
  (cd $BIN_DIR/..;mvn package -DskipTests)
fi

if [[ $(accumulo version) == *1.6* ]]
then
  (cd $BIN_DIR/..; mkdir -p git; cd git;git clone https://github.com/keith-turner/stress-balancer.git; cd stress-balancer; ./config-fluo.sh $FLUO_PROPS)
fi

if [ ! -f "$STRESS_JAR" ]
then
  echo "Stress jar not found : $STRESS_JAR" 
  exit 1;
fi

#copy stess jar
cp $STRESS_JAR $FLUO_HOME/apps/$FLUO_APP_NAME/lib

#determine a good stop level
if (("$MAX" <= $((10**9)))) 
then 
  STOP=6
elif (("$MAX" <= $((10**12))))
then
  STOP=5
else
  STOP=4
fi

#delete existing config in fluo.properties if it exist
$SED '/io.fluo.observer/d' $FLUO_PROPS
$SED '/io.fluo.app.trie/d' $FLUO_PROPS

#append stress specific config
echo "io.fluo.observer.0=io.fluo.stress.trie.NodeObserver" >> $FLUO_PROPS
echo "io.fluo.app.trie.nodeSize=8" >> $FLUO_PROPS
echo "io.fluo.app.trie.stopLevel=$STOP" >> $FLUO_PROPS
