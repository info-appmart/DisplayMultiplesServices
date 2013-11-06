InBillingSample
===============

■複数のアイテムを取得方法

→必要なパラメータの保存場所：
src.jp.app_mart.billing.StoreActivity

//デベロッパＩＤ

APPMART_DEVELOPER_ID = "your_developper_id";


//ライセンスキー

APPMART_LICENSE_KEY = "your_licence_key";


//公開鍵

APPMART_PUBLIC_KEY = "your_public_key";


//アプリＩＤ

APPMART_APP_ID = "your_application_id";

同ファイルのallSkuIds（配列）の中に上記アプリのサービス名を入れる：


   private final List<String> allSkuIds = new ArrayList<String>() {
        {
            add("test-1");
            add("test-2");
            add("test-3");
        }
    };

