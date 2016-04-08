#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
SKIP_STRESS_JAR_CHECK="true"
SKIP_FLUO_PROPS_CHECK="true"

. $BIN_DIR/load-env.sh

unset SKIP_STRESS_JAR_CHECK
unset SKIP_FLUO_PROPS_CHECK

if [ ! -d $FLUO_HOME/apps/$FLUO_APP_NAME ]; then
  $FLUO_HOME/bin/fluo new $FLUO_APP_NAME
else
  echo "Restarting '$FLUO_APP_NAME' application.  Errors may be printed if it's not running..."
  $FLUO_HOME/bin/fluo stop $FLUO_APP_NAME || true
  rm -rf $FLUO_HOME/apps/$FLUO_APP_NAME
  $FLUO_HOME/bin/fluo new $FLUO_APP_NAME
fi

if [ "$BUILD" = "true" ]; then
  # build fluo-stress
  (cd $BIN_DIR/..;mvn package -DskipTests)
fi

if [[ $(accumulo version) == *1.6* ]]; then
  # build stress balancer
  (cd $BIN_DIR/..; mkdir -p git; cd git;git clone https://github.com/keith-turner/stress-balancer.git; cd stress-balancer; ./config-fluo.sh $FLUO_PROPS)
fi

if [ ! -f "$STRESS_JAR" ]; then
  echo "Stress jar not found : $STRESS_JAR" 
  exit 1
fi

# copy stess jar
cp $STRESS_JAR $FLUO_HOME/apps/$FLUO_APP_NAME/lib

# determine a good stop level
if (("$MAX" <= $((10**9)))); then
  STOP=6
elif (("$MAX" <= $((10**12)))); then
  STOP=5
else
  STOP=4
fi

# delete existing config in fluo.properties if it exist
$SED '/io.fluo.observer/d' $FLUO_PROPS
$SED '/io.fluo.app.trie/d' $FLUO_PROPS

# append stress specific config
echo "io.fluo.observer.0=io.fluo.stress.trie.NodeObserver" >> $FLUO_PROPS
echo "io.fluo.app.trie.nodeSize=8" >> $FLUO_PROPS
echo "io.fluo.app.trie.stopLevel=$STOP" >> $FLUO_PROPS

$FLUO_HOME/bin/fluo init $FLUO_APP_NAME -f
$FLUO_HOME/bin/fluo start $FLUO_APP_NAME

mkdir -p $LOG_DIR
rm -f $LOG_DIR/*

# configure balancer for fluo table
if [[ $(accumulo version) == *1.6* ]]; then
  (cd $BIN_DIR/../git/stress-balancer; ./config-accumulo.sh $FLUO_PROPS)
fi # TODO setup RegexGroupBalancer built into Accumulo 1.7.0... may be easier to do from java

hadoop fs -rm -r /stress/
# add splits to Fluo table
echo "*****Presplitting table*****"
$BIN_DIR/split.sh $SPLITS >$LOG_DIR/split.out 2>$LOG_DIR/split.err

if (( GEN_INIT > 0 )); then
  # generate and load intial data using map reduce writing directly to table
  echo "*****Generating and loading initial data set*****"
  $BIN_DIR/generate.sh $MAPS $((GEN_INIT / MAPS)) $MAX /stress/init >$LOG_DIR/generate_0.out 2>$LOG_DIR/generate_0.err
  $BIN_DIR/init.sh /stress/init /stress/initTmp $REDUCES >$LOG_DIR/init.out 2>$LOG_DIR/init.err
  hadoop fs -rm -r /stress/initTmp
fi

# load data incrementally
for i in $(seq 1 $ITERATIONS); do
  echo "*****Generating and loading incremental data set $i*****"
  $BIN_DIR/generate.sh $MAPS $((GEN_INCR / MAPS)) $MAX /stress/$i >$LOG_DIR/generate_$i.out 2>$LOG_DIR/generate_$i.err
  $BIN_DIR/load.sh /stress/$i >$LOG_DIR/load_$i.out 2>$LOG_DIR/load_$i.err
  # TODO could reload the same dataset sometimes, maybe when i%5 == 0 or something
  $BIN_DIR/compact-ll.sh $MAX $COMPACT_CUTOFF >$LOG_DIR/compact-ll_$i.out 2>$LOG_DIR/compact-ll_$i.err
  sleep $SLEEP
done

# print unique counts
echo "*****Calculating # of unique integers using MapReduce*****"
$BIN_DIR/unique.sh $REDUCES /stress/* >$LOG_DIR/unique.out 2>$LOG_DIR/unique.err
grep UNIQUE $LOG_DIR/unique.err

echo "*****Wait for Fluo to finish processing*****"
$FLUO_HOME/bin/fluo wait $FLUO_APP_NAME

echo "*****Printing # of unique integers calculated by Fluo*****"
$BIN_DIR/print.sh >$LOG_DIR/print.out 2>$LOG_DIR/print.err
cat $LOG_DIR/print.out

echo "*****Verifying Fluo & MapReduce results match*****"
MAPR_TOTAL=`grep UNIQUE $LOG_DIR/unique.err | cut -d = -f 2`
FLUO_TOTAL=`grep "Total at root" $LOG_DIR/print.out | cut -d ' ' -f 5`
if [ $MAPR_TOTAL -eq $FLUO_TOTAL ]; then
  echo "Success! Fluo & MapReduce both calculated $FLUO_TOTAL unique integers"
  exit 0
else
  echo "ERROR - Results do not match. Fluo calculated $FLUO_TOTAL unique integers while MapReduce calculated $MAPR_TOTAL integers"
  exit 1
fi
