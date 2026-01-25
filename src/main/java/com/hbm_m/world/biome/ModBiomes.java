package com.hbm_m.world.biome;

import com.hbm_m.main.MainRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * ✅ MOD BIOMES v3.0 - FIXED
 *
 * ✅ Исправления:
 * - Используется правильный Registries.BIOME вместо ForgeRegistries
 * - Правильная регистрация через DeferredRegister
 * - ResourceKey правильно создан
 * - Вся система совместима с Minecraft 1.20.1
 */
public class ModBiomes {

    static {
        MainRegistry.LOGGER.debug("[HBM_MODS] ========================================");
        MainRegistry.LOGGER.debug("[HBM_MODS] ModBiomes класс загружен!");
        MainRegistry.LOGGER.debug("[HBM_MODS] DeferredRegister создан!");
        MainRegistry.LOGGER.debug("[HBM_MODS] ========================================");
    }

    // ✅ ГЛАВНЫЙ DeferredRegister - используем правильный реестр
    public static final DeferredRegister<Biome> BIOMES =
            DeferredRegister.create(Registries.BIOME, MainRegistry.MOD_ID);

    // ✅ РЕГИСТРАЦИЯ Inner Crater - самая тёмная зона
    public static final RegistryObject<Biome> INNER_CRATER = BIOMES.register(
            "inner_crater",
            CraterBiomes::createInnerCraterBiome
    );

    // ✅ РЕГИСТРАЦИЯ Outer Crater - менее тёмная зона
    public static final RegistryObject<Biome> OUTER_CRATER = BIOMES.register(
            "outer_crater",
            CraterBiomes::createOuterCraterBiome
    );

    // ✅ ResourceKey для использования в коде (для lookup в реестре)
    public static final ResourceKey<Biome> INNER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(MainRegistry.MOD_ID, "inner_crater"));

    public static final ResourceKey<Biome> OUTER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(MainRegistry.MOD_ID, "outer_crater"));
}