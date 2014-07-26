package com.kenny.openimgur.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.util.DBContracts.UserContract;

/**
 * Created by kcampagna on 7/25/14.
 */
public class SqlHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "open_imgur.db";

    public SqlHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(UserContract.CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        onCreate(db);
    }

    /**
     * Inserts the user to the database
     *
     * @param db
     * @param user
     */
    public void insertUser(SQLiteDatabase db, @NonNull ImgurUser user) {
        // Wipe any users before we add the new one in
        db.delete(DBContracts.UserContract.TABLE_NAME, null, null);

        ContentValues values = new ContentValues();
        values.put(UserContract._ID, user.getId());
        values.put(UserContract.COLUMN_NAME, user.getUsername());
        values.put(UserContract.COLUMN_BIO, user.getBio());
        values.put(UserContract.COLUMN_ACCESS_TOKEN, user.getAccessToken());
        values.put(UserContract.COLUMN_REFRESH_TOKEN, user.getRefreshToken());
        values.put(UserContract.COLUMN_ACCESS_TOKEN_EXPIRATION, user.getAccessTokenExpiration());
        values.put(UserContract.COLUMN_CREATED, user.getCreated());
        values.put(UserContract.COLUMN_PRO_EXPIRATION, user.getProExpiration());
        values.put(UserContract.COLUMN_REPUTATION, user.getReputation());

        db.insert(UserContract.TABLE_NAME, null, values);
    }
}
