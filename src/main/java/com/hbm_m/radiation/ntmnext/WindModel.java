// Port-eigene Klasse (HBM-Modernized), Stufe 4 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.server.level.ServerLevel;

/**
 * Globaler Wind je Dimension: Richtung und Staerke driften langsam und sind deterministisch aus
 * Weltseed, Dimension und Spielzeit (glattes Wert-Rauschen, Kosinus-Interpolation). Dazu optional
 * schnellere, schwaechere Boeen. Nur fuer Dimensionen mit Himmel (Himmelslicht, keine Decke).
 */
public final class WindModel {

    private WindModel() {}

    /** Hat die Dimension Wetter/Wind (Oberwelt-artig)? */
    public static boolean hasWind(ServerLevel level) {
        return NtmRadiationConfig.windEnabled
                && level.dimensionType().hasSkyLight()
                && !level.dimensionType().hasCeiling();
    }

    /** Windvektor in Bloecken/s: {vx, vz}. Null-Vektor ohne Wind. */
    public static double[] velocity(ServerLevel level) {
        if (!hasWind(level)) return new double[] {0.0D, 0.0D};
        long seed = level.getSeed() ^ (long) level.dimension().location().hashCode() * 0x9E3779B97F4A7C15L;
        double t = level.getGameTime() / 20.0D;

        double dirPeriod = Math.max(1.0D, NtmRadiationConfig.windDirectionPeriod);
        double angle = smoothNoise(seed, t / dirPeriod) * Math.PI * 4.0D;
        double speed =
                NtmRadiationConfig.windBaseSpeed
                        + NtmRadiationConfig.windSpeedVariation
                                * (smoothNoise(seed + 0x51ED27L, t / dirPeriod * 1.7D) * 2.0D - 1.0D);
        double vx = Math.cos(angle) * speed;
        double vz = Math.sin(angle) * speed;

        if (NtmRadiationConfig.gustStrength > 0.0D) {
            double gp = Math.max(1.0D, NtmRadiationConfig.gustPeriod);
            double ga = smoothNoise(seed + 0xA3B1L, t / gp) * Math.PI * 2.0D;
            double gs = NtmRadiationConfig.gustStrength * smoothNoise(seed + 0x7F4AL, t / gp * 1.3D);
            vx += Math.cos(ga) * gs;
            vz += Math.sin(ga) * gs;
        }
        if (level.isThundering()) {
            vx *= 1.5D;
            vz *= 1.5D;
        }
        return new double[] {vx, vz};
    }

    /** Glattes Wert-Rauschen in [0,1]. */
    private static double smoothNoise(long seed, double x) {
        long i = (long) Math.floor(x);
        double f = x - i;
        double a = unit(seed, i);
        double b = unit(seed, i + 1);
        double w = (1.0D - Math.cos(f * Math.PI)) * 0.5D;
        return a + (b - a) * w;
    }

    private static double unit(long seed, long i) {
        long h = HashCommon.mix(seed + i * 0x9E3779B97F4A7C15L);
        return (h >>> 11) * 0x1.0p-53;
    }
}
