
package jp.app_mart.billing;

/**
 * 例外発生時呼ばれるクラス
 */
public class AppmartException extends Exception{
    AppmartResult mResult;

    public AppmartException(AppmartResult r) {
        this(r, null);
    }
    public AppmartException(int response, String message) {
        this(new AppmartResult(response, message));
    }
    public AppmartException(AppmartResult r, Exception cause) {
        super(r.getMessage(), cause);
        mResult = r;
    }
    public AppmartException(int response, String message, Exception cause) {
        this(new AppmartResult(response, message), cause);
    }

    public AppmartResult getResult() { return mResult; }
}