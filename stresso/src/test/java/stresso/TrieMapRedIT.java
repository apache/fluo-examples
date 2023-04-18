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

package stresso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.config.ObserverSpecification;
import org.apache.fluo.api.config.SimpleConfiguration;
import org.apache.fluo.api.mini.MiniFluo;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import stresso.trie.Constants;
import stresso.trie.Generate;
import stresso.trie.Init;
import stresso.trie.Load;
import stresso.trie.NodeObserver;
import stresso.trie.Print;
import stresso.trie.StressoObserverProvider;
import stresso.trie.Unique;

/**
 * Tests Trie Stress Test using MapReduce Ingest
 */
public class TrieMapRedIT extends ITBase {

  @Override
  protected void preInit(FluoConfiguration conf) {
    conf.setObserverProvider(StressoObserverProvider.class);

    SimpleConfiguration appCfg = conf.getAppConfiguration();
    appCfg.setProperty(Constants.STOP_LEVEL_PROP, 0);
    appCfg.setProperty(Constants.NODE_SIZE_PROP, 8);
  }

  static void generate(int numMappers, int numPerMapper, int max, File out1) throws Exception {
    int ret = ToolRunner.run(new Generate(),
        new String[] {"-D", "mapred.job.tracker=local", "-D", "fs.defaultFS=file:///",
            "" + numMappers, numPerMapper + "", max + "", out1.toURI().toString()});
    Assert.assertEquals(0, ret);
  }

  static void load(int nodeSize, File fluoPropsFile, String appName, File input) throws Exception {
    int ret = ToolRunner.run(new Load(),
        new String[] {"-D", "mapred.job.tracker=local", "-D", "fs.defaultFS=file:///",
            fluoPropsFile.getAbsolutePath(), appName, input.toURI().toString()});
    Assert.assertEquals(0, ret);
  }

  static void init(int nodeSize, File fluoPropsFile, String appName, File input, File tmp)
      throws Exception {
    int ret = ToolRunner.run(new Init(),
        new String[] {"-D", "mapred.job.tracker=local", "-D", "fs.defaultFS=file:///",
            fluoPropsFile.getAbsolutePath(), appName, input.toURI().toString(),
            tmp.toURI().toString()});
    Assert.assertEquals(0, ret);
  }

  static int unique(File... dirs) throws Exception {

    ArrayList<String> args = new ArrayList<>(
        Arrays.asList("-D", "mapred.job.tracker=local", "-D", "fs.defaultFS=file:///"));
    for (File dir : dirs) {
      args.add(dir.toURI().toString());
    }

    int ret = ToolRunner.run(new Unique(), args.toArray(new String[args.size()]));
    Assert.assertEquals(0, ret);
    return Unique.getNumUnique();
  }

  @Test
  public void testEndToEnd() throws Exception {
    File testDir = new File("target/MRIT");
    FileUtils.deleteQuietly(testDir);
    Assert.assertTrue(testDir.mkdirs());
    File fluoPropsFile = new File(testDir, "fluo.props");

    config.save(fluoPropsFile);
    String appName = config.getApplicationName();

    File out1 = new File(testDir, "nums-1");

    generate(2, 100, 500, out1);
    init(8, fluoPropsFile, appName, out1, new File(testDir, "initTmp"));
    int ucount = unique(out1);

    Assert.assertTrue(ucount > 0);

    miniFluo.waitForObservers();

    Assert.assertEquals(new Print.Stats(0, ucount, false), Print.getStats(config));

    // reload same data
    load(8, fluoPropsFile, appName, out1);

    miniFluo.waitForObservers();

    Assert.assertEquals(new Print.Stats(0, ucount, false), Print.getStats(config));

    // load some new data
    File out2 = new File(testDir, "nums-2");
    generate(2, 100, 500, out2);
    load(8, fluoPropsFile, appName, out2);
    int ucount2 = unique(out1, out2);
    Assert.assertTrue(ucount2 > ucount); // used > because the probability that no new numbers are
                                         // chosen is exceedingly small

    miniFluo.waitForObservers();

    Assert.assertEquals(new Print.Stats(0, ucount2, false), Print.getStats(config));

    File out3 = new File(testDir, "nums-3");
    generate(2, 100, 500, out3);
    load(8, fluoPropsFile, appName, out3);
    int ucount3 = unique(out1, out2, out3);
    Assert.assertTrue(ucount3 > ucount2); // used > because the probability that no new numbers are
                                          // chosen is exceedingly small

    miniFluo.waitForObservers();

    Assert.assertEquals(new Print.Stats(0, ucount3, false), Print.getStats(config));
  }
}
