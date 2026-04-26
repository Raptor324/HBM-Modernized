package com.hbm_m.worldgen;

import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.world.BiomeModifier;

public class ModWorldGen {

    // Biome modifiers are Forge-only registry (forge:biome_modifier), not a vanilla registry.
    // Architectury's DeferredRegister cannot access it via RegistrarManager.
    //? if forge {
    public static final net.minecraftforge.registries.DeferredRegister<BiomeModifier> BIOME_MODIFIERS =
            net.minecraftforge.registries.DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIERS, RefStrings.MODID);
    //?}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(RefStrings.MODID, Registries.FEATURE);


    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(RefStrings.MODID, Registries.STRUCTURE_PROCESSOR);

    public static final ResourceKey<ConfiguredFeature<?, ?>> URANIUM_ORE_CONFIGURED_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, RefStrings.resourceLocation("ore_uranium"));

    public static final RegistrySupplier<StructureProcessorType<StructureFoundationProcessor>>
            FOUNDATION_PROCESSOR = PROCESSORS.register("foundation_processor",
            () -> () -> StructureFoundationProcessor.CODEC);

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> OILCLASTER_SURROUNDED =
            FEATURES.register("oilclaster_surrounded", () -> new OilClasterSurroundedFeature(NoneFeatureConfiguration.CODEC));

    public static final ResourceKey<PlacedFeature> URANIUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, RefStrings.resourceLocation("ore_uranium_placed"));

    public static final ResourceKey<PlacedFeature> STRAWBERRY_BUSH_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    //? if fabric && < 1.21.1 {
                    /*new ResourceLocation(RefStrings.MODID, "strawberry_bush_placed"));
                    *///?} else {
                                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "strawberry_bush_placed"));
                    //?}


    /** Регистрация worldgen DeferredRegister на Forge mod event bus (как в старом {@code MainRegistry}). */
    public static void register(IEventBus modEventBus) {
        //? if forge {
        BIOME_MODIFIERS.register(modEventBus);
        //?}
        FEATURES.register();
        PROCESSORS.register();
    }
}
