package com.hbm_m.world.biome;

import com.hbm_m.main.MainRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CraterBiomes {

    public static final DeferredRegister<Biome> BIOMES =
            DeferredRegister.create(Registries.BIOME, MainRegistry.MOD_ID);

    public static final RegistryObject<Biome> INNER_CRATER = BIOMES.register("inner_crater", CraterBiomes::createInnerCraterBiome);
    public static final RegistryObject<Biome> OUTER_CRATER = BIOMES.register("outer_crater", CraterBiomes::createOuterCraterBiome);

    public static final ResourceKey<Biome> INNER_CRATER_KEY = ResourceKey.create(Registries.BIOME, new ResourceLocation(MainRegistry.MOD_ID, "inner_crater"));
    public static final ResourceKey<Biome> OUTER_CRATER_KEY = ResourceKey.create(Registries.BIOME, new ResourceLocation(MainRegistry.MOD_ID, "outer_crater"));

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
                        // 🔥 ПОВЫШЕНА ВЕРОЯТНОСТЬ: 0.118 -> 0.5 (очень густой пепел)
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
                        // 🔥 ПОВЫШЕНА ВЕРОЯТНОСТЬ: 0.025 -> 0.15 (заметный пепел)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.15F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }
}
