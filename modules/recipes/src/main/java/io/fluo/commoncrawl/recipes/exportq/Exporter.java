package io.fluo.commoncrawl.recipes.exportq;

import java.util.Iterator;

import io.fluo.api.client.TransactionBase;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;
import io.fluo.api.observer.AbstractObserver;
import io.fluo.commoncrawl.recipes.Encoder;

public abstract class Exporter<K, V> extends AbstractObserver {

  private String queueId;

  protected Exporter(String queueId) {
    this.queueId = queueId;
  }

  protected abstract Encoder<K> getKeyEncoder();

  protected abstract Encoder<V> getValueEncoder();

  @Override
  public ObservedColumn getObservedColumn() {
    return new ObservedColumn(new Column("fluoRecipes", "eq:" + queueId), NotificationType.WEAK);
  }

  @Override
  public void process(TransactionBase tx, Bytes row, Column column) throws Exception {
    ExportQueueModel model = new ExportQueueModel(tx);

    int bucket = model.getBucket(row, column);

    Iterator<ExportEntry> exportIterator = model.getExportIterator(queueId, bucket);

    startingToProcessBatch();

    while (exportIterator.hasNext()) {
      ExportEntry ee = exportIterator.next();
      processExport(getKeyEncoder().decode(ee.key), ee.seq, getValueEncoder().decode(ee.value));
      exportIterator.remove();
    }

    finishedProcessingBatch();
  }

  protected void startingToProcessBatch() {
  }

  /**
   * Must be able to handle same key being exported multiple times and key being exported out of
   * order.   The sequence number is meant to help with this.
   */
  protected abstract void processExport(K key, long sequenceNumber, V value);

  protected void finishedProcessingBatch() {
  }

}
