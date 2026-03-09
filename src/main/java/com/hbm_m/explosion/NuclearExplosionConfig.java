package com.hbm_m.explosion;

/**
 * Конфигурация ядерного взрыва MK5. Используется модульным API для любой взрывчатки.
 */
public final class NuclearExplosionConfig {

    /** Базовая сила (радиус); после нормализации удваивается и задаёт length/speed. */
    public final int baseStrength;
    public final boolean falloutEnabled;
    /** Дополнительный радиус fallout (additive to scale). */
    public final int extraFalloutRadius;
    public final boolean radiationEnabled;
    /** Тип грибного облака (0 = standard, 1 = bale и т.д.). */
    public final int mushroomType;

    private NuclearExplosionConfig(int baseStrength, boolean falloutEnabled, int extraFalloutRadius,
                                   boolean radiationEnabled, int mushroomType) {
        this.baseStrength = baseStrength;
        this.falloutEnabled = falloutEnabled;
        this.extraFalloutRadius = extraFalloutRadius;
        this.radiationEnabled = radiationEnabled;
        this.mushroomType = mushroomType;
    }

    public static Builder builder(int baseStrength) {
        return new Builder(baseStrength);
    }

    /** Пресет для Fat Man (стандартный радиус из конфига или 50). */
    public static NuclearExplosionConfig fatManDefault(int radius) {
        return builder(radius)
                .fallout(true)
                .radiation(true)
                .mushroomType(0)
                .build();
    }

    /** Ослабленный взрыв для тестов. */
    public static NuclearExplosionConfig smallNuke() {
        return builder(25)
                .fallout(false)
                .radiation(true)
                .mushroomType(0)
                .build();
    }

    /** Минимальная нагрузка для отладки. */
    public static NuclearExplosionConfig debugLowLag() {
        return builder(15)
                .fallout(false)
                .radiation(false)
                .mushroomType(0)
                .build();
    }

    public static final class Builder {
        private final int baseStrength;
        private boolean falloutEnabled = true;
        private int extraFalloutRadius = 0;
        private boolean radiationEnabled = true;
        private int mushroomType = 0;

        Builder(int baseStrength) {
            this.baseStrength = baseStrength;
        }

        public Builder fallout(boolean enabled) {
            this.falloutEnabled = enabled;
            return this;
        }

        public Builder extraFalloutRadius(int add) {
            this.extraFalloutRadius = add;
            return this;
        }

        public Builder radiation(boolean enabled) {
            this.radiationEnabled = enabled;
            return this;
        }

        public Builder mushroomType(int type) {
            this.mushroomType = type;
            return this;
        }

        public NuclearExplosionConfig build() {
            return new NuclearExplosionConfig(
                    baseStrength,
                    falloutEnabled,
                    extraFalloutRadius,
                    radiationEnabled,
                    mushroomType
            );
        }
    }
}
