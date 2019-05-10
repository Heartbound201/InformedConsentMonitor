package unimib.eu.informedconsentmonitor.datamodel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import com.opencsv.CSVWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import unimib.eu.informedconsentmonitor.datamodel.DatabaseContract.JavascriptDataEntry;
import unimib.eu.informedconsentmonitor.datamodel.DatabaseContract.SessionEntry;
import unimib.eu.informedconsentmonitor.datamodel.DatabaseContract.ShimmerDataEntry;

public class SQLiteDbHelper extends SQLiteOpenHelper {
    private static final String LOG_TAG = "SQLite";
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "InformedConsentMonitor.db";
    private final String STORAGE_FOLDER = "/InformedConsent/";
    private long lastSession = 0l;

    private static final String SQL_CREATE_SESSION_ENTRY =
            "CREATE TABLE " + SessionEntry.TABLE_NAME + " (" +
                    SessionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SessionEntry.COLUMN_TIMESTAMP_IN + " TEXT," +
                    SessionEntry.COLUMN_TIMESTAMP_OUT + " TEXT," +
                    SessionEntry.COLUMN_PAGE_URL + " TEXT," +
                    SessionEntry.COLUMN_TIME_ON_PARAGRAPHS + " TEXT," +
                    SessionEntry.COLUMN_PATIENT + " TEXT);";
    private static final String SQL_CREATE_SHIMMER_ENTRY =
            "CREATE TABLE " + ShimmerDataEntry.TABLE_NAME + " (" +
                    ShimmerDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ShimmerDataEntry.COLUMN_ID_SESSION + " INTEGER," +
                    ShimmerDataEntry.COLUMN_TIMESTAMP + " TEXT," +
                    ShimmerDataEntry.COLUMN_BASELINE + " INTEGER DEFAULT 0," +
                    ShimmerDataEntry.COLUMN_GSR_CONDUCTANCE + " REAL," +
                    ShimmerDataEntry.COLUMN_GSR_RESISTANCE + " REAL," +
                    ShimmerDataEntry.COLUMN_PPG_A13 + " REAL," +
                    ShimmerDataEntry.COLUMN_SKIN_TEMPERATURE + " REAL);";
    private static final String SQL_CREATE_JAVASCRIPT_ENTRY =
            "CREATE TABLE " + JavascriptDataEntry.TABLE_NAME + " (" +
                    JavascriptDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    JavascriptDataEntry.COLUMN_ID_SESSION + " INTEGER," +
                    JavascriptDataEntry.COLUMN_TIMESTAMP + " TEXT," +
                    JavascriptDataEntry.COLUMN_PARAGRAPHS + " BLOB," +
                    JavascriptDataEntry.COLUMN_WEBGAZER + " BLOB);";

    private static final String SQL_DELETE_SESSION_ENTRY =
            "DROP TABLE IF EXISTS " + SessionEntry.TABLE_NAME;
    private static final String SQL_DELETE_SHIMMER_ENTRY =
            "DROP TABLE IF EXISTS " + ShimmerDataEntry.TABLE_NAME;
    private static final String SQL_DELETE_JAVASCRIPT_ENTRY =
            "DROP TABLE IF EXISTS " + JavascriptDataEntry.TABLE_NAME;

