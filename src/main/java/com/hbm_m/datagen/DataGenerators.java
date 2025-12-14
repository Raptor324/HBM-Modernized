package com.hbm_m.datagen;

// Класс для регистрации всех генераторов данных (дата генераторов) мода.
// Здесь мы регистрируем провайдеры для тегов блоков, предметов, рецептов, моделей, локализаций и других данных.
// Также настраиваем встроенные записи в datapack, такие как типы урона и генерация мира.
// Используется в основном классе мода для инициализации датагенов при сборке.

import com.hbm_m.datagen.assets.ModBlockStateProvider;
import com.hbm_m.datagen.assets.ModBlockTagProvider;
import com.hbm_m.datagen.assets.ModItemModelProvider;
import com.hbm_m.datagen.assets.ModItemTagProvider;
import com.hbm_m.datagen.recipes.ModRecipeProvider;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.worldgen.ModWorldGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageType;
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
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        ModBlockTagProvider blockTagProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagProvider);
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput));

        // Создаем и регистрируем провайдер для тегов ПРЕДМЕТОВ.
        // Он зависит от провайдера блоков, поэтому мы передаем в него blockTagProvider.contentsGetter()
        generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider, blockTagProvider.contentsGetter(), existingFileHelper));

        DatapackBuiltinEntriesProvider datapackProvider = new DatapackBuiltinEntriesProvider(
                packOutput, lookupProvider, getRegistrySetBuilder(), Set.of(RefStrings.MODID)
        );


        @SuppressWarnings("deprecation")
        CompletableFuture<HolderLookup.Provider> newLookupProvider = datapackProvider.getRegistryProvider();
        generator.addProvider(event.includeServer(), datapackProvider);
        generator.addProvider(event.includeServer(), new ModDamageTypeTagProvider(packOutput, newLookupProvider, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "ru_ru"));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "en_us"));
    }

    public static RegistrySetBuilder getRegistrySetBuilder() {
        return new RegistrySetBuilder()
                .add(Registries.DAMAGE_TYPE, context -> {
                    for (Field field : ModDamageTypes.class.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(ResourceKey.class)) {
                            try {
                                @SuppressWarnings("unchecked")
                                ResourceKey<DamageType> key = (ResourceKey<DamageType>) field.get(null);
                                context.register(key, new DamageType(key.location().getPath(), 0.1F));
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Could not access DamageType key field: " + field.getName(), e);
                            }
                        }
                    }
                })
                .add(Registries.CONFIGURED_FEATURE, context -> {
                    RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
                    List<OreConfiguration.TargetBlockState> targets = List.of(OreConfiguration.target(stoneReplaceables, ModBlocks.URANIUM_ORE.get().defaultBlockState()));
                    context.register(ModWorldGen.URANIUM_ORE_CONFIGURED_KEY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets, 1)));
                })
                .add(Registries.PLACED_FEATURE, context -> {
                    var configuredFeature = context.lookup(Registries.CONFIGURED_FEATURE);
                    context.register(ModWorldGen.URANIUM_ORE_PLACED_KEY, new PlacedFeature(
                            configuredFeature.getOrThrow(ModWorldGen.URANIUM_ORE_CONFIGURED_KEY),
                            orePlacement(CountPlacement.of(1), HeightRangePlacement.uniform(VerticalAnchor.absolute(-60), VerticalAnchor.absolute(-62)))
                    ));
                })

                .add(ForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    context.register(
                            ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "add_uranium_ore")),
                            new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                                    context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                                    HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(ModWorldGen.URANIUM_ORE_PLACED_KEY)),
                                    GenerationStep.Decoration.UNDERGROUND_ORES
                            )
                    );
                });


    }

    // Вспомогательный метод для создания списка правил размещения
    public static List<PlacementModifier> orePlacement(PlacementModifier p_195347_, PlacementModifier p_195348_) {
        return List.of(p_195347_, InSquarePlacement.spread(), p_195348_, BiomeFilter.biome());
    }


}