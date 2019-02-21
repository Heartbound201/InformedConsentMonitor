package unimib.eu.informedconsentmonitor;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

public class CustomJavaScriptInterface {

    protected MainActivity parentActivity;
    protected WebView mWebView;
    protected SQLiteDbHelper mDbHelper;
    private long lastSession;

    public CustomJavaScriptInterface(MainActivity _activity, WebView _webView)  {
        parentActivity = _activity;
        mWebView = _webView;
        mDbHelper = new SQLiteDbHelper(_activity.getApplicationContext());
    }

    @JavascriptInterface
    public void trackWebSession(long timestamp, String url){
        Log.d("JavascriptInterface", "timestamp: " + timestamp + " url: " + url);
        boolean isShimmerConnected = parentActivity.shimmerDevice != null;
        lastSession = mDbHelper.insertSessionEntry(timestamp, url, isShimmerConnected);
        parentActivity.lastSession = lastSession;
    }

    @JavascriptInterface
    public void trackJavascriptData(long timestamp, String paragraph, String webgazer){
        Log.d("JavascriptInterface", "timestamp: " + timestamp + " paragraph: " + paragraph + " webgazer: " + webgazer);
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_time), Long.toString(timestamp));
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_scroll), paragraph);

        mDbHelper.insertJavascriptDataEntry(lastSession, timestamp, paragraph, webgazer);
    }

}

