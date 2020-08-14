<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Stresso

An example application designed to stress Apache Fluo.  This Fluo application computes the
number of unique integers through the process of building a bitwise trie.  New numbers
are added to the trie as leaf nodes.  Observers watch all nodes in the trie to create
parents and percolate counts up to the root nodes such that each node in the trie keeps
track of the number of leaf nodes below it. The count at the root nodes should equal
the total number of leaf nodes.  This makes it easy to verify if the test ran correctly.
The test stresses Apache Fluo in that multiple transactions can operate on the same data
as counts are percolated up the trie.

## Concepts and definitions

This test has the following set of configurable parameters.

 * **nodeSize** : The number of bits chopped off the end each time a number is
   percolated up.  Must choose a nodeSize such that `64 % nodeSize == 0`
 * **stopLevel** : The number of levels in the tree is a function of the
   nodeSize.  The deepest possible level is `64 / nodeSize`.  Levels are
   decremented going up the tree.  Setting the stop level determines how far up
   to percolate.  The lower the stop level, the more root nodes there are.
   Having more root nodes means less collisions, but all roots need to be
   scanned to get the count of unique numbers.  Having ~64k root nodes is a
   good choice.
 * **max** : Random numbers are generated modulo the max.

Setting the stop level such that you have ~64k root nodes is dependent on the
max and nodeSize.  For example assume we choose a max of 10<sup>12</sup> and a
node size of 8.  The following table shows information about each level in the
tree using this configuration.  So for a max of 10<sup>12</sup> choosing a stop
level of 5 would result in 59,604 root nodes.  With this many root nodes there
would not be many collisions and scanning 59,604 nodes to compute the unique
number of intergers is a quick operation.

|Level|Max Node             |Number of possible Nodes|
|:---:|---------------------|-----------------------:|
|  0  |`0xXXXXXXXXXXXXXXXX` |                 1      |
|  1  |`0x00XXXXXXXXXXXXXX` |                 1      |
|  2  |`0x0000XXXXXXXXXXXX` |                 1      |
|  3  |`0x000000XXXXXXXXXX` |                 1      |
|  4  |`0x000000E8XXXXXXXX` |               232      |
|  5  |`0x000000E8D4XXXXXX` |            59,604      |
|  6  |`0x000000E8D4A5XXXX` |        15,258,789      |
|  7  |`0x000000E8D4A510XX` |     3,906,250,000      |
|  8  |`0x000000E8D4A51000` | 1,000,000,000,000      |

In the table above, X indicates nibbles that are always zeroed out for every
node at that level.  You can easily view nodes at a level using a row prefix
with the fluo scan command.  For example `fluo scan -p 05` shows all nodes at
level 5.

For small scale test a max of 10<sup>9</sup> and a stop level of 6 is a good
choice.

## Building Stresso

```
mvn package
```

This will create a jar and shaded jar in target:

```
$ ls target/stresso-*
target/stresso-0.0.1-SNAPSHOT.jar  target/stresso-0.0.1-SNAPSHOT-shaded.jar
```

## Run Stresso using MiniFluo

There are several integration tests that run Stresso on a MiniFluo instance.
These tests can be run using `mvn verify`.

## Run Stresso on cluster

The [bin directory](/stresso/bin) contains a set of scripts to help run this test on a
cluster.  These scripts make the following assumpitions.

 * `FLUO_HOME` environment variable is set.  If not set, then set it in `conf/env.sh`.
 * Hadoop `yarn` command is on path.
 * Hadoop `hadoop` command is on path.
 * Accumulo `accumulo` command is on path.

Copy [conf/env.sh.example](/stresso/conf/env.sh.example) and
[conf/fluo-app.properties.example](/stresso/conf/fluo-app.properties.example)
to `conf/env.sh` and `conf/fluo-app.properties`, then inspect and modify these
files. Then initialize using the following commands.

```bash
# populate the lib dir needed by init
./bin/build.sh

# initialize the stresso Fluo application
fluo init -a stresso -p conf/fluo-app.properties
```

After initialization the Fluo application needs to be started.  There are many possible ways to
do this.  The following commands will start it locally.

```bash
mkdir -p logs
fluo oracle -a stresso &> logs/oracle.log &
fluo worker -a stresso &> logs/worker.log &
```

Next, execute the [run-test.sh](/stresso/bin/run-test.sh) script.
This script loads a lot of data directly into Accumulo without transactions and then
incrementally loads smaller amounts of data using transactions.  After incrementally
loading some data, it computes the expected number of unique integers using map reduce.
It then prints the number of unique integers computed by Apache Fluo.

## Additional Scripts

The script [generate.sh](/stresso/bin/generate.sh) starts a map reduce job to generate
random integers.

```
generate.sh <num files> <num per file> <max> <out dir>

where:

num files = Number of files to generate (and number of map task)
numPerMap = Number of random numbers to generate per file
max       = Generate random numbers between 0 and max
out dir   = Output directory
```

The script [split.sh](/stresso/bin/split.sh) pre-splits the Accumulo table used by Apache
Fluo.  Consider running this command before loading data.

```
split.sh <num tablets> <max>

where:

num tablets = Num tablets to create for lowest level of tree.  Will create less tablets for higher levels based on the max.
```
After generating random numbers, load them into Apache Fluo with one of the following
commands.  The script [bulk_load.sh](/stresso/bin/bulk_load.sh) intializes any empty table using
map reduce.  This simulates the case where a user has a lot of initial data to
load into Fluo.  This command should only be run when the table is empty
because it writes directly to the Fluo table w/o using transactions.

```
init.sh <input dir> <tmp dir> <num reducers>

where:

input dir    = A directory with file created by stresso.trie.Generate
node size    = Size of node in bits which must be a divisor of 32/64
tmp dir      = This command runs two map reduce jobs and needs an intermediate directory to store data.
num reducers = Number of reduce task map reuduce job should run
```

Run the [load.sh](/stresso/bin/load.sh) script on a table with existing data. It starts
a map reduce job that executes load transactions.  Loading the same directory
multiple times should not result in incorrect counts.

```
load.sh <input dir>
```

After loading data, run the [print.sh](/stresso/bin/print.sh) script to check the
status of the computation of the number of unique integers within Apache Fluo.  This
command will print two numbers, the sum of the root nodes and number of root
nodes.  If there are outstanding notification to process, this count may not be
accurate.

```
print.sh
```

In order to know how many unique numbers are expected, run the [unique.sh](/stresso/bin/unique.sh)
script.  This scrpt runs a map reduce job that calculates the number of
unique integers.  This script can take a list of directories created by
multiple runs of [generate.sh](/stresso/bin/generate.sh)

```
unique.sh <num reducers> <input dir>{ <input dir>}
```

As transactions execute they leave a trail of history behind.  The nodes in the
lower levels of the tree are updated by many transactions and therefore have a
long history trail.  A long transactional history can slow down transactions.
Forcing a compaction in Accumulo will clean up this history.  However
compacting the entire table is expensive.  To avoid this expense, compact only the
lower levels of the tree.  The following command will compact levels of the
tree with a maximum number of nodes less than the specified cutoff.

```
compact-ll.sh <max> <cutoff>
```

where:

```
cutoff    = Any level of the tree with a maximum number of nodes that is less than this cutoff will be compacted.
```
