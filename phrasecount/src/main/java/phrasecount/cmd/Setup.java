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

package phrasecount.cmd;

import java.io.File;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.fluo.api.config.FluoConfiguration;
import phrasecount.Application;
import phrasecount.Application.Options;

public class Setup {

  public static void main(String[] args) throws Exception {
    FluoConfiguration config = new FluoConfiguration(new File(args[0]));

    String exportTable = args[1];

    Connector conn =
        new ZooKeeperInstance(config.getAccumuloInstance(), config.getAccumuloZookeepers())
            .getConnector("root", new PasswordToken("secret"));
    try {
      conn.tableOperations().delete(exportTable);
    } catch (TableNotFoundException e) {
      // ignore if table not found
    }

    conn.tableOperations().create(exportTable);

    Options opts =
        new Options(103, 103, config.getAccumuloInstance(), config.getAccumuloZookeepers(),
            config.getAccumuloUser(), config.getAccumuloPassword(), exportTable);

    FluoConfiguration observerConfig = new FluoConfiguration();
    Application.configure(observerConfig, opts);
    observerConfig.save(System.out);
  }
}
