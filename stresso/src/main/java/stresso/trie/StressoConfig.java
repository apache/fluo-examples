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

import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.config.SimpleConfiguration;

public class StressoConfig {
  public final int nodeSize;
  public final int stopLevel;

  public StressoConfig(int nodeSize, int stopLevel) {
    this.nodeSize = nodeSize;
    this.stopLevel = stopLevel;
  }

  public static StressoConfig retrieve(FluoConfiguration fc) {
    try (FluoClient client = FluoFactory.newClient(fc)) {
      return retrieve(client);
    }
  }

  public static StressoConfig retrieve(FluoClient client) {
    SimpleConfiguration ac = client.getAppConfiguration();
    return new StressoConfig(ac.getInt(Constants.NODE_SIZE_PROP),
        ac.getInt(Constants.STOP_LEVEL_PROP));
  }
}
