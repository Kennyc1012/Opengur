package com.kenny.openimgur.api;

import com.kenny.openimgur.api.responses.AlbumResponse;
import com.kenny.openimgur.api.responses.BasicObjectResponse;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.CommentPostResponse;
import com.kenny.openimgur.api.responses.CommentResponse;
import com.kenny.openimgur.api.responses.ConverastionResponse;
import com.kenny.openimgur.api.responses.ConvoResponse;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.api.responses.OAuthResponse;
import com.kenny.openimgur.api.responses.PhotoResponse;
import com.kenny.openimgur.api.responses.TagResponse;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.api.responses.UserResponse;

import retrofit.Callback;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;

/**
 * Created by kcampagna on 7/10/15.
 */
public interface ImgurService {

    // Get Requests
    @GET("/3/gallery/{section}/{sort}/{page}")
    void getGallery(@Path("section") String section, @Path("sort") String sort, @Path("page") int page, @Query("showViral") boolean showViral, Callback<GalleryResponse> callback);

    @GET("/3/gallery/{section}/{sort}/{page}")
    GalleryResponse getGallery(@Path("section") String section, @Path("sort") String sort, @Path("page") int page, @Query("showViral") boolean showViral);

    @GET("/3/gallery/{section}/top/{window}/{page}")
    void getGalleryForTopSorted(@Path("section") String section, @Path("window") String window, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/gallery/{id}")
    void getGalleryDetails(@Path("id") String itemId, Callback<BasicObjectResponse> callback);

    @GET("/3/image/{id}")
    void getImageDetails(@Path("id") String imageId, Callback<PhotoResponse> callback);

    @GET("/3/gallery/{id}/images")
    void getAlbumImages(@Path("id") String albumId, Callback<AlbumResponse> callback);

    @GET("/3/gallery/{id}/comments/{sort}")
    void getComments(@Path("id") String itemId, @Path("sort") String commentSort, Callback<CommentResponse> callback);

    @GET("/3/account/{user}")
    void getProfile(@Path("user") String username, Callback<UserResponse> callback);

    @GET("/3/account/{user}/favorites")
    void getProfileFavorites(@Path("user") String username, Callback<GalleryResponse> callback);

    @GET("/3/account/{user}/gallery_favorites/{page}/newest")
    void getProfileGalleryFavorites(@Path("user") String username, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/account/{user}/submissions/{page}")
    void getProfileSubmissions(@Path("user") String username, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/account/{user}/comments/{sort}/{page}")
    void getProfileComments(@Path("user") String username, @Path("sort") String sort, @Path("page") int page, Callback<CommentResponse> callback);

    @GET("/3/account/{user}/albums/{page}")
    void getProfileAlbums(@Path("user") String username, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/account/{user}/images/{page}")
    void getProfileUploads(@Path("user") String username, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/conversations")
    void getConversations(Callback<ConvoResponse> callback);

    @GET("/3/gallery/r/{subreddit}/{sort}/{page}")
    void getSubReddit(@Path("subreddit") String query, @Path("sort") String sort, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/gallery/r/{subreddit}/{sort}/{page}")
    GalleryResponse getSubReddit(@Path("subreddit") String query, @Path("sort") String sort, @Path("page") int page);

    @GET("/3/gallery/r/{subreddit}/top/{window}/{page}")
    void getSubRedditForTopSorted(@Path("subreddit") String query, @Path("window") String window, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/gallery/random/{page}")
    void getRandomGallery(@Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/topics/defaults")
    void getDefaultTopics(Callback<TopicResponse> callback);

    @GET("/3/topics/{topic}/{sort}/{page}")
    void getTopic(@Path("topic") int topicId, @Path("sort") String sort, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/topics/{topic}/{sort}/{page}")
    GalleryResponse getTopic(@Path("topic") int topicId, @Path("sort") String sort, @Path("page") int page);

    @GET("/3/topics/{topic}/top/{window}/{page}")
    void getTopicForTopSorted(@Path("topic") int topicId, @Path("window") String window, @Path("page") int page, Callback<GalleryResponse> callback);

    @GET("/3/memegen/defaults")
    void getDefaultMemes(Callback<GalleryResponse> callback);

    @GET("/3/gallery/search/{sort}/{page}")
    void searchGallery(@Path("sort") String sort, @Path("page") int page, @Query("q") String query, Callback<GalleryResponse> callback);

    @GET("/3/gallery/search/top/{window}/{page}")
    void searchGalleryForTopSorted(@Path("window") String window, @Path("page") int page, @Query("q") String query, Callback<GalleryResponse> callback);

    @GET("/3/gallery/{id}/tags")
    void getTags(@Path("id") String itemId, Callback<TagResponse> callback);

    @GET("/3/conversations/{id}/{page}/0")
    void getMessages(@Path("id") String conversationId, @Path("page") int page, Callback<ConverastionResponse> callback);

    @GET("/3/notification?new=true")
    void getNotifications(Callback<NotificationResponse> response);

    @GET("/3/notification?new=true")
    NotificationResponse getNotifications();


    // Post Requests. Some of the POST requests have fields when they are not needed. This is because OKHTTP requires a body when posting
    @FormUrlEncoded
    @POST("/3/image/{id}/favorite")
    void favoriteImage(@Path("id") String imageId, @Field("id") String id, Callback<BasicResponse> callback);

    @FormUrlEncoded
    @POST("/3/album/{id}/favorite")
    void favoriteAlbum(@Path("id") String albumId, @Field("id") String id, Callback<BasicResponse> callback);

    @Multipart
    @POST("/3/upload")
    PhotoResponse uploadPhoto(@Part("image") TypedFile file, @Part("title") TypedString title, @Part("description") TypedString description, @Part("type") TypedString type);

    @FormUrlEncoded
    @POST("/3/upload")
    PhotoResponse uploadLink(@Field("image") String link, @Field("title") String title, @Field("description") String description, @Field("type") String type);

    @FormUrlEncoded
    @POST("/3/gallery/{id}")
    BasicResponse submitToGallery(@Path("id") String id, @Field("title") String title, @Field("topic") int topicId, @Field("terms") String terms);

    @FormUrlEncoded
    @POST("/3/album")
    BasicObjectResponse createAlbum(@Field("ids") String ids, @Field("cover") String coverId, @Field("title") String title, @Field("description") String description);

    @FormUrlEncoded
    @POST("/3/gallery/{id}/vote/{vote}")
    void voteOnGallery(@Path("id") String itemId, @Path("vote") String vote, @Field("vote") String itemVote, Callback<BasicResponse> callback);

    @FormUrlEncoded
    @POST("/3/comment/{id}/vote/{vote}")
    void voteOnComment(@Path("id") String itemId, @Path("vote") String vote, @Field("vote") String itemVote, Callback<BasicResponse> callback);

    @FormUrlEncoded
    @POST("/3/gallery/{galleryId}/comment")
    void postComment(@Path("galleryId") String galleryId, @Field("comment") String comment, Callback<CommentPostResponse> callback);

    @FormUrlEncoded
    @POST("/3/gallery/{galleryId}/comment/{parentId}")
    void postCommentReply(@Path("galleryId") String galleryId, @Path("parentId") String parentId, @Field("comment") String comment, Callback<CommentPostResponse> callback);

    @FormUrlEncoded
    @POST("/3/conversations/{recipient}")
    void sendMessage(@Path("recipient") String recipientId, @Field("body") String message, Callback<BasicResponse> callback);

    @FormUrlEncoded
    @POST("/3/conversations/block/{username}")
    void blockUser(@Path("username") String username, @Field("username") String user, Callback<BasicResponse> response);

    @FormUrlEncoded
    @POST("/3/conversations/report/{username}")
    void reportUser(@Path("username") String username, @Field("username") String user, Callback<BasicResponse> response);

    @FormUrlEncoded
    @POST("/oauth2/token")
    OAuthResponse refreshToken(@Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("refresh_token") String refreshToken, @Field("grant_type") String grantType);

    @FormUrlEncoded
    @POST("/3/gallery/{id}/report")
    void reportPost(@Path("id") String galleryId, @Field("reason") int reason, Callback<BasicResponse> response);

    @FormUrlEncoded
    @POST("/3/notification/")
    void markNotificationsRead(@Field("ids") String ids, Callback<BasicResponse> response);

    // Delete Requests
    @DELETE("/3/album/{deleteHash}")
    void deleteAlbum(@Path("deleteHash") String deleteHash, Callback<BasicResponse> callback);

    @DELETE("/3/image/{deleteHash}")
    void deletePhoto(@Path("deleteHash") String deleteHash, Callback<BasicResponse> callback);

    @DELETE("/3/conversations/{id}")
    void deleteConversation(@Path("id") String conversationId, Callback<BasicResponse> callback);
}
