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
 * Фабрики биомов кратера: на Forge / NeoForge регистрируются из {@link ModBiomes}; на Fabric те же id
 * загружаются из datapack мода ({@code data/hbm_m/worldgen/biome/}). Ключи — {@link ModBiomes#INNER_CRATER_KEY},
 * {@link ModBiomes#OUTER_CRATER_KEY} (туман — {@link com.hbm_m.client.CraterFogHandler}).
 */
public final class CraterBiomes {

    public static final ResourceKey<Biome> INNER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(MainRegistry.MOD_ID, "inner_crater"));

    public static final ResourceKey<Biome> OUTER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(MainRegistry.MOD_ID, "outer_crater"));

    private CraterBiomes() {}

    public static Biome createInnerCraterBiome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.8F)
                .downfall(0.9F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x1a1a1a)
                        .grassColorOverride(0x2d2d2d)
                        .foliageColorOverride(0x2d2d2d)
                        .waterColor(0x0a0a1a)
                        .waterFogColor(0x050510)
                        .fogColor(0x1a1a1a)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.5F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    public static Biome createOuterCraterBiome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.8F)
                .downfall(0.4F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x3d3d3d)
                        .grassColorOverride(0x4a4a3d)
                        .foliageColorOverride(0x4a4a3d)
                        .waterColor(0x0a0a1a)
                        .waterFogColor(0x050510)
                        .fogColor(0x3d3d3d)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.15F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }
}
