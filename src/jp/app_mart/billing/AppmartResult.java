package jp.app_mart.billing;

/**
 * 結果を処理するクラス
 */
public class AppmartResult {
	
    int mResponse;
    String mMessage;

    public AppmartResult(int response, String message) {
        mResponse = response;
        if (message == null || message.trim().length() == 0) {
            mMessage = AppmartHelper.getResponseDesc(response);
        }
        else {
            mMessage = message + " (response: " + AppmartHelper.getResponseDesc(response) + ")";
        }
    }
    
    public int getResponse() { return mResponse; }
    public String getMessage() { return mMessage; }
    public boolean isSuccess() { return mResponse == AppmartHelper.BILLING_RESPONSE_RESULT_OK; }
    public boolean isFailure() { return !isSuccess(); }
    public String toString() { return "IabResult: " + getMessage(); }
    
}