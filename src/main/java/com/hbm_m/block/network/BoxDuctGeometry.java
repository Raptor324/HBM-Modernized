package com.hbm_m.block.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemeinsame Geometrie von {@code FluidDuctBox}, {@code FluidDuctBoxExhaust} und {@code PowerCableBox} samt
 * {@code RenderBoxDuct}: Teilquader, je Seite die Textur ({@code getIcon}) und die {@code uvRotate}-Werte.
 * Seiten wie in 1.7.10: 0 unten, 1 oben, 2 Nord (-Z), 3 Sued (+Z), 4 West (-X), 5 Ost (+X).
 */
public final class BoxDuctGeometry {

    private BoxDuctGeometry() {}

    public enum Kind { FLUID, EXHAUST, CABLE }

    private static final String[] MATERIALS = { "silver", "copper", "white" };

    /** Ein zu zeichnender Quader: Grenzen und uvRotate {Bottom, Top, East, West, North, South}. */
    public record Part(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int[] uvRotate) {}

    /** {lower, upper, jLower, jUpper} je nach Groesse. */
    public static double[] sizes(Kind kind, int meta) {
        double lower = 0.125D, upper = 0.875D, jLower, jUpper;
        if (kind == Kind.CABLE) {
            jLower = lower; jUpper = upper;
            for (int i = 0; i < 5; i++) if (meta > i) { lower += 0.0625D; upper -= 0.0625D; jLower += 0.0625D; jUpper -= 0.0625D; }
        } else {
            jLower = 0.0625D; jUpper = 0.9375D;
            for (int i = 2; i < 13; i += 3) if (meta > i) { lower += 0.0625D; upper -= 0.0625D; jLower += 0.0625D; jUpper -= 0.0625D; }
        }
        return new double[] { lower, upper, jLower, jUpper };
    }

    public static int mask(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        return (pX ? 32 : 0) + (nX ? 16 : 0) + (pY ? 8 : 0) + (nY ? 4 : 0) + (pZ ? 2 : 0) + (nZ ? 1 : 0);
    }

    /** {@code RenderBoxDuct.renderWorldBlock}. */
    public static List<Part> renderParts(Kind kind, int meta, boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        double[] s = sizes(kind, meta);
        double lower = s[0], upper = s[1], jLower = s[2], jUpper = s[3];
        int mask = mask(pX, nX, pY, nY, pZ, nZ);
        int count = Integer.bitCount(mask);
        List<Part> out = new ArrayList<>();

        if ((mask & 0b001111) == 0 && mask > 0) {
            out.add(new Part(0, lower, lower, 1, upper, upper, new int[] { 1, 1, 2, 1, 0, 0 }));
        } else if ((mask & 0b111100) == 0 && mask > 0) {
            out.add(new Part(lower, lower, 0, upper, upper, 1, new int[] { 0, 0, 0, 0, 1, 2 }));
        } else if ((mask & 0b110011) == 0 && mask > 0) {
            out.add(new Part(lower, 0, lower, upper, 1, upper, new int[6]));
        } else if (count == 2) {
            int[] r = new int[6];
            if (nY && (pX || nX)) { r[0] = 1; r[1] = 1; }
            if (pY && (pX || nX)) { r[0] = 1; r[1] = 1; }
            if (!nY && !pY) { r[4] = 1; r[5] = 2; r[2] = 2; r[3] = 1; }
            out.add(new Part(lower, lower, lower, upper, upper, upper, r));
            if (nY) out.add(new Part(lower, 0, lower, upper, lower, upper, r));
            if (pY) out.add(new Part(lower, upper, lower, upper, 1, upper, r));
            if (nX) out.add(new Part(0, lower, lower, lower, upper, upper, r));
            if (pX) out.add(new Part(upper, lower, lower, 1, upper, upper, r));
            if (nZ) out.add(new Part(lower, lower, 0, upper, upper, lower, r));
            if (pZ) out.add(new Part(lower, lower, upper, upper, upper, 1, r));
        } else {
            int[] r = new int[6];
            out.add(new Part(jLower, jLower, jLower, jUpper, jUpper, jUpper, r));
            if (nY) out.add(new Part(lower, 0, lower, upper, jLower, upper, r));
            if (pY) out.add(new Part(lower, jUpper, lower, upper, 1, upper, r));
            if (nX) out.add(new Part(0, lower, lower, jLower, upper, upper, r));
            if (pX) out.add(new Part(jUpper, lower, lower, 1, upper, upper, r));
            if (nZ) out.add(new Part(lower, lower, 0, upper, upper, jLower, r));
            if (pZ) out.add(new Part(lower, lower, jUpper, upper, upper, 1, r));
        }
        return out;
    }

