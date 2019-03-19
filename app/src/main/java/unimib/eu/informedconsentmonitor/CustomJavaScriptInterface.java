package unimib.eu.informedconsentmonitor;

import android.os.Environment;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
    public void startStreaming(){
        parentActivity.startStreaming(null);
    }

    @JavascriptInterface
    public void stopStreaming(){
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
        Log.d("JavascriptInterface", "timestamp: " + timestamp + " paragraph: " + paragraph + " webgazer: " + webgazer);
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_time), Long.toString(timestamp));
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_scroll), paragraph);
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_webgazer), webgazer);
        mDbHelper.insertJavascriptDataEntry(lastSession, timestamp, paragraph, webgazer);
    }

    @JavascriptInterface
    public void saveWebgazerData(JSONObject data){
        BufferedWriter writer = null;
        try {
            File file = new File(Environment.getExternalStorageDirectory() + STORAGE_FOLDER + "webgazer_regression_data.txt");

            writer = new BufferedWriter(new FileWriter(file));
            writer.write(data.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (IOException e) {
            }
        }
    }

    @JavascriptInterface
    public String loadWebgazerData(){
        String data = "{}";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + STORAGE_FOLDER + "webgazer_regression_data.txt"));

            data = "";
            String sCurrentLine;
            while ((sCurrentLine = reader.readLine()) != null) {
                data += sCurrentLine;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }

        }
        return data;
    }

}

