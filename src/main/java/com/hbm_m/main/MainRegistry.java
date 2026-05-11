package com.hbm_m.main;

import org.slf4j.Logger;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.api.fluids.bootstrap.ModFluidTraitsBootstrap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.event.BombDefuser;
import com.hbm_m.event.CrateBreaker;
import com.hbm_m.event.HazardEventHandler;
import com.hbm_m.event.PlayerHazardHandler;
import com.hbm_m.event.ScrewdriverInteractionHandler;
import com.hbm_m.handler.MobGearHandler;
import com.hbm_m.hazard.ModHazards;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.multiblock.LadderClimbHandler;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.powerarmor.PowerArmorHandlers;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.recipe.CentrifugeRecipes;
import com.hbm_m.recipe.ChemicalPlantRecipes;
import com.hbm_m.recipe.CrystallizerRecipes;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.world.biome.ModBiomes;
import com.mojang.logging.LogUtils;
import com.hbm_m.recipe.CrystallizerRecipes;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;

public final class MainRegistry {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = RefStrings.MODID;

    static {
        ModClothConfig.register();
    }

    private MainRegistry() {
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
        PowerArmorHandlers.register();
        LadderClimbHandler.register();

        

        // Common lifecycle hooks
        LifecycleEvent.SETUP.register(MainRegistry::commonSetup);

        TickEvent.SERVER_POST.register(server -> {
            ServerLevel level = server.overworld();
            EnergyNetworkManager.get(level).tick();
            com.hbm_m.api.network.UniNodespace.updateNodespace(server);
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
        ModPacketHandler.register();
        ModHazards.registerHazards();
        DamageResistanceHandler.initArmorStats();

        CentrifugeRecipes.registerRecipes();
        ChemicalPlantRecipes.registerRecipes();
        CrystallizerRecipes.registerDefaults();

        ModFluidTraitsBootstrap.registerAll();

        LOGGER.info("Common setup finished");
    }
}