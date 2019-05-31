package unimib.eu.informedconsentmonitor.datamodel;

import android.provider.BaseColumns;

public class DatabaseContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DatabaseContract() {
    }

    /* Inner classes that define the tables contents */

    /*
    COLUMN_TYPE values:
    0, reading informed consent text
    1, calibration webgazer
    2, baseline calculation
     */
    public static class SessionEntry implements BaseColumns {
        public static final String TABLE_NAME = "WebSession";
        public static final String COLUMN_TIMESTAMP_IN = "timestamp_in";
        public static final String COLUMN_TIMESTAMP_OUT = "timestamp_out";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_PAGE_URL = "url";
        public static final String COLUMN_PATIENT = "patient";
        public static final String COLUMN_REPORT = "report";
        public static final String COLUMN_TIME_ON_PARAGRAPHS = "time_on_paragraphs_normalized";
    }

    public static class ShimmerDataEntry implements BaseColumns {
        public static final String TABLE_NAME = "ShimmerData";
        public static final String COLUMN_ID_SESSION = "id_session";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_GSR_CONDUCTANCE = "gsr_conductance";
        public static final String COLUMN_GSR_RESISTANCE = "gsr_resistance";
        public static final String COLUMN_PPG_A13 = "ppg";
        public static final String COLUMN_SKIN_TEMPERATURE = "skin_temperature";
    }

    public static class JavascriptDataEntry implements BaseColumns {
        public static final String TABLE_NAME = "JavascriptData";
        public static final String COLUMN_ID_SESSION = "id_session";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_PARAGRAPHS = "paragraphs";
        public static final String COLUMN_WEBGAZER = "webgazer_prediction";
    }
}

