
package jp.app_mart.billing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jp.app_mart.billing.AppmartHelper.OnAppmartPurchaseFinishedListener;
import jp.app_mart.billing.AppmartHelper.OnAppmartSetupFinishedListener;
import jp.app_mart.billing.AppmartHelper.QueryAppmartInventoryFinishedListener;
import jp.app_mart.sample.BuildConfig;
import jp.app_mart.sample.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * 作成者　：　@author canu
 */
public class StoreActivity extends Activity {

	//デベロッパＩＤ
    public static final String APPMART_DEVELOPER_ID = "your_developer_id";
    //ライセンスキー
    public static final String APPMART_LICENSE_KEY = "your_licence_key";
    //公開鍵
    public static final String APPMART_PUBLIC_KEY = "your_public_key";
    //アプリＩＤ
    public static final String APPMART_APP_ID = "your_application_id";

    //Views
    TextView mAppmartStatusLabel;
    ListView mAddonsList;
    
    private AppmartInventory mAppmartInventory;
    
    //appmartヘルパー
    private AppmartHelper mAppmartHelper;
    //接続状態フラグ
    private boolean appmartSetUpDone = false;
    //対象サービスＩＤ
    private final List<String> allSkuIds = new ArrayList<String>() {
        {
            add("your_service_id_1");
            add("your_service_id_2");
        }
    };

    //ListViewアダプター
    private ShopAdapter mAdapter;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        mAppmartStatusLabel = (TextView) findViewById(R.id.appmart_status);
        
        //アダプターの初期設定
        mAdapter = new ShopAdapter();
        mAddonsList = (ListView) findViewById(R.id.addons_list); 
        mAddonsList.setAdapter(mAdapter);
        mAddonsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                requestBilling(allSkuIds.get(position));
            }
        });

        //appmart設定
        setupBilling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tearDownBilling();	//receiverとserviceを停止
    }

    
    /* Appmartのヘルパーの設定   */
    private void setupBilling() {

        //Appmart helperインスタンス
        mAppmartHelper = new AppmartHelper(this, APPMART_DEVELOPER_ID,  APPMART_LICENSE_KEY, APPMART_PUBLIC_KEY, APPMART_APP_ID);
        //デバッグの有効性
        mAppmartHelper.enableDebugLogging(BuildConfig.DEBUG);
        //外部サービスにに接続し、callbackとしてパラメータを指定
        mAppmartHelper.startSetup(new OnAppmartSetupFinishedListener() {
            @Override
            public void onAppmartSetupFinished(AppmartResult result) {
                appmartSetUpDone = true;
                if (result.isFailure()) {
                    mAppmartHelper = null;
                    debugLog("Appmart setup failure.");
                    mAppmartStatusLabel.setText("状態: 接続不可");
                    return;
                }
                
                //正常に接続できた場合はステータスフラグを更新し、サービス情報を取得
                debugLog("接続成功です。情報を取得します。");
                mAppmartStatusLabel.setText("状態: 準備完了");
                queryInventoryAsync();
            }
        });
    }

    
    /* serviceとreceiverを停止  */
    private void tearDownBilling() {
        if (mAppmartHelper != null) {
            try {
                mAppmartHelper.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }        
        mAppmartHelper = null;
        mAppmartInventory = null;
        appmartSetUpDone = false;
    }

    /* appmartサービス経由でサービス情報を取得   */
    private void queryInventoryAsync() {

        if (!appmartSetUpDone)
            return;
        
        //appmartに問い合わせる
        mAppmartHelper.queryInventoryAsync(allSkuIds, new QueryAppmartInventoryFinishedListener() {
        	public void onQueryInventoryFinished(AppmartResult result, AppmartInventory inventory) {
        		if (result.isFailure()) {
        			debugLog("クエリエラーが発生しました。");
                    return;
                }
        		
        		//成功の場合はアイテム情報を {@link mAppmartInventory} に代入
                debugLog("情報取得完了");
                mAppmartInventory = inventory;
                mAdapter.notifyDataSetChanged();
        	}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
    }
    
    /* アイテムが選択された時に実行   */
    protected void requestBilling(String sku) {

        int requestCode = new Random().nextInt(999999);

        if (mAppmartHelper != null) {        	
        	
            mAppmartHelper.launchPurchaseFlow(this, sku, requestCode,
                    new OnAppmartPurchaseFinishedListener() {
                        
            			//最終確認の後にヘルパーから呼ばれる
                        public void onAppmartPurchaseFinished(AppmartResult result, Payment payment) {
                            debugLog("購入が完了になりました； " + result + ", 購入：: " + payment);
                            if (result.isFailure()) {
                                debugLog("購入は失敗しました：: " + result.getMessage());
                                return;
                            }
                            debugLog("購入完了");

                            //preferencesを更新
                            savePurchase(payment.getSku());

                            //ListViewを更新
                            mAdapter.notifyDataSetChanged();
                        }
                    });
            return;
        }
    }

    
    /**
     * adapter用のクラス
     */
    private class ShopAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return allSkuIds.size();
        }

        @Override
        public Object getItem(int position) {
            return allSkuIds.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LinearLayout layout = new LinearLayout(StoreActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            convertView = layout;

            //名前
            TextView tvName = new TextView(StoreActivity.this);
            tvName.setText("---");
            layout.addView(tvName);

            //ステータス
            TextView tvPrice = new TextView(StoreActivity.this);
            tvPrice.setText("---");
            layout.addView(tvPrice);
            
            //ステータス
            TextView tvDescription = new TextView(StoreActivity.this);
            tvDescription.setText("---");
            layout.addView(tvDescription);
            
            //ステータス
            TextView tvStatusCust = new TextView(StoreActivity.this);
            tvStatusCust.setText("---");
            layout.addView(tvStatusCust);
            
            //情報があれば、ステータスの値を入れ直す
            String sku = (String) getItem(position);
            if (mAppmartInventory != null) {
                ServiceDetails details = mAppmartInventory.getServiceDetails(sku);
                if (details != null) {
                    tvName.setText(details.getTitle());
                    tvPrice.setText(details.getPrice());
                    tvDescription.setText(details.getDescription());
                    tvStatusCust.setText(hasPurchase(sku) ? "購入済み" : "未購入");
                }
            }

            return convertView;
        }
    }

    private static final String PREF_KEY = "AppPreference";
    private static final String KEY_PURCHASE_PREFIX = "purchase_%s";

    private void savePurchase(String sku) {
        SharedPreferences pref = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putBoolean(String.format(KEY_PURCHASE_PREFIX, sku), true);
        editor.commit();
    }
    
    public boolean hasPurchase(String sku) {
        SharedPreferences pref = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        return pref.getBoolean(String.format(KEY_PURCHASE_PREFIX, sku), false);
    }

    /* 情報更新   */
    public void refresh(View view) {
        tearDownBilling();
        setupBilling();
    }
    
    /*　端末内の履歴情報を削除  */
    public void deleteHistory(View view){
    	SharedPreferences pref = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        Editor editor = pref.edit();
    	//
    	for(String a : allSkuIds){
            editor.putBoolean(String.format(KEY_PURCHASE_PREFIX, a), false);
    	}
    	
    	 editor.commit();    	 
    	 mAdapter.notifyDataSetChanged();
    	 debugLog("情報が削除されました");
    }

    protected void debugLog(String msg) {
        if (!BuildConfig.DEBUG)
            return;
        Log.d("AppmartBilling", msg);
    }

}
