package com.kenny.openimgur.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurNotification;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.util.DBContracts.GallerySearchContract;
import com.kenny.openimgur.util.DBContracts.MemeContract;
import com.kenny.openimgur.util.DBContracts.MuzeiContract;
import com.kenny.openimgur.util.DBContracts.NotificationContract;
import com.kenny.openimgur.util.DBContracts.ProfileContract;
import com.kenny.openimgur.util.DBContracts.SubRedditContract;
import com.kenny.openimgur.util.DBContracts.TopicsContract;
import com.kenny.openimgur.util.DBContracts.UploadContract;
import com.kenny.openimgur.util.DBContracts.UserContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kcampagna on 7/25/14.
 */
public class SqlHelper extends SQLiteOpenHelper {
    private static final String TAG = "SqlHelper";

    private static final int DB_VERSION = 11;

    private static final String DB_NAME = "open_imgur.db";

    private static SQLiteDatabase mReadableDatabase;

    private static SQLiteDatabase mWritableDatabase;

    private static SqlHelper sInstance;

    public static SqlHelper getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new SqlHelper(context.getApplicationContext());
        }

        return sInstance;
    }

    private SqlHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(UserContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(ProfileContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(UploadContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(TopicsContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(SubRedditContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(MemeContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(GallerySearchContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(MuzeiContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(NotificationContract.CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        /* V2 Added uploads Table
         V3 Added topics Table
         v4 Added Subreddits Table
         V5 Added Meme Table
         V6 Added GallerySearch Table
         v7 Added Muzei Table
         V8 Skipped number
         v9 Alter Uploads table for albums
         v10 Added Notifications table, only 3.4.0 Beta users will be upgrading from this version
         v11 Added viewed field in Notifications*/

        // Checking for is_album and cover_id column
        Cursor cursor = db.rawQuery("SELECT * FROM " + UploadContract.TABLE_NAME + " LIMIT 0,1", null);

        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(UploadContract.COLUMN_IS_ALBUM);

            if (index == -1) {
                // Column doesn't exist, add it
                db.execSQL("ALTER TABLE " + UploadContract.TABLE_NAME + "  ADD COLUMN " + UploadContract.COLUMN_IS_ALBUM + "  INTEGER");
                db.execSQL("ALTER TABLE " + UploadContract.TABLE_NAME + "  ADD COLUMN " + UploadContract.COLUMN_COVER_ID + "  TEXT");
            }
        } else {
            // No records found to see if the column exists, delete it and it will get recreated
            db.execSQL("DROP TABLE IF EXISTS " + UploadContract.TABLE_NAME);
        }

        cursor.close();
        onCreate(db);
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if (mReadableDatabase == null || !mReadableDatabase.isOpen()) {
            mReadableDatabase = super.getReadableDatabase();
        }

        return mReadableDatabase;
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if (mWritableDatabase == null || !mWritableDatabase.isOpen()) {
            mWritableDatabase = super.getWritableDatabase();
        }

        return mWritableDatabase;
    }

    /**
     * Inserts the user to the database
     *
     * @param user
     */
    public void insertUser(@NonNull ImgurUser user) {
        LogUtil.v(TAG, "Inserting user " + user.toString());
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
        values.put(UserContract.COLUMN_REPUTATION, user.getReputation());
        db.insert(UserContract.TABLE_NAME, null, values);
    }

    /**
     * Updates the user's information
     *
     * @param user
     */
    public void updateUserInfo(@NonNull ImgurUser user) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserContract._ID, user.getId());
        values.put(UserContract.COLUMN_NAME, user.getUsername());
        values.put(UserContract.COLUMN_ACCESS_TOKEN, user.getAccessToken());
        values.put(UserContract.COLUMN_REFRESH_TOKEN, user.getRefreshToken());
        values.put(UserContract.COLUMN_ACCESS_TOKEN_EXPIRATION, user.getAccessTokenExpiration());
        values.put(UserContract.COLUMN_CREATED, user.getCreated());
        values.put(UserContract.COLUMN_REPUTATION, user.getReputation());
        db.update(UserContract.TABLE_NAME, values, null, null);
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
            LogUtil.v(TAG, "User present");
            user = new ImgurUser(cursor, true);
        } else {
            LogUtil.v(TAG, "No user present");
        }

        cursor.close();
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
        db.delete(NotificationContract.TABLE_NAME, null, null);
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
        Cursor cursor = db.rawQuery(ProfileContract.SEARCH_USER_SQL, new String[]{username});

        if (cursor.moveToFirst()) {
            user = new ImgurUser(cursor, false);
        }

        cursor.close();
        return user;
    }

    /**
     * Inserts a new profile into the database for caching purposes
     *
     * @param profile
     */
    public void insertProfile(@NonNull ImgurUser profile) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(6);
        values.put(ProfileContract._ID, profile.getId());
        values.put(ProfileContract.COLUMN_USERNAME, profile.getUsername());
        values.put(ProfileContract.COLUMN_BIO, profile.getBio());
        values.put(ProfileContract.COLUMN_REP, profile.getReputation());
        values.put(ProfileContract.COLUMN_LAST_SEEN, profile.getLastSeen());
        values.put(ProfileContract.COLUMN_CREATED, profile.getCreated() / DateUtils.SECOND_IN_MILLIS);
        db.insertWithOnConflict(ProfileContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Inserts an uploaded photo into the database
     *
     * @param photo
     */
    public void insertUploadedPhoto(ImgurPhoto photo) {
        if (photo == null || TextUtils.isEmpty(photo.getLink())) {
            LogUtil.w(TAG, "Null photo can not be inserted");
            return;
        }

        LogUtil.v(TAG, "Inserting Uploaded photo: " + photo.getLink());
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(4);
        values.put(UploadContract.COLUMN_URL, photo.getLink());
        values.put(UploadContract.COLUMN_DELETE_HASH, photo.getDeleteHash());
        values.put(UploadContract.COLUMN_DATE, System.currentTimeMillis());
        values.put(UploadContract.COLUMN_IS_ALBUM, 0);
        db.insert(UploadContract.TABLE_NAME, null, values);
    }

    /**
     * Inserts an uploaded album to the database
     *
     * @param album
     */
    public void insertUploadedAlbum(ImgurAlbum album) {
        if (album == null || TextUtils.isEmpty(album.getLink())) {
            LogUtil.w(TAG, "Null album can not be inserted");
            return;
        }

        LogUtil.v(TAG, "Inserting Uploaded album: " + album.getLink());
        ContentValues values = new ContentValues(5);
        values.put(UploadContract.COLUMN_URL, album.getLink());
        values.put(UploadContract.COLUMN_DELETE_HASH, album.getDeleteHash());
        values.put(UploadContract.COLUMN_DATE, System.currentTimeMillis());
        values.put(UploadContract.COLUMN_IS_ALBUM, 1);
        values.put(UploadContract.COLUMN_COVER_ID, album.getCoverId());
        getWritableDatabase().insert(UploadContract.TABLE_NAME, null, values);
    }

    public Cursor getUploadedPhotos() {
        return getReadableDatabase().rawQuery(UploadContract.GET_UPLOADS_SQL, null);
    }

    /**
     * Deletes the given photo from the Uploaded Photos table
     *
     * @param deleteHash
     */
    public void deleteUploadedPhoto(String deleteHash) {
        if (TextUtils.isEmpty(deleteHash)) return;

        SQLiteDatabase db = getWritableDatabase();
        db.delete(UploadContract.TABLE_NAME, UploadContract.COLUMN_DELETE_HASH + "=?", new String[]{deleteHash});
    }

    /**
     * Adds a list of topics into the database
     *
     * @param topics
     */
    public void addTopics(List<ImgurTopic> topics) {
        if (topics == null || topics.isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(3);

        for (ImgurTopic topic : topics) {
            values.clear();
            values.put(TopicsContract._ID, topic.getId());
            values.put(TopicsContract.COLUMN_NAME, topic.getName());
            values.put(TopicsContract.COLUMN_DESC, topic.getDescription());
            db.insertWithOnConflict(TopicsContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /**
     * Returns a list of all the cached topics
     *
     * @return
     */
    @NonNull
    public List<ImgurTopic> getTopics() {
        List<ImgurTopic> topics = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(TopicsContract.GET_TOPICS_SQL, null);

        while (cursor.moveToNext()) {
            topics.add(new ImgurTopic(cursor));
        }

        cursor.close();
        return topics;
    }

    /**
     * Returns a single topic given its id
     *
     * @param id
     * @return
     */
    public ImgurTopic getTopic(int id) {
        // Wrap in a try/catch to avoid a crash that can occur when a '?' is passed as id
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery(TopicsContract.GET_TOPIC_SQL, new String[]{String.valueOf(id)});
            ImgurTopic topic = null;

            if (cursor.moveToFirst()) {
                topic = new ImgurTopic(cursor);
            }

            cursor.close();
            return topic;
        } catch (SQLiteException ex) {
            LogUtil.e(TAG, "Unable to find topic", ex);
            return null;
        }
    }

    /**
     * Deletes a topic from the databse given its id
     *
     * @param id
     */
    public void deleteTopic(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TopicsContract.TABLE_NAME, TopicsContract._ID + " =?", new String[]{String.valueOf(id)});
    }

    /**
     * Inserts a subreddit into the database
     *
     * @param name
     */
    public void addSubReddit(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(1);
        values.put(SubRedditContract.COLUMN_NAME, name);
        db.insertWithOnConflict(SubRedditContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /**
     * Returns a cursor containing all the search subreddits
     *
     * @return
     */
    public Cursor getSubReddits() {
        return getReadableDatabase().rawQuery(SubRedditContract.GET_SUBREDDITS_SQL, null);
    }

    /**
     * Returns subreddits that match the given name
     *
     * @param name
     * @return
     */
    public Cursor getSubReddits(String name) {
        name = "%" + name + "%";
        return getReadableDatabase().rawQuery(SubRedditContract.SEARCH_SUBREDDIT_SQL, new String[]{name});
    }

    /**
     * Adds a list of Memes to the database
     *
     * @param memes
     */
    public void addMemes(List<ImgurBaseObject> memes) {
        if (memes == null || memes.isEmpty()) {
            LogUtil.w(TAG, "Memes list null or is empty");
            return;
        }

        ContentValues values = new ContentValues();
        SQLiteDatabase db = getWritableDatabase();

        for (ImgurBaseObject i : memes) {
            values.clear();
            values.put(MemeContract._ID, i.getId());
            values.put(MemeContract.COLUMN_TITLE, i.getTitle());
            values.put(MemeContract.COLUMN_LINK, i.getLink());
            db.insert(MemeContract.TABLE_NAME, null, values);
        }
    }

    /**
     * Returns all Memes in the database
     *
     * @return
     */
    @NonNull
    public List<ImgurBaseObject> getMemes() {
        List<ImgurBaseObject> memes = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(MemeContract.GET_MEMES_SQL, null);

        while (cursor.moveToNext()) {
            String id = cursor.getString(MemeContract.COLUMN_INDEX_ID);
            String title = cursor.getString(MemeContract.COLUMN_INDEX_TITLE);
            String link = cursor.getString(MemeContract.COLUMN_INDEX_LINK);
            memes.add(new ImgurBaseObject(id, title, link));
        }

        cursor.close();
        return memes;
    }

    /**
     * Returns Cursor containing all previous gallery searches
     *
     * @return
     */
    public Cursor getPreviousGallerySearches() {
        return getReadableDatabase().rawQuery(GallerySearchContract.GET_PREVIOUS_SEARCHES_SQL, null);
    }

    /**
     * Returns Cursor containing all previous gallery search that are similar to given string
     *
     * @param name
     * @return
     */
    public Cursor getPreviousGallerySearches(String name) {
        name = "%" + name + "%";
        return getReadableDatabase().rawQuery(GallerySearchContract.SEARCH_GALLERY_SQL, new String[]{name});
    }

    /**
     * Adds an entry to the previous gallery search table
     *
     * @param name
     */
    public void addPreviousGallerySearch(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(1);
        values.put(GallerySearchContract.COLUMN_NAME, name);
        db.insertWithOnConflict(GallerySearchContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /**
     * returns the last seen time for a link in use with Muzei. Will return -1 if not found
     *
     * @param link
     * @return
     */
    public long getMuzeiLastSeen(String link) {
        if (TextUtils.isEmpty(link)) return -1;

        long lastSeen = -1;
        Cursor cursor = getReadableDatabase().rawQuery(MuzeiContract.GET_LAST_SEEN_SQL, new String[]{link});
        if (cursor.moveToFirst()) lastSeen = cursor.getLong(0);
        cursor.close();
        return lastSeen;
    }

    /**
     * Adds a new link to the Muzei table. Will replace any duplicate entries
     *
     * @param link
     */
    public void addMuzeiLink(String link) {
        if (TextUtils.isEmpty(link)) return;

        ContentValues values = new ContentValues(2);
        values.put(MuzeiContract.COLUMN_LINK, link);
        values.put(MuzeiContract.COLUMN_LAST_SEEN, System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(MuzeiContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Inserts notifications into the database
     *
     * @param response
     */
    public void insertNotifications(NotificationResponse response) {
        if (response == null || response.data == null) return;

        ContentValues values = new ContentValues();
        SQLiteDatabase db = getWritableDatabase();

        if (!response.data.replies.isEmpty()) {
            LogUtil.v(TAG, "Inserting " + response.data.replies.size() + " reply notifications");

            for (NotificationResponse.Replies r : response.data.replies) {
                values.clear();
                values.put(NotificationContract._ID, r.id);
                values.put(NotificationContract.COLUMN_AUTHOR, r.content.getAuthor());
                values.put(NotificationContract.COLUMN_CONTENT, r.content.getComment());
                values.put(NotificationContract.COLUMN_DATE, r.content.getDate());
                values.put(NotificationContract.COLUMN_CONTENT_ID, r.content.getImageId());
                values.put(NotificationContract.COLUMN_ALBUM_COVER, r.content.getAlbumCoverId());
                values.put(NotificationContract.COLUMN_VIEWED, 0);
                db.insertWithOnConflict(NotificationContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    /**
     * Marks a notification that it has been read
     *
     * @param content The content to be deleted. Either a message, or comment
     */
    public void markNotificationRead(ImgurBaseObject content) {
        if (content == null) return;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(1);
        values.put(NotificationContract.COLUMN_VIEWED, 1);
        String where = null;
        String[] args = null;

        if (content instanceof ImgurComment) {
            ImgurComment comment = (ImgurComment) content;
            where = NotificationContract.COLUMN_CONTENT + "=? AND " + NotificationContract.COLUMN_CONTENT_ID + "=?";
            args = new String[]{comment.getComment(), comment.getImageId()};
        } else {
            LogUtil.w(TAG, "Invalid type of content for deleting notification :" + content.getClass().getSimpleName());
        }

        if (!TextUtils.isEmpty(where) && args != null) {
            db.update(NotificationContract.TABLE_NAME, values, where, args);
        }
    }

    /**
     * Marks all notifications that they have been read
     */
    public void markNotificationsRead() {
        ContentValues values = new ContentValues(1);
        values.put(NotificationContract.COLUMN_VIEWED, 1);
        getWritableDatabase().update(NotificationContract.TABLE_NAME, values, null, null);
    }

    /**
     * Returns the comma separated notification ids
     *
     * @param content
     * @return
     */
    @Nullable
    public String getNotificationIds(ImgurBaseObject content) {
        if (content == null) return null;
        String query;
        String[] args;

        if (content instanceof ImgurComment) {
            query = NotificationContract.GET_NOTIFICATION_ID;
            args = new String[]{((ImgurComment) content).getImageId()};
        } else {
            LogUtil.w(TAG, "Invalid type of content for retrieving  notification id :" + content.getClass().getSimpleName());
            return null;
        }

        if (!TextUtils.isEmpty(query)) {
            Cursor cursor = getReadableDatabase().rawQuery(query, args);
            String[] ids = new String[cursor.getCount()];
            int i = 0;

            while (cursor.moveToNext()) {
                ids[i] = cursor.getString(0);
                i++;
            }

            cursor.close();
            return TextUtils.join(",", ids);
        }

        return null;
    }

    /**
     * Returns the comma separated notification ids for all notifications in the database
     *
     * @return
     */
    @Nullable
    public String getNotificationIds() {
        Cursor cursor = getReadableDatabase().rawQuery(NotificationContract.GET_UNREAD_NOTIFICATIONS_SQL, null);
        String[] ids = new String[cursor.getCount()];
        int i = 0;

        while (cursor.moveToNext()) {
            ids[i] = cursor.getString(0);
            i++;
        }

        cursor.close();
        return ids.length > 0 ? TextUtils.join(",", ids) : null;
    }

    /**
     * Returns all the notifications in the database, minus the duplicate messages
     *
     * @param unreadOnly If the returned notifications should be unread only
     * @return
     */
    @NonNull
    public List<ImgurNotification> getNotifications(boolean unreadOnly) {
        List<ImgurNotification> notifications = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String query = unreadOnly ? NotificationContract.GET_UNREAD_REPLIES_SQL : NotificationContract.GET_REPLIES_SQL;
        Cursor repliesCursor = db.rawQuery(query, null);

        while (repliesCursor.moveToNext()) {
            notifications.add(new ImgurNotification(repliesCursor));
        }

        repliesCursor.close();
        Collections.sort(notifications);
        return notifications;
    }

    /**
     * Deletes the given notifications from the database
     *
     * @param notifications
     */
    public void deleteNotifications(List<ImgurNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();
        Set<String> ids = new HashSet<>();

        for (ImgurNotification n : notifications) {
            if (n.getType() == ImgurNotification.TYPE_MESSAGE) {
                // Messages also have to delete all rows associated with their content_id
                String delete = String.format(NotificationContract.DELETE_MESSAGE_SQL, n.getContentId());
                db.execSQL(delete);
            } else {
                ids.add(String.valueOf(n.getId()));
            }
        }

        if (!ids.isEmpty()) {
            String delete = String.format(NotificationContract.DELETE_NOTIFICATIONS_SQL, TextUtils.join(",", ids));
            db.execSQL(delete);
        }
    }

    /**
     * Deletes all records from a table
     *
     * @param tableName The table to delete from
     * @return the number of records deleted
     */
    public int deleteFromTable(String tableName) {
        return getWritableDatabase().delete(tableName, null, null);
    }

    @Override
    public synchronized void close() {
        if (mReadableDatabase != null) {
            mReadableDatabase.close();
            mReadableDatabase = null;
        }

        if (mWritableDatabase != null) {
            mWritableDatabase.close();
            mWritableDatabase = null;
        }

        super.close();
    }
}
