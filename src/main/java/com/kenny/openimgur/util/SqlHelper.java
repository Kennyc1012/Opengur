package com.kenny.openimgur.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.util.DBContracts.ProfileContract;
import com.kenny.openimgur.util.DBContracts.RedditContract;
import com.kenny.openimgur.util.DBContracts.UserContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/25/14.
 */
public class SqlHelper extends SQLiteOpenHelper {
    private static final String TAG = "SqlHelper";

    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "open_imgur.db";

    public SqlHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(UserContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(ProfileContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(RedditContract.CREATE_TABLE_SQL);
        sqLiteDatabase.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        onCreate(db);
    }

    /**
     * Inserts the user to the database
     *
     * @param user
     */
    public void insertUser(@NonNull ImgurUser user) {
        Log.v(TAG, "Inserting user " + user.toString());
        // Wipe any users before we add the new one in
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DBContracts.UserContract.TABLE_NAME, null, null);

        ContentValues values = new ContentValues();
        values.put(UserContract._ID, user.getId());
        values.put(UserContract.COLUMN_NAME, user.getUsername());
        values.put(UserContract.COLUMN_ACCESS_TOKEN, user.getAccessToken());
        values.put(UserContract.COLUMN_REFRESH_TOKEN, user.getRefreshToken());
        values.put(UserContract.COLUMN_ACCESS_TOKEN_EXPIRATION, user.getAccessTokenExpiration());
        values.put(UserContract.COLUMN_CREATED, user.getCreated());
        values.put(UserContract.COLUMN_PRO_EXPIRATION, user.getProExpiration());

        db.insert(UserContract.TABLE_NAME, null, values);
        db.close();
    }

    /**
     * Returns the currently logged in User
     *
     * @return User, or null if no one is logged in
     */
    @Nullable
    public ImgurUser getUser() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(UserContract.TABLE_NAME, null, null, null, null, null, null);
        ImgurUser user = null;

        if (cursor.moveToFirst()) {
            Log.v(TAG, "User present");
            user = new ImgurUser(cursor, true);
        } else {
            Log.v(TAG, "No user present");
        }

        cursor.close();
        db.close();
        return user;

    }

    /**
     * Updates the users tokens
     *
     * @param accessToken
     * @param refreshToken
     * @param expiration
     */
    public void updateUserTokens(String accessToken, String refreshToken, long expiration) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(3);

        values.put(UserContract.COLUMN_ACCESS_TOKEN_EXPIRATION, expiration);
        values.put(UserContract.COLUMN_ACCESS_TOKEN, accessToken);
        values.put(UserContract.COLUMN_REFRESH_TOKEN, refreshToken);

        db.update(UserContract.TABLE_NAME, values, null, null);
    }

    /**
     * Clears the user from the database
     */
    public void onUserLogout() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(UserContract.TABLE_NAME, null, null);
        db.close();
    }

    /**
     * Returns a user based on the username
     *
     * @param username
     * @return Profile of user, or null if none exists
     */
    @Nullable
    public ImgurUser getUser(String username) {
        ImgurUser user = null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(String.format(ProfileContract.SEARCH_USER_SQL, username), null);

        if (cursor.moveToFirst()) {
            user = new ImgurUser(cursor, false);

        }

        cursor.close();
        db.close();
        return user;
    }

    /**
     * Inserts a new profile into the database for caching purposes
     *
     * @param profile
     */
    public void insertProfile(@NonNull ImgurUser profile) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(5);
        values.put(ProfileContract._ID, profile.getId());
        values.put(ProfileContract.COLUMN_USERNAME, profile.getUsername());
        values.put(ProfileContract.COLUMN_BIO, profile.getBio());
        values.put(ProfileContract.COLUMN_REP, profile.getReputation());
        values.put(ProfileContract.COLUMN_LAST_SEEN, profile.getLastSeen());
        db.insertWithOnConflict(ProfileContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    /**
     * Returns all the previously searched sub reddits
     *
     * @return
     */
    public List<String> getSubReddits() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(RedditContract.SEARCH_SUBBRET_SQL, null);
        List<String> results = null;

        if (cursor != null) {
            results = new ArrayList<String>(cursor.getCount());

            while (cursor.moveToNext()) {
                results.add(cursor.getString(RedditContract.COLUMN_INDEX_SUBREDDIT));
            }

            cursor.close();
        }

        db.close();
        return results;
    }

    /**
     * Inserts a sub reddit into the database
     *
     * @param subReddit
     */
    public void insertSubReddit(String subReddit) {
        if (TextUtils.isEmpty(subReddit)) {
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(1);
        values.put(RedditContract.COLUMN_SUBREDDIT, subReddit.toLowerCase());
        db.insertWithOnConflict(RedditContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    /**
     * Deletes the subreddit search history
     *
     * @return Number of records deleted
     */
    public int deleteAllSubRedditSearches() {
        SQLiteDatabase db = getWritableDatabase();
        int deleted = 0;
        deleted = db.delete(RedditContract.TABLE_NAME, null, null);
        db.close();
        return deleted;
    }
}
