package unimib.eu.informedconsentmonitor;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.Date;

import unimib.eu.informedconsentmonitor.datamodel.SessionContract;
import unimib.eu.informedconsentmonitor.datamodel.SessionContract.SessionEntry;
import unimib.eu.informedconsentmonitor.datamodel.SqliteDbHelper;

public class CustomJavaScriptInterface {

    protected MainActivity parentActivity;
    protected WebView mWebView;
    protected SqliteDbHelper mDbHelper;

    public CustomJavaScriptInterface(MainActivity _activity, WebView _webView)  {
        parentActivity = _activity;
        mWebView = _webView;
        mDbHelper = new SqliteDbHelper(_activity.getApplicationContext());
    }

    @JavascriptInterface
    public void trackTime(String message){
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_time), message);
    }

    @JavascriptInterface
    public void trackScroll(String message){
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_scroll), message);
    }

    @JavascriptInterface
    public void trackEyes(String message){
        parentActivity.updateDebugText((TextView) parentActivity.findViewById(R.id.debug_webgazer), message);
    }

    private void insertEntry(String url){
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SessionEntry.DATE, new Date().toString());
        values.put(SessionEntry.PAGE_URL, url);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(SessionEntry.TABLE_NAME, null, values);
    }
}
