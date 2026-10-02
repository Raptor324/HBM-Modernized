package com.hbm_m.worldgen;

import java.util.Random;

/**
 * Самодостаточный 2D-перлин-шум (классический improved perlin), порт семантики
 * 1.7.10 {@code NoiseGeneratorPerlin}: N октав, амплитуды 1, 1/2, 1/4, ...,
 * частота удваивается с каждой октавой, сид задаётся через {@link Random}.
 *
 * Отличие от оригинала: перлин 1.7.10 был известен выходом значений за диапазон
 * [-1, 1] (отсюда пороги оригинала вроде 230/300/1.5). Здесь значения
 * нормированы примерно в [-1, 1], поэтому вызывающий код компенсирует это
 * коэффициентом-масштабом (noiseSpan), сохраняя форму отложений.
 */
public class HbmPerlinNoise {

    private final int[] permutation = new int[512];
    private final double offsetX;
    private final double offsetY;
    private final int octaves;

    public HbmPerlinNoise(long seed, int octaves) {
        this.octaves = octaves;
        Random random = new Random(seed);
        this.offsetX = random.nextDouble() * 256.0D;
        this.offsetY = random.nextDouble() * 256.0D;
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) permutation[i] = p[i & 255];
    }

    /** Значение шума, примерно в [-1, 1] (сумма октав с амплитудами 1, 1/2, ...). */
    public double getValue(double x, double z) {
        double value = 0.0D;
        double amplitude = 1.0D;
        double frequency = 1.0D;
        double norm = 0.0D;
        for (int i = 0; i < octaves; i++) {
            value += noiseAt(x * frequency, z * frequency) * amplitude;
            norm += amplitude;
            amplitude *= 0.5D;
            frequency *= 2.0D;
        }
        return value / norm;
    }

    private double noiseAt(double x, double z) {
        double x2 = x + offsetX;
        double z2 = z + offsetY;
        int xi = floorInt(x2) & 255;
        int zi = floorInt(z2) & 255;
        double xf = x2 - floor(x2);
        double zf = z2 - floor(z2);
        double u = fade(xf);
        double v = fade(zf);

        int a = permutation[xi] + zi;
        int b = permutation[xi + 1] + zi;

        return lerp(v,
                lerp(u, grad(permutation[a], xf, zf), grad(permutation[b], xf - 1.0D, zf)),
                lerp(u, grad(permutation[a + 1], xf, zf - 1.0D), grad(permutation[b + 1], xf - 1.0D, zf - 1.0D)));
    }

    private static double grad(int hash, double x, double z) {
        switch (hash & 7) {
            case 0: return  x + z;
            case 1: return -x + z;
            case 2: return  x - z;
            case 3: return -x - z;
            case 4: return  x;
            case 5: return -x;
            case 6: return  z;
            default: return -z;
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static int floorInt(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
