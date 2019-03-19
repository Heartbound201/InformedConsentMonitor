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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import unimib.eu.informedconsentmonitor.datamodel.DatabaseContract.JavascriptDataEntry;
import unimib.eu.informedconsentmonitor.datamodel.DatabaseContract.SessionEntry;
import unimib.eu.informedconsentmonitor.datamodel.DatabaseContract.ShimmerDataEntry;

public class SQLiteDbHelper extends SQLiteOpenHelper {
    public static final String LOG_TAG = "SQLite";
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "InformedConsentMonitor.db";
    private final String STORAGE_FOLDER = "/InformedConsent/";

    private static final String SQL_CREATE_SESSION_ENTRY =
            "CREATE TABLE " + SessionEntry.TABLE_NAME + " (" +
                    SessionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SessionEntry.COLUMN_TIMESTAMP_IN + " TEXT," +
                    SessionEntry.COLUMN_TIMESTAMP_OUT + " TEXT," +
                    SessionEntry.COLUMN_PAGE_URL + " TEXT);";
    private static final String SQL_CREATE_SHIMMER_ENTRY =
            "CREATE TABLE " + ShimmerDataEntry.TABLE_NAME + " (" +
                    ShimmerDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ShimmerDataEntry.COLUMN_ID_SESSION + " INTEGER," +
                    ShimmerDataEntry.COLUMN_TIMESTAMP + " TEXT," +
                    ShimmerDataEntry.COLUMN_GSR_CONDUCTANCE + " REAL," +
                    ShimmerDataEntry.COLUMN_GSR_RESISTANCE + " REAL," +
                    ShimmerDataEntry.COLUMN_PPG_A13 + " REAL);";
    private static final String SQL_CREATE_JAVASCRIPT_ENTRY =
            "CREATE TABLE " + JavascriptDataEntry.TABLE_NAME + " (" +
                    JavascriptDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    JavascriptDataEntry.COLUMN_ID_SESSION + " INTEGER," +
                    JavascriptDataEntry.COLUMN_TIMESTAMP + " TEXT," +
                    JavascriptDataEntry.COLUMN_PARAGRAPHS + " BLOB," +
                    JavascriptDataEntry.COLUMN_WEBGAZER + " BLOB);";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SessionEntry.TABLE_NAME;

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
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long updateWebSessionEntry(long id, long timestamp){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_TIMESTAMP_OUT, timestamp2DateString(timestamp));

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.update(SessionEntry.TABLE_NAME, values, SessionEntry._ID + " = ?", new String[]{Long.toString(id)});
        Log.d(LOG_TAG, "session entry row updated. id: " + newRowId);
        return newRowId;
    }

    public long insertWebSessionEntry(long timestamp, String url){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_TIMESTAMP_IN, timestamp2DateString(timestamp));
        values.put(SessionEntry.COLUMN_PAGE_URL, url);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(SessionEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "new session entry row insert. id: " + newRowId);
        return newRowId;
    }

    public long insertShimmerDataEntry(long session, long timestamp, double gsrConductance,
                                       double gsrResistance, double ppg){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ShimmerDataEntry.COLUMN_ID_SESSION, session);
        values.put(ShimmerDataEntry.COLUMN_TIMESTAMP,
                DateFormat.format("dd-MM-yyyy hh:mm:ss", timestamp).toString());
        values.put(ShimmerDataEntry.COLUMN_GSR_CONDUCTANCE, gsrConductance);
        values.put(ShimmerDataEntry.COLUMN_GSR_RESISTANCE, gsrResistance);
        values.put(ShimmerDataEntry.COLUMN_PPG_A13, ppg);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(ShimmerDataEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "new shimmer data row insert. id: " + newRowId);
        return newRowId;
    }

    public long insertJavascriptDataEntry(long session, long timestamp, String paragraphs,
                                          String webgazer){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(JavascriptDataEntry.COLUMN_ID_SESSION, session);
        values.put(JavascriptDataEntry.COLUMN_TIMESTAMP, timestamp2DateString(timestamp));
        values.put(JavascriptDataEntry.COLUMN_PARAGRAPHS, paragraphs);
        values.put(JavascriptDataEntry.COLUMN_WEBGAZER, webgazer);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(JavascriptDataEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "new javascript data row insert. id: " + newRowId);
        return newRowId;
    }

    public void clearData(){
        this.getWritableDatabase().execSQL("delete from "+ SessionEntry.TABLE_NAME);
        this.getWritableDatabase().execSQL("delete from "+ ShimmerDataEntry.TABLE_NAME);
        this.getWritableDatabase().execSQL("delete from "+ JavascriptDataEntry.TABLE_NAME);
    }

    public void exportCsv() throws IOException {
        File exportDir = new File(Environment.getExternalStorageDirectory() + STORAGE_FOLDER, "");
        if (!exportDir.exists())
        {
            exportDir.mkdirs();
        }

        Date date = new Date();
        String fileDate = new SimpleDateFormat("yyyyMMdd").format(date);
        String rowDate = new SimpleDateFormat("dd-MM-yyyy").format(date);
        // Exporting WebSession Table
        {
            String csvFileName = fileDate + "_websessions.csv";
            File file = new File(exportDir, csvFileName);
            // we delete/overwrite any file with the same name
            file.delete();
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            //String query = String.format("SELECT * FROM %s WHERE %s LIKE '%s%%';", SessionEntry.TABLE_NAME, SessionEntry.COLUMN_TIMESTAMP_IN, rowDate);
            String query = String.format("SELECT * FROM %s ;", SessionEntry.TABLE_NAME);
            Cursor curCSV = db.rawQuery(query, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {
                        curCSV.getString(0),
                        curCSV.getString(1),
                        curCSV.getString(2),
                        curCSV.getString(3)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            String message = SessionEntry.TABLE_NAME + " table exported as .csv at " + file.getAbsolutePath();
            Log.d(LOG_TAG, message);
        }
        // Exporting JavascriptData Table
        {
            String csvFileName = fileDate + "_javascriptdata.csv";
            File file = new File(exportDir, csvFileName);
            // we delete/overwrite any file with the same name
            file.delete();
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            //String query = String.format("SELECT * FROM %s WHERE %s LIKE '%s%%';", JavascriptDataEntry.TABLE_NAME, JavascriptDataEntry.COLUMN_TIMESTAMP, rowDate);
            String query = String.format("SELECT * FROM %s ;", JavascriptDataEntry.TABLE_NAME);
            Cursor curCSV = db.rawQuery(query, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {
                        curCSV.getString(0),
                        curCSV.getString(1),
                        curCSV.getString(2),
                        curCSV.getString(3),
                        curCSV.getString(4)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            String message = JavascriptDataEntry.TABLE_NAME + " table exported as .csv at " + file.getAbsolutePath();
            Log.d(LOG_TAG, message);
        }
        // Exporting ShimmerData Table
        {
            String csvFileName = fileDate + "_shimmerdata.csv";
            File file = new File(exportDir, csvFileName);
            // we delete/overwrite any file with the same name
            file.delete();
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            //String query = String.format("SELECT * FROM %s WHERE %s LIKE '%s%%';", ShimmerDataEntry.TABLE_NAME, ShimmerDataEntry.COLUMN_TIMESTAMP, rowDate);
            String query = String.format("SELECT * FROM %s ;", ShimmerDataEntry.TABLE_NAME);
            Cursor curCSV = db.rawQuery(query, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {
                        curCSV.getString(0),
                        curCSV.getString(1),
                        curCSV.getString(2),
                        curCSV.getString(3),
                        curCSV.getString(4),
                        curCSV.getString(5)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            String message = ShimmerDataEntry.TABLE_NAME + " table exported as .csv at " + file.getAbsolutePath();
            Log.d(LOG_TAG, message);
        }

    }

    public String timestamp2DateString(long timestamp){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("dd-MM-yyyy HH:mm:ss", cal).toString();
    }
}