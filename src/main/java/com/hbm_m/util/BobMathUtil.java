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
}