    /** Textur-Grundname je Seite ({@code getIcon(world, x, y, z, side)}), ohne Namensraum. */
    public static String icon(Kind kind, int meta, int side, boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        int mask = mask(pX, nX, pY, nY, pZ, nZ);
        int count = Integer.bitCount(mask);
        String base;
        String end, straight, tl, tr, bl, br, junction;
        if (kind == Kind.CABLE) {
            base = "boxduct_cable";
            end = base + "_end_" + meta; straight = base + "_straight"; junction = base + "_junction";
        } else {
            int m = kind == Kind.EXHAUST ? 0 : Math.abs(meta % 3);
            base = kind == Kind.EXHAUST ? "boxduct_exhaust" : "boxduct_" + MATERIALS[m];
            end = base + "_end"; straight = base + "_straight"; junction = base + "_junction_" + (meta / 3);
        }
        tl = base + "_curve_tl"; tr = base + "_curve_tr"; bl = base + "_curve_bl"; br = base + "_curve_br";

        if ((mask & 0b001111) == 0 && mask > 0) return (side == 4 || side == 5) ? end : straight;
        if ((mask & 0b111100) == 0 && mask > 0) return (side == 2 || side == 3) ? end : straight;
        if ((mask & 0b110011) == 0 && mask > 0) return (side == 0 || side == 1) ? end : straight;

        boolean endSide = side == 0 && nY || side == 1 && pY || side == 2 && nZ || side == 3 && pZ || side == 4 && nX || side == 5 && pX;
        if (kind == Kind.CABLE && endSide) return end;
        if (count == 2) {
            if (endSide) return end;
            if (side == 1 && nY || side == 0 && pY || side == 3 && nZ || side == 2 && pZ || side == 5 && nX || side == 4 && pX) return straight;
            if (nY && pZ) return side == 4 ? br : bl;
            if (nY && nZ) return side == 5 ? br : bl;
            if (nY && pX) return side == 3 ? br : bl;
            if (nY && nX) return side == 2 ? br : bl;
            if (pY && pZ) return side == 4 ? tr : tl;
            if (pY && nZ) return side == 5 ? tr : tl;
            if (pY && pX) return side == 3 ? tr : tl;
            if (pY && nX) return side == 2 ? tr : tl;
            if (pX && nZ) return tr;
            if (pX && pZ) return br;
            if (nX && nZ) return tl;
            if (nX && pZ) return bl;
        }
        return junction;
    }

    /** Auswahlrahmen ({@code setBlockBoundsBasedOnState}). */
    public static double[] selection(Kind kind, int meta, boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        double[] s = sizes(kind, meta);
        double lower = s[0], upper = s[1], jLower = s[2], jUpper = s[3];
        int mask = mask(pX, nX, pY, nY, pZ, nZ);
        int count = Integer.bitCount(mask);
        if (mask == 0) return new double[] { jLower, jLower, jLower, jUpper, jUpper, jUpper };
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) return new double[] { 0, lower, lower, 1, upper, upper };
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) return new double[] { lower, 0, lower, upper, 1, upper };
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) return new double[] { lower, lower, 0, upper, upper, 1 };
        double lo = kind == Kind.CABLE || count == 2 ? lower : jLower, hi = kind == Kind.CABLE || count == 2 ? upper : jUpper;
        return new double[] { nX ? 0 : lo, nY ? 0 : lo, nZ ? 0 : lo, pX ? 1 : hi, pY ? 1 : hi, pZ ? 1 : hi };
    }

    /** Kollision ({@code addCollisionBoxesToList}). */
    public static List<double[]> collision(Kind kind, int meta, boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        double[] s = sizes(kind, meta);
        double lower = s[0], upper = s[1], jLower = s[2], jUpper = s[3];
        int mask = mask(pX, nX, pY, nY, pZ, nZ);
        int count = Integer.bitCount(mask);
        List<double[]> bbs = new ArrayList<>();
        if (mask == 0) {
            bbs.add(new double[] { jLower, jLower, jLower, jUpper, jUpper, jUpper });
        } else if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
            bbs.add(new double[] { 0, lower, lower, 1, upper, upper });
        } else if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
            bbs.add(new double[] { lower, 0, lower, upper, 1, upper });
        } else if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
            bbs.add(new double[] { lower, lower, 0, upper, upper, 1 });
        } else {
            if (kind != Kind.CABLE && count != 2) bbs.add(new double[] { jLower, jLower, jLower, jUpper, jUpper, jUpper });
            else bbs.add(new double[] { lower, lower, lower, upper, upper, upper });
            if (pX) bbs.add(new double[] { upper, lower, lower, 1, upper, upper });
            if (nX) bbs.add(new double[] { 0, lower, lower, lower, upper, upper });
            if (pY) bbs.add(new double[] { lower, upper, lower, upper, 1, upper });
            if (nY) bbs.add(new double[] { lower, 0, lower, upper, lower, upper });
            if (pZ) bbs.add(new double[] { lower, lower, upper, upper, upper, 1 });
            if (nZ) bbs.add(new double[] { lower, lower, 0, upper, upper, lower });
        }
        return bbs;
    }

    /** Alle Texturnamen eines Typs (fuer den Atlas). */
    public static List<String> allIcons(Kind kind) {
        List<String> out = new ArrayList<>();
        if (kind == Kind.CABLE) {
            String b = "boxduct_cable";
            out.add(b + "_straight"); out.add(b + "_junction");
            for (int i = 0; i < 5; i++) out.add(b + "_end_" + i);
            for (String c : new String[] { "tl", "tr", "bl", "br" }) out.add(b + "_curve_" + c);
            return out;
        }
        String[] bases = kind == Kind.EXHAUST ? new String[] { "boxduct_exhaust" } : new String[] { "boxduct_silver", "boxduct_copper", "boxduct_white" };
        for (String b : bases) {
            out.add(b + "_straight"); out.add(b + "_end");
            for (int i = 0; i < 5; i++) out.add(b + "_junction_" + i);
            for (String c : new String[] { "tl", "tr", "bl", "br" }) out.add(b + "_curve_" + c);
        }
        return out;
    }
}
