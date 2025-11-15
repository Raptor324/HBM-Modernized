package com.hbm_m.world.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Фабрика для создания кастомных биомов кратера.
 */
public class CraterBiomes {

    /**
     * Inner Crater – самый тёмный и серый, только в селлафитовой воронке.
     */
    public static Biome createInnerCraterBiome(HolderGetter<PlacedFeature> placedFeatures,
                                               HolderGetter<ConfiguredWorldCarver<?>> carvers) {

        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder().build();
        BiomeGenerationSettings generationSettings =
                new BiomeGenerationSettings.Builder(placedFeatures, carvers).build();

        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .skyColor(0x1a1a1a)
                .grassColorOverride(0x2d2d2d)
                .foliageColorOverride(0x2d2d2d)
                .waterColor(0x0a0a1a)
                .waterFogColor(0x050510)
                .fogColor(0x1a1a1a)
                .build();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(-0.5F)
                .downfall(0.0F)
                .specialEffects(effects)
                .mobSpawnSettings(mobSpawnSettings)
                .generationSettings(generationSettings)
                .build();
    }

    /**
     * Outer Crater – менее тёмный, тусклые цвета, вода как у Inner.
     */
    public static Biome createOuterCraterBiome(HolderGetter<PlacedFeature> placedFeatures,
                                               HolderGetter<ConfiguredWorldCarver<?>> carvers) {

        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder().build();
        BiomeGenerationSettings generationSettings =
                new BiomeGenerationSettings.Builder(placedFeatures, carvers).build();

        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .skyColor(0x3d3d3d)
                .grassColorOverride(0x4a4a3d)
                .foliageColorOverride(0x4a4a3d)
                .waterColor(0x0a0a1a)      // как Inner
                .waterFogColor(0x050510)   // как Inner
                .fogColor(0x3d3d3d)
                .build();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(-0.2F)
                .downfall(0.0F)
                .specialEffects(effects)
                .mobSpawnSettings(mobSpawnSettings)
                .generationSettings(generationSettings)
                .build();
    }
}
