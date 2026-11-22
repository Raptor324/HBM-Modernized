package com.hbm_m.client.render;

/**
 * IEEE 754 binary16 (half float) codec for the packed AnimParams record words
 * (attrib 15): two 16-bit joint parameters per record float, decoded in the
 * vertex shaders by {@code halfDecode} bit math (unpackHalf2x16 is GLSL 4.20
 * core / ARB_gpu_shader5 - NOT available on a #version 330 shader; the record
 * stays a float-typed vec4 attribute and the shader reinterprets it via
 * floatBitsToUint, which IS 3.30 core). {@code Float.floatToFloat16} does not
 * exist on the 1.20.1 toolchain (Java 17 - the method is Java 20+), so the
 * conversion is done by hand: round-to-nearest-even, subnormals included.
 * <p>
 * No Mojang/loader imports on purpose: pure math, unit-testable standalone and
 * server-safe (hence no {@code @OnlyIn}).
 * <p>
 * Precision contract: rotate angles are wrapped to (-180, 180] by the encoder
 * (ulp &lt;= 0.125 deg), translate distances are physically bounded blocks
 * (ulp ~1 mm) - the quantization is far below the visible threshold, and the
 * step function of the packed value makes the record skip-write MORE stable
 * than raw float32 (sub-ulp jitter vanishes). Consumers compare records with
 * {@code floatToRawIntBits} (non-canonicalizing) - a packed NaN payload cannot
 * be silently rewritten.
 */
public final class HalfFloat {

    private HalfFloat() {}

    /** float -> binary16 bit pattern (returned as a 16-bit unsigned int). RNE rounding. */
    public static int pack(float f) {
        int bits = Float.floatToRawIntBits(f);
        int sign = (bits >>> 16) & 0x8000;
        int exp = (bits >>> 23) & 0xFF;
        int mant = bits & 0x007FFFFF;
        if (exp == 255) {
            // Inf / NaN: keep the sign, force the NaN payload non-zero (quiet bit).
            return sign | 0x7C00 | (mant != 0 ? 0x0200 | (mant >>> 13) : 0);
        }
        int e = exp - 127 + 15;
        if (e >= 31) {
            return sign | 0x7C00; // overflow -> Inf (encoder inputs are physically bounded)
        }
        if (e <= 0) {
            if (e < -10) {
                return sign; // rounds to zero (sign preserved)
            }
            // Subnormal half: shift the (implicit-bit-restored) mantissa down and
            // round to nearest even. half 0x3FF rounding up to 0x400 lands exactly
            // on the smallest normal (2^-14) - the carry is the correct value.
            mant |= 0x00800000;
            int shift = 126 - exp; // 14..24
            int half = mant >> shift;
            int rem = mant & ((1 << shift) - 1);
            int halfPoint = 1 << (shift - 1);
            if (rem > halfPoint || (rem == halfPoint && (half & 1) != 0)) {
                half++;
            }
            return sign | half;
        }
        int half = mant >> 13;
        int rem = mant & 0x1FFF;
        if (rem > 0x1000 || (rem == 0x1000 && (half & 1) != 0)) {
            half++;
        }
        // ADD, not OR: the rounded half can carry into the exponent (mant
        // 0x7FFF.. -> half 0x1000; e.g. 127.9995 must round to 128.0, an OR
        // would fold the carry into e's LSB and return 64.0). half=0x1000 +
        // e=30 (values ~2^16) overflows past the max normal - clamp to Inf.
        int res = (e << 10) + half;
        if (res >= 0x7C00) {
            res = 0x7C00;
        }
        return sign | res;
    }

    /**
     * Canonical packed form: two half words into a 32-bit int ({@code lo} in the
     * low 16 bits, {@code hi} in the high) - the exact layout the VSH's
     * {@code halfDecode} (and GLSL 4.20's unpackHalf2x16) expect. Use this when
     * the destination is an int-typed buffer or a bit comparison.
     */
    public static int pack2Int(float lo, float hi) {
        return ((pack(hi) & 0xFFFF) << 16) | (pack(lo) & 0xFFFF);
    }

    /** {@link #pack2Int} for ALREADY-ENCODED half words. */
    public static int pack2RawInt(int lo16, int hi16) {
        return ((hi16 & 0xFFFF) << 16) | (lo16 & 0xFFFF);
    }

    /**
     * Float-typed bridge over {@link #pack2Int}: the instance record (attrib 15)
     * is a float-typed vec4 attribute, so the packed bits travel as a float
     * (bit pattern preserved via intBitsToFloat; the shader reinterprets it
     * with floatBitsToUint).
     */
    public static float pack2(float lo, float hi) {
        return Float.intBitsToFloat(pack2Int(lo, hi));
    }

    /** Float-typed bridge over {@link #pack2RawInt}. */
    public static float pack2Raw(int lo16, int hi16) {
        return Float.intBitsToFloat(pack2RawInt(lo16, hi16));
    }

    /**
     * Wraps degrees into (-180, 180] so the packed half keeps full 0.125 deg
     * precision. Deliberately NOT {@code Mth.wrapDegrees} (which returns
     * [-180, 180)): this class is Mojang-free (standalone unit tests); the
     * one-degree equivalence at the boundary is irrelevant here - the value is
     * a rotation ARGUMENT, never interpolated across the seam by this class.
     */
    public static float wrapDegrees(float deg) {
        float w = deg % 360.0f;
        if (w > 180.0f) {
            w -= 360.0f;
        } else if (w <= -180.0f) {
            w += 360.0f;
        }
        return w;
    }
}
