package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;
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

public class ModWorldGen {

    // Biome modifiers are Forge-only registry (forge:biome_modifier), not a vanilla registry.
    // Architectury's DeferredRegister cannot access it via RegistrarManager.
    //? if forge {
    public static final net.minecraftforge.registries.DeferredRegister<net.minecraftforge.common.world.BiomeModifier> BIOME_MODIFIERS =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, RefStrings.MODID);
    //?}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(RefStrings.MODID, Registries.FEATURE);


    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(RefStrings.MODID, Registries.STRUCTURE_PROCESSOR);

    public static final ResourceKey<ConfiguredFeature<?, ?>> URANIUM_ORE_CONFIGURED_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "ore_uranium"));

    public static final RegistrySupplier<StructureProcessorType<StructureFoundationProcessor>>
            FOUNDATION_PROCESSOR = PROCESSORS.register("foundation_processor",
            () -> () -> StructureFoundationProcessor.CODEC);

    /**
     * Процессор, назначающий лут-таблицы сундукам и ящикам HBM при генерации
     * структур. Подключён в {@code worldgen/processor_list/foundation_processor.json},
     * на который ссылается подавляющее большинство структурных template_pool.
     */
    public static final RegistrySupplier<StructureProcessorType<StructureLootProcessor>>
            LOOT_PROCESSOR = PROCESSORS.register("loot_processor",
            () -> () -> StructureLootProcessor.CODEC);

    /**
     * Процессор, фиксирующий соединения решёток/паней после спавна структур
     * (см. {@link StructureConnectionFixProcessor}).
     */
    public static final RegistrySupplier<StructureProcessorType<StructureConnectionFixProcessor>>
            CONNECTION_FIX_PROCESSOR = PROCESSORS.register("connection_fix_processor",
            () -> () -> StructureConnectionFixProcessor.CODEC);

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> OILCLASTER_SURROUNDED =
            FEATURES.register("oilclaster_surrounded", () -> new OilClasterSurroundedFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> BEDROCK_OIL_ORE =
            FEATURES.register("bedrock_oil_ore", () -> new BedrockOilOreFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> BEDROCK_ORE =
            FEATURES.register("ore_bedrock_mineral", () -> new BedrockOreFeature(NoneFeatureConfiguration.CODEC));

    /** Порт незерской бедрок-руды (BedrockOre.weightedOresNether). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> NETHER_BEDROCK_ORE =
            FEATURES.register("nether_bedrock_ore", () -> new NetherBedrockOreFeature(NoneFeatureConfiguration.CODEC));

    /** Порт колтановых месторождений (HbmWorldGen enable528ColtanDeposit, seed-гауссовы центры). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> COLTAN_DEPOSIT =
            FEATURES.register("coltan_deposit", () -> new ColtanDepositFeature());

    /** Порт MapGenBubble (каменные нефтяные месторождения). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> OIL_DEPOSIT =
            FEATURES.register("oil_deposit", () -> OilDepositFeature.stone(NoneFeatureConfiguration.CODEC));

    /** Порт песчаных нефтяных месторождения (sandOilBubble). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> SAND_OIL_DEPOSIT =
            FEATURES.register("sand_oil_deposit", () -> OilDepositFeature.sand(NoneFeatureConfiguration.CODEC));

    // ===================================================================================== //
    // Пластовые руды (порты SchistStratum / OreCave / OreLayer3D из 1.7.10)
    // ===================================================================================== //

    /**
     * Порт SchistStratum: перлиновая полоса каменного гнейса.
     * Оригинал: scale 0.01, threshold 5, rangeMult 3, maxRange 4, yLevel 30 (4 октавы).
     * Порт: yLevel 30−64 = −34; noiseSpan 10 компенсирует нормированный шум.
     */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> GNEISS_STRATUM =
            FEATURES.register("gneiss_stratum", () -> new PerlinBandFeature(
                    ModBlocks.STONE_GNEISS, PerlinBandFeature.MODE_SOLID,
                    10.0D, 5.0D, 3, 4, -34, 4, 0L, null, null));

    /**
     * Порт OreCave (сера): кожа пещер stone_resource SULFUR на y≈−54..−14.
     * Оригинал: threshold 1.5, rangeMult 20, maxRange 20, yLevel 30, 2 октавы,
     * сид salt = meta*31 + yLevel = 0*31 + 30. Пулы серной кислоты не портируются
     * (в порту нет мирового блока серной кислоты), декор — сталактит/сталагмит.
     */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> SULFUR_CAVE =
            FEATURES.register("sulfur_cave", () -> new PerlinBandFeature(
                    ModBlocks.RESOURCE_SULFUR, PerlinBandFeature.MODE_SKIN,
                    2.5D, 1.5D, 20, 20, -34, 2, 30L,
                    ModBlocks.STALACTITE_SULFUR, ModBlocks.STALAGMITE_SULFUR));

    /**
     * Порт OreCave (асбест): кожа пещер stone_resource ASBESTOS, yLevel 25 → −39,
     * threshold 1.75, без жидкости.
     */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> ASBESTOS_CAVE =
            FEATURES.register("asbestos_cave", () -> new PerlinBandFeature(
                    ModBlocks.RESOURCE_ASBESTOS, PerlinBandFeature.MODE_SKIN,
                    2.5D, 1.75D, 20, 20, -39, 2, 56L,
                    ModBlocks.STALACTITE_ASBESTOS, ModBlocks.STALAGMITE_ASBESTOS));

    /**
     * Порт OreLayer3D (гематит): scaleH 0.04, scaleV 0.25, оригинальный порог 230 →
     * откалиброванный 0.10 для нормированного шума (id шумов 0).
     */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> ORE_LAYER_HEMATITE =
            FEATURES.register("ore_layer_hematite", () -> new OreLayer3DFeature(
                    ModBlocks.RESOURCE_HEMATITE, 0.04D, 0.25D, 0.10D, 0));

    /** Порт OreLayer3D (боксит): scaleH 0.03, scaleV 0.15, порог 300 → 0.18 (id 1). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> ORE_LAYER_BAUXITE =
            FEATURES.register("ore_layer_bauxite", () -> new OreLayer3DFeature(
                    ModBlocks.RESOURCE_BAUXITE, 0.03D, 0.15D, 0.18D, 1));

    /** Порт OreLayer3D (малахит): scaleH 0.1, scaleV 0.15, порог 275 → 0.14 (id 2). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> ORE_LAYER_MALACHITE =
            FEATURES.register("ore_layer_malachite", () -> new OreLayer3DFeature(
                    ModBlocks.RESOURCE_MALACHITE, 0.1D, 0.15D, 0.14D, 2));

    public static final ResourceKey<ConfiguredFeature<?, ?>> BEDROCK_ORE_CONFIGURED_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "ore_bedrock_mineral"));

    public static final ResourceKey<PlacedFeature> BEDROCK_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "ore_bedrock_mineral_placed"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> BEDROCK_OIL_ORE_CONFIGURED_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "ore_bedrock_oil"));

    public static final ResourceKey<PlacedFeature> BEDROCK_OIL_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "ore_bedrock_oil_placed"));

    public static final ResourceKey<PlacedFeature> URANIUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "ore_uranium_placed"));

    public static final ResourceKey<PlacedFeature> STRAWBERRY_BUSH_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "strawberry_bush_placed"));


    /** Регистрация worldgen DeferredRegister на всех лоадерах. */
    public static void register() {
        FEATURES.register();
        PROCESSORS.register();
    }

    //? if forge {
    /** Регистрация worldgen DeferredRegister на Forge mod event bus (как в старом {@code MainRegistry}). */
    public static void register(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        BIOME_MODIFIERS.register(modEventBus);
        register();
    }
    //?}
}
