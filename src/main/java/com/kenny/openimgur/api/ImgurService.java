package com.kenny.openimgur.api;

import com.kenny.openimgur.api.responses.AlbumResponse;
import com.kenny.openimgur.api.responses.BasicObjectResponse;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.CommentPostResponse;
import com.kenny.openimgur.api.responses.CommentResponse;
import com.kenny.openimgur.api.responses.ConversationResponse;
import com.kenny.openimgur.api.responses.ConvoResponse;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.api.responses.OAuthResponse;
import com.kenny.openimgur.api.responses.PhotoResponse;
import com.kenny.openimgur.api.responses.TagResponse;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.api.responses.UserResponse;
import com.squareup.okhttp.RequestBody;

import retrofit.Call;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by kcampagna on 7/10/15.
 */
public interface ImgurService {

    // Get Requests
    @GET("/3/gallery/{section}/{sort}/{page}")
    Call<GalleryResponse> getGallery(@Path("section") String section, @Path("sort") String sort, @Path("page") int page, @Query("showViral") boolean showViral);

    @GET("/3/gallery/{section}/top/{window}/{page}")
    Call<GalleryResponse> getGalleryForTopSorted(@Path("section") String section, @Path("window") String window, @Path("page") int page);

    @GET("/3/gallery/{id}")
    Call<BasicObjectResponse> getGalleryDetails(@Path("id") String itemId);

    @GET("/3/image/{id}")
    Call<PhotoResponse> getImageDetails(@Path("id") String imageId);

    @GET("/3/gallery/{id}/images")
    Call<AlbumResponse> getAlbumImages(@Path("id") String albumId);

    @GET("/3/gallery/{id}/comments/{sort}")
    Call<CommentResponse> getComments(@Path("id") String itemId, @Path("sort") String commentSort);

    @GET("/3/account/{user}")
    Call<UserResponse> getProfile(@Path("user") String username);

    @GET("/3/account/{user}/favorites/{page}")
    Call<GalleryResponse> getProfileFavorites(@Path("user") String username,@Path("page") int page);

    @GET("/3/account/{user}/gallery_favorites/{page}/newest")
    Call<GalleryResponse> getProfileGalleryFavorites(@Path("user") String username, @Path("page") int page);

    @GET("/3/account/{user}/submissions/{page}")
    Call<GalleryResponse> getProfileSubmissions(@Path("user") String username, @Path("page") int page);

    @GET("/3/account/{user}/comments/{sort}/{page}")
    Call<CommentResponse> getProfileComments(@Path("user") String username, @Path("sort") String sort, @Path("page") int page);

    @GET("/3/account/{user}/albums/{page}")
    Call<GalleryResponse> getProfileAlbums(@Path("user") String username, @Path("page") int page);

    @GET("/3/account/{user}/images/{page}")
    Call<GalleryResponse> getProfileUploads(@Path("user") String username, @Path("page") int page);

    @GET("/3/conversations")
    Call<ConvoResponse> getConversations();

    @GET("/3/gallery/r/{subreddit}/{sort}/{page}")
    Call<GalleryResponse> getSubReddit(@Path("subreddit") String query, @Path("sort") String sort, @Path("page") int page);

    @GET("/3/gallery/r/{subreddit}/top/{window}/{page}")
    Call<GalleryResponse> getSubRedditForTopSorted(@Path("subreddit") String query, @Path("window") String window, @Path("page") int page);

    @GET("/3/gallery/random/{page}")
    Call<GalleryResponse> getRandomGallery(@Path("page") int page);

    @GET("/3/topics/defaults")
    Call<TopicResponse> getDefaultTopics();

    @GET("/3/topics/{topic}/{sort}/{page}")
    Call<GalleryResponse> getTopic(@Path("topic") int topicId, @Path("sort") String sort, @Path("page") int page);

    @GET("/3/topics/{topic}/top/{window}/{page}")
    Call<GalleryResponse> getTopicForTopSorted(@Path("topic") int topicId, @Path("window") String window, @Path("page") int page);

    @GET("/3/memegen/defaults")
    Call<GalleryResponse> getDefaultMemes();

    @GET("/3/gallery/search/{sort}/{page}")
    Call<GalleryResponse> searchGallery(@Path("sort") String sort, @Path("page") int page, @Query("q") String query);

