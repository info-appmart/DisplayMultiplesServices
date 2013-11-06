package jp.app_mart.billing;

import org.json.JSONException;
import org.json.JSONObject;

public class ServiceDetails {
    
    String mItemType;
    String mSku;
    String mType;
    String mPrice;
    String mTitle;
    String mDescription;
    String mJson;

    
    public ServiceDetails(String jsonServiceDetails) throws JSONException{
        mJson = jsonServiceDetails;
        JSONObject o = new JSONObject(jsonServiceDetails);
        JSONObject app = o.optJSONObject("application");
        
        if(app == null) return;
        
        mSku = app.optString("serviceId");
        mType = app.optString("saveType");
        mPrice = app.optString("appmartPrice") + " " + app.optString("setlCrcy");
        mTitle = app.optString("serviceName");
        mDescription = app.optString("exp");
    }

    public String getSku() { return mSku; }
    public String getType() { return mType; }
    public String getPrice() { return mPrice; }
    public String getTitle() { return mTitle; }
    public String getDescription() { return mDescription; }

    @Override
    public String toString() {
        return "ServiceDetails:" + mJson;
    }
}
