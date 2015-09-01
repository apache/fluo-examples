package io.fluo.commoncrawl.data.fluo;

import java.util.Map;

import io.fluo.api.client.TransactionBase;
import io.fluo.api.config.ScannerConfiguration;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;
import io.fluo.api.data.Span;
import io.fluo.api.iterator.ColumnIterator;
import io.fluo.api.iterator.RowIterator;
import io.fluo.api.observer.AbstractObserver;
import io.fluo.api.types.TypedTransactionBase;
import io.fluo.commoncrawl.core.Constants;
import io.fluo.commoncrawl.data.util.FluoConstants;
import io.fluo.recipes.export.ExportQueue;
import io.fluo.recipes.transaction.RecordingTransactionBase;
import io.fluo.recipes.transaction.TxLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InlinksObserver extends AbstractObserver {

  private static final Logger log = LoggerFactory.getLogger(InlinksObserver.class);
  private ExportQueue<String, TxLog> exportQueue;

  @Override
  public void init(Context context) throws Exception {
    exportQueue = new IndexExporter().getExportQueue(context.getAppConfiguration());
  }

  @Override
  public void process(TransactionBase tx, Bytes row, Column col) throws Exception {

    RecordingTransactionBase rtx = RecordingTransactionBase.wrap(tx);
    TypedTransactionBase ttx = FluoConstants.TYPEL.wrap(rtx);

    String pageUri = row.toString().substring(2);

    log.debug("Running InLinksObserver for page {}", pageUri);

    ScannerConfiguration scanConf = new ScannerConfiguration();
    scanConf.setSpan(Span.exact(row, new Column(FluoConstants.INLINKS_UPDATE)));

    long change = 0;
    RowIterator rowIter = ttx.get(scanConf);
    if (rowIter.hasNext()) {
      Map.Entry<Bytes, ColumnIterator> rowEntry = rowIter.next();
      ColumnIterator colIter = rowEntry.getValue();
      while (colIter.hasNext()) {
        Map.Entry<Column, Bytes> colEntry = colIter.next();
        String linkUri = colEntry.getKey().getQualifier().toString();
        String update = colEntry.getValue().toString();
        if (update.startsWith("del")) {
          ttx.mutate().row(row).fam(Constants.INLINKS).qual(linkUri).delete();
          ttx.mutate().row(row).col(colEntry.getKey()).delete();
          change--;
          log.debug("Deleted inlink {} for page {}", linkUri, pageUri);
        } else if (update.startsWith("add")) {
          ttx.mutate().row(row).fam(Constants.INLINKS).qual(linkUri).set(update.substring(4));
          ttx.mutate().row(row).col(colEntry.getKey()).delete();
          change++;
          log.debug("Added inlink {} for page {}", linkUri, pageUri);
        } else {
          log.error("Unknown update format: {}", update);
        }
      }
    }

    Long incount = ttx.get().row(row).col(FluoConstants.PAGE_INCOUNT_COL).toLong(0) + change;
    if (incount <= 0) {
      if (incount < 0) {
        log.error("Incount for {} is negative: {}", row, incount);
      }
      ttx.mutate().row(row).col(FluoConstants.PAGE_INCOUNT_COL).delete();
    } else {
      ttx.mutate().row(row).col(FluoConstants.PAGE_INCOUNT_COL).set(incount);
    }

    Long score = ttx.get().row(row).col(FluoConstants.PAGE_SCORE_COL).toLong(0) + change;
    if (score <= 0) {
      if (score < 0) {
        log.error("Score for {} is negative: {}", row, incount);
      }
      ttx.mutate().row(row).col(FluoConstants.PAGE_SCORE_COL).delete();
    } else {
      ttx.mutate().row(row).col(FluoConstants.PAGE_SCORE_COL).set(score);
    }

    TxLog txLog = rtx.getTxLog();
    if (txLog.getLogEntries().size() > 0) {
      exportQueue.add(tx, row.toString(), txLog);
    }
  }

  @Override
  public ObservedColumn getObservedColumn() {
    return new ObservedColumn(FluoConstants.INLINKS_CHG_NTFY, NotificationType.WEAK);
  }
}
