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

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.fluo.api.client.FluoAdmin;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;

public class AccumuloUtil {

  public interface TableOp<T> {
    T run(TableOperations tableOps, String tableName) throws Exception;
  }

  public interface VoidTableOp {
    void run(TableOperations tableOps, String tableName) throws Exception;
  }

  public static void doTableOp(FluoConfiguration fc, VoidTableOp tableOp) {
    getTableOp(fc, (to, tn) -> {
      tableOp.run(to, tn);
      return null;
    });
  }

  public static <T> T getTableOp(FluoConfiguration fc, TableOp<T> tableOp) {
    try (FluoAdmin fadmin = FluoFactory.newAdmin(fc)) {
      FluoConfiguration appCfg = new FluoConfiguration(fadmin.getApplicationConfig());
      appCfg.setApplicationName(fc.getApplicationName());
      try (AccumuloClient client =
          Accumulo.newClient().to(appCfg.getAccumuloInstance(), appCfg.getAccumuloZookeepers())
              .as(appCfg.getAccumuloUser(), appCfg.getAccumuloPassword()).build()) {
        return tableOp.run(client.tableOperations(), appCfg.getAccumuloTable());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