    @GET("/3/gallery/search/top/{window}/{page}")
    Call<GalleryResponse> searchGalleryForTopSorted(@Path("window") String window, @Path("page") int page, @Query("q") String query);

    @GET("/3/gallery/{id}/tags")
    Call<TagResponse> getTags(@Path("id") String itemId);

    @GET("/3/conversations/{id}/{page}/0")
    Call<ConversationResponse> getMessages(@Path("id") String conversationId, @Path("page") int page);

    @GET("/3/notification?new=true")
    Call<NotificationResponse> getNotifications();


    // Post Requests. Some of the POST requests have fields when they are not needed. This is because OKHTTP requires a body when posting
    @FormUrlEncoded
    @POST("/3/image/{id}/favorite")
    Call<BasicResponse> favoriteImage(@Path("id") String imageId, @Field("id") String id);

    @FormUrlEncoded
    @POST("/3/album/{id}/favorite")
    Call<BasicResponse> favoriteAlbum(@Path("id") String albumId, @Field("id") String id);

    @Multipart
    @POST("/3/upload")
    Call<PhotoResponse> uploadPhoto(@Part("image") RequestBody file, @Part("title") RequestBody title, @Part("description") RequestBody description, @Part("type") RequestBody type);

    @FormUrlEncoded
    @POST("/3/upload")
    Call<PhotoResponse> uploadLink(@Field("image") String link, @Field("title") String title, @Field("description") String description, @Field("type") String type);

    @FormUrlEncoded
    @POST("/3/gallery/{id}")
    Call<BasicResponse> submitToGallery(@Path("id") String id, @Field("title") String title, @Field("topic") int topicId, @Field("terms") String terms);

    @FormUrlEncoded
    @POST("/3/album")
    Call<BasicObjectResponse> createAlbum(@Field("ids") String ids, @Field("cover") String coverId, @Field("title") String title, @Field("description") String description);

    @FormUrlEncoded
    @POST("/3/gallery/{id}/vote/{vote}")
    Call<BasicResponse> voteOnGallery(@Path("id") String itemId, @Path("vote") String vote, @Field("vote") String itemVote);

    @FormUrlEncoded
    @POST("/3/comment/{id}/vote/{vote}")
    Call<BasicResponse> voteOnComment(@Path("id") String itemId, @Path("vote") String vote, @Field("vote") String itemVote);

    @FormUrlEncoded
    @POST("/3/gallery/{galleryId}/comment")
    Call<CommentPostResponse> postComment(@Path("galleryId") String galleryId, @Field("comment") String comment);

    @FormUrlEncoded
    @POST("/3/gallery/{galleryId}/comment/{parentId}")
    Call<CommentPostResponse> postCommentReply(@Path("galleryId") String galleryId, @Path("parentId") String parentId, @Field("comment") String comment);

    @FormUrlEncoded
    @POST("/3/conversations/{recipient}")
    Call<BasicResponse> sendMessage(@Path("recipient") String recipientId, @Field("body") String message);

    @FormUrlEncoded
    @POST("/3/conversations/block/{username}")
    Call<BasicResponse> blockUser(@Path("username") String username, @Field("username") String user);

    @FormUrlEncoded
    @POST("/3/conversations/report/{username}")
    Call<BasicResponse> reportUser(@Path("username") String username, @Field("username") String user);

    @FormUrlEncoded
    @POST("/oauth2/token")
    Call<OAuthResponse> refreshToken(@Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("refresh_token") String refreshToken, @Field("grant_type") String grantType);

    @FormUrlEncoded
    @POST("/3/gallery/{id}/report")
    Call<BasicResponse> reportPost(@Path("id") String galleryId, @Field("reason") int reason);

    @FormUrlEncoded
    @POST("/3/notification/")
    Call<BasicResponse> markNotificationsRead(@Field("ids") String ids);

    // Delete Requests
    @DELETE("/3/album/{deleteHash}")
    Call<BasicResponse> deleteAlbum(@Path("deleteHash") String deleteHash);

    @DELETE("/3/image/{deleteHash}")
    Call<BasicResponse> deletePhoto(@Path("deleteHash") String deleteHash);

    @DELETE("/3/conversations/{id}")
    Call<BasicResponse> deleteConversation(@Path("id") String conversationId);
}
