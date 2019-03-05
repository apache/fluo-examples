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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Strings;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.hadoop.io.Text;

public class Split {

  private static final String RGB_CLASS =
      "org.apache.accumulo.server.master.balancer.RegexGroupBalancer";
  private static final String RGB_PATTERN_PROP = "table.custom.balancer.group.regex.pattern";
  private static final String RGB_DEFAULT_PROP = "table.custom.balancer.group.regex.default";
  private static final String TABLE_BALANCER_PROP = "table.balancer";

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.err.println("Usage: " + Split.class.getSimpleName()
          + " <fluo conn props> <app name> <table props> <tablets per level>");
      System.exit(-1);
    }

    FluoConfiguration config = new FluoConfiguration(new File(args[0]));
    config.setApplicationName(args[1]);

    int maxTablets = Integer.parseInt(args[3]);

    StressoConfig sconf = StressoConfig.retrieve(config);

    AccumuloUtil.doTableOp(config, (tableOps, table) -> {
      setupBalancer(tableOps, table);

      int level = 64 / sconf.nodeSize;

      while (level >= sconf.stopLevel) {
        int numTablets = maxTablets;
        if (numTablets == 0) {
          break;
        }

        TreeSet<Text> splits = genSplits(level, numTablets);
        tableOps.addSplits(table, splits);
        System.out.printf("Added %d tablets for level %d\n", numTablets, level);

        level--;
      }

      optimizeAccumulo(tableOps, table, args[2]);
    });
  }

  private static void optimizeAccumulo(TableOperations tableOps, String table, String tableProps)
      throws Exception {

    Properties tprops = new Properties();
    tprops.load(new ByteArrayInputStream(tableProps.getBytes(StandardCharsets.UTF_8)));

    Set<Entry<Object, Object>> es = tprops.entrySet();
    for (Entry<Object, Object> e : es) {
      tableOps.setProperty(table, e.getKey().toString(), e.getValue().toString());
    }
    try {
      tableOps.setProperty(table, "table.durability", "flush");
      tableOps.removeProperty("accumulo.metadata", "table.durability");
      tableOps.removeProperty("accumulo.root", "table.durability");
    } catch (AccumuloException e) {
      System.err.println(
          "Unable to set durability settings (error expected in Accumulo 1.6) : " + e.getMessage());
    }
  }

  private static void setupBalancer(TableOperations tableOps, String table)
      throws AccumuloSecurityException, AccumuloException {
    tableOps.setProperty(table, RGB_PATTERN_PROP, "(\\d\\d).*");
    tableOps.setProperty(table, RGB_DEFAULT_PROP, "none");
    tableOps.setProperty(table, TABLE_BALANCER_PROP, RGB_CLASS);
    System.out.println("Setup tablet group balancer");
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

    splits.add(new Text(ls + "~"));

    return splits;
  }
}
