package unimib.eu.informedconsentmonitor.datamodel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import unimib.eu.informedconsentmonitor.datamodel.SessionContract.SessionEntry;

public class SQLiteDbHelper extends SQLiteOpenHelper {
    public static final String LOG_TAG = "SQLite";
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "InformedConsentMonitor.db";
    private final String CSV_FILE = "data.csv";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SessionEntry.TABLE_NAME + " (" +
                    SessionEntry._ID + " INTEGER PRIMARY KEY," +
                    SessionEntry.COLUMN_DATE + " TEXT," +
                    SessionEntry.COLUMN_PAGE_URL + " TEXT," +
                    SessionEntry.COLUMN_TIME + " TEXT," +
                    SessionEntry.COLUMN_SCROLL + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SessionEntry.TABLE_NAME;

    public SQLiteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
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

    public void insertEntry(String url, String time, String scroll){
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_DATE, new Date().toString());
        values.put(SessionEntry.COLUMN_PAGE_URL, url);
        values.put(SessionEntry.COLUMN_TIME, time);
        values.put(SessionEntry.COLUMN_SCROLL, scroll);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(SessionEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "new row insert. id: " + newRowId);
    }

    private void clearData(){
        this.getWritableDatabase().execSQL("delete from "+ SessionEntry.TABLE_NAME);
    }

    public void exportCsv(){
        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists())
        {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, CSV_FILE);
        try
        {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM " + SessionEntry.TABLE_NAME,null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while(curCSV.moveToNext())
            {
                //Which column you want to export
                String arrStr[] ={
                        curCSV.getString(0),
                        curCSV.getString(1),
                        curCSV.getString(2),
                        curCSV.getString(3),
                        curCSV.getString(4)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            Log.d(LOG_TAG, SessionEntry.TABLE_NAME + "Table exported as .csv at " + Environment.getExternalStorageDirectory()+"/"+CSV_FILE);
        }
        catch(Exception sqlEx)
        {
            Log.e(LOG_TAG, sqlEx.getMessage(), sqlEx);
        }
    }
}