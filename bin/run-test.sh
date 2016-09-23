#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
SKIP_JAR_CHECKS="true"
SKIP_FLUO_PROPS_CHECK="true"

. $BIN_DIR/load-env.sh

unset SKIP_JAR_CHECKS
unset SKIP_FLUO_PROPS_CHECK

# stop if any command fails
set -e

if [ ! -d $FLUO_HOME/apps/$FLUO_APP_NAME ]; then
  $FLUO_CMD new $FLUO_APP_NAME
else
  echo "Restarting '$FLUO_APP_NAME' application.  Errors may be printed if it's not running..."
  $FLUO_CMD stop $FLUO_APP_NAME || true
  rm -rf $FLUO_HOME/apps/$FLUO_APP_NAME
  $FLUO_CMD new $FLUO_APP_NAME
fi

# build stresso
(cd $BIN_DIR/..;mvn package -Dfluo.version=$FLUO_VERSION -Daccumulo.version=$ACCUMULO_VERSION -DskipTests)

if [[ $(accumulo version) == *1.6* ]]; then
  # build stress balancer
  (cd $BIN_DIR/..; mkdir -p git; cd git;git clone https://github.com/keith-turner/stress-balancer.git; cd stress-balancer; ./config-fluo.sh $FLUO_PROPS)
fi

if [ ! -f "$STRESSO_JAR" ]; then
  echo "Stresso jar not found : $STRESSO_JAR"
  exit 1
fi
if [ ! -d $FLUO_APP_LIB ]; then
  echo "Fluo app lib $FLUO_APP_LIB does not exist" 
  exit 1
fi
cp $STRESSO_JAR $FLUO_APP_LIB
mvn dependency:copy-dependencies  -DincludeArtifactIds=fluo-recipes-core -DoutputDirectory=$FLUO_APP_LIB

# determine a good stop level
if (("$MAX" <= $((10**9)))); then
  STOP=6
elif (("$MAX" <= $((10**12)))); then
  STOP=5
else
  STOP=4
fi

# delete existing config in fluo.properties if it exist
$SED '/fluo.observer/d' $FLUO_PROPS
$SED '/fluo.app.trie/d' $FLUO_PROPS

# append stresso specific config
echo "fluo.observer.0=stresso.trie.NodeObserver" >> $FLUO_PROPS
echo "fluo.app.trie.nodeSize=8" >> $FLUO_PROPS
echo "fluo.app.trie.stopLevel=$STOP" >> $FLUO_PROPS

$FLUO_CMD init $FLUO_APP_NAME -f
$FLUO_CMD start $FLUO_APP_NAME

echo "Removing any previous logs in $LOG_DIR"
mkdir -p $LOG_DIR
rm -f $LOG_DIR/*

# configure balancer for fluo table
if [[ $(accumulo version) == *1.6* ]]; then
  (cd $BIN_DIR/../git/stress-balancer; ./config-accumulo.sh $FLUO_PROPS)
fi # TODO setup RegexGroupBalancer built into Accumulo 1.7.0... may be easier to do from java

hadoop fs -rm -r -f /stresso/

set -e

# add splits to Fluo table
echo "*****Presplitting table*****"
$BIN_DIR/split.sh $SPLITS >$LOG_DIR/split.out 2>$LOG_DIR/split.err

if (( GEN_INIT > 0 )); then
  # generate and load intial data using map reduce writing directly to table
  echo "*****Generating and loading initial data set*****"
  $BIN_DIR/generate.sh $MAPS $((GEN_INIT / MAPS)) $MAX /stresso/init >$LOG_DIR/generate_0.out 2>$LOG_DIR/generate_0.err
  $BIN_DIR/init.sh /stresso/init /stresso/initTmp $REDUCES >$LOG_DIR/init.out 2>$LOG_DIR/init.err
  hadoop fs -rm -r /stresso/initTmp
fi

# load data incrementally
for i in $(seq 1 $ITERATIONS); do
  echo "*****Generating and loading incremental data set $i*****"
  $BIN_DIR/generate.sh $MAPS $((GEN_INCR / MAPS)) $MAX /stresso/$i >$LOG_DIR/generate_$i.out 2>$LOG_DIR/generate_$i.err
  $BIN_DIR/load.sh /stresso/$i >$LOG_DIR/load_$i.out 2>$LOG_DIR/load_$i.err
  # TODO could reload the same dataset sometimes, maybe when i%5 == 0 or something
  $BIN_DIR/compact-ll.sh $MAX $COMPACT_CUTOFF >$LOG_DIR/compact-ll_$i.out 2>$LOG_DIR/compact-ll_$i.err
  sleep $SLEEP
done

# print unique counts
echo "*****Calculating # of unique integers using MapReduce*****"
$BIN_DIR/unique.sh $REDUCES /stresso/* >$LOG_DIR/unique.out 2>$LOG_DIR/unique.err
grep UNIQUE $LOG_DIR/unique.err

echo "*****Wait for Fluo to finish processing*****"
$FLUO_CMD wait $FLUO_APP_NAME

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
