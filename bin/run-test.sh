#!/bin/bash

BIN_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. $BIN_DIR/load-env.sh

mkdir -p $LOG_DIR
rm $LOG_DIR/*

hadoop fs -rm -r /stress/
#add splits to Fluo table
echo "*****Presplitting table*****"
$BIN_DIR/split.sh $SPLITS $MAX >$LOG_DIR/split.out 2>$LOG_DIR/split.err

#generate and load intial data using map reduce writing directly to table
echo "*****Generating and loading initial data set*****"
$BIN_DIR/generate.sh $MAPS $((GEN_INIT / MAPS)) $MAX /stress/init >$LOG_DIR/generate_0.out 2>$LOG_DIR/generate_0.err
$BIN_DIR/init.sh /stress/init /stress/initTmp $REDUCES >$LOG_DIR/init.out 2>$LOG_DIR/init.err
hadoop fs -rm -r /stress/initTmp

#load data incrementally
for i in $(seq 1 $ITERATIONS); do
  echo "*****Generating and loading incremental data set $i*****"
  $BIN_DIR/generate.sh $MAPS $((GEN_INCR / MAPS)) $MAX /stress/$i >$LOG_DIR/generate_$i.out 2>$LOG_DIR/generate_$i.err
  $BIN_DIR/load.sh /stress/$i >$LOG_DIR/load_$i.out 2>$LOG_DIR/load_$i.err
  #TODO could reload the same dataset sometimes, maybe when i%5 == 0 or something
  sleep $SLEEP
done

#print unique counts
echo "*****Calculating # of unique integers using MapReduce*****"
$BIN_DIR/unique.sh $REDUCES /stress/* >$LOG_DIR/unique.out 2>$LOG_DIR/unique.err
grep UNIQUE $LOG_DIR/unique.err

echo "*****Wait for Fluo to finish processing*****"
$FLUO_HOME/bin/fluo wait $APP_NAME

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
