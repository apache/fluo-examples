/*
 * Copyright 2014 Fluo authors (see AUTHORS)
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
package io.fluo.stress.trie;

import java.util.Map;

import org.apache.fluo.api.client.TransactionBase;
import org.apache.fluo.api.data.Bytes;
import org.apache.fluo.api.data.Column;
import org.apache.fluo.api.observer.AbstractObserver;
import org.apache.fluo.api.types.TypedSnapshotBase.Value;
import org.apache.fluo.api.types.TypedTransactionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observer that looks for count:wait for nodes. If found, it increments count:seen and increments
 * count:wait of parent node in trie
 */
public class NodeObserver extends AbstractObserver {

  private static final Logger log = LoggerFactory.getLogger(NodeObserver.class);

  private int stopLevel = 0;

  @Override
  public void process(TransactionBase tx, Bytes row, Column col) throws Exception {

    final TypedTransactionBase ttx = Constants.TYPEL.wrap(tx);

    Map<Column, Value> colVals =
        ttx.get().row(row).columns(Constants.COUNT_SEEN_COL, Constants.COUNT_WAIT_COL);

    final Integer childWait = colVals.get(Constants.COUNT_WAIT_COL).toInteger(0);

    if (childWait > 0) {
      Integer childSeen = colVals.get(Constants.COUNT_SEEN_COL).toInteger(0);

      ttx.mutate().row(row).col(Constants.COUNT_SEEN_COL).set(childSeen + childWait);
      ttx.mutate().row(row).col(Constants.COUNT_WAIT_COL).delete();

      try {
        Node node = new Node(row.toString());
        if (node.getLevel() > stopLevel) {
          Node parent = node.getParent();
          Integer parentWait =
              ttx.get().row(parent.getRowId()).col(Constants.COUNT_WAIT_COL).toInteger(0);
          ttx.mutate().row(parent.getRowId()).col(Constants.COUNT_WAIT_COL)
              .set(parentWait + childWait);
        }
      } catch (IllegalArgumentException e) {
        log.error(e.getMessage());
        e.printStackTrace();
      }
    }
  }

  @Override
  public void init(Context context) throws Exception {
    stopLevel = context.getAppConfiguration().getInt(Constants.STOP_LEVEL_PROP);
  }

  @Override
  public ObservedColumn getObservedColumn() {
    return new ObservedColumn(Constants.COUNT_WAIT_COL, NotificationType.STRONG);
  }
}
