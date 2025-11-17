package com.hbm_m.world.biome;

import com.hbm_m.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ v2: Правильное создание и регистрация кастомных биомов
 *
 * ИСПРАВЛЕНИЯ:
 * ✅ Добавлены ResourceKey для корректной регистрации в реестре
 * ✅ Правильное использование DeferredRegister для биомов
 * ✅ Обе версии создания биомов работают одновременно
 */
public class CraterBiomes {

    // ✅ Главный DeferredRegister для биомов
    public static final DeferredRegister<Biome> BIOMES =
            DeferredRegister.create(Registries.BIOME, MainRegistry.MOD_ID);

    // ✅ РЕГИСТРАЦИЯ Inner Crater
    public static final RegistryObject<Biome> INNER_CRATER = BIOMES.register(
            "inner_crater",
            CraterBiomes::createInnerCraterBiome
    );

    // ✅ РЕГИСТРАЦИЯ Outer Crater
    public static final RegistryObject<Biome> OUTER_CRATER = BIOMES.register(
            "outer_crater",
            CraterBiomes::createOuterCraterBiome
    );

    // ✅ ResourceKey для использования в коде
    public static final ResourceKey<Biome> INNER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(MainRegistry.MOD_ID, "inner_crater"));

    public static final ResourceKey<Biome> OUTER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(MainRegistry.MOD_ID, "outer_crater"));

    /**
     * Inner Crater - самый тёмный биом (зона 1: 0-190 блоков)
     */
    public static Biome createInnerCraterBiome() {
        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder().build();
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .skyColor(0x1a1a1a) // Очень тёмное небо
                .grassColorOverride(0x2d2d2d) // Тёмная трава
                .foliageColorOverride(0x2d2d2d) // Тёмная листва
                .waterColor(0x0a0a1a) // Очень тёмная вода
                .waterFogColor(0x050510) // Ещё более тёмный туман воды
                .fogColor(0x1a1a1a) // Тёмный туман воздуха
                .build();

        return new Biome.BiomeBuilder()
                .specialEffects(effects)
                .mobSpawnSettings(mobSpawnSettings)
                .hasPrecipitation(false)
                .temperature(-0.5F)
                .downfall(0.0F)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    /**
     * Outer Crater - менее тёмный биом (зона 2-3: 190-260 блоков)
     */
    public static Biome createOuterCraterBiome() {
        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder().build();
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .skyColor(0x3d3d3d) // Менее тёмное небо (серое)
                .grassColorOverride(0x4a4a3d) // Тусклая трава
                .foliageColorOverride(0x4a4a3d) // Тусклая листва
                .waterColor(0x0a0a1a) // Вода такая же как в Inner Crater
                .waterFogColor(0x050510) // Туман воды такой же
                .fogColor(0x3d3d3d) // Тусклый туман воздуха
                .build();

        return new Biome.BiomeBuilder()
                .specialEffects(effects)
                .mobSpawnSettings(mobSpawnSettings)
                .hasPrecipitation(false)
                .temperature(-0.2F)
                .downfall(0.0F)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }
}