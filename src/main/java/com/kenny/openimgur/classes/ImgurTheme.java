package com.kenny.openimgur.classes;

import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.annotation.StyleRes;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 12/8/14.
 */
public enum ImgurTheme {
    BLUE("blue", R.style.Theme_Blue, R.style.Theme_Blue_Dark, R.color.theme_blue_primary, R.color.theme_blue_dark, R.color.theme_blue_accent),
    ORANGE("orange", R.style.Theme_Orange, R.style.Theme_Orange_Dark, R.color.theme_orange_primary, R.color.theme_orange_dark, R.color.theme_light_blue_accent),
    CYAN("cyan", R.style.Theme_Cyan, R.style.Theme_Cyan_Dark, R.color.theme_cyan_primary, R.color.theme_cyan_dark, R.color.theme_cyan_accent),
    GREEN("green", R.style.Theme_Green, R.style.Theme_Green_Dark, R.color.theme_green_primary, R.color.theme_green_dark, R.color.theme_green_accent),
    TEAL("teal", R.style.Theme_Teal, R.style.Theme_Teal_Dark, R.color.theme_teal_primary, R.color.theme_teal_dark, R.color.theme_teal_accent),
    RED("red", R.style.Theme_Red, R.style.Theme_Red_Dark, R.color.theme_red_primary, R.color.theme_red_dark, R.color.theme_red_accent),
    PINK("pink", R.style.Theme_Pink, R.style.Theme_Pink_Dark, R.color.theme_pink_primary, R.color.theme_pink_dark, R.color.theme_pink_accent),
    PURPLE("purple", R.style.Theme_Purple, R.style.Theme_Purple_Dark, R.color.theme_purple_primary, R.color.theme_purple_dark, R.color.theme_purple_accent),
    GREY("gray", R.style.Theme_Grey, R.style.Theme_Grey_Dark, R.color.theme_grey_primary, R.color.theme_grey_dark, R.color.theme_grey_accent),
    BLACK("black", R.style.Theme_Black, R.style.Theme_Black_Dark, R.color.theme_black_primary, R.color.theme_black_dark, R.color.theme_black_accent);

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
