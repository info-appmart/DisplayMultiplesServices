アプリ内課金システム
======================


![last-version](http://img.shields.io/badge/last%20version-1.0-green.svg "last version:1.0") 

![license apache 2.0](http://img.shields.io/badge/license-apache%202.0-brightgreen.svg "licence apache 2.0")


Appmartアプリ内課金システムのサンプルコードです。特定アプリの複数のアイテム情報を取得し、決済可能にするサンプルコードです。

課金サービスとやり取りするためにヘルパークラスが用意されております（AppmartHelper）。

各メソッドの【引数】・【戻り値】 は この[READMEファイル](https://github.com/info-appmart/inBillingSampleOnePage/blob/master/README.md#%E3%83%AA%E3%83%95%E3%82%A1%E3%83%AC%E3%83%B3%E3%82%B9)をご参照ください。


---

###　パーミッション追加

課金システムを使うには下記パーミッションが必要となります。

```xml
<uses-permission android:name="jp.app_mart.permissions.APPMART_BILLING" />
<uses-permission android:name="android.permission.INTERNET" />
```

## 引数の設定

下記引数を直してください。 (src.jp.app_mart.billing.StoreActivity.java)

```java
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
