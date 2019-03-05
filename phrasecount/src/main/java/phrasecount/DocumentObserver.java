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

package phrasecount;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.fluo.api.client.TransactionBase;
import org.apache.fluo.api.data.Bytes;
import org.apache.fluo.api.data.Column;
import org.apache.fluo.api.observer.AbstractObserver;
import org.apache.fluo.recipes.core.map.CollisionFreeMap;
import org.apache.fluo.recipes.core.types.TypedTransactionBase;
import phrasecount.pojos.Counts;
import phrasecount.pojos.Document;

import static phrasecount.Constants.DOC_CONTENT_COL;
import static phrasecount.Constants.DOC_REF_COUNT_COL;
import static phrasecount.Constants.INDEX_CHECK_COL;
import static phrasecount.Constants.INDEX_STATUS_COL;
import static phrasecount.Constants.PCM_ID;
import static phrasecount.Constants.TYPEL;

/**
 * An Observer that updates phrase counts when a document is added or removed.
 */
public class DocumentObserver extends AbstractObserver {

  private CollisionFreeMap<String, Counts> pcMap;

  private enum IndexStatus {
    INDEXED, UNINDEXED
  }

  @Override
  public void init(Context context) throws Exception {
    pcMap = CollisionFreeMap.getInstance(PCM_ID, context.getAppConfiguration());
  }

  @Override
  public void process(TransactionBase tx, Bytes row, Column col) throws Exception {

    TypedTransactionBase ttx = TYPEL.wrap(tx);

    IndexStatus status = getStatus(ttx, row);
    int refCount = ttx.get().row(row).col(DOC_REF_COUNT_COL).toInteger(0);

    if (status == IndexStatus.UNINDEXED && refCount > 0) {
      updatePhraseCounts(ttx, row, 1);
      ttx.mutate().row(row).col(INDEX_STATUS_COL).set(IndexStatus.INDEXED.name());
    } else if (status == IndexStatus.INDEXED && refCount == 0) {
      updatePhraseCounts(ttx, row, -1);
      deleteDocument(ttx, row);
    }

    // TODO modifying the trigger is currently broken, enable more than one observer to commit for a
    // notification
    // tx.delete(row, col);

  }

  @Override
  public ObservedColumn getObservedColumn() {
    return new ObservedColumn(INDEX_CHECK_COL, NotificationType.STRONG);
  }

  private void deleteDocument(TypedTransactionBase tx, Bytes row) {
    // TODO it would probably be useful to have a deleteRow method on Transaction... this method
    // could start off w/ a simple implementation and later be
    // optimized... or could have a delete range option

    // TODO this is brittle, this code assumes it knows all possible columns
    tx.delete(row, DOC_CONTENT_COL);
    tx.delete(row, DOC_REF_COUNT_COL);
    tx.delete(row, INDEX_STATUS_COL);
  }

  private void updatePhraseCounts(TypedTransactionBase ttx, Bytes row, int multiplier) {
    String content = ttx.get().row(row).col(Constants.DOC_CONTENT_COL).toString();

    // this makes the assumption that the implementation of getPhrases is invariant. This is
    // probably a bad assumption. A possible way to make this more robust
    // is to store the output of getPhrases when indexing and use the stored output when unindexing.
    // Alternatively, could store the version of Document used for
    // indexing.
    Map<String, Integer> phrases = new Document(null, content).getPhrases();
    Map<String, Counts> updates = new HashMap<>(phrases.size());
    for (Entry<String, Integer> entry : phrases.entrySet()) {
      updates.put(entry.getKey(), new Counts(multiplier, entry.getValue() * multiplier));
    }

    pcMap.update(ttx, updates);
  }

  private IndexStatus getStatus(TypedTransactionBase tx, Bytes row) {
    String status = tx.get().row(row).col(INDEX_STATUS_COL).toString();

    if (status == null) {
      return IndexStatus.UNINDEXED;
    }

    return IndexStatus.valueOf(status);
  }
}
