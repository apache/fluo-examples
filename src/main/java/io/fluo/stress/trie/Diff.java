package io.fluo.stress.trie;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

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

public class Diff {
  public static Map<String, Long> getRootCount(FluoClient client, Snapshot snap, int level,
      int stopLevel, int nodeSize) throws Exception {
    ScannerConfiguration scanConfig = new ScannerConfiguration();
    scanConfig.setSpan(Span.prefix(String.format("%02d:", level)));
    scanConfig.fetchColumn(Constants.COUNT_SEEN_COL.getFamily(),
        Constants.COUNT_SEEN_COL.getQualifier());
    scanConfig.fetchColumn(Constants.COUNT_WAIT_COL.getFamily(),
        Constants.COUNT_WAIT_COL.getQualifier());

    RowIterator rowIter = snap.get(scanConfig);

    HashMap<String, Long> counts = new HashMap<>();

    while (rowIter.hasNext()) {
      Entry<Bytes, ColumnIterator> rowEntry = rowIter.next();
      String row = rowEntry.getKey().toString();
      Node node = new Node(row);

      while (node.getLevel() > stopLevel) {
        node = node.getParent();
      }

      String stopRow = node.getRowId();
      long count = counts.getOrDefault(stopRow, 0L);

      if (node.getNodeSize() == nodeSize) {
        ColumnIterator colIter = rowEntry.getValue();
        while (colIter.hasNext()) {
          Entry<Column, Bytes> colEntry = colIter.next();
          count += Long.parseLong(colEntry.getValue().toString());
        }
      } else {
        throw new RuntimeException("TODO");
      }

      counts.put(stopRow, count);
    }

    return counts;
  }

  public static void main(String[] args) throws Exception {

    if (args.length != 1) {
      System.err.println("Usage: " + Print.class.getSimpleName() + " <fluo props>");
      System.exit(-1);
    }

    FluoConfiguration config = new FluoConfiguration(new File(args[0]));

    try (FluoClient client = FluoFactory.newClient(config); Snapshot snap = client.newSnapshot()) {

      int stopLevel = client.getAppConfiguration().getInt(Constants.STOP_LEVEL_PROP);
      int nodeSize = client.getAppConfiguration().getInt(Constants.NODE_SIZE_PROP);

      Map<String, Long> rootCounts = getRootCount(client, snap, stopLevel, stopLevel, nodeSize);
      ArrayList<String> rootRows = new ArrayList<>(rootCounts.keySet());
      Collections.sort(rootRows);

      // TODO 8
      for (int level = stopLevel + 1; level <= 8; level++) {
        System.out.printf("Level %d:\n",level);

        Map<String, Long> counts = getRootCount(client, snap, level, stopLevel, nodeSize);

        long sum = 0;

        for (String row : rootRows) {
          long c1 = rootCounts.get(row);
          long c2 = counts.getOrDefault(row, -1L);

          if (c1 != c2) {
            System.out.printf("\tdiff: %s %d %d\n", row, c1, c2);
          }

          if(c2 > 0){
            sum += c2;
          }
        }

        HashSet<String> extras = new HashSet<>(counts.keySet());
        extras.removeAll(rootCounts.keySet());

        for (String row : extras) {
          long c = counts.get(row);
          System.out.printf("\textra: %s %d\n", row, c);
        }

        System.out.println("\tsum "+sum);
      }
    }
  }
}