    public SQLiteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SESSION_ENTRY);
        db.execSQL(SQL_CREATE_SHIMMER_ENTRY);
        db.execSQL(SQL_CREATE_JAVASCRIPT_ENTRY);
        Log.d(LOG_TAG, db.getPath());
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_SESSION_ENTRY);
        db.execSQL(SQL_DELETE_SHIMMER_ENTRY);
        db.execSQL(SQL_DELETE_JAVASCRIPT_ENTRY);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void updateWebSessionEntry(long timestamp){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_TIMESTAMP_OUT, timestampToDateString(timestamp));
        values.put(SessionEntry.COLUMN_TIME_ON_PARAGRAPHS, Arrays.toString(getTimeSpentOnParagraphsDuringLastSession().entrySet().toArray()));

        db.update(SessionEntry.TABLE_NAME, values, SessionEntry._ID + " = ?", new String[]{Long.toString(lastSession)});
        Log.d(LOG_TAG, "session entry row updated. id: " + lastSession);
    }

    public long insertWebSessionEntry(long timestamp, String url){ // TODO add patient data
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_TIMESTAMP_IN, timestampToDateString(timestamp));
        values.put(SessionEntry.COLUMN_PAGE_URL, url);

        long newRowId = db.insert(SessionEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "new session entry row insert. id: " + newRowId);
        lastSession = newRowId;
        return newRowId;
    }

    public long insertShimmerDataEntry(long timestamp, boolean isBaseline,
                                       double gsrConductance, double gsrResistance,
                                       double ppg, double skinTemperature){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ShimmerDataEntry.COLUMN_ID_SESSION, lastSession);
        values.put(ShimmerDataEntry.COLUMN_TIMESTAMP,
                DateFormat.format("dd-MM-yyyy hh:mm:ss", timestamp).toString());
        values.put(ShimmerDataEntry.COLUMN_BASELINE, isBaseline?1:0);
        values.put(ShimmerDataEntry.COLUMN_GSR_CONDUCTANCE, gsrConductance);
        values.put(ShimmerDataEntry.COLUMN_GSR_RESISTANCE, gsrResistance);
        values.put(ShimmerDataEntry.COLUMN_PPG_A13, ppg);
        values.put(ShimmerDataEntry.COLUMN_SKIN_TEMPERATURE, skinTemperature);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(ShimmerDataEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "new shimmer data row insert. id: " + newRowId);
        return newRowId;
    }

    public long insertJavascriptDataEntry(long timestamp, String paragraphs,
                                          String webgazer){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(JavascriptDataEntry.COLUMN_ID_SESSION, lastSession);
        values.put(JavascriptDataEntry.COLUMN_TIMESTAMP, timestampToDateString(timestamp));
        values.put(JavascriptDataEntry.COLUMN_PARAGRAPHS, paragraphs);
        values.put(JavascriptDataEntry.COLUMN_WEBGAZER, webgazer);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(JavascriptDataEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "new javascript data row insert. id: " + newRowId);
        return newRowId;
    }

    public void clearDataBase(){
        this.getWritableDatabase().execSQL("delete from "+ SessionEntry.TABLE_NAME);
        this.getWritableDatabase().execSQL("delete from "+ ShimmerDataEntry.TABLE_NAME);
        this.getWritableDatabase().execSQL("delete from "+ JavascriptDataEntry.TABLE_NAME);
    }

    public List<String> getTablesOnDataBase(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        List<String> tables = new ArrayList<>();
        try{
            c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            if (c.moveToFirst()) {
                while ( !c.isAfterLast() ) {
                    tables.add(c.getString(0));
                    c.moveToNext();
                }
            }
        }
        catch(Exception throwable){
            Log.e(LOG_TAG, "Could not get the table names from db", throwable);
        }
        finally{
            if(c!=null)
                c.close();
        }
        return tables;
    }

    public void exportDataBaseToCsv(){
        // file save location
        File exportDir = new File(Environment.getExternalStorageDirectory() + STORAGE_FOLDER, "");
        if (!exportDir.exists()) exportDir.mkdirs();

        // database to export
        SQLiteDatabase db = this.getReadableDatabase();

        // how many tables are there?
        List<String> tables = getTablesOnDataBase();
        CSVWriter csvWrite = null;
        Cursor curCSV = null;
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        for (String t: tables) {
            try {
                // filename stuff - [date]_[table].csv
                String csvFileName = date + "_" + t + ".csv";
                File file = new File(exportDir, csvFileName);
                boolean isFileNew = file.createNewFile();
                csvWrite = new CSVWriter(new FileWriter(file, true),
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.NO_ESCAPE_CHARACTER,
                        CSVWriter.RFC4180_LINE_END);

                curCSV = db.rawQuery("SELECT * FROM " + t,null);
                if(isFileNew) csvWrite.writeNext(curCSV.getColumnNames());
                while(curCSV.moveToNext()) {
                    int columns = curCSV.getColumnCount();
                    String[] columnArr = new String[columns];
                    for( int i = 0; i < columns; i++){
                        columnArr[i] = curCSV.getString(i);
                    }
                    csvWrite.writeNext(columnArr, false);
                }

            }
            catch(Exception sqlEx) {
                Log.e(LOG_TAG, sqlEx.getMessage(), sqlEx);
            }finally {
                if(csvWrite != null){
                    try {
                        csvWrite.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if( curCSV != null ){
                    curCSV.close();
                }
            }

        }

        // clear database
        clearDataBase();
    }

    public HashMap getTimeSpentOnParagraphsDuringLastSession(){
        HashMap<String, Long> parTimeSpent = new HashMap<>();
        long previousTime = 0l;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + JavascriptDataEntry.TABLE_NAME +
                " WHERE " + JavascriptDataEntry.COLUMN_ID_SESSION + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{Long.toString(lastSession)});
        while(cursor.moveToNext()) {
            try {
                JSONObject obj = new JSONObject(cursor.getString(3));
                String parId = obj.getString("id");
                int nwords = obj.getInt("numwords");
                String timestamp = cursor.getString(2);
                Date date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(timestamp);
                if(previousTime == 0l) previousTime = date.getTime();
                if(parTimeSpent.containsKey(parId)){
                    long time = (date.getTime() - previousTime) / nwords;
                    parTimeSpent.replace(parId, parTimeSpent.get(parId) + time);
                }
                else{
                    parTimeSpent.put(parId, 0l);
                }
                previousTime = date.getTime();
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }

        }
        return parTimeSpent;
    }

    public String timestampToDateString(long timestamp){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("dd-MM-yyyy HH:mm:ss", cal).toString();
    }
}