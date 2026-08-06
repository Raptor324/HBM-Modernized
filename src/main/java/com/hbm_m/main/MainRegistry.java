package com.hbm_m.main;

import org.slf4j.Logger;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.api.fluids.bootstrap.ModFluidTraitsBootstrap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.event.BombDefuser;
import com.hbm_m.event.CrateBreaker;
import com.hbm_m.event.HazardEventHandler;
import com.hbm_m.event.PlayerHazardHandler;
import com.hbm_m.event.ScrewdriverInteractionHandler;
import com.hbm_m.handler.MobGearHandler;
import com.hbm_m.handler.rbmk.NeutronNodeWorld;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.multiblock.LadderClimbHandler;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.powerarmor.PowerArmorHandlers;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.recipe.CentrifugeRecipes;
import com.hbm_m.recipe.ChemicalPlantRecipes;
import com.hbm_m.recipe.CyclotronRecipes;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.recipe.CrystallizerRecipes;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.world.biome.ModBiomes;
import com.mojang.logging.LogUtils;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;

public final class MainRegistry {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = RefStrings.MODID;

    /** Порт {@code MainRegistry.missileTab} / {@code MainRegistry.nukeTab} (1.7.10). */
    public static final RegistrySupplier<CreativeModeTab> missileTab = ModCreativeTabs.NTM_MISSILES_TAB;
    public static final RegistrySupplier<CreativeModeTab> nukeTab = ModCreativeTabs.NTM_BOMBS_TAB;

    static {
        // Загрузка JSON-конфига (client.json + server.json) ДО любой инициализации,
        // чтобы классы, захватывающие значения в static final (ChunkRadiationHandlerSimple.MAX_RAD),
        // видели загруженные значения. Замена AutoConfig.register(...).
        ModClothConfig.load();
    }

    public static void init() {
        LOGGER.info("Initializing {}", RefStrings.NAME);

        // Registries (common)
        DoorDeclRegistry.init();
        ModBiomes.init();
        ModBlocks.init();
        ModEntities.init();
        ModExplosionParticles.init();
        ModSounds.init();
        ModItems.init();
        ModMenuTypes.init();
        ModCreativeTabs.init();
        ModParticleTypes.init();
        ModBlockEntities.init();
        ModEffects.init();
        ModRecipes.init();
        MobGearHandler.init();
        CrateBreaker.init();
        HazardEventHandler.init();
        PlayerHazardHandler.init();
        ScrewdriverInteractionHandler.init();
        BombDefuser.init();
        PlayerHandler.register();
        ChunkRadiationManager.init();
        ModEventHandler.register();
        PowerArmorHandlers.register();
        LadderClimbHandler.register();
        com.hbm_m.server.missile.MissileTrackBroadcaster.register();


        // Common lifecycle hooks
        LifecycleEvent.SETUP.register(MainRegistry::commonSetup);

        TickEvent.SERVER_POST.register(server -> {
            EnergyNetworkManager.get(server.overworld()).tick();
            com.hbm_m.api.network.UniNodespace.updateNodespace(server);
            // Process RBMK neutron streams for every loaded server level
            for (ServerLevel level : server.getAllLevels()) {
                NeutronNodeWorld.tick(level);
            }
        });

        LifecycleEvent.SERVER_LEVEL_LOAD.register((ServerLevel level) -> {
            EnergyNetworkManager.get(level).rebuildAllNetworks();
        });

        LifecycleEvent.SERVER_LEVEL_UNLOAD.register((ServerLevel level) -> {
            com.hbm_m.api.network.UniNodespace.onLevelUnload(level);
        });

        LifecycleEvent.SERVER_STOPPED.register(server -> {
            com.hbm_m.api.network.UniNodespace.onServerStop();
            com.hbm_m.api.fluids.FluidNetProvider.clearAll();
        });
    }

    private static void commonSetup() {
        com.hbm_m.handler.HTTPHandler.loadStats();
        ModPacketHandler.register();
        com.hbm_m.handler.HazmatRegistry.registerHazmats();
        HazardRegistry.registerItems();
        com.hbm_m.config.FalloutConfigJSON.initialize();
        DamageResistanceHandler.initArmorStats();
        com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity.registerLaunchables();
        com.hbm_m.satellite.Satellite.register();

        CentrifugeRecipes.registerRecipes();
        CyclotronRecipes.registerRecipes();
        com.hbm_m.inventory.recipes.ArcWelderRecipes.registerDefaults();
        com.hbm_m.recipe.CrucibleSmeltingRecipes.registerDefaults();
        com.hbm_m.recipe.MoltenAlloyRecipes.registerDefaults();
        com.hbm_m.inventory.recipes.SolderingRecipes.registerDefaults();

        // На Fabric DeferredRegister жидкостей ещё не заполнил BuiltInRegistries на момент SETUP
        // (см. FabricEntrypoint#registerFluidDependentSetupWhenReady).
        
        //? if forge || neoforge {
        ChemicalPlantRecipes.registerRecipes();
        CrystallizerRecipes.registerDefaults();
        ModFluidTraitsBootstrap.registerAll();
        //?}

        LOGGER.info("Common setup finished");
    }
}
