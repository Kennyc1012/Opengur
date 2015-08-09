package com.kenny.openimgur.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurNotification;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.Bind;

/**
 * Created by Kenny-PC on 8/9/2015.
 */
public class NotificationAdapter extends BaseRecyclerAdapter<ImgurNotification> {

    private View.OnClickListener mClickListener;

    private int mDividerColor;

    private int mCircleSize;

    public NotificationAdapter(Context context, List<ImgurNotification> notifications, View.OnClickListener clickListener) {
        super(context, notifications, true);
        mCircleSize = context.getResources().getDimensionPixelSize(R.dimen.avatar_size);
        mClickListener = clickListener;
        mDividerColor = mIsDarkTheme ? context.getResources().getColor(R.color.primary_dark_material_light) : context.getResources().getColor(R.color.primary_dark_material_dark);
    }

    @Override
    public void onDestroy() {
        mClickListener = null;
        super.onDestroy();
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForComments().build();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.profile_comment_item, parent, false);
        view.setOnClickListener(mClickListener);
        NotificationHolder holder = new NotificationHolder(view);
        holder.divider.setBackgroundColor(mDividerColor);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        NotificationHolder notificationHolder = (NotificationHolder) holder;
        ImgurNotification notification = getItem(position);

        if (notification.getType() == ImgurNotification.TYPE_MESSAGE) {
            renderMessage(notification, notificationHolder);
        } else {
            renderReply(notification, notificationHolder);
        }
    }

    private void renderMessage(ImgurNotification notification, NotificationHolder holder) {
        String firstLetter = notification.getAuthor().substring(0, 1);
        int color = ColorGenerator.DEFAULT.getColor(notification.getAuthor());

        holder.image.setImageDrawable(
                TextDrawable.builder()
                        .beginConfig()
                        .toUpperCase()
                        .width(mCircleSize)
                        .height(mCircleSize)
                        .endConfig()
                        .buildRound(firstLetter, color));

        holder.author.setText(notification.getAuthor() + " " + getDateFormattedTime(notification.getDate(), holder.author.getContext()));
        holder.content.setText(notification.getContent());
    }

    private void renderReply(ImgurNotification notification, NotificationHolder holder) {
        String photoUrl;

        holder.author.setText(notification.getAuthor() + " " + getDateFormattedTime(notification.getDate(), holder.author.getContext()));
        holder.content.setText(notification.getContent());

        if (TextUtils.isEmpty(notification.getAlbumCover())) {
            photoUrl = "https://imgur.com/" + notification.getGalleryId() + ImgurPhoto.THUMBNAIL_SMALL + ".jpeg";
        } else {
            photoUrl = String.format(ImgurAlbum.ALBUM_COVER_URL, notification.getAlbumCover() + ImgurPhoto.THUMBNAIL_SMALL);
        }

        displayImage(holder.image, photoUrl);
    }

    private CharSequence getDateFormattedTime(long commentDate, Context context) {
        long now = System.currentTimeMillis();
        long difference = System.currentTimeMillis() - commentDate;

        return (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                context.getResources().getString(R.string.moments_ago) :
                DateUtils.getRelativeTimeSpanString(
                        commentDate,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_RELATIVE
                                | DateUtils.FORMAT_ABBREV_ALL);
    }

    static class NotificationHolder extends BaseViewHolder {
        @Bind(R.id.author)
        TextView author;

        @Bind(R.id.comment)
        TextView content;

        @Bind(R.id.image)
        ImageView image;

        @Bind(R.id.divider)
        View divider;

        public NotificationHolder(View view) {
            super(view);
        }
    }
}
