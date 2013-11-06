
package jp.app_mart.billing;

import java.util.HashMap;
import java.util.Map;

/**
 * アイテム情報を持つクラス
 */
public class AppmartInventory {

    Map<String, ServiceDetails> mSkuMap = new HashMap<String, ServiceDetails>();

    public AppmartInventory() {
    }
    
    public ServiceDetails getServiceDetails(String sku) {
        return mSkuMap.get(sku);
    }

    /** Returns whether or not there exists a purchase of the given product. */
    public boolean hasPayment(String sku) {
        return false;
    }

    /** Return whether or not details about the given product are available. */
    public boolean hasDetails(String sku) {
        return mSkuMap.containsKey(sku);
    }

    public void addServiceDetails(ServiceDetails d) {
        mSkuMap.put(d.getSku(), (ServiceDetails)d);
    }

}
