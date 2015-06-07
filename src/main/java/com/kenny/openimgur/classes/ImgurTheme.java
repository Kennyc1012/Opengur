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
    BLUE("blue", R.style.Theme_Light_Blue, R.style.Theme_Dark_Blue, R.color.theme_blue_primary, R.color.theme_blue_dark, R.color.theme_blue_accent),
    ORANGE("orange", R.style.Theme_Light_Orange, R.style.Theme_Dark_Orange, R.color.theme_orange_primary, R.color.theme_orange_dark, R.color.theme_light_blue_accent),
    CYAN("cyan", R.style.Theme_Light_Cyan, R.style.Theme_Dark_Cyan, R.color.theme_cyan_primary, R.color.theme_cyan_dark, R.color.theme_cyan_accent),
    GREEN("green", R.style.Theme_Light_Green, R.style.Theme_Dark_Green, R.color.theme_green_primary, R.color.theme_green_dark, R.color.theme_green_accent),
    TEAL("teal", R.style.Theme_Light_Teal, R.style.Theme_Dark_Teal, R.color.theme_teal_primary, R.color.theme_teal_dark, R.color.theme_teal_accent),
    RED("red", R.style.Theme_Light_Red, R.style.Theme_Dark_Red, R.color.theme_red_primary, R.color.theme_red_dark, R.color.theme_red_accent),
    PINK("pink", R.style.Theme_Light_Pink, R.style.Theme_Dark_Pink, R.color.theme_pink_primary, R.color.theme_pink_dark, R.color.theme_pink_accent),
    PURPLE("purple", R.style.Theme_Light_Purple, R.style.Theme_Dark_Purple, R.color.theme_purple_primary, R.color.theme_purple_dark, R.color.theme_purple_accent),
    GREY("gray", R.style.Theme_Light_Grey, R.style.Theme_Dark_Grey, R.color.theme_grey_primary, R.color.theme_grey_dark, R.color.theme_grey_accent),
    BLACK("black", R.style.Theme_Light_Black, R.style.Theme_Dark_Black, R.color.theme_black_primary, R.color.theme_black_dark, R.color.theme_black_accent);

    public final String themeName;

    public final int theme;

    public final int darkTheme;

    public final int primaryColor;

    public final int darkColor;

    public final int accentColor;

    public boolean isDarkTheme = false;

    ImgurTheme(String themeName, @StyleRes int theme, @StyleRes int darkTheme, @ColorRes int primaryColor, @ColorRes int darkColor, @ColorRes int accentColor) {
        this.themeName = themeName;
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
    public int getDialogTheme() {
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
     * Returns the theme corresponding with the given string resource
     *
     * @param themeName
     * @return
     */
    public static ImgurTheme getThemeFromString(String themeName) {
        ImgurTheme theme = GREY;

        if (ORANGE.themeName.equalsIgnoreCase(themeName)) {
            theme = ORANGE;
        } else if (CYAN.themeName.equalsIgnoreCase(themeName)) {
            theme = CYAN;
        } else if (GREEN.themeName.equalsIgnoreCase(themeName)) {
            theme = GREEN;
        } else if (TEAL.themeName.equalsIgnoreCase(themeName)) {
            theme = TEAL;
        } else if (RED.themeName.equalsIgnoreCase(themeName)) {
            theme = RED;
        } else if (PINK.themeName.equalsIgnoreCase(themeName)) {
            theme = PINK;
        } else if (PURPLE.themeName.equalsIgnoreCase(themeName)) {
            theme = PURPLE;
        } else if (BLUE.themeName.equalsIgnoreCase(themeName)) {
            theme = BLUE;
        } else if (BLACK.themeName.equalsIgnoreCase(themeName)) {
            theme = BLACK;
        }

        return theme;
    }
}
