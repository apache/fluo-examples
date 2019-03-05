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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.accumulo.minicluster.MemoryUnit;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.minicluster.ServerType;
import org.apache.fluo.api.client.FluoAdmin.InitializationOptions;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.mini.MiniFluo;
import phrasecount.Application;

public class Mini {

  static class Parameters {
    @Parameter(names = {"-m", "--moreMemory"}, description = "Use more memory")
    boolean moreMemory = false;

    @Parameter(names = {"-w", "--workerThreads"}, description = "Number of worker threads")
    int workerThreads = 5;

    @Parameter(names = {"-t", "--tabletServers"}, description = "Number of tablet servers")
    int tabletServers = 2;

    @Parameter(names = {"-z", "--zookeeperPort"}, description = "Port to use for zookeeper")
    int zookeeperPort = 0;

    @Parameter(description = "<MAC dir> <output props file>")
    List<String> args;
  }

  public static void main(String[] args) throws Exception {

    Parameters params = new Parameters();
    JCommander jc = new JCommander(params);

    try {
      jc.parse(args);
      if (params.args == null || params.args.size() != 2) {
        throw new ParameterException("Expected two arguments");
      }
    } catch (ParameterException pe) {
      System.out.println(pe.getMessage());
      jc.setProgramName(Mini.class.getSimpleName());
      jc.usage();
      System.exit(-1);
    }

    MiniAccumuloConfig cfg = new MiniAccumuloConfig(new File(params.args.get(0)), "secret");
    cfg.setZooKeeperPort(params.zookeeperPort);
    cfg.setNumTservers(params.tabletServers);
    if (params.moreMemory) {
      cfg.setMemory(ServerType.TABLET_SERVER, 2, MemoryUnit.GIGABYTE);
      Map<String, String> site = new HashMap<>();
      site.put("tserver.cache.data.size", "768M");
      site.put("tserver.cache.index.size", "256M");
      cfg.setSiteConfig(site);
    }

    MiniAccumuloCluster cluster = new MiniAccumuloCluster(cfg);
    cluster.start();

    FluoConfiguration fluoConfig = new FluoConfiguration();

    fluoConfig.setMiniStartAccumulo(false);
    fluoConfig.setAccumuloInstance(cluster.getInstanceName());
    fluoConfig.setAccumuloUser("root");
    fluoConfig.setAccumuloPassword("secret");
    fluoConfig.setAccumuloZookeepers(cluster.getZooKeepers());
    fluoConfig.setInstanceZookeepers(cluster.getZooKeepers() + "/fluo");

    fluoConfig.setAccumuloTable("data");
    fluoConfig.setWorkerThreads(params.workerThreads);

    fluoConfig.setApplicationName("phrasecount");

    Application.configure(fluoConfig, new Application.Options(17, 17, cluster.getInstanceName(),
        cluster.getZooKeepers(), "root", "secret", "pcExport"));

    FluoFactory.newAdmin(fluoConfig).initialize(new InitializationOptions());

    MiniFluo miniFluo = FluoFactory.newMiniFluo(fluoConfig);

    miniFluo.getClientConfiguration().save(new File(params.args.get(1)));

    System.out.println();
    System.out.println("Wrote : " + params.args.get(1));
  }
}
