package com.kenny.openimgur.util;

import android.provider.BaseColumns;

/**
 * Created by kcampagna on 7/25/14.
 */
public class DBContracts {

    public static class UserContract implements BaseColumns {
        public static final String TABLE_NAME = "user";

        public static final String COLUMN_ACCESS_TOKEN = "access_token";

        public static final String COLUMN_REFRESH_TOKEN = "refresh_token";

        public static final String COLUMN_ACCESS_TOKEN_EXPIRATION = "at_expiration";

        public static final String COLUMN_NAME = "name";

        public static final String COLUMN_CREATED = "created";

        public static final String COLUMN_PRO_EXPIRATION = "pro_expiration";

        public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL," +
                COLUMN_ACCESS_TOKEN + " TEXT NOT NULL," +
                COLUMN_REFRESH_TOKEN + " TEXT NOT NULL," +
                COLUMN_CREATED + " INTEGER," +
                COLUMN_ACCESS_TOKEN_EXPIRATION + " INTEGER," +
                COLUMN_PRO_EXPIRATION + " INTEGER);";

        public static int COLUMN_INDEX_ID = 0;

        public static int COLUMN_INDEX_NAME = 1;

        public static int COLUMN_INDEX_ACCESS_TOKEN = 2;

        public static int COLUMN_INDEX_REFRESH_TOKEN = 3;

        public static int COLUMN_INDEX_CREATED = 4;

        public static int COLUMN_INDEX_ACCESS_TOKEN_EXPIRATION = 5;

        public static int COLUMN_INDEX_PRO_EXPIRATION = 6;
    }

    public static class ProfileContract implements BaseColumns {
        public static final String TABLE_NAME = "profiles";

        public static final String COLUMN_USERNAME = "username";

        public static final String COLUMN_REP = "rep";

        public static final String COLUMN_BIO = "bio";

        public static final String COLUMN_LAST_SEEN = "last_seen";

        public static final String COLUMN_CREATED = "created";

        public static final int COLUMN_INDEX_ID = 0;

        public static final int COLUMN_INDEX_USERNAME = 1;

        public static final int COLUMN_INDEX_REP = 2;

        public static final int COLUMN_INDEX_BIO = 3;

        public static final int COLUMN_INDEX_LAST_SEEN = 4;

        public static final int COLUMN_INDEX_CREATED = 5;

        public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT NOT NULL, " +
                COLUMN_REP + " INTEGER, " +
                COLUMN_BIO + " TEXT, " +
                COLUMN_LAST_SEEN + " INTEGER, " +
                COLUMN_CREATED + " INTEGER);";

        public static final String SEARCH_USER_SQL = "SELECT * FROM " + TABLE_NAME +
                " WHERE " + COLUMN_USERNAME + " = '%s' LIMIT 0,1";
    }
}
