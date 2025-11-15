package com.hbm_m.world.biome;

import com.hbm_m.main.MainRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.biome.Biome;

/**
 * Реестр для кастомных биомов кратера
 * Inner Crater и Outer Crater
 */
public class ModBiomes {

    public static final ResourceKey<Biome> INNER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("hbm_m", "inner_crater"));
    public static final ResourceKey<Biome> OUTER_CRATER_KEY =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("hbm_m", "outer_crater"));

    public static void bootstrap(BootstapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        context.register(INNER_CRATER_KEY,
                CraterBiomes.createInnerCraterBiome(placedFeatures, carvers));
        context.register(OUTER_CRATER_KEY,
                CraterBiomes.createOuterCraterBiome(placedFeatures, carvers));
    }
}
