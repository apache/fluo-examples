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

import com.google.common.collect.Iterables;
import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.client.Snapshot;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.data.Column;
import org.apache.fluo.api.data.Span;
import phrasecount.Constants;
import phrasecount.pojos.PhraseAndCounts;
import phrasecount.query.PhraseCountTable;

public class Print {

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err
          .println("Usage : " + Print.class.getName() + " <fluo props file> <export table name>");
      System.exit(-1);
    }

    FluoConfiguration fluoConfig = new FluoConfiguration(new File(args[0]));

    PhraseCountTable pcTable = new PhraseCountTable(fluoConfig, args[1]);
    for (PhraseAndCounts phraseCount : pcTable) {
      System.out.printf("%7d %7d '%s'\n", phraseCount.docPhraseCount, phraseCount.totalPhraseCount,
          phraseCount.phrase);
    }

    try (FluoClient fluoClient = FluoFactory.newClient(fluoConfig);
        Snapshot snap = fluoClient.newSnapshot()) {

      // TODO could precompute this using observers
      int uriCount = count(snap, "uri:", Constants.DOC_HASH_COL);
      int documentCount = count(snap, "doc:", Constants.DOC_REF_COUNT_COL);
      int numIndexedDocs = count(snap, "doc:", Constants.INDEX_STATUS_COL);

      System.out.println();
      System.out.printf("# uris                : %,d\n", uriCount);
      System.out.printf("# unique documents    : %,d\n", documentCount);
      System.out.printf("# processed documents : %,d\n", numIndexedDocs);
      System.out.println();
    }

    // TODO figure what threads are hanging around
    System.exit(0);
  }

  private static int count(Snapshot snap, String prefix, Column col) {
    return Iterables.size(snap.scanner().over(Span.prefix(prefix)).fetch(col).byRow().build());
  }
}
