package com.hbm_m.datagen;
//? if forge {

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.worldgen.ModWorldGen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Датаген всех configured/placed feature и biome-модификаторов мода — полная замена
 * рукописным JSON в {@code data/hbm_m/worldgen} и {@code data/hbm_m/forge/biome_modifier}.
 *
 * Баланс высот: оригинальные высоты 1.7.10 (мир 0..128) перенесены под новую генерацию
 * 1.18+ (камень до ~y0, глыбовый сланец ниже): низкоплавковые руды ушли в глубокий сланец,
 * поверхностные (лигнит, известняк) подняты выше; Незер не изменился (там те же 0..127).
 * Числа жил/размеры взяты из {@code HbmWorldGen} + {@code WorldConfig} оригинала.
 */
public final class ModWorldGenProvider {

    private ModWorldGenProvider() {}

    private static ResourceKey<ConfiguredFeature<?, ?>> cf(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name));
    }

    private static ResourceKey<PlacedFeature> pf(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name));
    }

    private static ResourceKey<BiomeModifier> bm(String name) {
        return ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS,
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name));
    }

    // ====================================================================

    public static void bootstrapConfigured(BootstapContext<ConfiguredFeature<?, ?>> ctx) {
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrack = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest gneiss = new BlockMatchTest(ModBlocks.STONE_GNEISS.get());

        // --- обычные руды верхнего мира (камень + глубинный сланец) ---
        ore(ctx, "uranium_ore", stone, deepslate, ModBlocks.URANIUM_ORE.get(), ModBlocks.URANIUM_ORE_DEEPSLATE.get(), 4);
        ore(ctx, "thorium_ore", stone, deepslate, ModBlocks.THORIUM_ORE.get(), ModBlocks.THORIUM_ORE_DEEPSLATE.get(), 7);
        ore(ctx, "titanium_ore", stone, deepslate, ModBlocks.TITANIUM_ORE.get(), ModBlocks.TITANIUM_ORE_DEEPSLATE.get(), 7);
        ore(ctx, "sulfur_ore", stone, deepslate, ModBlocks.SULFUR_ORE.get(), ModBlocks.SULFUR_ORE_DEEPSLATE.get(), 7);
        ore(ctx, "aluminum_ore", stone, deepslate, ModBlocks.ALUMINUM_ORE.get(), ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(), 7);
        // Медную руду не генерируем — в 1.18+ есть ванильная медь.
        ore(ctx, "fluorite_ore", stone, deepslate, ModBlocks.FLUORITE_ORE.get(), ModBlocks.FLUORITE_ORE_DEEPSLATE.get(), 7);
        ore(ctx, "niter_ore", stone, deepslate, ModBlocks.NITER_ORE.get(), ModBlocks.NITER_ORE_DEEPSLATE.get(), 6);
        ore(ctx, "tungsten_ore", stone, deepslate, ModBlocks.TUNGSTEN_ORE.get(), ModBlocks.TUNGSTEN_ORE_DEEPSLATE.get(), 6);
        ore(ctx, "lead_ore", stone, deepslate, ModBlocks.LEAD_ORE.get(), ModBlocks.LEAD_ORE_DEEPSLATE.get(), 6);
        ore(ctx, "beryllium_ore", stone, deepslate, ModBlocks.BERYLLIUM_ORE.get(), ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get(), 6);
        ore(ctx, "rareground_ore", stone, deepslate, ModBlocks.RAREGROUND_ORE.get(), ModBlocks.RAREGROUND_ORE_DEEPSLATE.get(), 4);
        ore(ctx, "lignite_ore", stone, deepslate, ModBlocks.LIGNITE_ORE.get(), ModBlocks.LIGNITE_ORE_DEEPSLATE.get(), 15);
        ore(ctx, "asbestos_ore", stone, deepslate, ModBlocks.ASBESTOS_ORE.get(), ModBlocks.ASBESTOS_ORE_DEEPSLATE.get(), 7);
        ore(ctx, "cinnabar_ore", stone, deepslate, ModBlocks.CINNABAR_ORE.get(), ModBlocks.CINNABAR_ORE_DEEPSLATE.get(), 4);
        ore(ctx, "cobalt_ore", stone, deepslate, ModBlocks.COBALT_ORE.get(), ModBlocks.COBALT_ORE_DEEPSLATE.get(), 5);
        ore(ctx, "lithium_ore", stone, deepslate, ModBlocks.LITHIUM_ORE.get(), ModBlocks.LITHIUM_ORE_DEEPSLATE.get(), 6);
        ore(ctx, "coltan_ore", stone, deepslate, ModBlocks.COLTAN_ORE.get(), ModBlocks.COLTAN_ORE_DEEPSLATE.get(), 4);
        ore(ctx, "alexandrite_ore", stone, null, ModBlocks.ALEXANDRITE_ORE.get(), null, 3);
        ore(ctx, "australium_ore", stone, deepslate, ModBlocks.AUSTRALIUM_ORE.get(), null, 6);
        ore(ctx, "sequestrum_ore", stone, deepslate, ModBlocks.SEQUESTRUM_ORE.get(), null, 7);

        // Потайные скважины красной комнаты (1/4 чанка, y 6..19 в 1.7.10 -> 6..30)
        simpleBlock(ctx, "stone_keyhole", ModBlocks.STONE_KEYHOLE.get());

        // Газовые пузыри под землёй (gasbubbleSpawn оригинала)
        ore(ctx, "gas_flammable", stone, deepslate, ModBlocks.GAS_FLAMMABLE.get(), null, 10);

        // --- гнейсовые пласты и руды в них (SchistStratum + ore_gneiss_* оригинала) ---
        ore(ctx, "stone_gneiss", stone, deepslate, ModBlocks.STONE_GNEISS.get(), null, 32);
        ore(ctx, "gneiss_iron_ore", gneiss, null, ModBlocks.GNEISS_IRON_ORE.get(), null, 6);
        ore(ctx, "gneiss_gold_ore", gneiss, null, ModBlocks.GNEISS_GOLD_ORE.get(), null, 6);
        ore(ctx, "gneiss_uranium_ore", gneiss, null, ModBlocks.GNEISS_URANIUM_ORE.get(), null, 6);
        ore(ctx, "gneiss_copper_ore", gneiss, null, ModBlocks.GNEISS_COPPER_ORE.get(), null, 6);
        ore(ctx, "gneiss_asbestos_ore", gneiss, null, ModBlocks.GNEISS_ASBESTOS_ORE.get(), null, 6);
        ore(ctx, "gneiss_lithium_ore", gneiss, null, ModBlocks.GNEISS_LITHIUM_ORE.get(), null, 6);
        ore(ctx, "gneiss_rare_ore", gneiss, null, ModBlocks.GNEISS_RARE_ORE.get(), null, 6);
        ore(ctx, "gneiss_gas_ore", gneiss, null, ModBlocks.GNEISS_GAS_ORE.get(), null, 10);

        // --- ресурсные скопления (resource_*) ---
        ore(ctx, "resource_asbestos", stone, deepslate, ModBlocks.RESOURCE_ASBESTOS.get(), null, 5);
        ore(ctx, "resource_bauxite", stone, deepslate, ModBlocks.RESOURCE_BAUXITE.get(), null, 40);
        ore(ctx, "resource_hematite", stone, deepslate, ModBlocks.RESOURCE_HEMATITE.get(), null, 60);
        ore(ctx, "resource_limestone", stone, deepslate, ModBlocks.RESOURCE_LIMESTONE.get(), null, 20);
        ore(ctx, "resource_malachite", stone, deepslate, ModBlocks.RESOURCE_MALACHITE.get(), null, 40);
        ore(ctx, "resource_sulfur", stone, deepslate, ModBlocks.RESOURCE_SULFUR.get(), null, 40);

        // --- незерские руды (ore_nether_* оригинала, target netherrack) ---
        ore(ctx, "nether_uranium_ore", netherrack, null, ModBlocks.NETHER_URANIUM_ORE.get(), null, 6);
        ore(ctx, "nether_tungsten_ore", netherrack, null, ModBlocks.NETHER_TUNGSTEN_ORE.get(), null, 10);
        ore(ctx, "nether_sulfur_ore", netherrack, null, ModBlocks.NETHER_SULFUR_ORE.get(), null, 12);
        ore(ctx, "nether_fire_ore", netherrack, null, ModBlocks.NETHER_FIRE_ORE.get(), null, 6);
        ore(ctx, "nether_coal_ore", netherrack, null, ModBlocks.NETHER_COAL_ORE.get(), null, 32);
        ore(ctx, "nether_cobalt_ore", netherrack, null, ModBlocks.NETHER_COBALT_ORE.get(), null, 6);
        ore(ctx, "nether_plutonium_ore", netherrack, null, ModBlocks.NETHER_PLUTONIUM_ORE.get(), null, 4);
        ore(ctx, "nether_smoldering_ore", netherrack, null, ModBlocks.NETHER_SMOLDERING_ORE.get(), null, 8);
        ore(ctx, "depth_nether_neodymium_bottom", netherrack, null, ModBlocks.DEPTH_NETHER_NEODYMIUM.get(), null, 9);
        ore(ctx, "depth_nether_neodymium_top", netherrack, null, ModBlocks.DEPTH_NETHER_NEODYMIUM.get(), null, 9);

        // --- руда Энда (ore_tikite оригинала) ---
        ore(ctx, "tikite_ore", new BlockMatchTest(Blocks.END_STONE), null, ModBlocks.TIKITE_ORE.get(), null, 6);

        // --- кастомные фичи (раньше — рукописные JSON) ---
        ctx.register(cf("ore_bedrock_mineral"), new ConfiguredFeature<>(ModWorldGen.BEDROCK_ORE.get(), new NoneFeatureConfiguration()));
        ctx.register(cf("ore_bedrock_oil"), new ConfiguredFeature<>(ModWorldGen.BEDROCK_OIL_ORE.get(), new NoneFeatureConfiguration()));
        ctx.register(cf("nether_bedrock_ore"), new ConfiguredFeature<>(ModWorldGen.NETHER_BEDROCK_ORE.get(), new NoneFeatureConfiguration()));
        ctx.register(cf("oil_deposit"), new ConfiguredFeature<>(ModWorldGen.OIL_DEPOSIT.get(), new NoneFeatureConfiguration()));
        ctx.register(cf("sand_oil_deposit"), new ConfiguredFeature<>(ModWorldGen.SAND_OIL_DEPOSIT.get(), new NoneFeatureConfiguration()));

        simpleBlock(ctx, "mine_ap", ModBlocks.MINE_AP.get());
        simpleBlock(ctx, "dud_conventional", ModBlocks.DUD_CONVENTIONAL.get());
        simpleBlock(ctx, "dud_nuke", ModBlocks.DUD_NUKE.get());
        simpleBlock(ctx, "dud_salted", ModBlocks.DUD_SALTED.get());

        // Земляничный куст (random_patch, как в оригинальном strawberry.json)
        ctx.register(cf("strawberry_bush_configured"), new ConfiguredFeature<>(Feature.RANDOM_PATCH, new RandomPatchConfiguration(
                64, 7, 3, PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.STRAWBERRY_BUSH.get()))))));
    }

    public static void bootstrapPlaced(BootstapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> features = ctx.lookup(Registries.CONFIGURED_FEATURE);

        // Порядок модификаторов: (rarity) → count → in_square → height → biome
        Map<String, PlacementParams> p = new LinkedHashMap<>();
        // Существующий баланс 1.20.1 (высоты уже адаптированы под 1.18+)
        p.put("uranium_ore", PlacementParams.of(6, -64, -20));
        p.put("thorium_ore", PlacementParams.of(6, -64, -20));
        p.put("titanium_ore", PlacementParams.of(12, -64, 30));
        p.put("sulfur_ore", PlacementParams.of(10, 0, 60));
        p.put("aluminum_ore", PlacementParams.of(13, -60, 128));
        p.put("fluorite_ore", PlacementParams.of(10, 0, 60));
        p.put("niter_ore", PlacementParams.of(6, -64, -8));
        p.put("tungsten_ore", PlacementParams.of(8, 1, 30));
        p.put("lead_ore", PlacementParams.of(20, -64, 10));
        p.put("beryllium_ore", PlacementParams.of(12, -64, 30));
        p.put("rareground_ore", PlacementParams.of(8, -64, 10));
        p.put("lignite_ore", PlacementParams.of(12, 0, 128));
        p.put("asbestos_ore", PlacementParams.of(10, 0, 128));
        p.put("cinnabar_ore", PlacementParams.of(8, -64, 10));
        p.put("cobalt_ore", PlacementParams.of(6, -64, -20));
        p.put("lithium_ore", PlacementParams.of(4, -64, -40));
        p.put("coltan_ore", PlacementParams.of(2, -56, 0));
        p.put("alexandrite_ore", PlacementParams.of(1, -48, -24));
        p.put("australium_ore", PlacementParams.rare(1, -64, -48, 12));
        p.put("sequestrum_ore", PlacementParams.of(10, 0, 128));
        p.put("gas_flammable", PlacementParams.of(12, -60, 0));
        // Гнейсовый пласт: полоса у бывшей поверхности 1.7.10 (y≈30 → -48..0)
        p.put("stone_gneiss", PlacementParams.of(1, -48, 0));
        p.put("gneiss_iron_ore", PlacementParams.of(25, -48, 0));
        p.put("gneiss_gold_ore", PlacementParams.of(10, -48, 0));
        p.put("gneiss_uranium_ore", PlacementParams.of(18, -48, 0));
        p.put("gneiss_copper_ore", PlacementParams.of(36, -48, 0));
        p.put("gneiss_asbestos_ore", PlacementParams.of(12, -48, 0));
        p.put("gneiss_lithium_ore", PlacementParams.of(6, -48, 0));
        p.put("gneiss_rare_ore", PlacementParams.of(6, -48, 0));
        p.put("gneiss_gas_ore", PlacementParams.of(15, -48, 0));
        // Ресурсные скопления
        p.put("resource_asbestos", PlacementParams.of(12, -60, 128));
        p.put("resource_bauxite", PlacementParams.of(1, -60, 128));
        p.put("resource_hematite", PlacementParams.of(1, -60, 128));
        p.put("resource_limestone", PlacementParams.of(6, -60, 128));
        p.put("resource_malachite", PlacementParams.of(1, -60, 128));
        p.put("resource_sulfur", PlacementParams.of(1, -60, 128));
        // Незер
        p.put("nether_uranium_ore", PlacementParams.of(6, 10, 117));
        p.put("nether_tungsten_ore", PlacementParams.of(10, 10, 117));
        p.put("nether_sulfur_ore", PlacementParams.of(12, 10, 117));
        p.put("nether_fire_ore", PlacementParams.of(6, 10, 117));
        p.put("nether_coal_ore", PlacementParams.of(12, 16, 112));
        p.put("nether_cobalt_ore", PlacementParams.of(6, 90, 120));
        p.put("nether_plutonium_ore", PlacementParams.of(4, 10, 117));
        p.put("nether_smoldering_ore", PlacementParams.of(12, 16, 112));
        p.put("depth_nether_neodymium_bottom", PlacementParams.of(1, 0, 2));
        p.put("depth_nether_neodymium_top", PlacementParams.of(1, 124, 126));

        for (Map.Entry<String, PlacementParams> e : p.entrySet()) {
            PlacementParams v = e.getValue();
            List<PlacementModifier> mods = new ArrayList<>();
            if (v.rarity > 0) mods.add(RarityFilter.onAverageOnceEvery(v.rarity));
            mods.add(CountPlacement.of(v.count));
            mods.add(InSquarePlacement.spread());
            mods.add(HeightRangePlacement.triangle(VerticalAnchor.absolute(v.minY), VerticalAnchor.absolute(v.maxY)));
            mods.add(BiomeFilter.biome());
            placed(ctx, features, e.getKey(), mods);
        }

        // Кастомные фичи
        placed(ctx, features, "ore_bedrock_mineral", List.of(
                RarityFilter.onAverageOnceEvery(12), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-64)),
                BiomeFilter.biome()));
        placed(ctx, features, "ore_bedrock_oil", List.of(
                RarityFilter.onAverageOnceEvery(200), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-63)),
                BiomeFilter.biome()));
        // Нефтяные месторождения (MapGenBubble oilSpawn=100: 1/100 чанков, y 15..40 → -48..-8)
        placed(ctx, features, "oil_deposit", List.of(
                RarityFilter.onAverageOnceEvery(100), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(-8)),
                BiomeFilter.biome()));
        // Песчаные месторождения (sandBubbleSpawn=200, пустынные биомы)
        placed(ctx, features, "sand_oil_deposit", List.of(
                RarityFilter.onAverageOnceEvery(200), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(60)),
                BiomeFilter.biome()));
        // Руда Энда (ore_tikite: 6 жил, y 0..127 → 5..120)
        placed(ctx, features, "tikite_ore", List.of(
                CountPlacement.of(6), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(120)),
                BiomeFilter.biome()));
        // Скважины красной комнаты (rand.nextInt(4)==0 на чанк, y 6..19 -> 6..30)
        placed(ctx, features, "stone_keyhole", List.of(
                RarityFilter.onAverageOnceEvery(4), CountPlacement.of(1), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(6), VerticalAnchor.absolute(30)),
                BiomeFilter.biome()));
        // Незерская бедрок-руда (1/10 чанков, y 0)
        placed(ctx, features, "nether_bedrock_ore", List.of(
                RarityFilter.onAverageOnceEvery(10), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(0)),
                BiomeFilter.biome()));

        // Поверхностные: мины и неразорвавшиеся боеприпасы (surface_structures)
        surfacePlaced(ctx, features, "mine_ap", 200, -2);
        surfacePlaced(ctx, features, "dud_conventional", 900, -1);
        surfacePlaced(ctx, features, "dud_nuke", 900, -1);
        surfacePlaced(ctx, features, "dud_salted", 1000, -1);

        placedRef(ctx, features, "strawberry_bush", "strawberry_bush_configured", List.of(
                RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                BiomeFilter.biome()));
    }

    public static void bootstrapBiomeModifiers(BootstapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);

        // Overworld: все подземные руды
        List<String> overworldOres = List.of(
                "uranium_ore", "thorium_ore", "titanium_ore", "sulfur_ore", "aluminum_ore",
                "fluorite_ore", "niter_ore", "tungsten_ore", "lead_ore",
                "beryllium_ore", "rareground_ore", "lignite_ore", "asbestos_ore", "cinnabar_ore",
                "cobalt_ore", "lithium_ore", "coltan_ore", "alexandrite_ore", "australium_ore",
                "sequestrum_ore", "gas_flammable",
                "stone_gneiss", "gneiss_iron_ore", "gneiss_gold_ore", "gneiss_uranium_ore",
                "gneiss_copper_ore", "gneiss_asbestos_ore", "gneiss_lithium_ore", "gneiss_rare_ore", "gneiss_gas_ore",
                "resource_asbestos", "resource_bauxite", "resource_hematite", "resource_limestone",
                "resource_malachite", "resource_sulfur",
                "ore_bedrock_mineral", "ore_bedrock_oil", "oil_deposit", "stone_keyhole");
        addModifier(ctx, biomes, placed, "add_overworld_ores", BiomeTags.IS_OVERWORLD, overworldOres,
                GenerationStep.Decoration.UNDERGROUND_ORES);

        // Overworld: поверхностные структуры
        addModifier(ctx, biomes, placed, "add_surface_features", BiomeTags.IS_OVERWORLD,
                List.of("mine_ap", "dud_conventional", "dud_nuke", "dud_salted"),
                GenerationStep.Decoration.SURFACE_STRUCTURES);

        // Overworld: растительность
        addModifier(ctx, biomes, placed, "add_vegetal_features", BiomeTags.IS_OVERWORLD,
                List.of("strawberry_bush"), GenerationStep.Decoration.VEGETAL_DECORATION);

        // Nether
        addModifier(ctx, biomes, placed, "add_nether_ores", BiomeTags.IS_NETHER,
                List.of("nether_uranium_ore", "nether_tungsten_ore", "nether_sulfur_ore", "nether_fire_ore",
                        "nether_coal_ore", "nether_cobalt_ore", "nether_plutonium_ore", "nether_smoldering_ore",
                        "depth_nether_neodymium_bottom", "depth_nether_neodymium_top", "nether_bedrock_ore"),
                GenerationStep.Decoration.UNDERGROUND_ORES);

        // End: тикит (endOre оригинала)
        addModifier(ctx, biomes, placed, "add_end_ores", BiomeTags.IS_END,
                List.of("tikite_ore"), GenerationStep.Decoration.UNDERGROUND_ORES);

        // Пустынные биомы: песчаные нефтяные месторождения (canSpawn: !rain && t>=1.5)
        addModifierForBiomes(ctx, biomes, placed, "add_sand_oil",
                List.of(net.minecraft.world.level.biome.Biomes.DESERT,
                        net.minecraft.world.level.biome.Biomes.BADLANDS,
                        net.minecraft.world.level.biome.Biomes.WOODED_BADLANDS,
                        net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS,
                        net.minecraft.world.level.biome.Biomes.SAVANNA,
                        net.minecraft.world.level.biome.Biomes.SAVANNA_PLATEAU,
                        net.minecraft.world.level.biome.Biomes.WINDSWEPT_SAVANNA,
                        net.minecraft.world.level.biome.Biomes.BEACH),
                List.of("sand_oil_deposit"), GenerationStep.Decoration.UNDERGROUND_ORES);
    }

    private static void addModifier(BootstapContext<BiomeModifier> ctx,
            HolderGetter<Biome> biomes, HolderGetter<PlacedFeature> placed, String name,
            TagKey<Biome> biomeTag, List<String> featureNames, GenerationStep.Decoration step) {
        addModifierDirect(ctx, placed, name, biomes.getOrThrow(biomeTag), featureNames, step);
    }

    private static void addModifierForBiomes(BootstapContext<BiomeModifier> ctx,
            HolderGetter<Biome> biomes, HolderGetter<PlacedFeature> placed, String name,
            List<net.minecraft.resources.ResourceKey<Biome>> biomeKeys, List<String> featureNames,
            GenerationStep.Decoration step) {
        List<net.minecraft.core.Holder<Biome>> biomeHolders = new ArrayList<>();
        for (net.minecraft.resources.ResourceKey<Biome> key : biomeKeys) {
            biomeHolders.add(biomes.getOrThrow(key));
        }
        addModifierDirect(ctx, placed, name, HolderSet.direct(biomeHolders), featureNames, step);
    }

    private static void addModifierDirect(BootstapContext<BiomeModifier> ctx,
            HolderGetter<PlacedFeature> placed, String name,
            net.minecraft.core.HolderSet<Biome> biomeSet, List<String> featureNames,
            GenerationStep.Decoration step) {
        List<Holder<PlacedFeature>> holders = new ArrayList<>();
        for (String fn : featureNames) {
            holders.add(placed.getOrThrow(pf(fn)));
        }
        ctx.register(bm(name), new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                biomeSet, HolderSet.direct(holders), step));
    }

    // ====================================================================

    private record PlacementParams(int count, int minY, int maxY, int rarity) {
        static PlacementParams of(int count, int minY, int maxY) {
            return new PlacementParams(count, minY, maxY, 0);
        }

        static PlacementParams rare(int count, int minY, int maxY, int rarity) {
            return new PlacementParams(count, minY, maxY, rarity);
        }
    }

    private static void placed(BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> features,
            String name, List<PlacementModifier> mods) {
        placedRef(ctx, features, name, name, mods);
    }

    private static void placedRef(BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> features,
            String name, String configuredName, List<PlacementModifier> mods) {
        ctx.register(pf(name), new PlacedFeature(features.getOrThrow(cf(configuredName)), mods));
    }

    private static void surfacePlaced(BootstapContext<PlacedFeature> ctx,
            HolderGetter<ConfiguredFeature<?, ?>> features, String name, int rarity, int soilOffset) {
        placed(ctx, features, name, List.of(
                RarityFilter.onAverageOnceEvery(rarity), InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(
                        new Vec3i(0, soilOffset, 0),
                        Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.RED_SAND,
                        Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MYCELIUM)),
                BiomeFilter.biome()));
    }

    private static void ore(BootstapContext<ConfiguredFeature<?, ?>> ctx, String name,
            RuleTest target, RuleTest deepTarget, Block block, Block deepBlock, int size) {
        List<OreConfiguration.TargetBlockState> states = new ArrayList<>();
        states.add(OreConfiguration.target(target, block.defaultBlockState()));
        if (deepTarget != null && deepBlock != null) {
            states.add(OreConfiguration.target(deepTarget, deepBlock.defaultBlockState()));
        }
        ctx.register(cf(name), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(states, size)));
    }

    private static void simpleBlock(BootstapContext<ConfiguredFeature<?, ?>> ctx, String name, Block block) {
        // random_patch tries=1, xz=7, y=0 — как в рукописных JSON (mine_ap, dud_*)
        ctx.register(cf(name), new ConfiguredFeature<>(Feature.RANDOM_PATCH, new RandomPatchConfiguration(
                1, 7, 0, PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(block.defaultBlockState()))))));
    }
}
//?}
