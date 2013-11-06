
package jp.app_mart.billing;

import android.annotation.SuppressLint;
import java.text.SimpleDateFormat;

import org.json.JSONObject;

/**
 * 決済情報を保持するクラス
 */
public class Payment {

    String mItemType;
    String mOrderId;
    String mSku;
    long mPurchaseTime;
    String mOriginalJson;

    @SuppressLint("SimpleDateFormat")
	public Payment(String itemType, String serviceId, String jsonPurchaseInfo, String transactionId) throws Exception{    	
	        mItemType = itemType;
	        mOriginalJson = jsonPurchaseInfo;
	        mOrderId = transactionId;
	        mSku = serviceId;
	        JSONObject o = new JSONObject(jsonPurchaseInfo);
	        JSONObject app = o.optJSONObject("application");
	        String purchseDateStr = app.optString("setlDt");
	        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");	        
            mPurchaseTime = df.parse(purchseDateStr).getTime();
    }

    public String getItemType() {
        return mItemType;
    }

    public String getOrderId() {
        return mOrderId;
    }

    public String getSku() {
        return mSku;
    }

    public long getPurchaseTime() {
        return mPurchaseTime;
    }

    public String getOriginalJson() {
        return mOriginalJson;
    }

}
