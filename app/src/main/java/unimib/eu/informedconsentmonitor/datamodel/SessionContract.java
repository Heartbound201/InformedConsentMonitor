package unimib.eu.informedconsentmonitor.datamodel;

import android.provider.BaseColumns;

public class SessionContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private SessionContract() {
    }

    /* Inner class that defines the table contents */
    public static class SessionEntry implements BaseColumns {
        public static final String TABLE_NAME = "Session";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_PAGE_URL = "page_url";
        public static final String COLUMN_TIME = "time_on_page";
        public static final String COLUMN_SCROLL = "scroll_percentage";

    }
}

