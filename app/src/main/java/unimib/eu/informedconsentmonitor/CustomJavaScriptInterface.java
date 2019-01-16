package unimib.eu.informedconsentmonitor;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class CustomJavaScriptInterface {

    protected Activity parentActivity;
    protected WebView mWebView;

    public CustomJavaScriptInterface(Activity _activity, WebView _webView)  {
        parentActivity = _activity;
        mWebView = _webView;

    }

    @JavascriptInterface
    public void testInterface(String message){
        System.out.println(message);
    }

}
