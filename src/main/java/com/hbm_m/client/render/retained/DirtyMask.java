package com.hbm_m.client.render.retained;

/**
 * Event-driven bitmask identifying which slices of the 32-float (128-byte) instance record
 * require GPU update.
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class DirtyMask {

    public static final int TRANSFORM = 1 << 0; // Floats 0..4  (InstPos, InstRot)
    public static final int BOUNDS    = 1 << 1; // Floats 5..11 (InstBboxMin, InstBboxSize, Fade)
    public static final int LIGHT     = 1 << 2; // Floats 12..15 (InstLight corners)
    public static final int UV        = 1 << 3; // Floats 16..19 (InstUvRect)
    public static final int COLOR     = 1 << 4; // Floats 20..23 (InstColor)
    public static final int GRAD      = 1 << 5; // Floats 24..27 (GradParams)
    public static final int ANIM      = 1 << 6; // Floats 28..31 (AnimParams)
    public static final int ALL       = 0x7F;   // All 32 floats (128 bytes)

    private DirtyMask() {}
}
