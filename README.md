
Fluo Stress
===========

[![Build Status](https://travis-ci.org/fluo-io/fluo-stress.svg?branch=master)](https://travis-ci.org/fluo-io/fluo-stress)

An example application designed to stress Fluo.  This Fluo application computes the 
number of unique integers through the process of building a bitwise trie.  New numbers
are added to the trie as leaf nodes.  Observers watch all nodes in the trie to create 
parents and percolate counts up to the root nodes such that each node in the trie keeps
track of the number of leaf nodes below it. The count at the root nodes should equal 
the total number of leaf nodes.  This makes it easy to verify if the test ran correctly. 
The test stresses Fluo in that multiple transactions can operate on the same data as 
counts are percolated up the trie.

Concepts and definitions
------------------------

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

Building stress test
--------------------

```
mvn package 
```

This will create a jar in target:

```
$ ls target/fluo-stress-*
target/fluo-stress-0.0.1-SNAPSHOT.jar  
```

Run trie stress test using Mini Fluo
----------------------------------------

There are several integration tests that run the trie stress test on a MiniFluo instance.
These tests can be run using `mvn verify`.

Run trie stress test on cluster
-------------------------------

The [bin directory](/bin) contains a set of scripts to help run this test on a
cluster.  These scripts make the following assumpitions.

 * `FLUO_HOME` environment variable is set.  If not set, then set it in `conf/env.sh`.
 * Hadoop `yarn` command is on path.
 * Hadoop `hadoop` command is on path.

Before running any of the scipts, copy [conf/env.sh.example](/conf/env.sh.example) 
to `conf/env.sh`, then inspect and modify the file.

Next, execute the [run-test.sh](/bin/run-test.sh) script.  This script will create a
new fluo app called `stress` (which can be changed by `FLUO_APP_NAME` in your env.sh). 
It will modify the application's fluo.properties, copy the stress jar to the `lib/` 
directory of the app and set the following in fluo.properties:

```
io.fluo.observer.0=io.fluo.stress.trie.NodeObserver
io.fluo.app.trie.nodeSize=X
io.fluo.app.trie.stopLevel=Y
```

The `run-test.sh` script will then initialize and start the Fluo 'stress' application.  
It will load a lot of data directly into Accumulo without transactions and then 
incrementally load smaller amounts of data using transactions.  After incrementally 
loading some data, it computes the expected number of unique integers using map reduce.
It then prints the number of unique integers computed by Fluo. 

Additional Scripts
------------------

The script [generate.sh](/bin/generate.sh) starts a map reduce job to generate
random integers.

```
generate.sh <num files> <num per file> <max> <out dir>

where:

num files = Number of files to generate (and number of map task)
numPerMap = Number of random numbers to generate per file
max       = Generate random numbers between 0 and max
out dir   = Output directory
```

The script [split.sh](/bin/split.sh) pre-splits the Accumulo table used by
Fluo.  Consider running this command before loading data.

```
split.sh <num tablets> <max>

where:

num tablets = Num tablets to create for lowest level of tree.  Will create less tablets for higher levels based on the max.
```
After generating random numbers, load them into Fluo with one of the following
commands.  The script [init.sh](/bin/init.sh) intializes any empty table using
map reduce.  This simulates the case where a user has a lot of initial data to
load into Fluo.  This command should only be run when the table is empty
because it writes directly to the Fluo table w/o using transactions.  

```
init.sh <input dir> <tmp dir> <num reducers>

where:

input dir    = A directory with file created by io.fluo.stress.trie.Generate
node size    = Size of node in bits which must be a divisor of 32/64
tmp dir      = This command runs two map reduce jobs and needs an intermediate directory to store data.
num reducers = Number of reduce task map reuduce job should run
```

Run the [load.sh](/bin/load.sh) script on a table with existing data. It starts
a map reduce job that executes load transactions.  Loading the same directory
multiple times should not result in incorrect counts.

```
load.sh <input dir>
```

After loading data, run the [print.sh](/bin/print.sh) script to check the
status of the computation of the number of unique integers within Fluo.  This
command will print two numbers, the sum of the root nodes and number of root
nodes.  If there are outstanding notification to process, this count may not be
accurate.

```
print.sh
```

In order to know how many unique numbers are expected, run the [unique.sh](/bin/unique.sh)
script.  This scrpt runs a map reduce job that calculates the number of
unique integers.  This script can take a list of directories created by
multiple runs of [generate.sh](/bin/generate.sh)

```
unique.sh <num reducers> <input dir>{ <input dir>}
```

