package jp.app_mart.billing;

import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.crypto.Cipher;

import jp.app_mart.sample.R;
import jp.app_mart.service.AppmartInBillingInterface;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

/**
 * appmartの課金システムヘルパークラスです
 * appmartの課金システムのご利用の際に必ずaildファイルを実装してください。
 * 詳細はこちら http://appmart.jp/
 */
public class AppmartHelper {
	
    //デバッグ関係
    boolean mDebugLog = false;
    String mDebugTag = "IabHelper";
    
  
    //Appmartのリレスポンスコード    	
    public static final String RESULT_CODE_STRING = "resultCode";
	public static final String BILLING_APPMART_PACKAGE_NAME = "jp.app_mart";
    public static final String BILLING_APPMART_SERVICE_NAME = "jp.app_mart.service.AppmartInBillingService";
    public static final int BILLING_RESPONSE_RESULT_OK = 1;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 91;
    public static final int BILLING_CONFIRM_TRANSACTION_FAILED = 92;
    public static final int APPMARTHELPER_GET_BUNDLE_FOR_PAYMENT_FAILED = 93;
    public static final int APPMARTHELPER_SEND_INTENT_FAILED = 94;
    public static final int APPMARTHELPER_REMOTE_EXCEPTION = 95;
    public static final int APPMARTHELPER_JSON_EXCEPTION = 96;
    public static final int APPMARTHELPER_BAD_RESPONSE = 97;
    public static final int APPMARTHELPER_INTERRUPTED = 98;
    public static final int APPMARTHELPER_EXCEPTION = 101;

    //コンテキスト関係
    public Context mContext;

    //アプリ・デベロッパの必要な情報
    public String mDeveloperId;
    public String mLicenseKey;
    public String mSignatureBase64;
    public String mAppId;

    //aidlファイルを元に生成されたインタフェース
    public AppmartInBillingInterface mService;

    //サービスの接続・切断情報を持つServiceConnection
    public ServiceConnection mServiceConn;

    //購入後の情報
    public AppmartReceiver mReceiver;

    //設定ステータス
    public boolean mSetupDone = false;

    //別スレッド
    public boolean mAsyncInProgress = false;    
    public String mAsyncOperation = "";
    public OnAppmartPurchaseFinishedListener mPurchaseListener;

    //購入されるアイテムの情報
    public String mPurchasingItemType;
    public String mPurchasingSku;

    /**
     * Constructor
     * @param ctx
     * @param developerId
     * @param licenseKey
     * @param base64PublicKey
     * @param appId
     */
    public AppmartHelper(Context ctx, String developerId, String licenseKey,
            String base64PublicKey, String appId) {

        mContext = ctx;
        mDeveloperId = developerId;
        mLicenseKey = licenseKey;
        mSignatureBase64 = base64PublicKey;
        mAppId = appId;
    }


    /**
     * Debug設定
     * @param enable
     * @param tag
     */
    public void enableDebugLogging(boolean enable, String tag) {
        mDebugLog = enable;
        mDebugTag = tag;
    }

    public void enableDebugLogging(boolean enable) {
        mDebugLog = enable;
    }


    /**
     * callback用のインタフェース
     * {@link #onAppmartSetupFinished}　メソッドが最後に呼ばれます
     */
    public interface OnAppmartSetupFinishedListener {
        public void onAppmartSetupFinished(AppmartResult result);
    }

