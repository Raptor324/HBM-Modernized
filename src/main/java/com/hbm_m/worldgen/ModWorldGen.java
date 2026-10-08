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

public class ModWorldGen {

    // Biome modifiers are Forge-only registry (forge:biome_modifier), not a vanilla registry.
    // Architectury's DeferredRegister cannot access it via RegistrarManager.
    //? if forge {
    public static final net.minecraftforge.registries.DeferredRegister<net.minecraftforge.common.world.BiomeModifier> BIOME_MODIFIERS =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, RefStrings.MODID);
    //?}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(RefStrings.MODID, Registries.FEATURE);


    /** R9: Weltgen-Teil von NBTStructure (Strukturtyp, Bauteil, Gitterplatzierung). */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(RefStrings.MODID, Registries.STRUCTURE_TYPE);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(RefStrings.MODID, Registries.STRUCTURE_PIECE);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> STRUCTURE_PLACEMENTS =
            DeferredRegister.create(RefStrings.MODID, Registries.STRUCTURE_PLACEMENT);

    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.StructureType<com.hbm_m.world.gen.nbt.NBTStructureGen.GenStructure>> NTM_STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("ntm", com.hbm_m.world.gen.nbt.NBTStructureGen::structureType);
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> NBT_COMPONENT =
            STRUCTURE_PIECES.register("nbt_component", () -> (net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.ContextlessType) com.hbm_m.world.gen.nbt.NBTStructureGen.Component::new);
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<com.hbm_m.world.gen.nbt.NBTStructureGen.GridPlacement>> NTM_GRID_PLACEMENT =
            STRUCTURE_PLACEMENTS.register("ntm_grid", com.hbm_m.world.gen.nbt.NBTStructureGen::placementType);
    /** R9: grosse Altbauwerke (Wuesten-Kraftwerk, Raumschiff, Dschungelverlies, Pyramide). */
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.StructureType<com.hbm_m.world.gen.LegacyDungeonStructure>> LEGACY_DUNGEON_TYPE =
            STRUCTURE_TYPES.register("legacy_dungeon", com.hbm_m.world.gen.LegacyDungeonStructure::structureType);
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> LEGACY_DUNGEON_PIECE =
            STRUCTURE_PIECES.register("legacy_dungeon", () -> (net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.ContextlessType) com.hbm_m.world.gen.LegacyDungeonStructure.Piece::new);

    /** R9: 1.7.10-MapGenBase-Generatoren (Krater, Oelblasen, Grundgestein-Oel) als Carver. */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.carver.WorldCarver<?>> CARVERS =
            DeferredRegister.create(RefStrings.MODID, Registries.CARVER);
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.carver.WorldCarver<net.minecraft.world.level.levelgen.carver.CarverConfiguration>> CRATER_CARVER =
            CARVERS.register("crater", () -> new com.hbm_m.world.gen.terrain.MapGenCrater(net.minecraft.world.level.levelgen.carver.CarverConfiguration.CODEC.codec()));
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.carver.WorldCarver<net.minecraft.world.level.levelgen.carver.CarverConfiguration>> OIL_BUBBLE_CARVER =
            CARVERS.register("oil_bubble", () -> new com.hbm_m.world.gen.terrain.MapGenBubble(net.minecraft.world.level.levelgen.carver.CarverConfiguration.CODEC.codec(), false));
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.carver.WorldCarver<net.minecraft.world.level.levelgen.carver.CarverConfiguration>> SAND_OIL_BUBBLE_CARVER =
            CARVERS.register("sand_oil_bubble", () -> new com.hbm_m.world.gen.terrain.MapGenBubble(net.minecraft.world.level.levelgen.carver.CarverConfiguration.CODEC.codec(), true));
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.carver.WorldCarver<net.minecraft.world.level.levelgen.carver.CarverConfiguration>> BEDROCK_OIL_CARVER =
            CARVERS.register("bedrock_oil", () -> new com.hbm_m.world.gen.terrain.MapGenBedrockOil(net.minecraft.world.level.levelgen.carver.CarverConfiguration.CODEC.codec()));

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

    /** 1:1 HbmWorldGen: Coltan-Lagerstaette um den Seed-Punkt (+ optional zufaellige Coltan-Adern). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> COLTAN_DEPOSIT =
            FEATURES.register("coltan_deposit", () -> new ColtanDepositFeature(NoneFeatureConfiguration.CODEC));

    /** HbmWorldGen: Glyphidennester (1/hiveSpawn je Chunk). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> GLYPHID_HIVE =
            FEATURES.register("glyphid_hive", () -> new GlyphidHiveFeature(NoneFeatureConfiguration.CODEC));
    /** HbmWorldGen: verrostete Landekapsel am Strand (1/capsuleStructure je Chunk). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> SOYUZ_CAPSULE =
            FEATURES.register("soyuz_capsule", () -> new SoyuzCapsuleFeature(NoneFeatureConfiguration.CODEC));
    /** 1:1 BiomeDecoratorNoMansLand: Sellafit-Flecken des Bioms No Man's Land. */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> NO_MANS_LAND_SPOTS =
            FEATURES.register("no_mans_land_spots", () -> new NoMansLandSpotsFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> LANTERN_BEHEMOTH =
            FEATURES.register("lantern_behemoth", () -> new LanternBehemothFeature(NoneFeatureConfiguration.CODEC));

    /** R9: 1:1 Ereignis-Generatoren aus MainRegistry (Gneisschiefer, Erzhoehlen, Erzlagen). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> HBM_STRATA =
            FEATURES.register("hbm_strata", () -> new HbmWorldGenFeature(NoneFeatureConfiguration.CODEC, true));
    /** R9: 1:1 HbmWorldGen (IWorldGenerator). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> HBM_WORLDGEN =
            FEATURES.register("hbm_worldgen", () -> new HbmWorldGenFeature(NoneFeatureConfiguration.CODEC, false));

    /** Порт MapGenBubble (каменные нефтяные месторождения). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> OIL_DEPOSIT =
            FEATURES.register("oil_deposit", () -> OilDepositFeature.stone(NoneFeatureConfiguration.CODEC));

    /** Порт песчаных нефтяных месторождения (sandOilBubble). */
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> SAND_OIL_DEPOSIT =
            FEATURES.register("sand_oil_deposit", () -> OilDepositFeature.sand(NoneFeatureConfiguration.CODEC));

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
                    //? if fabric && < 1.21.1 {
                    /*new ResourceLocation(RefStrings.MODID, "strawberry_bush_placed"));
                    *///?} else {
                                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "strawberry_bush_placed"));
                    //?}


    /** Регистрация worldgen DeferredRegister на всех лоадерах. */
    public static void register() {
        FEATURES.register();
        PROCESSORS.register();
        STRUCTURE_TYPES.register();
        STRUCTURE_PIECES.register();
        STRUCTURE_PLACEMENTS.register();
        CARVERS.register();
        com.hbm_m.world.gen.component.ComponentTypes.register();
    }

    //? if forge {
    /** Регистрация worldgen DeferredRegister на Forge mod event bus (как в старом {@code MainRegistry}). */
    public static void register(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        BIOME_MODIFIERS.register(modEventBus);
        register();
    }
    //?}
}
