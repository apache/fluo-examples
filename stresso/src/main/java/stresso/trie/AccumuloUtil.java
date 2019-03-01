package stresso.trie;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.fluo.api.client.FluoAdmin;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;

class AccumuloUtil {

  public interface TableOp<T> {
    T run(TableOperations tableOps, String tableName) throws Exception;
  }

  public interface VoidTableOp {
    void run(TableOperations tableOps, String tableName) throws Exception;
  }

  public static void doTableOp(FluoConfiguration fc, VoidTableOp tableOp) {
    getTableOp(fc, (to, tn) -> {
      tableOp.run(to, tn);
      return null;
    });
  }

  public static <T> T getTableOp(FluoConfiguration fc, TableOp<T> tableOp) {
    try (FluoAdmin fadmin = FluoFactory.newAdmin(fc)) {
      FluoConfiguration appCfg = new FluoConfiguration(fadmin.getApplicationConfig());
      appCfg.setApplicationName(fc.getApplicationName());
      AccumuloClient client = Accumulo.newClient()
          .to(appCfg.getAccumuloInstance(), appCfg.getAccumuloZookeepers())
          .as(appCfg.getAccumuloUser(), appCfg.getAccumuloPassword()).build();
      return tableOp.run(client.tableOperations(), appCfg.getAccumuloTable());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
