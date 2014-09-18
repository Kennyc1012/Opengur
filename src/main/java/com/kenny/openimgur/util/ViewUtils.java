package com.kenny.openimgur.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.TextViewRoboto;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kcampagna on 7/27/14.
 */
public class ViewUtils {

    /**
     * Returns the height of the actionbar and status bar (4.4+) needed for the translucent style
     *
     * @param context
     * @return
     */
    public static int getHeightForTranslucentStyle(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int height = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        // On 4.4 + devices, we need to account for the status bar
        if (OpenImgurApp.SDK_VERSION >= Build.VERSION_CODES.KITKAT) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                height += context.getResources().getDimensionPixelSize(resourceId);
            }
        }

        return height;
    }

    /**
     * Returns an empty view to occupy the space present in the translucent style
     *
     * @param context
     * @param additionalHeight Additonal height to be added to the view
     * @return
     */
    public static View getHeaderViewForTranslucentStyle(Context context, int additionalHeight) {
        View v = View.inflate(context, R.layout.empty_header, null);
        int height = getHeightForTranslucentStyle(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height + additionalHeight);
        v.setLayoutParams(lp);
        return v;
    }

    /**
     * Returns a view populated with a user's data
     *
     * @param user      The user for whom we are displaying data for
     * @param context   App Context
     * @param container The optional ViewGroup to attach to
     * @param listener  The Imgur listener to listen for link click events
     * @return
     */
    public static View getProfileView(@NonNull ImgurUser user, @NonNull Context context, @Nullable ViewGroup container, @NonNull ImgurListener listener) {
        View header;

        if (container != null) {
            header = LayoutInflater.from(context).inflate(R.layout.profile_header, container, false);
        } else {
            header = LayoutInflater.from(context).inflate(R.layout.profile_header, null);
        }

        String date = new SimpleDateFormat("MMM yyyy").format(new Date(user.getCreated()));
        String reputationText = user.getReputation() + " " + context.getString(R.string.profile_rep_date, date);
        TextViewRoboto notoriety = (TextViewRoboto) header.findViewById(R.id.notoriety);
        notoriety.setText(user.getNotoriety().getStringId());
        int notorietyColor = user.getNotoriety() == ImgurUser.Notoriety.FOREVER_ALONE ?
                context.getResources().getColor(android.R.color.holo_red_light) : context.getResources().getColor(android.R.color.holo_green_light);
        notoriety.setTextColor(notorietyColor);
        ((TextViewRoboto) header.findViewById(R.id.rep)).setText(reputationText);
        ((TextViewRoboto) header.findViewById(R.id.username)).setText(user.getUsername());

        if (!TextUtils.isEmpty(user.getBio())) {
            TextViewRoboto bio = (TextViewRoboto) header.findViewById(R.id.bio);
            bio.setText(user.getBio());
            bio.setMovementMethod(CustomLinkMovement.getInstance(listener));
            Linkify.addLinks(bio, Linkify.WEB_URLS);
        }

        return header;
    }

    /**
     * Returns the height of the navigation bar
     *
     * @param context
     * @return
     */
    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");

        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }

        return 0;
    }
}
