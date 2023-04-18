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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.minicluster.MiniAccumuloInstance;
import org.apache.commons.io.FileUtils;
import org.apache.fluo.api.client.FluoAdmin;
import org.apache.fluo.api.client.FluoAdmin.InitializationOptions;
import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.mini.MiniFluo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class ITBase {

  protected final static String USER = "root";
  protected final static String PASSWORD = "ITSecret";
  protected final static String TABLE_BASE = "table";
  protected final static String IT_INSTANCE_NAME_PROP =
      FluoConfiguration.FLUO_PREFIX + ".it.instance.name";
  protected final static String IT_INSTANCE_CLEAR_PROP =
      FluoConfiguration.FLUO_PREFIX + ".it.instance.clear";

  protected static String instanceName;
  protected static Connector conn;
  protected static Instance miniAccumulo;
  private static MiniAccumuloCluster cluster;
  private static boolean startedCluster = false;

  private static AtomicInteger tableCounter = new AtomicInteger(1);
  protected static final AtomicInteger testCounter = new AtomicInteger();

  protected FluoConfiguration config;
  protected FluoClient client;
  protected MiniFluo miniFluo;

  @BeforeClass
  public static void setUpAccumulo() throws Exception {
    instanceName = System.getProperty(IT_INSTANCE_NAME_PROP, "it-instance-default");
    File instanceDir = new File("target/accumulo2-maven-plugin/" + instanceName);
    boolean instanceClear =
        System.getProperty(IT_INSTANCE_CLEAR_PROP, "true").equalsIgnoreCase("true");
    if (instanceDir.exists() && instanceClear) {
      FileUtils.deleteDirectory(instanceDir);
    }
    if (!instanceDir.exists()) {
      MiniAccumuloConfig cfg = new MiniAccumuloConfig(instanceDir, PASSWORD);
      cfg.setInstanceName(instanceName);
      cluster = new MiniAccumuloCluster(cfg);
      cluster.start();
      startedCluster = true;
    }
    miniAccumulo = new MiniAccumuloInstance(instanceName, instanceDir);
    conn = miniAccumulo.getConnector(USER, new PasswordToken(PASSWORD));
  }


  @AfterClass
  public static void tearDownAccumulo() throws Exception {
    if (startedCluster) {
      cluster.stop();
    }
  }

  protected void preInit(FluoConfiguration config) {}

  public String getCurTableName() {
    return TABLE_BASE + tableCounter.get();
  }

  public String getNextTableName() {
    return TABLE_BASE + tableCounter.incrementAndGet();
  }

  @Before
  public void setUpFluo() throws Exception {

    config = new FluoConfiguration();
    config.setApplicationName("mini-test" + testCounter.getAndIncrement());
    config.setAccumuloInstance(miniAccumulo.getInstanceName());
    config.setAccumuloUser(USER);
    config.setAccumuloPassword(PASSWORD);
    config.setAccumuloZookeepers(miniAccumulo.getZooKeepers());
    config.setInstanceZookeepers(miniAccumulo.getZooKeepers() + "/fluo");
    config.setMiniStartAccumulo(false);
    config.setAccumuloTable(getNextTableName());
    config.setWorkerThreads(5);
    preInit(config);

    config.setTransactionRollbackTime(1, TimeUnit.SECONDS);

    try (FluoAdmin admin = FluoFactory.newAdmin(config)) {
      InitializationOptions opts =
          new InitializationOptions().setClearZookeeper(true).setClearTable(true);
      admin.initialize(opts);
    }

    config.getAppConfiguration().clear();

    client = FluoFactory.newClient(config);
    miniFluo = FluoFactory.newMiniFluo(config);
  }

  @After
  public void tearDownFluo() throws Exception {
    miniFluo.close();
    client.close();
  }
}
