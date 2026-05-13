package com.hbm_m.world.biome;

import com.hbm_m.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

//? if !fabric {

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
//?}

/**
 * Ключи биомов кратера для кода (смазывание, подмена биомов при взрывах и т.д.).
 * <p><b>Fabric 1.20.1:</b> во встроенных реестрах нет статического {@code BIOME}
 * — содержимое берётся из datapack ресурсов мода (те же {@code data/hbm_m/worldgen/biome/*.json},
 * регистрировать в коде через {@code BuiltInRegistries.BIOME} нельзя).</p>
 * <p><b>Forge / NeoForge:</b> те же JSON остаются в jar; дополнительно биомы регистрируются через
 * {@link DeferredRegister} и фабрики {@link CraterBiomes}, как раньше.</p>
 */
public class ModBiomes {

    static {
        MainRegistry.LOGGER.debug("[HBM_MODS] ModBiomes class loaded");
    }

    public static final ResourceKey<Biome> INNER_CRATER_KEY =
            ResourceKey.create(
                    Registries.BIOME,
                    //? if fabric && < 1.21.1 {
                    /*new ResourceLocation(MainRegistry.MOD_ID, "inner_crater")
                    *///?} else {

                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "inner_crater")
                    //?}
                    );

    public static final ResourceKey<Biome> OUTER_CRATER_KEY =
            ResourceKey.create(
                    Registries.BIOME,
                    //? if fabric && < 1.21.1 {
                    /*new ResourceLocation(MainRegistry.MOD_ID, "outer_crater")
                    *///?} else {

                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "outer_crater")
                    //?}
                    );

    //? if !fabric {

    public static final DeferredRegister<Biome> BIOMES =
            DeferredRegister.create(MainRegistry.MOD_ID, Registries.BIOME);

    public static final RegistrySupplier<Biome> INNER_CRATER = BIOMES.register(
            "inner_crater",
            CraterBiomes::createInnerCraterBiome
    );

    public static final RegistrySupplier<Biome> OUTER_CRATER = BIOMES.register(
            "outer_crater",
            CraterBiomes::createOuterCraterBiome
    );
    //?}

    public static void init() {
        //? if !fabric {

        BIOMES.register();
        //?}
    }
}
