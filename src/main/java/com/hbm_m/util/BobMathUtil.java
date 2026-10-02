package com.hbm_m.util;

/**
 * Math helpers used by particle and explosion code
 */
public final class BobMathUtil {

    private BobMathUtil() {}

    /** Soft sqrt-like curve used for nuke scale. */
    public static double squirt(double x) {
        return Math.sqrt(x + 1.0 / ((x + 2.0) * (x + 2.0))) - 1.0 / (x + 2.0);
    }

    /** 1:1 {@code BobMathUtil.getShortNumber}. */
    public static String getShortNumber(long l) {

        double res;
        String suffix = "";
        long abs = Math.abs(l);

        if (abs >= Math.pow(10, 18)) {
            res = l / Math.pow(10, 18);
            suffix = "E";
        } else if (abs >= Math.pow(10, 15)) {
            res = l / Math.pow(10, 15);
            suffix = "P";
        } else if (abs >= Math.pow(10, 12)) {
            res = l / Math.pow(10, 12);
            suffix = "T";
        } else if (abs >= Math.pow(10, 9)) {
            res = l / Math.pow(10, 9);
            suffix = "G";
        } else if (abs >= Math.pow(10, 6)) {
            res = l / Math.pow(10, 6);
            suffix = "M";
        } else if (abs >= Math.pow(10, 3)) {
            res = l / Math.pow(10, 3);
            suffix = "k";
        } else {
            return Long.toString(l);
        }

        // Edgecase: a negative triple digit number would result in a 8 character long result so we will loose one decimal place
        if (res <= -100.0) {
            res = Math.round(res * 10.0) / 10.0;
        } else {
            res = Math.round(res * 100.0) / 100.0;
        }

        return res + suffix;
    }
}