    /**
     * appmartの課金サービスに接続するメッソード
     * @param listener Activityクラスのcallback
     */
    public void startSetup(final OnAppmartSetupFinishedListener listener) {

        if (mSetupDone)
            throw new IllegalStateException(mContext.getString(R.string.already_connected));

        mServiceConn = new ServiceConnection() {

        	//外部サービスに接続
            public void onServiceConnected(ComponentName name, IBinder boundService) {
                mService = AppmartInBillingInterface.Stub.asInterface((IBinder) boundService);
                mSetupDone = true;
                if (listener != null) {
                    listener.onAppmartSetupFinished(new AppmartResult(BILLING_RESPONSE_RESULT_OK,  mContext.getString(R.string.is_now_connected)));
                }
                
                //決済後発信される信号をキャッチするreceiver
                IntentFilter filter = new IntentFilter("appmart_broadcast_return_service_payment");
                if (mReceiver == null) mReceiver = new AppmartReceiver();
                mContext.registerReceiver(mReceiver, filter);
            }

            //外部サービスに切断
            public void onServiceDisconnected(ComponentName name) {
                logDebug(mContext.getString(R.string.is_now_deconnected));
                mService = null;
            }
        };

        Intent serviceIntent = new Intent();
        serviceIntent.setClassName(BILLING_APPMART_PACKAGE_NAME, BILLING_APPMART_SERVICE_NAME);
        
        //端末内にappmartがインストールされていれば、バインドする
        if (!mContext.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty()) {
        	
            mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
            
        }else {
        	
            if (listener != null) {
                listener.onAppmartSetupFinished(
                        new AppmartResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                                mContext.getString(R.string.appmart_not_installed)));
            }
        }
    }
    
    
    /**
     * サービスなどを停止
     */
    public void dispose() {
        mSetupDone = false;
        if (mServiceConn != null) {
            if (mContext != null) {
                mContext.unregisterReceiver(mReceiver);
                mReceiver = null;
                mContext.unbindService(mServiceConn);
            }
            mServiceConn = null;
            mService = null;
            mPurchaseListener = null;
        }
    }

    /**
     * 購入後のcallbackクラス
     */
    public interface OnAppmartPurchaseFinishedListener {
        public void onAppmartPurchaseFinished(AppmartResult result, Payment info);
    }


    /**
     * 指定されたアイテムを購入
     * @param act
     * @param sku
     * @param requestCode
     * @param listener
     */
    public void launchPurchaseFlow(final Activity act, final String sku, final int requestCode,
            final OnAppmartPurchaseFinishedListener listener) {

    	final String itemType = "0";
    	
        checkSetupDone("launchPurchaseFlow");

        (new Thread(new Runnable() {
            public void run() {
            	
                logDebug(mContext.getString(R.string.get_information) + sku);
                AppmartResult result;

                try {
                    Context context = mContext.getApplicationContext();
                    String dataEncrypted = createEncryptedData(sku, mDeveloperId, mLicenseKey, mSignatureBase64);
                    Bundle bundleForPaymentInterface = mService.prepareForBillingService(mAppId, dataEncrypted);

                    if (bundleForPaymentInterface == null) {
                    	
                        logError(mContext.getString(R.string.bundle_unreachable));
                        
                        result = new AppmartResult(APPMARTHELPER_GET_BUNDLE_FOR_PAYMENT_FAILED,
                        		mContext.getString(R.string.bundle_unreachable));
                        
                        if (listener != null)
                            listener.onAppmartPurchaseFinished(result, null);
                        return;
                    }

                    int response = bundleForPaymentInterface.getInt("resultCode");
                    
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                    	
                        logError(mContext.getString(R.string.response_code_error) + response);
                        result = new AppmartResult(response, mContext.getString(R.string.product_not_purchasable));
                        
                        if (listener != null)
                            listener.onAppmartPurchaseFinished(result, null);
                        return;
                    }

                    PendingIntent pIntent = bundleForPaymentInterface.getParcelable("appmart_pending_intent");
                    mPurchaseListener = listener;
                    mPurchasingItemType = itemType;
                    mPurchasingSku = sku;
                    pIntent.send(context, 0, new Intent());
                    
                } catch (RemoteException e) {
                	
                    logError(mContext.getString(R.string.failed_prepare_bill_service));
                    
                    result = new AppmartResult(APPMARTHELPER_SEND_INTENT_FAILED,
                            mContext.getString(R.string.failed_sending_intent));
                    
                    if (listener != null)
                    listener.onAppmartPurchaseFinished(result, null);
                    
                } catch (PendingIntent.CanceledException e) {
                	
                    logError(  mContext.getString(R.string.failed_sending_intent));
           
                    result = new AppmartResult(APPMARTHELPER_SEND_INTENT_FAILED,
                    		  mContext.getString(R.string.failed_sending_intent));
                    
                    if (listener != null)
                    listener.onAppmartPurchaseFinished(result, null);
                }
            }
        })).start();
    }

    /**
     * Broadcast Receiverクラス
     * 購入後にメッセージが発信されるメッセージをゲット
     */
	private class AppmartReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context arg0, final Intent arg1) {

			(new Thread(new Runnable() {
				public void run() {

					AppmartResult result;

					try {

						Thread.sleep(1000);

						// 最終確認
						final String transactionId = arg1.getExtras()
								.getString("appmart_service_trns_id");

						int res = mService.confirmFinishedTransaction(
								transactionId, mPurchasingSku, mDeveloperId);

						if (res != BILLING_RESPONSE_RESULT_OK) {
							result = new AppmartResult(
									BILLING_CONFIRM_TRANSACTION_FAILED,
									"ErrorCode:" + res + " (" + transactionId
											+ ", " + mPurchasingSku + ", "
											+ mDeveloperId + ")");
							if (mPurchaseListener != null)
								mPurchaseListener.onAppmartPurchaseFinished(
										result, null);
							return;
						}

						String json = mService.getPaymentDetails(transactionId,
								mPurchasingSku, mDeveloperId);
						logDebug("Payment details: " + json);

						Payment payment = new Payment(mPurchasingItemType,
								mPurchasingSku, json, transactionId);
						result = new AppmartResult(BILLING_RESPONSE_RESULT_OK,
								null);

						if (mPurchaseListener != null)
							mPurchaseListener.onAppmartPurchaseFinished(result,
									payment);
					} catch (RemoteException e) {
						result = new AppmartResult(
								APPMARTHELPER_REMOTE_EXCEPTION, null);
						if (mPurchaseListener != null)
							mPurchaseListener.onAppmartPurchaseFinished(result,
									null);
						e.printStackTrace();
					} catch (JSONException e) {
						result = new AppmartResult(
								APPMARTHELPER_JSON_EXCEPTION, null);
						if (mPurchaseListener != null)
							mPurchaseListener.onAppmartPurchaseFinished(result,
									null);
						e.printStackTrace();
					} catch (InterruptedException e) {
						result = new AppmartResult(APPMARTHELPER_INTERRUPTED,
								null);
						if (mPurchaseListener != null)
							mPurchaseListener.onAppmartPurchaseFinished(result,
									null);
					} catch (Exception e) {
						result = new AppmartResult(APPMARTHELPER_EXCEPTION,
								null);
						if (mPurchaseListener != null)
							mPurchaseListener.onAppmartPurchaseFinished(result,
									null);
					}

				}

			})).start();
		}
	}

    /**
     * 
     * @param querySkuDetails
     * @param moreSkus
     * @return
     * @throws AppmartException
     */
    public AppmartInventory queryInventory(boolean querySkuDetails, List<String> moreSkus)
            throws AppmartException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }


    public interface QueryAppmartInventoryFinishedListener {
        public void onQueryInventoryFinished(AppmartResult result, AppmartInventory inv);
    }

    /**
     * 別threadでアイテム情報を取得
     * @param querySkuDetails
     * @param moreSkus
     * @param listener
     */
    public void queryInventoryAsync( final List<String> moreSkus,
            final QueryAppmartInventoryFinishedListener listener) {
    	
        final Handler handler = new Handler();
        
        checkSetupDone("queryInventory");
        flagStartAsync("refresh inventory");
        
        (new Thread(new Runnable() {
        	
            public void run() {
            	
                AppmartResult result = new AppmartResult(BILLING_RESPONSE_RESULT_OK,
                        mContext.getString(R.string.inventory_refreshed));
                AppmartInventory inv = null;
                
                try {
                    inv = queryInventory(true, moreSkus);
                }
                catch (AppmartException ex) {
                    result = ex.getResult();
                }

                flagEndAsync();

                final AppmartResult result_f = result;
                final AppmartInventory inv_f = inv;
                handler.post(new Runnable() {
                    public void run() {
                        listener.onQueryInventoryFinished(result_f, inv_f);
                    }
                });
            }
        })).start();
        
    }

    /**
     * 
     * @param querySkuDetails
     * @param moreItemSkus
     * @param moreSubsSkus
     * @return
     * @throws AppmartException
     */
    public AppmartInventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus,
            List<String> moreSubsSkus) throws AppmartException {
    	
        checkSetupDone("queryInventory");
        
        try {
            AppmartInventory inv = new AppmartInventory();

            if (querySkuDetails) {
                int r = querySkuDetails(inv, moreItemSkus);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new AppmartException(r,mContext.getString(R.string.inventory_refreshed_error));
                }
            }

            return inv;
            
        } catch (RemoteException e) {
            throw new AppmartException(APPMARTHELPER_REMOTE_EXCEPTION,
                    mContext.getString(R.string.inventory_refreshed_error_remote), e);
        } catch (JSONException e) {
            throw new AppmartException(APPMARTHELPER_BAD_RESPONSE,
                    mContext.getString(R.string.inventory_refreshed_error_json), e);
        }
    }

    /**
     * 実際に情報を取得し、AppmartInventoryオブジェクトに代入
     * @param inv　　			更新されるオブジェクト
     * @param serviceIds	対象サービスのＩＤ（リスト）
     * @throws RemoteException
     * @throws JSONException
     */
    int querySkuDetails(AppmartInventory inv, List<String> serviceIds) throws RemoteException,
            JSONException {
    	
        logDebug(mContext.getString(R.string.get_inventory));

        int response = BILLING_RESPONSE_RESULT_OK;

        //サービスの情報を一個ずつ取得
        for (String serviceId : serviceIds) {
        	
            String json = mService.getServiceDetails(mAppId, createEncryptedData(serviceId, mDeveloperId, mLicenseKey, mSignatureBase64));
                
            //JSON文字列のオブジェクト化
            JSONObject o = new JSONObject(json);
            
            //結果コード取得
            int tmpResponse = Integer.valueOf(o.optString(RESULT_CODE_STRING));
            if (tmpResponse != BILLING_RESPONSE_RESULT_OK) {
                response = tmpResponse;
            }
            
            ServiceDetails details = new ServiceDetails(json);
            inv.addServiceDetails(details);
        }

        return response;
    }

    /**
     * 引数暗号化
     * @param serviceId
     * @param developId
     * @param strLicenseKey
     * @param strPublicKey
     * @return
     */
    public static String createEncryptedData(String serviceId, String developId,
            String strLicenseKey, String strPublicKey) {
        final String SEP_SYMBOL = "&";
        StringBuilder infoDataSB = new StringBuilder();
        infoDataSB.append(serviceId).append(SEP_SYMBOL);
        // ディベロッパーID 引数を追加
        infoDataSB.append(developId).append(SEP_SYMBOL);
        // ライセンスキー引数を追加
        infoDataSB.append(strLicenseKey);
        String strEncryInfoData = "";
        try {
            KeyFactory keyFac = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new X509EncodedKeySpec(Base64.decode(strPublicKey.getBytes(),
                    Base64.DEFAULT));
            Key publicKey = keyFac.generatePublic(keySpec);
            if (publicKey != null) {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                byte[] EncryInfoData = cipher.doFinal(infoDataSB.toString().getBytes());
                strEncryInfoData = new String(Base64.encode(EncryInfoData, Base64.DEFAULT));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            strEncryInfoData = "";
            Log.e("AppmartHelper", "暗号化失敗");
        }
        return strEncryInfoData.replaceAll("(\\r|\\n)", "");
    }

    /**
     * 設定の問題ないかチェック
     * @param operation
     */
    void checkSetupDone(String operation) {
        if (!mSetupDone) {
            throw new IllegalStateException("例外が発生しました：　" + operation);
        }
    }

    /**
     * 
     * @param operation
     */
    void flagStartAsync(String operation) {
        if (mAsyncInProgress)
            throw new IllegalStateException("Can't start async operation (" +
                    operation + ") because another async operation(" + mAsyncOperation
                    + ") is in progress.");
        mAsyncOperation = operation;
        mAsyncInProgress = true;
        logDebug("Starting async operation: " + operation);
    }

    /**
     * 
     */
    void flagEndAsync() {
        logDebug("Ending async operation: " + mAsyncOperation);
        mAsyncOperation = "";
        mAsyncInProgress = false;
    }

    /**
     * レスポンス情報を取得
     * @param code
     * @return
     */
    public static String getResponseDesc(int code) {
        switch (code) {
            case BILLING_CONFIRM_TRANSACTION_FAILED:
                return "Confirm transaction failed";
            case APPMARTHELPER_REMOTE_EXCEPTION:
                return "Remote exception";
            case APPMARTHELPER_JSON_EXCEPTION:
                return "JSON exception";
            case APPMARTHELPER_INTERRUPTED:
                return "Thread error";
            default:
                return "tmp message";
        }
    }

    void logDebug(String msg) {
        if (mDebugLog)
            Log.d(mDebugTag, msg);
    }

    void logError(String msg) {
        Log.e(mDebugTag, "In-app billing error: " + msg);
    }

    void logWarn(String msg) {
        Log.w(mDebugTag, "In-app billing warning: " + msg);
    }

}
