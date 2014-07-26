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

        public static final String COLUMN_REPUTATION = "rep";

        public static final String COLUMN_NAME = "name";

        public static final String COLUMN_BIO = "bio";

        public static final String COLUMN_CREATED = "created";

        public static final String COLUMN_PRO_EXPIRATION = "pro_expiration";

        public static String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY ASC AUTOINCREMENT, " + COLUMN_NAME + " TEXT NOT NULL," +
                COLUMN_REPUTATION + " INTEGER," + COLUMN_ACCESS_TOKEN + " TEXT NOT NULL," + COLUMN_REFRESH_TOKEN + " TEXT NOT NULL," +
                COLUMN_CREATED + " INTEGER," + COLUMN_ACCESS_TOKEN_EXPIRATION + " INTEGER," + COLUMN_BIO + " TEXT," +
                COLUMN_PRO_EXPIRATION + " INTEGER);";
    }
}
