package com.kenny.openimgur.util;

/**
 * Created by DiegoFranco on 3/30/15.
 */
public class ScoreUtil {
    private static final String STRING_THOUSAND = "K";
    private static final String STRING_MILLION = "M";
    private static final int HUNDRED = 100;
    private static final int THOUSAND = 1000;
    private static final int MILLION = 1000000;

    /**
     * return the score convert to text in good format
     * @param score
     * @return
     */
    public static String getCounterScore(int score) {
        StringBuilder strB = new StringBuilder();
        if(score == 0 || score < HUNDRED) {
            return Integer.toString(score);
        } else if(score >= HUNDRED && score < THOUSAND) {
            return Integer.toString(score);
        } else if(score > HUNDRED && score < MILLION) {
            strB.append(Integer.toString(score/THOUSAND) + STRING_THOUSAND);
        } else if(score > MILLION) {
            strB.append(Integer.toString(score/MILLION) + STRING_MILLION);
        }
        return strB.toString();
    }
}
