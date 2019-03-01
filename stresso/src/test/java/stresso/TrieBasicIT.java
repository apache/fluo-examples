/*
 * Copyright 2014 Stresso authors (see AUTHORS)
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
package stresso;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.client.LoaderExecutor;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.config.ObserverSpecification;
import org.apache.fluo.recipes.core.types.TypedSnapshot;
import org.apache.fluo.recipes.test.FluoITHelper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stresso.trie.Constants;
import stresso.trie.Node;
import stresso.trie.NodeObserver;
import stresso.trie.NumberLoader;

import static stresso.trie.Constants.COUNT_SEEN_COL;
import static stresso.trie.Constants.TYPEL;

/**
 * Tests Trie Stress Test using Basic Loader
 */
public class TrieBasicIT extends ITBase {

    private static final Logger log = LoggerFactory.getLogger(TrieBasicIT.class);

    @Override
    protected void preInit(FluoConfiguration conf) {
        conf.addObserver(new ObserverSpecification(NodeObserver.class.getName()));
        conf.getAppConfiguration().setProperty(Constants.STOP_LEVEL_PROP, 0);
    }

    @Test
    public void testBit32() throws Exception {
        runTrieTest(20, Integer.MAX_VALUE, 32);
    }

    @Test
    public void testBit8() throws Exception {
        runTrieTest(25, Integer.MAX_VALUE, 8);
    }

    @Test
    public void testBit4() throws Exception {
        runTrieTest(10, Integer.MAX_VALUE, 4);
    }

    @Test
    public void testBit() throws Exception {
        runTrieTest(5, Integer.MAX_VALUE, 1);
    }

    @Test
    public void testDuplicates() throws Exception {
        runTrieTest(20, 10, 4);
    }

    private void runTrieTest(int ingestNum, int maxValue, int nodeSize) {

        log.info("Ingesting " + ingestNum + " unique numbers with a nodeSize of " + nodeSize + " bits");

        config.setLoaderThreads(0);
        config.setLoaderQueueSize(0);

        try (FluoClient fluoClient = FluoFactory.newClient(config)) {

            int uniqueNum;

            try (LoaderExecutor le = client.newLoaderExecutor()) {
                Random random = new Random();
                Set<Integer> ingested = new HashSet<>();
                for (int i = 0; i < ingestNum; i++) {
                    int num = Math.abs(random.nextInt(maxValue));
                    le.execute(new NumberLoader(num, nodeSize));
                    ingested.add(num);
                }

                uniqueNum = ingested.size();
                log.info(
                        "Ingested " + uniqueNum + " unique numbers with a nodeSize of " + nodeSize + " bits");
            }

            miniFluo.waitForObservers();

            try (TypedSnapshot tsnap = TYPEL.wrap(client.newSnapshot())) {
                Integer result =
                        tsnap.get().row(Node.generateRootId(nodeSize)).col(COUNT_SEEN_COL).toInteger();
                if (result == null) {
                    log.error("Could not find root node");
                    FluoITHelper.printFluoTable(client);
                }
                if (!(result != null ? result.equals(uniqueNum) : false)) {
                    log.error(
                            "Count (" + result + ") at root node does not match expected (" + uniqueNum + "):");
                    FluoITHelper.printFluoTable(client);
                }
                Assert.assertEquals(uniqueNum, result.intValue());
            }
        }
    }
}
