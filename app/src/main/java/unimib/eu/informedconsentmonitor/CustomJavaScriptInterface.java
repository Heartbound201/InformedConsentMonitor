package unimib.eu.informedconsentmonitor;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

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
        //parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_webgazer), webgazer);
        //parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_scroll), scroll);

        //mDbHelper.insertEntry(url, time, scroll);
    }

    @JavascriptInterface
    public void trackViewportVisibility(String json){
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_scroll), json);
    }

}

