アプリ内課金システム
======================

Appmartアプリ内課金システムのサンプルコードです。特定アプリの複数のアイテム情報を取得し、決済可能にするサンプルコードです。

課金サービスとやり取りするためにヘルパークラスが用意されております（AppmartHelper）。

各メッソードの【引数】・【戻り値】 は この[READMEファイル](https://github.com/info-appmart/inBillingSampleOnePage/blob/master/README.md#%E3%83%AA%E3%83%95%E3%82%A1%E3%83%AC%E3%83%B3%E3%82%B9)をご参照ください。


---


## 引数の設定

下記引数を直してください。 (src.jp.app_mart.billing.StoreActivity.java)

```
//デベロッパＩＤ
APPMART_DEVELOPER_ID = "your_developper_id";

//ライセンスキー
APPMART_LICENSE_KEY = "your_licence_key";

//公開鍵
APPMART_PUBLIC_KEY = "your_public_key";

//アプリＩＤ
APPMART_APP_ID = "your_application_id";

//サービスIDS
private final List<String> allSkuIds = new ArrayList<String>() {
{
  add("test-1");
  add("test-2");
  add("test-3");
}
};
    
```
