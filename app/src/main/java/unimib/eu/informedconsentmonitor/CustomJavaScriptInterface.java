package unimib.eu.informedconsentmonitor;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

public class CustomJavaScriptInterface {

    protected MainActivity parentActivity;
    protected WebView mWebView;
    protected SQLiteDbHelper mDbHelper;

    public CustomJavaScriptInterface(MainActivity _activity, WebView _webView)  {
        parentActivity = _activity;
        mWebView = _webView;
        mDbHelper = new SQLiteDbHelper(_activity.getApplicationContext());
    }

    @JavascriptInterface
    public void trackData(String url, String time, String scroll){
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_time), time);
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_scroll), scroll);

        mDbHelper.insertEntry(url, time, scroll);
    }
}
