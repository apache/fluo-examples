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

import java.util.Collections;
import java.util.List;

import org.apache.fluo.api.client.Snapshot;
import org.apache.fluo.api.config.ObserverSpecification;
import org.apache.fluo.api.config.SimpleConfiguration;
import org.apache.fluo.api.data.Bytes;
import org.junit.Assert;
import org.junit.Test;
import stresso.trie.Constants;
import stresso.trie.Node;
import stresso.trie.NodeObserver;

public class TrieStopLevelIT extends TrieMapRedIT {

  @Override
  protected List<ObserverSpecification> getObservers() {
    return Collections.singletonList(new ObserverSpecification(NodeObserver.class.getName()));
  }

  @Override
  protected void setAppConfig(SimpleConfiguration config) {
    config.setProperty(Constants.STOP_LEVEL_PROP, 7);
    config.setProperty(Constants.NODE_SIZE_PROP, 8);
  }

  @Test
  public void testEndToEnd() throws Exception {
    super.testEndToEnd();
    try (Snapshot snap = client.newSnapshot()) {
      Bytes row = Bytes.of(Node.generateRootId(8));
      Assert.assertNull(snap.get(row, Constants.COUNT_SEEN_COL));
      Assert.assertNull(snap.get(row, Constants.COUNT_WAIT_COL));
    }
  }
}
