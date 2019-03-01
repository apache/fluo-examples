/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stresso.trie;

import java.io.File;

import org.apache.accumulo.core.client.Connector;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.hadoop.io.Text;

/**
 * Compact the lower levels of the tree. The lower levels of the tree contain a small of nodes that
 * are frequently updated. Compacting these lower levels is a quick operation that cause the Fluo GC
 * iterator to cleanup past transactions.
 */

public class CompactLL {
  public static void main(String[] args) throws Exception {

    if (args.length != 4) {
      System.err.println(
          "Usage: " + Split.class.getSimpleName() + " <fluo conn props> <app name> <max> <cutoff>");
      System.exit(-1);
    }

    FluoConfiguration config = new FluoConfiguration(new File(args[0]));
    config.setApplicationName(args[1]);

    long max = Long.parseLong(args[2]);

    // compact levels that can contain less nodes than this
    int cutoff = Integer.parseInt(args[3]);

    StressoConfig sconf = StressoConfig.retrieve(config);

    int level = 64 / sconf.nodeSize;

    while (level >= sconf.stopLevel) {
      if (max < cutoff) {
        break;
      }

      max = max >> 8;
      level--;
    }

    String start = String.format("%02d", sconf.stopLevel);
    String end = String.format("%02d:~", (level));

    System.out.println("Compacting " + start + " to " + end);
    AccumuloUtil.doTableOp(config,
        (tableOps, table) -> tableOps.compact(table, new Text(start), new Text(end), true, false));
    System.exit(0);
  }
}
