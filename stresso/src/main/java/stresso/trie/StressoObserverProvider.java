package stresso.trie;

import static org.apache.fluo.api.observer.Observer.NotificationType.STRONG;
import static stresso.trie.Constants.COUNT_WAIT_COL;

import org.apache.fluo.api.observer.ObserverProvider;

class StressoObserverProvider implements ObserverProvider {
  @Override
  public void provide(Registry registry, Context ctx) {
    int stopLevel = ctx.getAppConfiguration().getInt(Constants.STOP_LEVEL_PROP);
    registry.forColumn(COUNT_WAIT_COL, STRONG).useObserver(new NodeObserver(stopLevel));
  }
}
