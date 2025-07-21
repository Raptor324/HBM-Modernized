package com.hbm_m.datagen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.tags.BlockTags;
import com.hbm_m.worldgen.ModWorldGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder()
                // --- ШАГ 1: Определяем ConfiguredFeature (ЧТО генерировать) ---
                .add(Registries.CONFIGURED_FEATURE, context -> {
                    RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);

                    List<OreConfiguration.TargetBlockState> targets = List.of(
                            OreConfiguration.target(stoneReplaceables, ModBlocks.URANIUM_ORE.get().defaultBlockState())
                    );

                    context.register(ModWorldGen.URANIUM_ORE_CONFIGURED_KEY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets, 7)));
                })
                // --- ШАГ 2: Определяем PlacedFeature (ГДЕ генерировать) ---
                .add(Registries.PLACED_FEATURE, context -> {
                    var configuredFeature = context.lookup(Registries.CONFIGURED_FEATURE);

                    context.register(ModWorldGen.URANIUM_ORE_PLACED_KEY, new PlacedFeature(
                            configuredFeature.getOrThrow(ModWorldGen.URANIUM_ORE_CONFIGURED_KEY),
                            orePlacement(CountPlacement.of(4), // 4 жилы на чанк
                                         HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(50)))
                    ));
                })
                // --- ШАГ 3: ИСПОЛЬЗУЕМ PlacedFeature в BiomeModifier ---
                .add(ForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    context.register(
                            ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, RefStrings.resourceLocation("add_uranium_ore")),
                            new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                                    context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                                    HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(ModWorldGen.URANIUM_ORE_PLACED_KEY)),
                                    GenerationStep.Decoration.UNDERGROUND_ORES
                            )
                    );
                });

        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                packOutput, lookupProvider, registrySetBuilder, Set.of(RefStrings.MODID)
        ));
    }

    // Вспомогательный метод для создания списка правил размещения (скопирован из ванильного кода для чистоты)
    public static List<PlacementModifier> orePlacement(PlacementModifier p_195347_, PlacementModifier p_195348_) {
        return List.of(p_195347_, InSquarePlacement.spread(), p_195348_, BiomeFilter.biome());
    }
}