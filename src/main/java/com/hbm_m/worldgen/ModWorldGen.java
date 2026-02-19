package com.hbm_m.worldgen;

import com.hbm_m.lib.RefStrings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModWorldGen {

    public static final DeferredRegister<BiomeModifier> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIERS, RefStrings.MODID);

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, RefStrings.MODID);


    // ✅ ПРАВИЛЬНЫЙ УНИВЕРСАЛЬНЫЙ СПОСОБ!
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, RefStrings.MODID);

    public static final ResourceKey<ConfiguredFeature<?, ?>> URANIUM_ORE_CONFIGURED_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, RefStrings.resourceLocation("ore_uranium"));

    public static final RegistryObject<StructureProcessorType<StructureFoundationProcessor>>
            FOUNDATION_PROCESSOR = PROCESSORS.register("foundation_processor",
            () -> () -> StructureFoundationProcessor.CODEC);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OILCLASTER_SURROUNDED =
            FEATURES.register("oilclaster_surrounded", () -> new OilClasterSurroundedFeature(NoneFeatureConfiguration.CODEC));

    public static final ResourceKey<PlacedFeature> URANIUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, RefStrings.resourceLocation("ore_uranium_placed"));

    public static final ResourceKey<PlacedFeature> STRAWBERRY_BUSH_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "strawberry_bush_placed"));
}
