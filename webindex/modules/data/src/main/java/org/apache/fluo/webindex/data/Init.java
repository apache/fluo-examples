/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.fluo.webindex.data;

import org.apache.fluo.webindex.core.WebIndexConfig;
import org.apache.fluo.webindex.core.models.Page;
import org.apache.fluo.webindex.data.spark.IndexEnv;
import org.apache.fluo.webindex.data.spark.IndexStats;
import org.apache.fluo.webindex.data.spark.IndexUtil;
import org.apache.fluo.webindex.data.util.WARCFileInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.archive.io.ArchiveReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Init {

  private static final Logger log = LoggerFactory.getLogger(Init.class);

  public static void main(String[] args) throws Exception {

    if (args.length > 1) {
      log.error("Usage: Init [<dataDir>]");
      System.exit(1);
    }
    WebIndexConfig webIndexConfig = WebIndexConfig.load();

    IndexEnv env = new IndexEnv(webIndexConfig);
    env.setFluoTableSplits();
    log.info("Initialized Fluo table splits");

    if (args.length == 1) {
      final String dataDir = args[0];
      IndexEnv.validateDataDir(dataDir);

      SparkConf sparkConf = new SparkConf().setAppName("webindex-init");
      try (JavaSparkContext ctx = new JavaSparkContext(sparkConf)) {
        IndexStats stats = new IndexStats(ctx);

        final JavaPairRDD<Text, ArchiveReader> archives = ctx.newAPIHadoopFile(dataDir,
            WARCFileInputFormat.class, Text.class, ArchiveReader.class, new Configuration());

        JavaRDD<Page> pages = IndexUtil.createPages(archives);

        env.initializeIndexes(ctx, pages, stats);

        stats.print();
      }
    } else {
      log.info("An init data dir was not specified");
    }
  }
}
