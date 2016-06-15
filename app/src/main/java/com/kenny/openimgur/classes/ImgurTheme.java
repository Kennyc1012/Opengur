package com.kenny.openimgur.classes;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.support.annotation.StyleRes;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 12/8/14.
 */
public enum ImgurTheme {
    BLUE(R.style.Theme_Opengur_Light_Blue, R.style.Theme_Dark_Blue, R.color.theme_blue_primary, R.color.theme_blue_dark, R.color.theme_blue_accent),
    ORANGE(R.style.Theme_Opengur_Light_Orange, R.style.Theme_Dark_Orange, R.color.theme_orange_primary, R.color.theme_orange_dark, R.color.theme_light_blue_accent),
    CYAN(R.style.Theme_Opengur_Light_Cyan, R.style.Theme_Dark_Cyan, R.color.theme_cyan_primary, R.color.theme_cyan_dark, R.color.theme_cyan_accent),
    GREEN(R.style.Theme_Opengur_Light_Green, R.style.Theme_Dark_Green, R.color.theme_green_primary, R.color.theme_green_dark, R.color.theme_green_accent),
    TEAL(R.style.Theme_Opengur_Light_Teal, R.style.Theme_Dark_Teal, R.color.theme_teal_primary, R.color.theme_teal_dark, R.color.theme_teal_accent),
    RED(R.style.Theme_Opengur_Light_Red, R.style.Theme_Dark_Red, R.color.theme_red_primary, R.color.theme_red_dark, R.color.theme_red_accent),
    PINK(R.style.Theme_Opengur_Light_Pink, R.style.Theme_Dark_Pink, R.color.theme_pink_primary, R.color.theme_pink_dark, R.color.theme_pink_accent),
    PURPLE(R.style.Theme_Opengur_Light_Purple, R.style.Theme_Dark_Purple, R.color.theme_purple_primary, R.color.theme_purple_dark, R.color.theme_purple_accent),
    GREY(R.style.Theme_Opengur_Light_Grey, R.style.Theme_Dark_Grey, R.color.theme_grey_primary, R.color.theme_grey_dark, R.color.theme_grey_accent),
    BLACK(R.style.Theme_Opengur_Light_Black, R.style.Theme_Dark_Black, R.color.theme_black_primary, R.color.theme_black_dark, R.color.theme_black_accent);

    public final int theme;

    public final int darkTheme;

    public final int primaryColor;

    public final int darkColor;

    public final int accentColor;

    public boolean isDarkTheme = false;

    ImgurTheme(@StyleRes int theme, @StyleRes int darkTheme, @ColorRes int primaryColor, @ColorRes int darkColor, @ColorRes int accentColor) {
        this.darkTheme = darkTheme;
        this.theme = theme;
        this.primaryColor = primaryColor;
        this.darkColor = darkColor;
        this.accentColor = accentColor;
    }

    public void applyTheme(Resources.Theme theme) {
        theme.applyStyle(isDarkTheme ? this.darkTheme : this.theme, true);
    }

    @StyleRes
    public int getAlertDialogTheme() {
        switch (this) {
            case BLUE:
            case ORANGE:
            case CYAN:
            case GREEN:
            case TEAL:
            case PURPLE:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Alert_Accent_Pink : R.style.Theme_AppCompat_Light_Dialog_Alert_Accent_Pink;

            case RED:
            case PINK:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Alert_Accent_Blue : R.style.Theme_AppCompat_Light_Dialog_Alert_Accent_Blue;

            case BLACK:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Alert_Accent_Yellow : R.style.Theme_AppCompat_Light_Dialog_Alert_Accent_Yellow;

            case GREY:
            default:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Alert_Accent_Green : R.style.Theme_AppCompat_Light_Dialog_Alert_Accent_Green;
        }
    }

    @StyleRes
    public int getDialogTheme() {
        switch (this) {
            case BLUE:
            case ORANGE:
            case CYAN:
            case GREEN:
            case TEAL:
            case PURPLE:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Accent_Pink : R.style.Theme_AppCompat_Light_Dialog_Accent_Pink;

            case RED:
            case PINK:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Accent_Blue : R.style.Theme_AppCompat_Light_Dialog_Accent_Blue;

            case BLACK:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Accent_Yellow : R.style.Theme_AppCompat_Light_Dialog_Accent_Yellow;

            case GREY:
            default:
                return isDarkTheme ? R.style.Theme_AppCompat_Dialog_Accent_Green : R.style.Theme_AppCompat_Light_Dialog_Accent_Green;
        }
    }

    @StyleRes
    public int getBottomSheetTheme() {
        return isDarkTheme ? R.style.BottomSheet_Dark : R.style.BottomSheet_Light;
    }

    /**
     * Returns the {@link ColorStateList} for the NavigationView
     *
     * @param res
     * @return
     */
    public ColorStateList getNavigationColors(Resources res) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };

        int[] colors = new int[]{
                res.getColor(accentColor),
                isDarkTheme ? Color.WHITE : Color.BLACK
        };

        return new ColorStateList(states, colors);
    }

    /**
     * Returns a copy of the supplied theme
     *
     * @param theme
     * @return
     */
    public static ImgurTheme copy(ImgurTheme theme) {
        ImgurTheme copy;

        switch (theme) {
            case BLUE:
                copy = BLUE;
                break;

            case ORANGE:
                copy = ORANGE;
                break;

            case CYAN:
                copy = CYAN;
                break;

            case GREEN:
                copy = GREEN;
                break;

            case TEAL:
                copy = TEAL;
                break;

            case RED:
                copy = RED;
                break;

            case PINK:
                copy = PINK;
                break;

            case PURPLE:
                copy = PURPLE;
                break;

            case GREY:
            default:
                copy = GREY;
                break;
        }

        copy.isDarkTheme = theme.isDarkTheme;
        return copy;
    }

    /**
     * Returns the {@link ImgurTheme} based on the saved primary color
     *
     * @param res          App Resources to decode the color
     * @param primaryColor The themes primary color
     * @return The Imgur theme containing the primary color, will return {@link #GREY} if nothing was found
     */
    public static ImgurTheme fromPreferences(Resources res, int primaryColor) {
        ImgurTheme theme = null;

        for (ImgurTheme t : ImgurTheme.values()) {
            if (res.getColor(t.primaryColor) == primaryColor) {
                theme = t;
                break;
            }
        }

        return theme != null ? theme : GREY;
    }
}
