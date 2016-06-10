/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fluo.stress.trie;

import java.io.File;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.client.Snapshot;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.config.ScannerConfiguration;
import org.apache.fluo.api.data.Bytes;
import org.apache.fluo.api.data.Column;
import org.apache.fluo.api.data.Span;
import org.apache.fluo.api.iterator.ColumnIterator;
import org.apache.fluo.api.iterator.RowIterator;

public class Print {

  public static class Stats {
    public long totalWait = 0;
    public long totalSeen = 0;
    public long nodes;
    public boolean sawOtherNodes = false;

    public Stats() {

    }

    public Stats(long tw, long ts, boolean son) {
      this.totalWait = tw;
      this.totalSeen = ts;
      this.sawOtherNodes = son;
    }

    public Stats(long tw, long ts, long nodes, boolean son) {
      this.totalWait = tw;
      this.totalSeen = ts;
      this.nodes = nodes;
      this.sawOtherNodes = son;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof Stats) {
        Stats os = (Stats) o;

        return totalWait == os.totalWait && totalSeen == os.totalSeen &&  sawOtherNodes == os.sawOtherNodes;
      }

      return false;
    }
  }

  public static Stats getStats(Configuration config) throws Exception {

    try (FluoClient client = FluoFactory.newClient(config); Snapshot snap = client.newSnapshot()) {

      int level = client.getAppConfiguration().getInt(Constants.STOP_LEVEL_PROP);
      int nodeSize = client.getAppConfiguration().getInt(Constants.NODE_SIZE_PROP);
      
      ScannerConfiguration scanConfig = new ScannerConfiguration();
      scanConfig.setSpan(Span.prefix(String.format("%02d:", level)));
      scanConfig.fetchColumn(Constants.COUNT_SEEN_COL.getFamily(), Constants.COUNT_SEEN_COL.getQualifier());
      scanConfig.fetchColumn(Constants.COUNT_WAIT_COL.getFamily(), Constants.COUNT_WAIT_COL.getQualifier());

      RowIterator rowIter = snap.get(scanConfig);

      long totalSeen = 0;
      long totalWait = 0;

      int otherNodeSizes = 0;
      
      long nodes = 0;
      
      while (rowIter.hasNext()) {
        Entry<Bytes,ColumnIterator> rowEntry = rowIter.next();
        String row = rowEntry.getKey().toString();
        Node node = new Node(row);

        if (node.getNodeSize() == nodeSize) {
          ColumnIterator colIter = rowEntry.getValue();
          while(colIter.hasNext()){
            Entry<Column,Bytes> colEntry = colIter.next();
            if(colEntry.getKey().equals(Constants.COUNT_SEEN_COL)){
              totalSeen += Long.parseLong(colEntry.getValue().toString());
            }else{
              totalWait += Long.parseLong(colEntry.getValue().toString());
            }
          }
          
          nodes++;
        } else {
          otherNodeSizes++;
        }
      }
      
      return new Stats(totalWait, totalSeen, nodes, otherNodeSizes != 0);
    }

  }

  public static void main(String[] args) throws Exception {

    if (args.length != 1) {
      System.err.println("Usage: " + Print.class.getSimpleName() + " <fluo props>");
      System.exit(-1);
    }
    
    Stats stats = getStats(new FluoConfiguration(new File(args[0])));

    System.out.println("Total at root : " + (stats.totalSeen + stats.totalWait));
    System.out.println("Nodes Scanned : " + stats.nodes);
    
    if (stats.sawOtherNodes) {
      System.err.println("WARN : Other node sizes were seen and ignored.");
    }
  }
}
