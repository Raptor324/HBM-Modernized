package com.hbm_m.main;

import org.slf4j.Logger;

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
import com.hbm_m.recipe.ModRecipes;
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

        //? if neoforge {
        /*// NeoForge 1.21+: регистрация сетевых пейлоадов обязана произойти ДО фазы
        // NetworkRegistry.setup() → RegisterPayloadHandlersEvent, которая выполняется
        // ПОСЛЕ конструкторов модов, но ДО FMLCommonSetupEvent/FMLClientSetupEvent.
        // Architectury добавляет слушатель RegisterPayloadHandlersEvent в момент вызова
        // registerReceiver, поэтому поздняя регистрация из common/client setup молча
        // не срабатывает → S2C-пакеты (радиация, гейгер, дебаг-рендер) не доходят до клиента.
        ModPacketHandler.register();
        ModPacketHandler.registerClientReceivers();
        *///?}

        // Registries (common)
        // Must run before any world loads: the RBMK dials are world game rules in the original.
        com.hbm_m.handler.rbmk.RBMKGameRules.register();
        DoorDeclRegistry.init();
        ModBiomes.init();
        ModBlocks.init();
        ModEntities.init();
        ModExplosionParticles.init();
        ModSounds.init();
        ModItems.init();
        //? if neoforge {
        /*com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess.init();
        *///?}
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
        // Опциональный Curios: слушатели вешаются только при наличии мода,
        // иначе классы Curios API вообще не загружаются (NoClassDefFoundError).
        com.hbm_m.compat.curios.CuriosCompat.init();
        PowerArmorHandlers.register();
        LadderClimbHandler.register();
        com.hbm_m.server.missile.MissileTrackBroadcaster.register();


        // Common lifecycle hooks
        LifecycleEvent.SETUP.register(MainRegistry::commonSetup);

        TickEvent.SERVER_POST.register(server -> {
            // 1. Защита от фейковых/недогруженных серверов (Flashback)
            if (server == null) {
                return; 
            }
            
            // 2. Защита от отсутствующего измерения
            ServerLevel level = server.overworld();
            if (level == null) {
                return;
            }
            // Энергосеть: сначала обновляем подписки машин, затем узлы и распределение —
            // единая точка тика для PowerNet и жидкостей
            com.hbm_m.api.energy.EnergySubscriptions.tickAll(server);
            com.hbm_m.api.network.UniNodespace.updateNodespace(server);
            // Process RBMK neutron streams for every loaded server level. The tick counter that
            // paces the node-cache sweep is advanced once per server tick, not once per level.
            NeutronNodeWorld.removeEmptyWorlds();
            boolean rbmkCacheClear = NeutronNodeWorld.advanceTick();
            for (ServerLevel rbmkLevel : server.getAllLevels()) {
                NeutronNodeWorld.tick(rbmkLevel, rbmkCacheClear);
            }
            // Отложенный фикс соединений решёток/паней после спавна структур
            com.hbm_m.worldgen.StructureConnectionFixProcessor.tickIfReady(server);
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
        // Panel slabs reference each other, so the pairing runs once after registration.
        com.hbm_m.block.ModBlocks.linkSlabPairs();
        com.hbm_m.handler.HTTPHandler.loadStats();
        ModPacketHandler.register();
        com.hbm_m.handler.HazmatRegistry.registerHazmats();
        com.hbm_m.handler.ArmorRegistryInit.init();
        HazardRegistry.registerItems();
        com.hbm_m.event.LungGasHandler.init();
        com.hbm_m.config.FalloutConfigJSON.initialize();
        DamageResistanceHandler.initArmorStats();
        com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity.registerLaunchables();
        com.hbm_m.satellite.Satellite.register();

        // Диагностика загрузки рецептов на 1.21.1 — запускается ПОСЛЕ RegisterEvent.
        //? if >= 1.21.1 {
        /*com.hbm_m.recipe.ModRecipes.debugRecipeSerializerRegistry();
        *///?}

        // CentrifugeRecipes.registerRecipes();
        // Рецепты Cyclotron, CrucibleSmelting, MoltenAlloy, ArcWelder и Soldering теперь data-driven (JSON)
        // — статические реестры (ArcWelderRecipes.registerDefaults / SolderingRecipes.registerDefaults) удалены.

        // На Fabric DeferredRegister жидкостей ещё не заполнил BuiltInRegistries на момент SETUP
        // (см. FabricEntrypoint#registerFluidDependentSetupWhenReady).
        
        //? if forge || neoforge {
        // Рецепты Crystallizer теперь data-driven (JSON), CrystallizerRecipes.registerDefaults удалён.
        ModFluidTraitsBootstrap.registerAll();
        //?}

        LOGGER.info("Common setup finished");
    }
}
