package stresso.trie;

import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.config.SimpleConfiguration;

public class StressoConfig {
  public final int nodeSize;
  public final int stopLevel;
  
  private StressoConfig(int nodeSize, int stopLevel) {
    this.nodeSize = nodeSize;
    this.stopLevel = stopLevel;
  }
  
  public static StressoConfig retrieve(FluoConfiguration fc) {
    try (FluoClient client = FluoFactory.newClient(fc)) {
      return retrieve(client);
    }
  }

  public static StressoConfig retrieve(FluoClient client) {
    SimpleConfiguration ac = client.getAppConfiguration();
    return new StressoConfig(ac.getInt(Constants.NODE_SIZE_PROP), ac.getInt(Constants.STOP_LEVEL_PROP));
  }
}
