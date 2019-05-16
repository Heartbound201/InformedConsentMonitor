package unimib.eu.informedconsentmonitor;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.Arrays;
import java.util.HashMap;

import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

public class CustomJavaScriptInterface {

    String LOG_TAG = "CustomJavaScriptInterface";
    protected MainActivity parentActivity;
    protected WebView mWebView;
    protected SQLiteDbHelper mDbHelper;
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
        Log.d(LOG_TAG, "timestamp: " + timestamp + " url: " + url);
        mDbHelper.insertWebSessionEntry(timestamp, url);
    }

    @JavascriptInterface
    public void updateWebSession(long timestamp, String report){
        Log.d(LOG_TAG, "timestamp: " + timestamp);
        mDbHelper.updateWebSessionEntry(timestamp, report);
    }

    @JavascriptInterface
    public void trackJavascriptData(long timestamp, String paragraph, String webgazer){
        mDbHelper.insertJavascriptDataEntry(timestamp, paragraph, webgazer);
    }

    @JavascriptInterface
    public void calculateBiologicalBaseline(){
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @JavascriptInterface
    public void calculateTimeSpentOnParagraphs(){
        HashMap<Integer, Long> parMap = mDbHelper.getTimeSpentOnParagraphsDuringLastSession();
        String values = Arrays.toString(parMap.entrySet().toArray());
        Log.d(LOG_TAG, "time spent on each par (normalized on #words): " + values);
    }

    @JavascriptInterface
    public void getSessionReport(String json){
        Log.d(LOG_TAG, json);
    }

}

