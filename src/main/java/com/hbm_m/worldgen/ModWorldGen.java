package com.hbm_m.worldgen;

// Класс для регистрации и управления генерацией мира, включая руды и биом-модификаторы.
// Использует DeferredRegister для регистрации биом-модификаторов и ресурсные ключи для настройки генерации руд.

import com.hbm_m.lib.RefStrings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModWorldGen {
    public static final DeferredRegister<BiomeModifier> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIERS, RefStrings.MODID);
            
    public static final ResourceKey<ConfiguredFeature<?, ?>> URANIUM_ORE_CONFIGURED_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, RefStrings.resourceLocation("ore_uranium"));
            
    public static final ResourceKey<PlacedFeature> URANIUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, RefStrings.resourceLocation("ore_uranium_placed"));

        public static final ResourceKey<PlacedFeature> STRAWBERRY_BUSH_PLACED =
                ResourceKey.create(
                        Registries.PLACED_FEATURE,
                        ResourceLocation.fromNamespaceAndPath("hbm_m", "strawberry_bush_placed")
                );

}
