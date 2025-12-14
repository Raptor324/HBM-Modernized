package com.hbm_m.util.explosions.nuclear;

/**
 * Улучшенный генератор кратеров с естественными границами
 * Использует Перлин шум для плавных переходов вместо ровных краёв
 *
 * ✅ ИСПРАВЛЕНО: Полностью переработано для использования с penetration
 * ✅ ОТДЕЛЕНО: Шум не влияет на прочность, только на расстояние отрисовки
 */
public class CraterNoiseGenerator {

    /**
     * Улучшенный Перлин шум для 3D координат
     */
    public static class PerlinNoise3D {
        private static final int[] PERMUTATION = new int[512];
        private static final double FADE_SMOOTHING = 6.0;

        static {
            int[] base = {
                    151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7,
                    225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190,
                    6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117,
                    35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136,
                    171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146,
                    158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 12, 107,
                    242, 119, 215, 163, 75, 190, 26, 97, 66, 228, 96, 211, 110, 200,
                    15, 130, 172, 140, 220, 93, 122, 60, 79, 90, 238, 215, 6, 244, 195,
                    12, 128, 248, 108, 67, 87, 185, 134, 193, 29, 158, 225, 248, 152,
                    17, 105, 217, 142, 148, 155, 30, 135, 233, 206, 85, 40, 223, 140,
                    161, 137, 13, 191, 230, 33, 206, 39, 200, 87, 141, 109, 19, 218,
                    169, 141, 33, 178, 52, 7, 165, 106, 208, 68, 117, 56, 76, 146, 83,
                    111, 57, 77, 146, 94, 89, 18, 150, 78, 176, 36, 143, 203, 124, 171,
                    12, 246, 185, 63, 236, 201, 206, 145, 211, 155, 188, 182, 218, 33,
                    32, 237, 179, 40, 87, 56, 11, 102, 180, 203, 11, 190, 114, 69, 192,
                    204, 162, 10, 183, 112, 102, 109, 184, 209, 214, 4, 247, 182, 130,
                    179, 38, 19, 83, 251, 235, 201, 5, 160, 89, 140, 132, 169, 194, 72,
                    42, 83, 59, 165, 231, 86, 105, 202, 186, 57, 74, 76, 88, 207, 208,
                    239, 170, 251, 67, 77, 51, 133, 69, 249, 2, 127, 80, 60, 159, 168,
                    81, 163, 64, 143, 146, 157, 56, 245, 188, 182, 218, 33, 32, 237,
                    179, 40, 87, 56, 11, 102, 180, 203, 11, 190, 114, 69, 192, 204, 162,
                    10, 183, 112, 102, 109, 184, 209, 214, 4, 247, 182, 130, 179, 38,
                    19, 83, 251, 235, 201, 5, 160, 89, 140, 132, 169, 194, 72, 42, 83,
                    59, 165, 231, 86, 105, 202, 186, 57, 74, 76, 88, 207, 208, 239, 170,
                    251, 67, 77, 51, 133, 69, 249, 2, 127, 80, 60, 159, 168, 81, 163,
                    64, 143, 146, 157
            };
            System.arraycopy(base, 0, PERMUTATION, 0, 256);
            System.arraycopy(base, 0, PERMUTATION, 256, 256);
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * FADE_SMOOTHING - 15.0) + 10.0);
        }

        private static double lerp(double a, double b, double t) {
            return a + t * (b - a);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 8 ? y : z;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }

        public static double noise(double x, double y, double z) {
            int xi = (int) Math.floor(x) & 255;
            int yi = (int) Math.floor(y) & 255;
            int zi = (int) Math.floor(z) & 255;

            double xf = x - Math.floor(x);
            double yf = y - Math.floor(y);
            double zf = z - Math.floor(z);

            double u = fade(xf);
            double v = fade(yf);
            double w = fade(zf);

            int p0 = PERMUTATION[xi];
            int p1 = PERMUTATION[xi + 1];
            int aa = PERMUTATION[p0 + yi];
            int ab = PERMUTATION[p0 + yi + 1];
            int ba = PERMUTATION[p1 + yi];
            int bb = PERMUTATION[p1 + yi + 1];

            double g000 = grad(PERMUTATION[aa + zi], xf, yf, zf);
            double g001 = grad(PERMUTATION[ab + zi], xf, yf - 1, zf);
            double g010 = grad(PERMUTATION[ba + zi], xf - 1, yf, zf);
            double g011 = grad(PERMUTATION[bb + zi], xf - 1, yf - 1, zf);
            double g100 = grad(PERMUTATION[aa + zi + 1], xf, yf, zf - 1);
            double g101 = grad(PERMUTATION[ab + zi + 1], xf, yf - 1, zf - 1);
            double g110 = grad(PERMUTATION[ba + zi + 1], xf - 1, yf, zf - 1);
            double g111 = grad(PERMUTATION[bb + zi + 1], xf - 1, yf - 1, zf - 1);

            double x0 = lerp(g000, g010, u);
            double x1 = lerp(g100, g110, u);
            double y0 = lerp(x0, x1, w);

            double x2 = lerp(g001, g011, u);
            double x3 = lerp(g101, g111, u);
            double y1 = lerp(x2, x3, w);

            return lerp(y0, y1, v);
        }
    }

    /**
     * Генерирует максимальное расстояние луча с шумом
     * Не влияет на значение penetration!
     *
     * @param baseMaxDistance базовое максимальное расстояние (напр. 100)
     * @param dirX направление луча X
     * @param dirY направление луча Y
     * @param dirZ направление луча Z
     * @param noiseScale масштаб шума (0.05-0.15)
     * @param noiseStrength сила вариаций (0.2-0.5)
     * @return модифицированное максимальное расстояние
     */
    public static int getNoiseModifiedMaxDistance(
            double baseMaxDistance,
            double dirX, double dirY, double dirZ,
            double noiseScale,
            double noiseStrength) {

        // Многоуровневый Перлин шум
        double noise1 = PerlinNoise3D.noise(
                dirX * noiseScale * 100,
                dirY * noiseScale * 100,
                dirZ * noiseScale * 100
        );

        double noise2 = PerlinNoise3D.noise(
                dirX * noiseScale * 250,
                dirY * noiseScale * 250,
                dirZ * noiseScale * 250
        ) * 0.5;

        double noise3 = PerlinNoise3D.noise(
                dirX * noiseScale * 600,
                dirY * noiseScale * 600,
                dirZ * noiseScale * 600
        ) * 0.25;

        // Нормализуем в [0..1]
        double combinedNoise = noise1 + noise2 + noise3;
        double normalized = (combinedNoise + 1.5) / 3.0;
        normalized = Math.max(0.0, Math.min(1.0, normalized));

        // Применяем вариацию к максимальному расстоянию
        double variation = baseMaxDistance * noiseStrength;
        double modifiedDistance = baseMaxDistance + (variation * normalized - variation * 0.5);

        // Ограничиваем результат
        modifiedDistance = Math.max(baseMaxDistance * 0.5, modifiedDistance);
        modifiedDistance = Math.min(baseMaxDistance * 1.5, modifiedDistance);

        return (int) Math.ceil(modifiedDistance);
    }

    /**
     * Генерирует "случайное" смещение максимального расстояния
     * основываясь на направлении луча для воспроизводимости
     */
    public static double getDirectionBasedVariation(
            double dirX, double dirY, double dirZ,
            double minVariation, double maxVariation) {

        // Детерминированный "случайный" хэш
        double hash = Math.sin(dirX * 12.9898 + dirY * 78.233 + dirZ * 43758.5453) * 43758.5453;
        double randomFraction = hash - Math.floor(hash);

        return minVariation + (maxVariation - minVariation) * randomFraction;
    }
}