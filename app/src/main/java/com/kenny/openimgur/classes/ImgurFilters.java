package com.kenny.openimgur.classes;

import android.support.annotation.StringRes;

import com.kenny.openimgur.R;

/**
 * Created by Kenny-PC on 2/7/2015.
 */
public class ImgurFilters {

    public enum GallerySection {
        HOT("hot"),
        USER("user");

        private final String mSection;

        GallerySection(String s) {
            mSection = s;
        }

        public String getSection() {
            return mSection;
        }

        /**
         * Returns the Enum value for the section based on the string
         *
         * @param section
         * @return
         */
        public static GallerySection getSectionFromString(String section) {
            if (USER.getSection().equals(section)) {
                return USER;
            }

            return HOT;
        }

        /**
         * Returns the String Resource for the section
         *
         * @return
         */
        @StringRes
        public int getResourceId() {
            switch (this) {
                case HOT:
                    return R.string.filter_viral;

                case USER:
                    return R.string.filter_user_sub;
            }

            return R.string.filter_viral;
        }
    }

    public enum GallerySort {
        TIME("time"),
        RISING("rising"),
        VIRAL("viral"),
        HIGHEST_SCORING("top");

        private final String mSort;

        GallerySort(String s) {
            mSort = s;
        }

        public String getSort() {
            return mSort;
        }

        /**
         * Returns the Enum value based on a string
         *
         * @param sort
         * @return
         */
        public static GallerySort getSortFromString(String sort) {
            if (TIME.getSort().equals(sort)) {
                return TIME;
            } else if (RISING.getSort().equals(sort)) {
                return RISING;
            } else if (HIGHEST_SCORING.getSort().equals(sort)) {
                return HIGHEST_SCORING;
            }

            return VIRAL;
        }
    }

    public enum RedditSort {
        TIME("time"),
        TOP("top");

        private final String mSort;

        RedditSort(String sort) {
            mSort = sort;
        }

        public String getSort() {
            return mSort;
        }

        public static RedditSort getSortFromString(String item) {
            for (RedditSort s : RedditSort.values()) {
                if (s.getSort().equals(item)) {
                    return s;
                }
            }

            return TIME;
        }
    }

    public enum TimeSort {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year"),
        ALL("all");

        private final String mSort;

        TimeSort(String sort) {
            mSort = sort;
        }

        public String getSort() {
            return mSort;
        }

        public static TimeSort getSortFromString(String sort) {
            for (TimeSort s : TimeSort.values()) {
                if (s.getSort().equals(sort)) {
                    return s;
                }
            }

            return DAY;
        }
    }

    public enum CommentSort {
        BEST("best"),
        WORST("worst"),
        NEWEST("newest"),
        OLDEST("oldest");

        private final String mSort;

        CommentSort(String sort) {
            mSort = sort;
        }

        public String getSort() {
            return mSort;
        }

        public static CommentSort getSortType(String sort) {
            if (BEST.getSort().equalsIgnoreCase(sort)) {
                return BEST;
            } else if (WORST.getSort().equalsIgnoreCase(sort)) {
                return WORST;
            } else if (OLDEST.getSort().equalsIgnoreCase(sort)) {
                return OLDEST;
            }

            return NEWEST;
        }
    }
}
