package unimib.eu.informedconsentmonitor;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.Arrays;
import java.util.HashMap;

import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

public class CustomJavaScriptInterface {

    protected MainActivity parentActivity;
    protected WebView mWebView;
    protected SQLiteDbHelper mDbHelper;
    private long lastSession;
    private final String STORAGE_FOLDER = "/InformedConsent/";

    public CustomJavaScriptInterface(MainActivity _activity, WebView _webView)  {
        parentActivity = _activity;
        mWebView = _webView;
        mDbHelper = new SQLiteDbHelper(_activity.getApplicationContext());
    }

    @JavascriptInterface
    public boolean isLoadedFromMobileApp(){ return true; }

    @JavascriptInterface
    public void startStreaming(){
        parentActivity.startStreaming(null);
    }

    @JavascriptInterface
    public void stopStreaming(){
        parentActivity.stopStreaming(null);
    }

    @JavascriptInterface
    public void startStreamingBaseline(){
        parentActivity.isBaseline = true;
        parentActivity.startStreaming(null);
    }

    @JavascriptInterface
    public void stopStreamingBaseline(){
        parentActivity.isBaseline = false;
        parentActivity.stopStreaming(null);
    }

    @JavascriptInterface
    public void trackWebSession(long timestamp, String url){
        Log.d("JavascriptInterface", "timestamp: " + timestamp + " url: " + url);
        lastSession = mDbHelper.insertWebSessionEntry(timestamp, url);
        parentActivity.lastSession = lastSession;
    }

    @JavascriptInterface
    public void updateWebSession(long timestamp){
        Log.d("JavascriptInterface", "timestamp: " + timestamp);
        mDbHelper.updateWebSessionEntry(lastSession, timestamp);
    }

    @JavascriptInterface
    public void trackJavascriptData(long timestamp, String paragraph, String webgazer){
        mDbHelper.insertJavascriptDataEntry(lastSession, timestamp, paragraph, webgazer);
    }

    @JavascriptInterface
    public void calculateBiologicalBaseline(){
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @JavascriptInterface
    public void calculateTimeSpentOnParagraphs(){
        HashMap<Integer, Long> parMap = mDbHelper.getTimeSpentOnParagraphsDuringSession(lastSession);
        String values = Arrays.toString(parMap.entrySet().toArray());
        Log.d("time spent on each par (normalized on #words)", values);
    }


}

