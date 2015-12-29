/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.fluo.stress.trie;

import java.io.File;
import java.util.TreeSet;

import com.google.common.base.Strings;
import io.fluo.api.client.FluoClient;
import io.fluo.api.client.FluoFactory;
import io.fluo.api.config.FluoConfiguration;
import io.fluo.core.util.AccumuloUtil;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.hadoop.io.Text;

public class Split {

  private static final String RGB_CLASS =
      "org.apache.accumulo.server.master.balancer.RegexGroupBalancer";
  private static final String RGB_PATTERN_PROP = "table.custom.balancer.group.regex.pattern";
  private static final String RGB_DEFAULT_PROP = "table.custom.balancer.group.regex.default";
  private static final String TABLE_BALANCER_PROP = "table.balancer";

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err
          .println("Usage: " + Split.class.getSimpleName() + " <fluo props> <tablets per level>");
      System.exit(-1);
    }

    FluoConfiguration config = new FluoConfiguration(new File(args[0]));

    int maxTablets = Integer.parseInt(args[1]);

    int nodeSize;
    int stopLevel;
    try (FluoClient client = FluoFactory.newClient(config)) {
      nodeSize = client.getAppConfiguration().getInt(Constants.NODE_SIZE_PROP);
      stopLevel = client.getAppConfiguration().getInt(Constants.STOP_LEVEL_PROP);
    }

    setupBalancer(config);

    int level = 64 / nodeSize;

    while (level >= stopLevel) {
      int numTablets = maxTablets;
      if (numTablets == 0)
        break;

      TreeSet<Text> splits = genSplits(level, numTablets);
      addSplits(config, splits);
      System.out.printf("Added %d tablets for level %d\n", numTablets, level);

      level--;
    }
  }

  private static void setupBalancer(FluoConfiguration config) throws AccumuloSecurityException {
    Connector conn = AccumuloUtil.getConnector(config);

    try {
      // setting this prop first intentionally because it should fail in 1.6
      conn.tableOperations().setProperty(config.getAccumuloTable(), RGB_PATTERN_PROP, "(\\d\\d).*");
      conn.tableOperations().setProperty(config.getAccumuloTable(), RGB_DEFAULT_PROP, "none");
      conn.tableOperations().setProperty(config.getAccumuloTable(), TABLE_BALANCER_PROP, RGB_CLASS);
      System.out.println("Setup tablet group balancer");
    } catch (AccumuloException e) {
      System.err
          .println("Unable to setup tablet balancer (error expected in 1.6) : " + e.getMessage());
    }
  }

  private static TreeSet<Text> genSplits(int level, int numTablets) {

    TreeSet<Text> splits = new TreeSet<>();

    String ls = String.format("%02d:", level);

    int numSplits = numTablets - 1;
    int distance = (((int) Math.pow(Character.MAX_RADIX, Node.HASH_LEN) - 1) / numTablets) + 1;
    int split = distance;
    for (int i = 0; i < numSplits; i++) {
      splits.add(new Text(
          ls + Strings.padStart(Integer.toString(split, Character.MAX_RADIX), Node.HASH_LEN, '0')));
      split += distance;
    }

    splits.add(new Text(ls+"~"));

    return splits;
  }

  private static void addSplits(FluoConfiguration config, TreeSet<Text> splits) throws Exception {
    Connector conn = AccumuloUtil.getConnector(config);
    conn.tableOperations().addSplits(config.getAccumuloTable(), splits);
  }
}
