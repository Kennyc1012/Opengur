package com.kenny.openimgur.classes;

import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.annotation.StyleRes;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 12/8/14.
 */
public enum ImgurTheme {
    BLUE("blue", R.style.Theme_Blue, R.color.theme_blue_primary, R.color.theme_blue_dark, R.color.theme_blue_accent),
    ORANGE("orange", R.style.Theme_Orange, R.color.theme_orange_primary, R.color.theme_orange_dark, R.color.theme_light_blue_accent),
    CYAN("cyan", R.style.Theme_Cyan, R.color.theme_cyan_primary, R.color.theme_cyan_dark, R.color.theme_cyan_accent),
    GREEN("green", R.style.Theme_Green, R.color.theme_green_primary, R.color.theme_green_dark, R.color.theme_green_accent),
    TEAL("teal", R.style.Theme_Teal, R.color.theme_teal_primary, R.color.theme_teal_dark, R.color.theme_teal_accent),
    RED("red", R.style.Theme_Red, R.color.theme_red_primary, R.color.theme_red_dark, R.color.theme_red_accent),
    PINK("pink", R.style.Theme_Pink, R.color.theme_pink_primary, R.color.theme_pink_dark, R.color.theme_pink_accent),
    PURPLE("purple", R.style.Theme_Purple, R.color.theme_purple_primary, R.color.theme_purple_dark, R.color.theme_purple_accent);

    public final String themeName;

    public final int theme;

    public final int primaryColor;

    public final int darkColor;

    public final int accentColor;

    private ImgurTheme(String themeName, @StyleRes int theme, @ColorRes int primaryColor, @ColorRes int darkColor, @ColorRes int accentColor) {
        this.themeName = themeName;
        this.theme = theme;
        this.primaryColor = primaryColor;
        this.darkColor = darkColor;
        this.accentColor = accentColor;
    }

    public void applyTheme(Resources.Theme theme) {
        theme.applyStyle(this.theme, true);
    }

    /**
     * Returns the theme corresponding with the given string resource
     *
     * @param themeName
     * @return
     */
    public static ImgurTheme getThemeFromString(String themeName) {
        ImgurTheme theme = BLUE;

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
        }
        return theme;
    }
}
