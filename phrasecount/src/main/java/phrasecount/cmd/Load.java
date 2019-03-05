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
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;
import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.client.LoaderExecutor;
import org.apache.fluo.api.config.FluoConfiguration;
import phrasecount.DocumentLoader;
import phrasecount.pojos.Document;

public class Load {

  public static void main(String[] args) throws Exception {

    if (args.length != 2) {
      System.err.println("Usage : " + Load.class.getName() + " <fluo props file> <txt file dir>");
      System.exit(-1);
    }

    FluoConfiguration config = new FluoConfiguration(new File(args[0]));
    config.setLoaderThreads(20);
    config.setLoaderQueueSize(40);

    try (FluoClient fluoClient = FluoFactory.newClient(config);
        LoaderExecutor le = fluoClient.newLoaderExecutor()) {
      File[] files = new File(args[1]).listFiles();

      if (files == null) {
        System.out.println("Text file dir does not exist: " + args[1]);
      } else {
        for (File txtFile : files) {
          if (txtFile.getName().endsWith(".txt")) {
            String uri = txtFile.toURI().toString();
            String content = Files.toString(txtFile, StandardCharsets.UTF_8);

            System.out.println("Processing : " + txtFile.toURI());
            le.execute(new DocumentLoader(new Document(uri, content)));
          } else {
            System.out.println("Ignoring : " + txtFile.toURI());
          }
        }
      }
    }

    // TODO figure what threads are hanging around
    System.exit(0);
  }
}
