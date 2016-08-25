package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurNotification;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.ColorUtils;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;

/**
 * Created by Kenny-PC on 8/9/2015.
 */
public class NotificationAdapter extends BaseRecyclerAdapter<ImgurNotification> {

    private View.OnClickListener mClickListener;

    private View.OnLongClickListener mLongClickListener;

    private int mDividerColor;

    private int mCircleSize;

    private int mSelectedColor;

    private final Set<ImgurNotification> mSelected = new HashSet<>();

    public NotificationAdapter(Context context, List<ImgurNotification> notifications, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        super(context, notifications, true);
        mCircleSize = getDimension(R.dimen.avatar_size);
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
        mDividerColor = isDarkTheme ? getColor(R.color.primary_dark_light) : Color.BLACK;
        mSelectedColor = getColor(R.color.comment_bg_selected);
    }

    @Override
    public void onDestroy() {
        mClickListener = null;
        mLongClickListener = null;
        super.onDestroy();
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForComments().build();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflateView(R.layout.profile_comment_item, parent);
        view.setOnClickListener(mClickListener);
        view.setOnLongClickListener(mLongClickListener);
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
        String firstLetter = notification.getAuthor().substring(0, 1).toUpperCase();
        int color = ColorUtils.getColor(notification.getAuthor());

        holder.image.setImageDrawable(
                new TextDrawable.Builder()
                        .setWidth(mCircleSize)
                        .setHeight(mCircleSize)
                        .setShape(TextDrawable.DRAWABLE_SHAPE_OVAL)
                        .setColor(color)
                        .setText(firstLetter)
                        .build());

        holder.author.setText(notification.getAuthor() + " " + getDateFormattedTime(notification.getDate()));
        holder.content.setText(notification.getContent());
        boolean selected = mSelected.contains(notification);
        holder.itemView.setBackgroundColor(selected ? mSelectedColor : Color.TRANSPARENT);
    }

    private void renderReply(ImgurNotification notification, NotificationHolder holder) {
        String photoUrl;

        holder.author.setText(notification.getAuthor() + " " + getDateFormattedTime(notification.getDate()));
        holder.content.setText(notification.getContent());

        if (TextUtils.isEmpty(notification.getAlbumCover())) {
            photoUrl = ApiClient.IMGUR_URL + notification.getContentId() + ImgurPhoto.THUMBNAIL_SMALL + ".jpeg";
        } else {
            photoUrl = String.format(ImgurAlbum.ALBUM_COVER_URL, notification.getAlbumCover() + ImgurPhoto.THUMBNAIL_SMALL);
        }

        displayImage(holder.image, photoUrl);
        boolean selected = mSelected.contains(notification);
        holder.itemView.setBackgroundColor(selected ? mSelectedColor : Color.TRANSPARENT);
    }

    /**
     * Sets the notification to be selected or un selected. Passing null will clear all selections
     *
     * @param notification
     * @return If the item was selected. False will infer that it was deselected
     */
    public boolean setSelected(ImgurNotification notification) {
        if (notification == null) {
            mSelected.clear();
            notifyDataSetChanged();
            return true;
        }

        boolean selected;

        if (mSelected.contains(notification)) {
            mSelected.remove(notification);
            selected = false;
        } else {
            mSelected.add(notification);
            selected = true;
        }

        notifyDataSetChanged();
        return selected;
    }

    public boolean hasSelectedItems() {
        return !mSelected.isEmpty();
    }

    public int getSelectedCount() {
        return mSelected.size();
    }

    public void deleteNotifications() {
        for (ImgurNotification n : mSelected) {
            removeItem(n);
        }
    }

    public List<ImgurNotification> getSelectedNotifications() {
        return new ArrayList<>(mSelected);
    }

    private CharSequence getDateFormattedTime(long commentDate) {
        long now = System.currentTimeMillis();
        long difference = System.currentTimeMillis() - commentDate;

        return (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                getString(R.string.moments_ago) :
                DateUtils.getRelativeTimeSpanString(
                        commentDate,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_RELATIVE
                                | DateUtils.FORMAT_ABBREV_ALL);
    }

    static class NotificationHolder extends BaseViewHolder {
        @BindView(R.id.author)
        TextView author;

        @BindView(R.id.comment)
        TextView content;

        @BindView(R.id.image)
        ImageView image;

        @BindView(R.id.divider)
        View divider;

        public NotificationHolder(View view) {
            super(view);
        }
    }
}
