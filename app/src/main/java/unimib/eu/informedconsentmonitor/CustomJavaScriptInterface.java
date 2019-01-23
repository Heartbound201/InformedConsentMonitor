package unimib.eu.informedconsentmonitor;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

public class CustomJavaScriptInterface {

    protected MainActivity parentActivity;
    protected WebView mWebView;

    public CustomJavaScriptInterface(MainActivity _activity, WebView _webView)  {
        parentActivity = _activity;
        mWebView = _webView;

    }

    @JavascriptInterface
    public void trackTime(String message){
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_time), message);
    }

    @JavascriptInterface
    public void trackScroll(String message){
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_scroll), message);
    }

}
