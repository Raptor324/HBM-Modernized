package com.hbm_m.config;

/**
 * Rest von {@code com.hbm.config.WeaponConfig} (1.7.10), statisch an {@code ConfigSchema} gebunden.
 * dropCell/dropSing/dropCrys/dropDead liegen in {@link ModClothConfig} (dropSing heisst dort dropSingularity).
 */
public final class WeaponConfig {

    private WeaponConfig() { }

    /** Original 7.03_ciwsAccuracy. */
    public static int ciwsHitrate = 50;
    /** Original 10.02_dropStar. */
    public static boolean dropStar = true;
    /** Original 18.00_linearAnimations (im Original nirgends gelesen). */
    public static boolean linearAnimations = false;
}
