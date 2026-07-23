package com.hbm_m.world.biome;

import com.hbm_m.main.MainRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * 1:1 порт {@code com.hbm.world.biome.BiomeGenCraterBase} (1.7.10).
 *
 * <p>Три биома кратера: {@code craterInnerBiome}, {@code craterBiome}, {@code craterOuterBiome}.
 * В оригинале:</p>
 * <ul>
 *   <li>{@code setDisableRain()} → {@code hasPrecipitation(false)}</li>
 *   <li>Все {@code spawnable*List.clear()} → {@link MobSpawnSettings#EMPTY}</li>
 *   <li>{@code waterColorMultiplier = 0xE0FFAE} (swamp yellow) — общий для всех</li>
 *   <li>{@code getBiomeGrassColor} использует plantNoise, возвращает ДВА значения в зависимости от noise</li>
 *   <li>{@code getBiomeFoliageColor = 0x6A7039} — общий для всех</li>
 *   <li>{@code getSkyColorByTemp} — индивидуальный</li>
 * </ul>
 *
 * <p>Т.к. в 1.20.1 {@code BiomeSpecialEffects} не поддерживает шум-функцию для цвета травы,
 * берём <b>среднее</b> из двух значений 1.7.10 (это даёт визуально близкий результат).</p>
 *
 * <p>Factory-методы используются только на Forge/NeoForge. На Fabric те же id загружаются
 * из датапака {@code data/hbm_m/worldgen/biome/*.json}.</p>
 */
public final class CraterBiomes {

    public static final ResourceKey<Biome> INNER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "inner_crater"));

    public static final ResourceKey<Biome> CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "crater"));

    public static final ResourceKey<Biome> OUTER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "outer_crater"));

    /** Общий цвет воды (1.7.10 {@code BiomeGenCraterBase.waterColorMultiplier = 0xE0FFAE}). */
    private static final int WATER_COLOR = 0xE0FFAE;
    /** Общий цвет листвы (1.7.10 {@code getBiomeFoliageColor = 0x6A7039}). */
    private static final int FOLIAGE_COLOR = 0x6A7039;

    private CraterBiomes() {}

    /**
     * Внутренний кратер (эпицентр, dist &lt; 15%).
     * 1.7.10: grass 0x404040/0x303030 → avg 0x383838, sky 0x424A42.
     */
    public static Biome createInnerCraterBiome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x424A42)
                        .grassColorOverride(0x383838)
                        .foliageColorOverride(FOLIAGE_COLOR)
                        .waterColor(WATER_COLOR)
                        .waterFogColor(0x050510)
                        .fogColor(0x424A42)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.5F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    /**
     * Средний кратер (15% &le; dist &lt; 55%).
     * 1.7.10: grass 0x606060/0x505050 → avg 0x585858, sky 0x525A52.
     */
    public static Biome createCraterBiome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x525A52)
                        .grassColorOverride(0x585858)
                        .foliageColorOverride(FOLIAGE_COLOR)
                        .waterColor(WATER_COLOR)
                        .waterFogColor(0x050510)
                        .fogColor(0x525A52)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.3F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    /**
     * Внешний кратер (dist &ge; 55%, на границе зоны).
     * 1.7.10: grass 0x776F59/0x6F6752 → avg 0x736B55, sky 0x6B9189.
     */
    public static Biome createOuterCraterBiome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x6B9189)
                        .grassColorOverride(0x736B55)
                        .foliageColorOverride(FOLIAGE_COLOR)
                        .waterColor(WATER_COLOR)
                        .waterFogColor(0x050510)
                        .fogColor(0x6B9189)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.15F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }
}
