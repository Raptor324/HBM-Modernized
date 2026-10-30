package com.hbm_m.client;

import com.hbm_m.particle.custom.MissileContrailParticle;
import com.hbm_m.particle.ModParticleTypes;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.loader.BoxCableModelLoader;
import com.hbm_m.client.loader.CargoElevatorModelLoader;
import com.hbm_m.client.loader.DoorModelLoader;
import com.hbm_m.client.loader.DaeModelLoader;
import com.hbm_m.client.loader.MachinePartsModelLoader;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
import com.hbm_m.client.render.machine.MachineRenderRegistry;
import com.hbm_m.client.loader.MachineBatterySocketModelLoader;
import com.hbm_m.client.loader.MissileModelLoader;
import com.hbm_m.client.loader.MachineRadarModelLoader;
import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.hbm_m.client.loader.PressModelLoader;
import com.hbm_m.client.loader.TemplateModelLoader;
// import com.hbm_m.client.loader.TestModelLoader;
import com.hbm_m.client.model.ConnectedDecoBlockBakedModel;
import com.hbm_m.client.overlay.OverlayGeiger;
import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.client.overlay.OverlayRadiationVisuals;
import com.hbm_m.client.render.EmptyEntityRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.shader.ModShaders;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.effect.FleijaSphereMesh;
import com.hbm_m.client.render.effect.RenderBlackHole;
import com.hbm_m.client.render.effect.RenderQuasar;
import com.hbm_m.client.render.effect.RenderCloudFleija;
import com.hbm_m.client.render.effect.RenderFallout;
import com.hbm_m.client.render.effect.RubbleEntityRenderer;
import com.hbm_m.client.render.implementations.AirBombProjectileEntityRenderer;
import com.hbm_m.client.render.implementations.ZirnoxDebrisRenderer;
import com.hbm_m.client.render.implementations.AirNukeBombProjectileEntityRenderer;
import com.hbm_m.client.render.implementations.AirstrikeEntityRenderer;
import com.hbm_m.client.render.implementations.AirstrikeNukeEntityRenderer;
import com.hbm_m.client.render.implementations.MachineDoorRenderer;
import com.hbm_m.client.render.implementations.HeaterRenderers;
import com.hbm_m.client.render.implementations.MachineFluidTankRenderer;
import com.hbm_m.client.render.implementations.MachineFoundryRenderer;
import com.hbm_m.client.render.implementations.MachineFoundryTankRenderer;
import com.hbm_m.client.render.implementations.BarrelTankRenderer;
import com.hbm_m.client.render.implementations.MachinePumpjackRenderer;
import com.hbm_m.client.render.implementations.MachinePetrochemRenderer;
import com.hbm_m.client.render.implementations.MachineTankRenderer;
import com.hbm_m.client.render.implementations.MachineSolidifierRenderer;
import com.hbm_m.client.render.implementations.MachineEngineRenderer;
import com.hbm_m.client.render.implementations.MachineTurbineRenderer;
import com.hbm_m.client.render.implementations.MachineBoilerRenderer;
import com.hbm_m.client.render.implementations.MachinePowerRenderer;
import com.hbm_m.client.render.implementations.MachineWorkbenchRenderer;
import com.hbm_m.client.render.implementations.MachineFurnaceRenderer;
import com.hbm_m.client.render.implementations.MachinePressGroupRenderer;
import com.hbm_m.client.render.implementations.MachineFactoryRenderer;
import com.hbm_m.client.render.implementations.MachineSawmillRenderer;
import com.hbm_m.client.render.implementations.MachineMetallurgyMiscRenderer;
import com.hbm_m.client.render.implementations.MachineChemistryRenderer;
import com.hbm_m.client.render.implementations.MachineSeparationRenderer;
import com.hbm_m.client.render.implementations.MachineCentrifugeRenderer;
import com.hbm_m.client.render.implementations.MachineReactorResearchRenderer;
import com.hbm_m.client.render.implementations.MachineAcceleratorRenderer;
import com.hbm_m.client.render.implementations.MachineDFCRenderers;
import com.hbm_m.client.render.implementations.MachinePileRenderers;
import com.hbm_m.client.render.implementations.MachineRbmkControlRenderer;
import com.hbm_m.client.render.implementations.MachineRbmkDisplayRenderer;
import com.hbm_m.client.render.implementations.MachineResearchFacilityRenderer;
import com.hbm_m.client.render.implementations.MachineFusionComplexRenderer;
import com.hbm_m.client.render.implementations.MachineLaserResearchRenderer;
import com.hbm_m.client.render.implementations.MachineAerospaceRenderer;
import com.hbm_m.client.render.implementations.LaunchPadMissileRenderer;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalFactoryRenderer;
import com.hbm_m.client.render.implementations.MachineFoundryChannelRenderer;
import com.hbm_m.client.render.implementations.MachineZirnoxDestroyedRenderer;
import com.hbm_m.client.render.implementations.MachineZirnoxRenderer;
import com.hbm_m.client.render.implementations.CrucibleRenderer;
import com.hbm_m.client.render.implementations.MachineCoolingTowerRenderer;
import com.hbm_m.client.render.implementations.MachineMiningDrillRenderer;
import com.hbm_m.client.render.implementations.MachineOreSlopperRenderer;
import com.hbm_m.client.render.implementations.MachineArcFurnaceRenderer;
import com.hbm_m.client.render.implementations.SoyuzRocketRenderer;
import com.hbm_m.client.render.implementations.MachineCrystallizerRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.SoyuzLauncherRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.client.render.implementations.MachineTurbofanRenderer;
import com.hbm_m.client.render.implementations.MachineRadarRenderer;
import com.hbm_m.client.render.implementations.MachineRBMKColumnRenderer;
import com.hbm_m.client.render.implementations.MachineRBMKPanelRenderers;
import com.hbm_m.client.render.implementations.MissileEntityRenderer;
import com.hbm_m.client.render.entity.mob.RenderCreeperUniversal;
import com.hbm_m.client.render.implementations.NoloEntityRenderer;
import com.hbm_m.client.render.shader.ShaderReloadListener;
import com.hbm_m.client.tooltip.CrateContentsTooltipComponent;
import com.hbm_m.client.tooltip.CrateContentsTooltipComponentRenderer;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.config.ModConfigKeybindHandler;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.inventory.gui.GUIAnvil;
import com.hbm_m.inventory.gui.GUIArmorTable;
import com.hbm_m.inventory.gui.GUIBatterySocket;
import com.hbm_m.inventory.gui.GUIBlastFurnace;
import com.hbm_m.inventory.gui.GUIDeshCrate;
import com.hbm_m.inventory.gui.GUIHeatingOven;
import com.hbm_m.inventory.gui.GUIIronCrate;
import com.hbm_m.inventory.gui.GUILaunchPadLarge;
import com.hbm_m.inventory.gui.GUILaunchPadRusted;
import com.hbm_m.inventory.gui.GUIMachineAdvancedAssembler;
import com.hbm_m.inventory.gui.GUIMachineAssembler;
import com.hbm_m.inventory.gui.GUIMachineBattery;
import com.hbm_m.inventory.gui.GUIMachineCentrifuge;
import com.hbm_m.inventory.gui.GUIMachineCyclotron;
import com.hbm_m.inventory.gui.GUIMachineArcWelder;
import com.hbm_m.inventory.gui.GUIMachineBreeder;
import com.hbm_m.inventory.gui.GUIMachineCrackingTower;
import com.hbm_m.inventory.gui.GUIMachineCrucible;
import com.hbm_m.inventory.gui.GUIMachineDerrick;
import com.hbm_m.inventory.gui.GUIMachineFel;
import com.hbm_m.inventory.gui.GUIMachineFlareStack;
import com.hbm_m.inventory.gui.GUIMachineGasCent;
import com.hbm_m.inventory.gui.GUIMachineIndustrialBoiler;
import com.hbm_m.inventory.gui.GUIMachineFractionTower;
import com.hbm_m.inventory.gui.GUIMachineLargePylon;
import com.hbm_m.inventory.gui.GUIMachineMixer;
import com.hbm_m.inventory.gui.GUIMachineMiningDrill;
import com.hbm_m.inventory.gui.GUIMachinePumpjack;
import com.hbm_m.inventory.gui.GUIMachineRadarNT;
import com.hbm_m.inventory.gui.GUIMachineRadarNTSlots;
import com.hbm_m.inventory.gui.GUIMachineRefinery;
import com.hbm_m.inventory.gui.GUIMachineRbmkConsole;
import com.hbm_m.inventory.gui.GUIRBMKRod;
import com.hbm_m.inventory.gui.GUIRBMKControl;
import com.hbm_m.inventory.gui.GUIRBMKBoiler;
import com.hbm_m.inventory.gui.GUIRBMKHeater;
import com.hbm_m.inventory.gui.GUIRBMKStorage;
import com.hbm_m.inventory.gui.GUIRBMKAutoloader;
import com.hbm_m.inventory.gui.GUIRBMKOutgasser;
import com.hbm_m.inventory.gui.GUIMachineSilex;
import com.hbm_m.inventory.gui.GUIMachineSolderingStation;
import com.hbm_m.inventory.gui.GUIMachineSubstation;
import com.hbm_m.inventory.gui.GUIMachineSteamTurbine;
import com.hbm_m.inventory.gui.GUIMachineTurbine;
import com.hbm_m.inventory.gui.GUIMachineZirnox;
import com.hbm_m.inventory.gui.GUIMachineChemicalPlant;
import com.hbm_m.inventory.gui.GUIMachineChemicalFactory;
import com.hbm_m.inventory.gui.GUIMachineFluidTank;
import com.hbm_m.inventory.gui.GUIMachineFrackingTower;
import com.hbm_m.inventory.gui.GUIMachinePress;
import com.hbm_m.inventory.gui.GUIMachineOreSlopper;
import com.hbm_m.inventory.gui.GUIMachineCombinationOven;
import com.hbm_m.inventory.gui.GUIMachineArcFurnace;
import com.hbm_m.inventory.gui.GUIMachineShredder;
import com.hbm_m.inventory.gui.GUIMachineWoodBurner;
import com.hbm_m.inventory.gui.GUISteelCrate;
import com.hbm_m.inventory.gui.GUITemplateCrate;
import com.hbm_m.inventory.gui.GUITungstenCrate;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.item.BlockAbsorberItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.particle.explosions.basic.CameraShakeHandler;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.powerarmor.PowerArmorSounds;
import com.hbm_m.powerarmor.PowerArmorStepSoundHandler;
import com.hbm_m.powerarmor.layer.AbstractObjArmorLayer;
import com.hbm_m.powerarmor.layer.ModModelLayers;
import com.hbm_m.powerarmor.layer.PowerArmorEmptyModel;
import com.hbm_m.powerarmor.overlay.HbmThermalHandler;
import com.hbm_m.powerarmor.overlay.OverlayPowerArmor;
import com.hbm_m.powerarmor.overlay.PowerArmorHardLandingCameraShakeClient;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.hbm_m.platform.LoaderHooks;
import com.hbm_m.platform.RenderHooks;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm_m.client.render.missile.MissileItemModelDefinitions;
//?} elif neoforge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm_m.client.render.missile.MissileItemModelDefinitions;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?} elif neoforge {
/*@EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
*///?}
@SuppressWarnings({"UnstableApiUsage", "removal"})
public class ClientSetup {

    private static boolean initialized = false;

    /**
     * Единая точка ранней клиентской регистрации: частицы, цветовые тинты, рендереры сущностей.
     * Вызывается из MainRegistry.init() (фаза конструктора мода, Env.CLIENT), потому что
     * на NeoForge 1.21.1 мод-шиночные Register-события стреляют РАНЬШЕ FMLClientSetupEvent —
     * регистрация из client setup опоздала бы. Architectury-реестры буферизуют записи
     * и применяют их на соответствующем событии лоадера на обеих платформах.
     */
    public static void registerCommonClientContent() {
        // Датаген запускается в client-dist БЕЗ окна (Env/Dist не различает!): Architectury
        // ColorHandlerRegistry сразу тянет Minecraft.getItemColors() → NPE, на dedicated
        // server клиентские классы вообще отсутствуют → NoClassDefFoundError. На реальном
        // клиенте оба вызова валидны — поэтому честный try/catch, а не проверка Env.
        try {
            ClientParticleHandler.init();
            registerColors();
            registerEntityRenderersCommon();
        } catch (NullPointerException | NoClassDefFoundError e) {
            MainRegistry.LOGGER.info("Skipping early client content: no client environment (datagen/dedicated server).");
        }
        MainRegistry.LOGGER.info("Registered early client content (particles, colors, entity renderers).");
    }

    /**
     * Loader-agnostic клиентская инициализация.
     */
    public static synchronized void initClient() {
        if (initialized) return;
        initialized = true;

        ModPacketHandler.registerClientReceivers();

        // Key mappings регистрируются в ModConfigKeybindHandler.init() через Architectury/обвязку.
        ModConfigKeybindHandler.init();
        // DH: официальный API-мост (рендер дальних ракет/гриба внутри DH FBO).
        // ВАЖНО: сам класс DhRenderBridge наследует DH-класс — его НЕЛЬЗЯ грузить
        // без DH (упадёт ClassNotFound ещё до проверки внутри tryRegister),
        // поэтому guard строго ДО первого упоминания класса.
        if (com.hbm_m.compat.dh.DhCompat.isModPresent()
                && com.hbm_m.config.ModClothConfig.get().enableDhRenderBridge) {
            com.hbm_m.client.compat.dh.DhRenderBridge.tryRegister();
        }
        ClientModEvents.init();
        // NT particle engine: Architectury quit/tick вместо loader-обработчиков.
        com.hbm_m.client.particle.EngineHandler.init();
        // Тултип взрывостойкости модульных блоков (на 1.21.1 он был мёртв).
        com.hbm_m.util.explosions.nuclear.BlockExplosionDefense.initClientTooltip();
        com.hbm_m.client.missile.track.MissileTrackClientEvents.register();
        CameraShakeHandler.initClient();
        PowerArmorHardLandingCameraShakeClient.initClient();
        PowerArmorSounds.register();
        PowerArmorStepSoundHandler.initClient();

        // Клиентские тикеры станков фабрики Nucleus (общий механизм без рефлексии:
        // common-BE дергает IClientTicker, рендерер кастует к конкретному классу).
        // Новый станок с клиентским тикером = одна строка здесь.
        com.hbm_m.util.ClientTickerRegistry.register(
                com.hbm_m.blockentity.ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(),
                be -> new com.hbm_m.client.machine.AdvancedAssemblerClientTicker(
                        (com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity) be));
        // Мост клиентских звуков: одна установка вместо рефлексии на каждый вызов
        // (все машины: циклы, дверные звуки, one-shot, танк/турбовентилятор).
        com.hbm_m.sound.ClientSoundBootstrap.install(new com.hbm_m.client.sound.ClientSoundBridge());

        // MOTD при входе в мир — решение принимает клиент (client.json -> enableMOTD).
        com.hbm_m.client.ClientMotdHandler.register();

        // Debug-дамп клиентских параметров при входе в мир (серверные приходят отдельно,
        // с первым ConfigSyncS2CPacket — см. ConfigDebugLogger).
        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(
                player -> com.hbm_m.config.ConfigDebugLogger.dumpSide(com.hbm_m.config.schema.ConfigSide.CLIENT));

        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            com.hbm_m.config.ModClothConfig.reloadServer();
            com.hbm_m.config.ConfigDebugLogger.resetClientSession();
            ClientRadiationData.clearAll();
            com.hbm_m.client.stress.NucleusStressManager.reset();
        });

        // Экраны меню: на Forge регистрируются напрямую, на NeoForge 1.21.1+ — через RegisterMenuScreensEvent ниже.
        //? if forge {
        registerScreens();
        //?}

        // Рендереры (entity + block entity).
        registerRenderersCommon();

        // Клиентские тэги/настройки рендера.
        OcclusionCullingHelper.setTransparentBlocksTag(ModTags.Blocks.NON_OCCLUDING);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MainRegistry.LOGGER.info("FMLClientSetupEvent fired. Initializing client.");
        initClient();

        // Регистрация тик/рендер-хендлеров на game event bus (Forge/NeoForge).
        //? if forge {
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientDisconnect);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::registerDebugClientCommands);
        //?} elif neoforge {
        /*NeoForge.EVENT_BUS.addListener(ClientSetup::onClientDisconnect);
        NeoForge.EVENT_BUS.addListener(ClientSetup::registerDebugClientCommands);
        *///?}

        event.enqueueWork(ClientSetup::registerRadAbsorberItemProperties);
        // SEDNA: Original GunFactoryClient.init() + Client-Empfaenger der Orchestras
        event.enqueueWork(() -> {
            com.hbm_m.item.weapon.sedna.factory.GunFactory.init();
            com.hbm_m.client.weapon.OrchestrasClient.init();
            com.hbm_m.client.weapon.GunFactoryClient.init();
        });
        event.enqueueWork(ClientSetup::registerAlexandriteItemProperties);
        event.enqueueWork(ClientSetup::registerR4ItemProperties);
        event.enqueueWork(ClientSetup::registerPolaroidItemProperties);
        event.enqueueWork(() -> {
            for (var b : new net.minecraft.world.level.block.Block[] { ModBlocks.BRICK_FORGOTTEN.get(), ModBlocks.BRICK_FORGOTTEN_LOCK.get() })
                net.minecraft.client.renderer.item.ItemProperties.register(b.asItem(), ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "meta"),
                        (stack, level, entity, seed) -> { try { return Integer.parseInt(com.hbm_m.block.generic.JungleBricks.stateOf(stack, "meta")); } catch (NumberFormatException e) { return 0; } });
        });
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "glyph"),
                (stack, level, entity, seed) -> { try { return Integer.parseInt(com.hbm_m.block.generic.JungleBricks.stateOf(stack, "glyph")); } catch (NumberFormatException e) { return 0; } }));
        event.enqueueWork(ClientSetup::registerBlueprintItemProperties);
        event.enqueueWork(ClientSetup::registerPipetteItemProperties);
        event.enqueueWork(ClientSetup::registerRbmkPelletItemProperties);
        event.enqueueWork(ClientSetup::registerRenderLayers);
    }

    /**
     * Exposes an RBMK pellet's depletion/xenon state (the original's item damage 0-9) to its model,
     * reproducing ItemRBMKPellet's enrichment/xenon overlay render passes as model layers.
     */
    private static void registerRbmkPelletItemProperties() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "pellet_state");
        for (com.hbm_m.item.rbmk.RBMKPelletItem pellet : com.hbm_m.item.rbmk.RBMKPelletItem.pellets) {
            net.minecraft.client.renderer.item.ItemProperties.register(pellet, id,
                    (stack, level, entity, seed) -> com.hbm_m.item.rbmk.RBMKPelletItem.getState(stack));
        }
    }

    //? if forge {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRegisterKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        ModConfigKeybindHandler.registerAll(event::register);
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        ModConfigKeybindHandler.registerAll(event::register);
    }
    *///?}

    //? if neoforge {
    /*@SubscribeEvent
    public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        MainRegistry.LOGGER.info("RegisterMenuScreensEvent fired. Registering HBM screens.");
        registerScreens();
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        // Только powered-комплекты (наследники ModPowerArmorItem — на Forge их расширение
        // побеждает через override initializeClient). Обычная FSB-броня (DNT, Bismuth)
        // регистрируется сама через ModArmorFSB.initializeClient — двойная регистрация крашит.
        var ext = com.hbm_m.powerarmor.ModPowerArmorItem.createNeoForgeClientExtensions();
        event.registerItem(ext,
                ModItems.T51_HELMET.get(), ModItems.T51_CHESTPLATE.get(), ModItems.T51_LEGGINGS.get(), ModItems.T51_BOOTS.get(),
                ModItems.AJR_HELMET.get(), ModItems.AJR_CHESTPLATE.get(), ModItems.AJR_LEGGINGS.get(), ModItems.AJR_BOOTS.get(),
                ModItems.AJRO_HELMET.get(), ModItems.AJRO_CHESTPLATE.get(), ModItems.AJRO_LEGGINGS.get(), ModItems.AJRO_BOOTS.get());
    }
    *///?}

    private static void registerDebugClientCommands(RegisterClientCommandsEvent event) {
        // Стресс-тест рендера Nucleus (/nucleus stress): spawn/clear/anims.
        com.hbm_m.client.stress.NucleusStressCommand.register(event.getDispatcher());
        event.getDispatcher().register(
            net.minecraft.commands.Commands.literal("debug_ntm_m_transition_seal")
                .executes(context -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                            Component.literal(MachineDoorRenderer.getSealDebugInfo()), false);
                    }
                    return 1;
                })
        );
    }

    // RegisterClientCommandsEvent is a GAME-bus event on Forge 1.20.1; the
    // listener is added to MinecraftForge.EVENT_BUS in onClientSetup (the
    // NeoForge branch does the same on NeoForge.EVENT_BUS).

    @SuppressWarnings("removal")
    private static void registerRenderLayers() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.TRANSITION_SEAL.get(), RenderType.cutout());
    }

    private static void registerRadAbsorberItemProperties() {
        net.minecraft.client.renderer.item.ItemProperties.register(
                ModBlocks.RAD_ABSORBER.get().asItem(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "tier"),
                (stack, level, entity, seed) -> BlockAbsorberItem.readTier(stack).ordinal()
        );
    }

    private static void registerR4ItemProperties() {
        net.minecraft.client.renderer.item.ItemProperties.register(ModItems.COLTAN_TOOL.get(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "angle"),
                (stack, level, entity, seed) -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.level == null || mc.player == null)
                        return com.hbm_m.item.tool.ItemColtanCompass.needleFrame(null, 0, 0, 0, 32) / 32F;
                    return com.hbm_m.item.tool.ItemColtanCompass.needleFrame(mc.level, mc.player.getX(), mc.player.getZ(), mc.player.getYRot(), 32) / 32F;
                });
        net.minecraft.client.renderer.item.ItemProperties.register(ModItems.TOOLBOX.get(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "open"),
                (stack, level, entity, seed) -> com.hbm_m.platform.StackNbt.has(stack) && com.hbm_m.platform.StackNbt.read(stack).getBoolean("isOpen") ? 1F : 0F);
        // Original ItemAmmoArty#getIconIndex: Frachtgranate mit Ladung
        net.minecraft.client.renderer.item.ItemProperties.register(ModItems.AMMO_ARTY_CARGO.get(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "cargo"),
                (stack, level, entity, seed) -> com.hbm_m.item.weapon.ItemAmmoArty.hasCargo(stack) ? 1F : 0F);
        // Original RenderStirling: Item-Schaden 1 = Motor ohne Zahnrad
        for (net.minecraft.world.level.ItemLike s : new net.minecraft.world.level.ItemLike[] { ModBlocks.STIRLING.get(), ModBlocks.STIRLING_STEEL.get() })
            net.minecraft.client.renderer.item.ItemProperties.register(s.asItem(),
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "no_cog"),
                    (stack, level, entity, seed) -> com.hbm_m.block.machines.MachineStirlingBlock.isNoCog(stack) ? 1F : 0F);
    }

    private static void registerAlexandriteItemProperties() {
        net.minecraft.client.renderer.item.ItemProperties.register(
                ModItems.GEM_ALEXANDRITE.get(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "light"),
                (stack, level, entity, seed) -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.level == null || mc.player == null) return 0F;
                    return mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, mc.player.blockPosition()) / 15F;
                }
        );
    }

    /** Original setTextureName("polaroid_" / "glitch_" + MainRegistry.polaroidID). */
    private static void registerPipetteItemProperties() {
        for (var item : new net.minecraft.world.item.Item[] { ModItems.PIPETTE.get(), ModItems.PIPETTE_BORON.get(), ModItems.PIPETTE_LABORATORY.get() }) {
            net.minecraft.client.renderer.item.ItemProperties.register(item,
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "filled"),
                    (stack, level, entity, seed) -> com.hbm_m.platform.StackNbt.has(stack) && com.hbm_m.platform.StackNbt.read(stack).getShort("fill") > 0 ? 1F : 0F);
        }
    }

    private static void registerBlueprintItemProperties() {
        net.minecraft.client.renderer.item.ItemProperties.register(ModItems.BLUEPRINTS.get(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blueprint"),
                (stack, level, entity, seed) -> com.hbm_m.item.industrial.ItemBlueprints.iconIndex(stack));
    }

    private static void registerPolaroidItemProperties() {
        for (var item : new net.minecraft.world.item.Item[] { ModItems.POLAROID.get(), ModItems.GLITCH.get() }) {
            net.minecraft.client.renderer.item.ItemProperties.register(item,
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "polaroid"),
                    (stack, level, entity, seed) -> com.hbm_m.main.Polaroid.id() / 18F);
        }
    }

    private static void registerScreens() {
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRYSTALLIZER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCrystallizer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BREEDER_MENU.get(), GUIMachineBreeder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LARGE_PYLON_MENU.get(), GUIMachineLargePylon::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CYCLOTRON_MENU.get(), GUIMachineCyclotron::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ZIRNOX_MENU.get(), GUIMachineZirnox::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.WATZ_POWERPLANT_MENU.get(), com.hbm_m.inventory.gui.GUIMachineWatzPowerplant::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PWR_CONTROLLER_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePWRController::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ARC_WELDER_MENU.get(), GUIMachineArcWelder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOLDERING_STATION_MENU.get(), GUIMachineSolderingStation::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MIXER_MENU.get(), GUIMachineMixer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DERRICK_MENU.get(), GUIMachineDerrick::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.COKER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoker::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PYROOVEN_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePyroOven::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOLIDIFIER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSolidifier::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ASHPIT_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAshpit::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REACTOR_RESEARCH_MENU.get(), com.hbm_m.inventory.gui.GUIMachineReactorResearch::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADGEN_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRadGen::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRANE_INSERTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneInserter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRANE_EXTRACTOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneExtractor::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRANE_GRABBER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneGrabber::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRANE_ROUTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneRouter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRANE_BOXER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneBoxer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRANE_UNBOXER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneUnboxer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DRONE_CRATE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DRONE_PROVIDER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneProvider::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DRONE_REQUESTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneRequester::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DRONE_DOCK_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneDock::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADIO_TORCH_COUNTER_MENU.get(), com.hbm_m.inventory.gui.radio.GUIRadioTorchCounter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_STORAGE_DRUM_MENU.get(), com.hbm_m.inventory.gui.GUIMachineStorageDrum::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_SIREN_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSiren::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFirebox::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_KEYFORGE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineKeyforge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_MASS_STORAGE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineMassStorage::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_CONSOLE_MENU.get(), GUIMachineRbmkConsole::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_ROD_MENU.get(), GUIRBMKRod::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_CONTROL_MENU.get(), GUIRBMKControl::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_CONTROL_AUTO_MENU.get(), com.hbm_m.inventory.gui.GUIRBMKControlAuto::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_BOILER_MENU.get(), GUIRBMKBoiler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_HEATER_MENU.get(), GUIRBMKHeater::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_STORAGE_MENU.get(), GUIRBMKStorage::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_AUTOLOADER_MENU.get(), GUIRBMKAutoloader::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_OUTGASSER_MENU.get(), GUIRBMKOutgasser::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FLARE_STACK_MENU.get(), GUIMachineFlareStack::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CORE_EMITTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoreEmitter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CORE_RECEIVER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoreReceiver::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.OILBURNER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineOilburner::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.HEATEX_MENU.get(), com.hbm_m.inventory.gui.GUIMachineHeatex::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PUMPJACK_MENU.get(), GUIMachinePumpjack::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADAR_MENU.get(), GUIMachineRadarNT::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADAR_SLOTS_MENU.get(), GUIMachineRadarNTSlots::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRACKING_TOWER_MENU.get(), GUIMachineCrackingTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FRACTION_TOWER_MENU.get(), GUIMachineFractionTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MINING_DRILL_MENU.get(), GUIMachineMiningDrill::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FEL_MENU.get(), GUIMachineFel::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SILEX_MENU.get(), GUIMachineSilex::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_GASCENT_MENU.get(), GUIMachineGasCent::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.INDUSTRIAL_BOILER_MENU.get(), GUIMachineIndustrialBoiler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURBINE_MENU.get(), GUIMachineTurbine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LARGE_TURBINE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineLargeTurbine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURBINEGAS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineTurbineGas::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.STEAM_TURBINE_MENU.get(), GUIMachineSteamTurbine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SUBSTATION_MENU.get(), GUIMachineSubstation::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRUCIBLE_MENU.get(), GUIMachineCrucible::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ARMOR_TABLE_MENU.get(), GUIArmorTable::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), GUIMachineAssembler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), GUIMachineAdvancedAssembler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_DIFURNACE_RTG_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDifurnaceRtg::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_BATTERY_MENU.get(), GUIMachineBattery::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BATTERY_SOCKET_MENU.get(), GUIBatterySocket::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BLAST_FURNACE_MENU.get(), GUIBlastFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_BLAST_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineBlastFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.HEATING_OVEN_MENU.get(), GUIHeatingOven::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PRESS_MENU.get(), GUIMachinePress::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SHREDDER_MENU.get(), GUIMachineShredder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ORE_SLOPPER_MENU.get(), GUIMachineOreSlopper::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.COMBINATION_OVEN_MENU.get(), GUIMachineCombinationOven::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ARC_FURNACE_MENU.get(), GUIMachineArcFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ANNIHILATOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAnnihilator::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MINING_LASER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineMiningLaser::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.AMMO_PRESS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAmmoPress::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.EPRESS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineEPress::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.AUTOCRAFTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAutocrafter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.INDUSTRIAL_GENERATOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineIndustrialGenerator::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DIESEL_GENERATOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDieselGenerator::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.COMBUSTION_ENGINE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCombustionEngine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURBOFAN_MENU.get(), com.hbm_m.inventory.gui.GUIMachineTurbofan::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUNNEL_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFunnel::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PUREX_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePUREX::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.WOOD_BURNER_MENU.get(), GUIMachineWoodBurner::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURRET_MENU.get(), com.hbm_m.inventory.gui.GUITurret::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MISSILE_ASSEMBLY_MENU.get(), com.hbm_m.inventory.gui.GUIMissileAssembly::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ANVIL_MENU.get(), GUIAnvil::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_CENTRIFUGE_MENU.get(), GUIMachineCentrifuge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.IRON_CRATE_MENU.get(), GUIIronCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.STEEL_CRATE_MENU.get(), GUISteelCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DESH_CRATE_MENU.get(), GUIDeshCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TUNGSTEN_CRATE_MENU.get(), GUITungstenCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TEMPLATE_CRATE_MENU.get(), GUITemplateCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FLUID_TANK_MENU.get(), GUIMachineFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BAT9000_MENU.get(), com.hbm_m.inventory.gui.GUIBat9000::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BOOK_MENU.get(), com.hbm_m.inventory.gui.GUIBook::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ORBUS_MENU.get(), com.hbm_m.inventory.gui.GUIFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_WASTE_DRUM_MENU.get(), com.hbm_m.inventory.gui.GUIMachineWasteDrum::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_IRON_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelIron::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_STEEL_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelSteel::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_TCALLOY_MENU.get(), com.hbm_m.inventory.gui.GUIFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_CORRODED_MENU.get(), com.hbm_m.inventory.gui.GUIFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_PLASTIC_MENU.get(), com.hbm_m.inventory.gui.GUIFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_ANTIMATTER_MENU.get(), com.hbm_m.inventory.gui.GUIFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CHEMICAL_PLANT_MENU.get(), GUIMachineChemicalPlant::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CHEMICAL_FACTORY_MENU.get(), GUIMachineChemicalFactory::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOYUZ_LAUNCHER_MENU.get(), com.hbm_m.inventory.gui.GUISoyuzLauncher::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_SATLINKER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSatLinker::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FRACTURING_TOWER_MENU.get(), GUIMachineFrackingTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REFINERY_MENU.get(), GUIMachineRefinery::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LAUNCH_PAD_LARGE_MENU.get(), GUILaunchPadLarge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LAUNCH_PAD_RUSTED_MENU.get(), GUILaunchPadRusted::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_FAT_MAN_MENU.get(), com.hbm_m.inventory.gui.GUINukeFatMan::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_PROTOTYPE_MENU.get(), com.hbm_m.inventory.gui.GUINukePrototype::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LARGE_NUKE_MENU.get(), com.hbm_m.inventory.gui.GUINukeLarge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_FLEIJA_MENU.get(), com.hbm_m.inventory.gui.GUINukeFleija::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_SOLINIUM_MENU.get(), com.hbm_m.inventory.gui.GUINukeSolinium::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_N2_MENU.get(), com.hbm_m.inventory.gui.GUINukeN2::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_FSTBMB_MENU.get(), com.hbm_m.inventory.gui.GUINukeFstbmb::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_CUSTOM_MENU.get(), com.hbm_m.inventory.gui.GUINukeCustom::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BOMB_MULTI_MENU.get(), com.hbm_m.inventory.gui.GUIBombMulti::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.VACUUM_DISTILL_MENU.get(), com.hbm_m.inventory.gui.GUIMachineVacuumDistill::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.HYDROTREATER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineHydrotreater::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CORE_INJECTOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoreInjector::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CATALYTIC_REFORMER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCatalyticReformer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LIQUEFACTOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineLiquefactor::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FURNACE_IRON_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFurnaceIron::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FURNACE_STEEL_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFurnaceSteel::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ROTARY_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRotaryFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.STRAND_CASTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineStrandCaster::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MICROWAVE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineMicrowave::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.EXPOSURE_CHAMBER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineExposureChamber::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADIOLYSIS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRadiolysis::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ELECTROLYSER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineElectrolyser::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.COMPRESSOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCompressor::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineElectricFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FURNACE_BRICK_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFurnaceBrick::new);

        // Restore of FFA merge loss (merge ab86420b9 dropped these): FFA registered 185 screen
        // factories, the merge kept only the machines list. Menus exist and are opened by
        // gameplay code, but without a factory the client silently fails to open the GUI.
        MenuRegistry.registerScreenFactory(ModMenuTypes.HELD_ITEM_MENU.get(), com.hbm_m.inventory.gui.GUIHeldItem::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SAFE_MENU.get(), com.hbm_m.inventory.gui.GUISafe::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FILE_CABINET_MENU.get(), com.hbm_m.inventory.gui.GUIFileCabinet::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CART_CRATE_MENU.get(), com.hbm_m.inventory.gui.GUICartScreens.Crate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CART_DESTROYER_MENU.get(), com.hbm_m.inventory.gui.GUICartScreens.Destroyer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.WEAPON_TABLE_MENU.get(), com.hbm_m.inventory.gui.GUIWeaponTable::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PRECASS_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePrecAss::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ROCKMILL_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRockMill::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SUPERCOMPUTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSuperComputer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ASSEMBLY_FACTORY_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAssemblyFactory::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ELECTROLYSER_FLUID_MENU.get(), com.hbm_m.inventory.gui.GUIElectrolyserFluid::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ELECTROLYSER_METAL_MENU.get(), com.hbm_m.inventory.gui.GUIElectrolyserMetal::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REACTOR_CONTROL_MENU.get(), com.hbm_m.inventory.gui.GUIReactorControl::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FORCE_FIELD_MENU.get(), com.hbm_m.inventory.gui.GUIForceField::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BATTERY_REDD_MENU.get(), com.hbm_m.inventory.gui.GUIBatteryREDD::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RTG_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRTG::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_RTG_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIRtgFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_BREEDER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionBreeder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_KLYSTRON_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionKlystron::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_PLASMA_FORGE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionPlasmaForge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_TORUS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionTorus::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DFC_CORE_MENU.get(), com.hbm_m.inventory.gui.GUIDFCCore::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DFC_STABILIZER_MENU.get(), com.hbm_m.inventory.gui.GUIDFCStabilizer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_ICF_MENU.get(), com.hbm_m.inventory.gui.GUIMachineICF::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_ICF_PRESS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineICFPress::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_SOURCE_MENU.get(), com.hbm_m.inventory.gui.GUIPASource::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_DETECTOR_MENU.get(), com.hbm_m.inventory.gui.GUIPADetector::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_DIPOLE_MENU.get(), com.hbm_m.inventory.gui.GUIPADipole::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_QUADRUPOLE_MENU.get(), com.hbm_m.inventory.gui.GUIPAQuadrupole::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_RFC_MENU.get(), com.hbm_m.inventory.gui.GUIPARFC::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_TUBE_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoTube::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_ACCESS_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageAccess::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_CLUTTER_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageClutter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_IMPORTER_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageImporter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_EXPORTER_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageExporter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_MONO_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageMono::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CUSTOM_LAUNCHER_MENU.get(), com.hbm_m.inventory.gui.GUICustomLauncher::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CUSTOM_MACHINE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCustom::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LAUNCHPAD_LAMBDA_MENU.get(), com.hbm_m.inventory.gui.GUILaunchpadLambda::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SAT_DOCK_MENU.get(), com.hbm_m.inventory.gui.GUISatDock::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOYUZ_CAPSULE_MENU.get(), com.hbm_m.inventory.gui.GUISoyuzCapsule::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LEMEGETON_MENU.get(), com.hbm_m.inventory.gui.GUILemegeton::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TAPE_DRIVE_MENU.get(), com.hbm_m.inventory.gui.GUITapeDrive::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REBAR_MENU.get(), com.hbm_m.inventory.gui.GUIRebar::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TRAIN_CARGO_TRAM_MENU.get(), com.hbm_m.inventory.gui.GUITrainCargoTram::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TRAIN_CARGO_TRAM_TRAILER_MENU.get(), com.hbm_m.inventory.gui.GUITrainCargoTramTrailer::new);
    }

    private static void registerRenderersCommon() {

        ModEntities.SOYUZ.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.SoyuzEntityRenderer::new));
        ModEntities.SOYUZ_CAPSULE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.SoyuzCapsuleEntityRenderer::new));
        ModEntities.ZIRNOX_DEBRIS.ifPresent(entityType -> EntityRenderers.register(entityType, ZirnoxDebrisRenderer::new));
        ModEntities.RBMK_DEBRIS.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.rbmk.RBMKDebrisRenderer::new));
        ModEntities.MOVING_CONVEYOR_ITEM.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.MOVING_CONVEYOR_PACKAGE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.DELIVERY_DRONE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.DeliveryDroneRenderer::new));
        ModEntities.REQUEST_DRONE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.DeliveryDroneRenderer::new));
        ModEntities.TURRET_BULLET.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.TURRET_ROCKET.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_NUC_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_FIRE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_SLIME_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_HE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADEHE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADEFIRE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADESMART_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADESLIME_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.MISSILE_TEST.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_ANTI_BALLISTIC.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_MICRO.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_SCHRABIDIUM.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BHOLE.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_TAINT.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_EMP.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_GENERIC.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_INCENDIARY.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_CLUSTER.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BUSTER.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DECOY.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_STEALTH.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_STRONG.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_INCENDIARY_STRONG.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_CLUSTER_STRONG.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BUSTER_STRONG.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_EMP_STRONG.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BURST.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_INFERNO.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_RAIN.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DRILL.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_SHUTTLE.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_NUCLEAR.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_NUCLEAR_CLUSTER.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_VOLCANO.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DOOMSDAY.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DOOMSDAY_RUSTED.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.CLUSTER_ROCKET.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.projectile.ClusterRocketEntityRenderer::new));
        ModEntities.SHRAPNEL.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.ShrapnelRenderer::new));
        ModEntities.EMP_PULSE.ifPresent(entityType -> EntityRenderers.register(entityType, EmptyEntityRenderer::new));
        ModEntities.BLACK_HOLE.ifPresent(entityType -> EntityRenderers.register(entityType, RenderBlackHole::new));
        ModEntities.VORTEX.ifPresent(entityType -> EntityRenderers.register(entityType, RenderBlackHole::new));
        ModEntities.RAGING_VORTEX.ifPresent(entityType -> EntityRenderers.register(entityType, RenderBlackHole::new));
        ModEntities.DIGAMMA_QUASAR.ifPresent(entityType -> EntityRenderers.register(entityType, RenderQuasar::new));
        ModEntities.DIGAMMA_SPEAR.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.SpearRenderer::new));
        ModEntities.RUBBLE.ifPresent(entityType -> EntityRenderers.register(entityType, RubbleEntityRenderer::new));

        // Сеть длинной ЛЭП: один рендерер кабелей для всех пилонов/коннекторов.
        {
            net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<com.hbm_m.blockentity.network.PylonBaseBlockEntity> wireRenderer =
                    com.hbm_m.client.render.implementations.RedPylonWireRenderer::new;
            BlockEntityRenderers.register(ModBlockEntities.RED_CONNECTOR_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_CONNECTOR_SUPER_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_PYLON_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_PYLON_MEDIUM_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_PYLON_LARGE_BE.get(), wireRenderer);
        BlockEntityRenderers.register(ModBlockEntities.SUBSTATION_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_CABLE_PAINTABLE_BE.get(), com.hbm_m.client.render.implementations.RedCablePaintableRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FLUID_DUCT_PAINTABLE.get(), com.hbm_m.client.render.implementations.PaintableDuctRenderer::new);
        }

        // Petrochem & Energy
        MachinePumpjackRenderer.register();
        MachinePetrochemRenderer.register();
        MachineTankRenderer.register();
        MachineSolidifierRenderer.register();
        MachineEngineRenderer.register();
        MachineTurbineRenderer.register();
        MachineBoilerRenderer.register();
        MachinePowerRenderer.register();

        // Metallurgy & Chemistry
        MachineWorkbenchRenderer.register();
        MachineFurnaceRenderer.register();
        MachinePressGroupRenderer.register();
        MachineFactoryRenderer.register();
        MachineSawmillRenderer.register();
        MachineMetallurgyMiscRenderer.register();
        MachineChemistryRenderer.register();
        MachineSeparationRenderer.register();
        MachineCentrifugeRenderer.register();

        // Nuclear & Research
        MachineReactorResearchRenderer.register();
        MachineAcceleratorRenderer.register();
        MachineDFCRenderers.register();
        MachinePileRenderers.register();
        MachineRbmkControlRenderer.register();
        MachineResearchFacilityRenderer.register();
        MachineFusionComplexRenderer.register();
        MachineLaserResearchRenderer.register();
        MachineAerospaceRenderer.register();

        MachineAdvancedAssemblerRenderer.register();
        MachineAssemblerRenderer.register();
        MachineDoorRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.PEDESTAL_BE.get(), com.hbm_m.client.render.implementations.PedestalRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DECO_LOOT_BE.get(), com.hbm_m.client.render.implementations.DecoLootRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DEMON_LAMP_BE.get(), com.hbm_m.client.render.implementations.DemonLampRenderer::new);
        MachinePressRenderer.register();
        MachineTurbofanRenderer.register();
        MachineChemicalPlantRenderer.register();
        MachineChemicalFactoryRenderer.register();
        MachineZirnoxRenderer.register();
        MachineZirnoxDestroyedRenderer.register();
        MachineHydraulicFrackiningTowerRenderer.register();
        SoyuzLauncherRenderer.register();
        SoyuzRocketRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.SOYUZ_CAPSULE_BE.get(), com.hbm_m.client.render.implementations.SoyuzCapsuleRenderer::new);
        HeaterRenderers.register();
        MachineCrystallizerRenderer.register();
        MachineFluidTankRenderer.register();
        BarrelTankRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_PAD_BE.get(), LaunchPadMissileRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), LaunchPadMissileRenderer::new);
        MachineCoolingTowerRenderer.register();
        MachineMiningDrillRenderer.register();
        MachineOreSlopperRenderer.register();
        MachineArcFurnaceRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.TURRET_SENTRY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_CHEKHOV_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_FRIENDLY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_JEREMY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_TAUON_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_RICHARD_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_HOWARD_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_MAXWELL_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_FRITZ_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_ARTY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_HIMARS_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        MachineRadarRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.RADAR_SCREEN_BE.get(), com.hbm_m.client.render.implementations.MachineRadarScreenRenderer::new);
        CrucibleRenderer.register();
        com.hbm_m.client.render.implementations.StrandCasterRenderer.register();
        MachineFoundryRenderer.register(); // basin + mold, Nucleus factory
        MachineFoundryChannelRenderer.register();
        MachineFoundryTankRenderer.register(); // tank melt, Nucleus factory
        // ─── RBMK column renderers ─────
        // Nucleus: 13 specs (one per column BE type), all sharing the MachineRBMKColumnRenderer
        // hook that draws the procedural column geometry (flat sides, lid plate, rod cap, OBJ
        // fuel-channel internals, Cherenkov glow).
        MachineRBMKColumnRenderer.register();
        // The 7 RTTY panel devices each get their own renderer, ported 1:1 from the original's
        // RenderRBMK* tile entity special renderers.
        // Nucleus: the display mini-map is a hook-only spec (the panel body is the static block model).
        MachineRbmkDisplayRenderer.register();
        // Nucleus: the remaining 7 panel devices are hook-only specs in MachineRBMKPanelRenderers
        // (the default factory block transform reproduces the legacy facing rotation).
        MachineRBMKPanelRenderers.register();
        // Steam inlet/outlet are floor blocks (not columns) — rendered via MODEL + JSON

        // Renderer registrations restored after the FFA merge (2026-10-24)
        // BARREL_*_BE: kein Legacy-TankRenderers.Barrel hier - es ueberschriebe den
        // Nucleus-BER aus BarrelTankRenderer.register() (Spec zeichnet Korpus per VBO,
        // der Legacy-BER nur Stutzen/Rauten => Fass unsichtbar).
        BlockEntityRenderers.register(ModBlockEntities.CARGO_ELEVATOR_BE.get(), com.hbm_m.client.render.implementations.CargoElevatorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CHARGE.get(), com.hbm_m.client.render.implementations.ChargeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.COMPACT_LAUNCHER_BE.get(), com.hbm_m.client.render.implementations.CustomLauncherRenderers.Compact::new);
        BlockEntityRenderers.register(ModBlockEntities.CRASHED_BOMB.get(), com.hbm_m.client.render.implementations.CrashedBombRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CUSTOM_MACHINE_BE.get(), com.hbm_m.client.render.implementations.CustomMachineRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DECO_BLOCK.get(), com.hbm_m.client.render.implementations.DecoBlockRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DECO_EMITTER.get(), com.hbm_m.client.render.implementations.EmitterRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FILE_CABINET.get(), com.hbm_m.client.render.implementations.FileCabinetRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FLOODLIGHT.get(), com.hbm_m.client.render.implementations.FloodlightRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FUSION_KLYSTRON_BE.get(), com.hbm_m.client.render.implementations.FusionKlystronRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FUSION_KLYSTRON_CREATIVE_BE.get(), com.hbm_m.client.render.implementations.FusionKlystronCreativeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FUSION_MHDT_BE.get(), com.hbm_m.client.render.implementations.FusionMhdtRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ICF_CONTROLLER_BE.get(), com.hbm_m.client.render.implementations.ICFControllerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ICF_STRUCT_BE.get(), com.hbm_m.client.render.implementations.ICFStructRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LANTERN_BEHEMOTH_BE.get(), com.hbm_m.client.render.implementations.LanternBehemothRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LANTERN.get(), com.hbm_m.client.render.implementations.LanternRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_TABLE_BE.get(), com.hbm_m.client.render.implementations.CustomLauncherRenderers.Table::new);
        BlockEntityRenderers.register(ModBlockEntities.LAUNCHPAD_LAMBDA_BE.get(), com.hbm_m.client.render.implementations.LaunchpadLambdaRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LOGIC_BLOCK.get(), com.hbm_m.client.render.implementations.LogicBlockRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ANNIHILATOR_BE.get(), com.hbm_m.client.render.implementations.AnnihilatorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_CHUNGUS_BE.get(), com.hbm_m.client.render.implementations.ChungusRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CONDENSER_POWERED_BE.get(), com.hbm_m.client.render.implementations.CondenserPoweredRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_FENSU_BE.get(), com.hbm_m.client.render.implementations.FENSURenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FAN_BE.get(), com.hbm_m.client.render.implementations.FanRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HEPHAESTUS_BE.get(), com.hbm_m.client.render.implementations.HephaestusRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.INTAKE_BE.get(), com.hbm_m.client.render.implementations.IntakeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PRECASS_BE.get(), com.hbm_m.client.render.implementations.MachinePrecAssRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_PUMP_ELECTRIC_BE.get(), com.hbm_m.client.render.implementations.PumpRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_PUMP_STEAM_BE.get(), com.hbm_m.client.render.implementations.PumpRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RADGEN_BE.get(), com.hbm_m.client.render.implementations.RadGenRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SATLINK_BE.get(), com.hbm_m.client.render.implementations.SatLinkRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TAPE_DRIVE_BE.get(), com.hbm_m.client.render.implementations.TapeDriveRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MISSILE_ASSEMBLY_BE.get(), com.hbm_m.client.render.implementations.MissileAssemblyRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MOBILE_LAUNCH_PAD_BE.get(), com.hbm_m.client.render.implementations.MobileLaunchPadMissileRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.NUKE_FSTBMB_BE.get(), com.hbm_m.client.render.implementations.NukeFstbmbRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.OBJ_TESTER.get(), com.hbm_m.client.render.implementations.ObjTesterRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PIPE_ANCHOR.get(), com.hbm_m.client.render.implementations.PipeAnchorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PIPE_ANCHOR_PNEUMATIC.get(), ctx -> new com.hbm_m.client.render.implementations.PipeAnchorRenderer<>(ctx, com.hbm_m.client.render.implementations.PipeAnchorRenderer.TEX_PNEUMATIC));
        BlockEntityRenderers.register(ModBlockEntities.PIPE_ANCHOR_EXHAUST.get(), ctx -> new com.hbm_m.client.render.implementations.PipeAnchorRenderer<>(ctx, com.hbm_m.client.render.implementations.PipeAnchorRenderer.TEX_EXHAUST));
        BlockEntityRenderers.register(ModBlockEntities.PISTON_INSERTER_BE.get(), com.hbm_m.client.render.implementations.PistonInserterRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PNEUMO_TUBE_PAINTABLE_BE.get(), com.hbm_m.client.render.implementations.PneumoTubePaintableRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_ABSORBER_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_BLANK_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_BOILER_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_CONTROL_AUTO_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_CONTROL_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_COOLER_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_HEATER_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_MODERATOR_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_OUTGASSER_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_PANEL_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_REFLECTOR_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_ROD_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_STORAGE_BE.get(), com.hbm_m.client.render.implementations.RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RADIOBOX_BE.get(), com.hbm_m.client.render.implementations.RadioboxRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RAIL_SWITCH.get(), com.hbm_m.client.render.implementations.RailSwitchRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.REBAR_BE.get(), com.hbm_m.client.render.implementations.RebarRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SKELETON_HOLDER.get(), com.hbm_m.client.render.implementations.SkeletonHolderRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SLAG_BE.get(), com.hbm_m.client.render.implementations.SlagRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SOLAR_MIRROR_BE.get(), com.hbm_m.client.render.implementations.SolarMirrorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.STATUE_ELB_F.get(), com.hbm_m.client.render.implementations.StatueRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.STRUCT_LAUNCHER_CORE_BE.get(), com.hbm_m.client.render.implementations.CustomLauncherRenderers.Struct::new);
        BlockEntityRenderers.register(ModBlockEntities.STRUCT_WATZ_CORE.get(), com.hbm_m.client.render.implementations.StructWatzCoreRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.STRUCT_TORUS_CORE_BE.get(), com.hbm_m.client.render.implementations.StructTorusCoreRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TOPOL_LAUNCH_PAD_BE.get(), com.hbm_m.client.render.implementations.TopolLauncherRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TRINKET.get(), com.hbm_m.client.render.implementations.TrinketRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.VENDING_MACHINE.get(), com.hbm_m.client.render.implementations.VendingMachineRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.WAND_STRUCTURE.get(), com.hbm_m.client.render.implementations.WandStructureRenderer::new);
        // Follow-up one-liners (same merge loss): renderer classes that already live in the tree
        BlockEntityRenderers.register(ModBlockEntities.TOWER_SMALL_BE.get(), com.hbm_m.client.render.implementations.CoolingTowerRenderer::small);
        BlockEntityRenderers.register(ModBlockEntities.BATTERY_REDD_BE.get(), com.hbm_m.client.render.implementations.BatteryREDDRenderer::new);
    }

    private static void clearClientCachesDeferred() {
        // Единая точка инвалидации: см. RenderCacheManager
        com.hbm_m.client.render.cache.RenderCacheManager.invalidateAll(
                com.hbm_m.client.render.cache.RenderCacheManager.Reason.SESSION_END);
    }

    public static void addTemplatesClient(java.util.function.Consumer<ItemStack> acceptor) {
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(recipeManager, AssemblerRecipe.Type.INSTANCE);

            Set<String> blueprintPools = new HashSet<>();
            for (AssemblerRecipe recipe : recipes) {
                String pool = recipe.getBlueprintPool();
                if (pool != null && !pool.isEmpty()) {
                    blueprintPools.add(pool);
                }
            }
            for (ChemicalPlantRecipe chem : RecipeHooks.getAllRecipes(recipeManager, ChemicalPlantRecipe.Type.INSTANCE)) {
                String pool = chem.getBlueprintPool();
                if (pool != null && !pool.isEmpty()) {
                    blueprintPools.add(pool);
                }
            }

            for (String pool : blueprintPools) {
                ItemStack folderStack = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
                ItemBlueprintFolder.writeBlueprintPool(folderStack, pool);
                acceptor.accept(folderStack);
            }

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added {} blueprint folders to NTM Templates tab", blueprintPools.size());
            }

            for (AssemblerRecipe recipe : recipes) {
                ItemStack templateStack = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
                ItemAssemblyTemplate.writeRecipeOutput(templateStack, recipe.getResultItemSafe());
                acceptor.accept(templateStack);
            }

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added {} templates to NTM Templates tab", recipes.size());
            }
        } else {
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.warn("Could not populate templates tab: Minecraft level is null.");
            }
        }
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        java.util.Map modelRegistry = event.getModels();
        Object leavesLocation = LoaderHooks.createModelLocation(ModBlocks.WASTE_LEAVES.getId(), "");
        BakedModel originalModel = (BakedModel) modelRegistry.get(leavesLocation);
        
        if (originalModel != null) {
            LeavesModelWrapper wrappedModel = new LeavesModelWrapper(originalModel);
            modelRegistry.put(leavesLocation, wrappedModel);
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("Successfully wrapped waste_leaves model for dynamic render types.");
            }
        } else {
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.warn("Could not find model for waste_leaves to wrap.");
            }
        }
    }

    //? if forge {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBakingCompletedDisplayGuards(ModelEvent.BakingCompleted event) {
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.installDisplayTransformGuards(
                event.getModelBakery().getBakedTopLevelModels());
    }
    //?}
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModelBakeUnwrapContinuity(ModelEvent.ModifyBakingResult event) {
        java.util.Map models = event.getModels();
        java.util.Map replacements = new java.util.HashMap<>();

        for (Object entryObj : models.entrySet()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) entryObj;
            ResourceLocation keyLoc = LoaderHooks.getModelId(entry.getKey());
            
            if (!RefStrings.MODID.equals(keyLoc.getNamespace())) continue;
            BakedModel original = (BakedModel) entry.getValue();
            BakedModel unwrapped = com.hbm_m.client.render.AbstractPartBasedRenderer
                    .unwrapFabricForwardingModels(original);
            if (unwrapped != original) {
                replacements.put(entry.getKey(), unwrapped);
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug(
                            "[HBM] Unwrapped Continuity model: {} ({} → {})",
                            keyLoc,
                            original.getClass().getSimpleName(),
                            unwrapped.getClass().getSimpleName());
                }
            }
        }

        if (!replacements.isEmpty()) {
            models.putAll(replacements);
            MainRegistry.LOGGER.info("[HBM] Unwrapped {} Continuity model wrappers from HBM models.",
                    replacements.size());
        }

        wrapConnectedDecoCtTerrainModels(models);
        wrapPileCtTerrainModels(models);
        wrapIcfCtTerrainModels(models);
        wrapPwrCtTerrainModels(models);
        wrapHadronCoilCtTerrainModels(models);
        wrapBoxDuctModels(models);
        // R6e: RenderReeds - Schilf bis zum Gewaessergrund
        {
            Object loc = LoaderHooks.createModelLocation(ModBlocks.PLANT_REEDS.getId(), "");
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked != null && !(baked instanceof com.hbm_m.client.model.ReedsBakedModel)) models.put(loc, new com.hbm_m.client.model.ReedsBakedModel(baked));
        }
        // checkTilt: gekippte Maschinen (Absenkung wie in den Originalrenderern)
        // BIGASSTANK без wrapTilted: его multipart-модель обязана остаться
        // ConfiguredMultipartBakedModel для биндинга спеки; наклон применяет хук
        // в MachineTankRenderer.registerBigAssTank().
        wrapTilted(models, ModBlocks.MACHINE_BLAST_FURNACE.get(), 0.25D);
        wrapTilted(models, ModBlocks.FLARE_STACK.get(), 0.25D);
        wrapTilted(models, ModBlocks.REFINERY.get(), 0.25D);
        wrapTilted(models, ModBlocks.ZIRNOX.get(), 0.5D);

        //? if forge {
        @SuppressWarnings("unchecked")
        Map<ResourceLocation, BakedModel> typedModels = (Map<ResourceLocation, BakedModel>) models;
        // 1:1 ItemRendererMeteorSword: eingefaerbter Glanz der Meteoritenschwerter
        com.hbm_m.client.model.MeteorSwordGlintModel.wrapAll(typedModels);
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.installDisplayTransformGuards(typedModels);
        //?} elif neoforge {
        /*@SuppressWarnings("unchecked")
        Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> typedModels =
                (Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel>) models;
        // 1:1 ItemRendererMeteorSword: eingefaerbter Glanz der Meteoritenschwerter
        com.hbm_m.client.model.MeteorSwordGlintModel.wrapAll(typedModels);
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.installDisplayTransformGuards(typedModels);
        *///?}
    }

    private static ResourceLocation ctTex(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/ported/" + name);
    }

    /**
     * 1:1-Port von {@code BlockPile.getFragments}: Deckel und Boden tragen ein anderes Texturpaar
     * als die Seiten, und Ein-, Auslass, Steuerung und Kern noch einmal ein eigenes. Verbunden wird
     * mit jedem anderen Meilerblock, gleich welcher Sorte.
     */
    private static void wrapPileCtTerrainModels(java.util.Map models) {
        var block = ModBlocks.PILE_BLOCK.get();
        java.util.function.BiPredicate<net.minecraft.world.level.block.Block,
                net.minecraft.world.level.block.Block> connects = (a, b) -> b == block;

        ResourceLocation sideFull = ctTex("pile_block");
        ResourceLocation topFull = ctTex("pile_block_top");

        for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
            var type = state.getValue(com.hbm_m.block.machines.pile.PileBlock.TYPE);

            // Oben und unten: die Steuerung hat einen eigenen Deckel, alle anderen den gewoehnlichen.
            ResourceLocation topCt = ctTex(type == com.hbm_m.block.machines.pile.PileBlockType.CONTROL
                    ? "pile_block_control_top_ct" : "pile_block_top_ct");

            // An den Seiten: Einlass, Auslass, Kern - sonst die glatte Ziegelwand.
            ResourceLocation[] side = switch (type) {
                case FUEL_IN, AIR_IN -> new ResourceLocation[] { sideFull, ctTex("pile_block_input_ct") };
                case FUEL_OUT, AIR_OUT -> new ResourceLocation[] { sideFull, ctTex("pile_block_output_ct") };
                // Original: der Kern nimmt hier ausdruecklich die Deckeltextur als Grundlage.
                case CORE -> new ResourceLocation[] { topFull, ctTex("pile_block_core_ct") };
                default -> new ResourceLocation[] { sideFull, ctTex("pile_block_ct") };
            };
            ResourceLocation[] top = { topFull, topCt };

            java.util.function.Function<net.minecraft.core.Direction, ResourceLocation[]> perSide =
                    dir -> dir.getAxis().isVertical() ? top : side;

            Object loc = LoaderHooks.createModelLocation(ModBlocks.PILE_BLOCK.getId(),
                    "type=" + type.getSerializedName());
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) continue;

            models.put(loc, new ConnectedDecoBlockBakedModel(baked, side[0], side[1], perSide, connects));
        }
    }

    /** 1:1 {@code BlockHadronCoil.canConnect}: jede Spule verbindet sich mit jeder anderen Spule. */
    private static void wrapHadronCoilCtTerrainModels(java.util.Map models) {
        java.util.function.BiPredicate<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> connects =
                (a, b) -> b instanceof com.hbm_m.block.machines.HadronCoilBlock;
        for (RegistrySupplier<Block> coil : List.of(ModBlocks.HADRON_COIL_ALLOY, ModBlocks.HADRON_COIL_GOLD, ModBlocks.HADRON_COIL_NEODYMIUM,
                ModBlocks.HADRON_COIL_MAGTUNG, ModBlocks.HADRON_COIL_SCHRABIDIUM, ModBlocks.HADRON_COIL_SCHRABIDATE, ModBlocks.HADRON_COIL_STARMETAL,
                ModBlocks.HADRON_COIL_CHLOROPHYTE, ModBlocks.HADRON_COIL_MESE)) {
            Object loc = LoaderHooks.createModelLocation(coil.getId(), "");
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) continue;
            String base = coil.getId().getPath();
            models.put(loc, new ConnectedDecoBlockBakedModel(baked, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/" + base),
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/" + base + "_ct"), null, connects));
        }
    }

    /** 1:1 {@code BlockPWR.getFragments}: Huelle und Anschluss je mit eigener CT-Textur, verbunden mit Traeger und Controller. */
    private static void wrapPwrCtTerrainModels(java.util.Map models) {
        var block = ModBlocks.PWR_BLOCK.get();
        var controller = ModBlocks.PWR_CONTROLLER.get();
        java.util.function.BiPredicate<net.minecraft.world.level.block.Block,
                net.minecraft.world.level.block.Block> connects = (a, b) -> b == block || b == controller;

        for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
            boolean port = state.getValue(com.hbm_m.block.machines.PWRBlock.PORT);
            ResourceLocation full = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, port ? "block/pwr_casing_port" : "block/pwr_block");
            ResourceLocation ct = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, port ? "block/pwr_casing_port_ct" : "block/pwr_block_ct");

            Object loc = LoaderHooks.createModelLocation(ModBlocks.PWR_BLOCK.getId(), "port=" + port);
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) continue;

            models.put(loc, new ConnectedDecoBlockBakedModel(baked, full, ct, null, connects));
        }
    }

    private static void wrapIcfCtTerrainModels(java.util.Map models) {
        var block = ModBlocks.ICF_BLOCK.get();
        var controller = ModBlocks.ICF_CONTROLLER.get();
        java.util.function.BiPredicate<net.minecraft.world.level.block.Block,
                net.minecraft.world.level.block.Block> connects = (a, b) -> b == block || b == controller;

        for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
            boolean port = state.getValue(com.hbm_m.block.machines.icf.ICFPhantomBlock.PORT);

            ResourceLocation full = port
                    ? ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/icf_block_port")
                    : ctTex("icf_block");
            ResourceLocation ct = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID,
                    port ? "block/icf_block_port_ct" : "block/icf_block_ct");

            Object loc = LoaderHooks.createModelLocation(ModBlocks.ICF_BLOCK.getId(), "port=" + port);
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) continue;

            models.put(loc, new ConnectedDecoBlockBakedModel(baked, full, ct, null, connects));
        }
    }

    /** R7i: Kastenrohre - jeder Blockzustand und das Gegenstandsmodell bekommen das RenderBoxDuct-Modell. */
    @SuppressWarnings("unchecked")
    private static void wrapTilted(java.util.Map models, Block block, double drop) {
        // Phase C: auch 1.21.1 (Modelle und Schluessel gibt es dort genauso; vorher fehlten Kippung/Kastenrohre)
        for (net.minecraft.world.level.block.state.BlockState st : block.getStateDefinition().getPossibleStates()) {
            Object loc = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(st);
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked != null && !(baked instanceof com.hbm_m.client.model.TiltedBakedModel))
                models.put(loc, new com.hbm_m.client.model.TiltedBakedModel(baked, drop));
        }
    }

    private static void wrapBoxDuctModels(java.util.Map models) {
        // Phase C: auch 1.21.1 (Modelle und Schluessel gibt es dort genauso; vorher fehlten Kippung/Kastenrohre)
        for (RegistrySupplier<Block> sup : List.of(ModBlocks.FLUID_DUCT_BOX, ModBlocks.FLUID_DUCT_EXHAUST, ModBlocks.RED_CABLE_BOX)) {
            com.hbm_m.block.network.BoxDuctBlock block = (com.hbm_m.block.network.BoxDuctBlock) sup.get();
            BakedModel shared = null;
            for (net.minecraft.world.level.block.state.BlockState st : block.getStateDefinition().getPossibleStates()) {
                Object loc = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(st);
                BakedModel orig = (BakedModel) models.get(loc);
                if (orig == null || orig instanceof com.hbm_m.client.model.BoxDuctBakedModel) continue;
                if (shared == null) shared = new com.hbm_m.client.model.BoxDuctBakedModel(block.kind, orig);
                models.put(loc, shared);
            }
            Object itemLoc = new net.minecraft.client.resources.model.ModelResourceLocation(sup.getId(), "inventory");
            BakedModel itemOrig = (BakedModel) models.get(itemLoc);
            if (itemOrig != null && !(itemOrig instanceof com.hbm_m.client.model.BoxDuctBakedModel.Item))
                models.put(itemLoc, new com.hbm_m.client.model.BoxDuctBakedModel.Item(block.kind, itemOrig));
        }
    }

    /**
     * Связывание спек станков с запечёнными multipart-моделями: berWorld /
     * item-части / render types вливаются в ConfiguredMultipartBakedModel.
     * LOWEST + последним: работаем по ФИНАЛЬным инстансам моделей в карте.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModelBakeBindMachineModels(ModelEvent.ModifyBakingResult event) {
        MachineRenderRegistry.bindBakedModels(event.getModels());
        com.hbm_m.client.render.weapon.WeaponRenderRegistry.bindBakedModels(event.getModels());
    }

    private static void wrapConnectedDecoCtTerrainModels(java.util.Map models) {
        record CtEntry(RegistrySupplier<Block> block, String textureBase) {}

        CtEntry[] entries = {
                new CtEntry(ModBlocks.DECO_STEEL, "deco_steel"),
                new CtEntry(ModBlocks.DECO_RUSTY_STEEL, "deco_rusty_steel"),
                new CtEntry(ModBlocks.DECO_TUNGSTEN, "deco_tungsten"),
                new CtEntry(ModBlocks.DECO_RED_COPPER, "deco_red_copper"),
                new CtEntry(ModBlocks.DECO_ALUMINUM, "deco_aluminum"),
                new CtEntry(ModBlocks.DECO_BERYLLIUM, "deco_beryllium"),
                new CtEntry(ModBlocks.DECO_LEAD, "deco_lead"),
                // Коатированный красномедный провод (1.7.10 WireCoated implements IBlockCT) — соединяется только с собой
                new CtEntry(ModBlocks.RED_WIRE_COATED, "red_wire_coated"),
        };

        for (CtEntry e : entries) {
            Object loc = LoaderHooks.createModelLocation(e.block.getId(), "");
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) {
                continue;
            }
            
            ResourceLocation full = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/" + e.textureBase);
            ResourceLocation ct = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/" + e.textureBase + "_ct");
            models.put(loc, new ConnectedDecoBlockBakedModel(baked, full, ct));
        }
    }

    @SubscribeEvent
    public static void onModelRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Weapon models (Milestone M1)
        com.hbm_m.client.render.weapon.WeaponRenderRegistry.onModelRegisterAdditional(event);

        // Power armor: общие multipart-модели сетов (рендер на entity через OBJ-слои)
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.T51_MODEL_ID);
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.AJR_MODEL_ID);
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.AJRO_MODEL_ID);
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.BISMUTH_MODEL_ID);
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.DNT_MODEL_ID);
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.HAZMAT_MODEL_ID);
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.HAZMAT_RED_MODEL_ID);
        LoaderHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.HAZMAT_GREY_MODEL_ID);

        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_fluid"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_spinner"));

        // mining_drill
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_bit"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_shaft"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_crusher1"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_crusher2"));

        // ore_slopper
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_fan"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_blades_left"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_blades_right"));

        // arc_furnace
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/arc_furnace_electrodes_cold"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/arc_furnace_electrodes_hot"));

        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket"));

        for (String part : new String[] {
                "chekhov_carriage", "chekhov_carriage_friendly", "chekhov_body", "chekhov_barrels",
                "jeremy_gun", "tauon_cannon", "tauon_rotor", "richard_launcher",
                "howard_carriage", "howard_body", "howard_barrelstop", "howard_barrelsbottom",
                "fritz_gun", "maxwell_microwave",
                "arty_carriage", "arty_cannon", "arty_barrel",
                "himars_carriage", "himars_launcher", "himars_crane",
                "sentry_pivot", "sentry_body", "sentry_drum", "sentry_barrell", "sentry_barrelr"
        }) {
            LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/turret_parts/" + part));
        }

        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_clean"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_green"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_rad"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_clean"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_black"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_orange"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_trefoil"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_yellow"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_gray"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_yellow"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_black"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_clean"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil_yellow"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant1"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant2"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_industrial"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_legacy"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_modern"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_modern_clean"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_modern_trefoil"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_modern_skull"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern_black"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern_grid"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern_plate"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern_white"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_black"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_blue"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_red"));

        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_2"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_81"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_87"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_99"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_101"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_106"));
        LoaderHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_111"));

        for (MissileItemModelDefinitions.Definition definition : MissileItemModelDefinitions.all()) {
            ResourceLocation meshId = MissileRenderHelper.meshModelId(
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, definition.itemPath()));
            LoaderHooks.registerAdditionalModel(event, meshId);
        }

        MainRegistry.LOGGER.debug("Registered door and missile variant models for loading");
    }

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterGeometryLoaders event) {
        MainRegistry.LOGGER.info("DoorDeclRegistry initialized with {} doors", DoorDeclRegistry.getAll().size());

        // Станки используют единый machine_parts_loader; конфиг — в JSON модели.
        LoaderHooks.registerGeometryLoader(event, "battery_socket_loader", new MachineBatterySocketModelLoader());
        LoaderHooks.registerGeometryLoader(event, "battery_pack", new com.hbm_m.client.loader.BatteryPackModelLoader());
        LoaderHooks.registerGeometryLoader(event, "door", new DoorModelLoader());
        LoaderHooks.registerGeometryLoader(event, "cargo_elevator", new CargoElevatorModelLoader());
        LoaderHooks.registerGeometryLoader(event, "dae", new DaeModelLoader());
        LoaderHooks.registerGeometryLoader(event, "template_loader", new TemplateModelLoader());
        LoaderHooks.registerGeometryLoader(event, "box_cable", new BoxCableModelLoader());
        LoaderHooks.registerGeometryLoader(event, "press_loader", new PressModelLoader());
        LoaderHooks.registerGeometryLoader(event, "missile_loader", new MissileModelLoader());
        LoaderHooks.registerGeometryLoader(event, "machine_parts_loader", new MachinePartsModelLoader<ConfiguredMultipartBakedModel>());
        // LoaderHooks.registerGeometryLoader(event, "test", new TestModelLoader());

        // Weapon OBJ loader (Milestone M1)
        com.hbm_m.client.loader.WeaponPartsModelLoader weaponLoader = new com.hbm_m.client.loader.WeaponPartsModelLoader();
        LoaderHooks.registerGeometryLoader(event, "weapon_obj_parts", weaponLoader);
        LoaderHooks.registerGeometryLoader(event, "weapon_parts_loader", weaponLoader);

        //? if neoforge {
        /*event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "composite"), net.neoforged.neoforge.client.model.CompositeModel.Loader.INSTANCE);
        event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "obj"), net.minecraftforge.client.model.obj.ObjLoader.INSTANCE);
        *///?}
        
        LoaderHooks.registerGeometryLoader(event, "radar_loader", new MachineRadarModelLoader());
        LoaderHooks.registerGeometryLoader(event, "soyuz_launcher_loader", new com.hbm_m.client.loader.SoyuzLauncherModelLoader());
        LoaderHooks.registerGeometryLoader(event, "soyuz_rocket_loader", new com.hbm_m.client.loader.SoyuzRocketModelLoader());

        // Power armor OBJ loaders
        LoaderHooks.registerGeometryLoader(event, "t51_armor_parts", new com.hbm_m.powerarmor.render.T51ArmorModelLoader());
        LoaderHooks.registerGeometryLoader(event, "ajr_armor_parts", new com.hbm_m.powerarmor.render.AJRArmorModelLoader());
        LoaderHooks.registerGeometryLoader(event, "bismuth_armor_parts", new com.hbm_m.powerarmor.render.BismuthArmorModelLoader());
        LoaderHooks.registerGeometryLoader(event, "dnt_armor_parts", new com.hbm_m.powerarmor.render.DNTArmorModelLoader());
        LoaderHooks.registerGeometryLoader(event, "hazmat_armor_parts", new com.hbm_m.item.hazmat.render.HazmatArmorModelLoader());

        MainRegistry.LOGGER.info("Registered geometry loaders successfully");
    }

    // 1.21.1: тинт применяется как полный ARGB (альфа тоже умножается), а HBM-тинты хранятся
    // как RGB без альфы → 0x00RRGGBB делал иконку полностью прозрачной. Форсируем непрозрачную альфу.
    private static int opaqueTint(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    // Фрагменты бедрок-руды: в 1.7.10 (ItemAutogen.registerIcons) текстура
    // bedrock_ore_fragment.png мутировалась RGBMutatorInterpolatedComponentRemap
    // (0xFFFFFF -> mat.solidColorLight, 0x505050 -> mat.solidColorDark).
    // В порте эти иконки ЗАПЕЧЕНЫ в отдельные текстуры bedrock_ore_fragment_<мат>.png
    // скриптом scripts/bake_autogen_icons.py с точным мутатором оригинала, поэтому
    // vanilla ItemColor-тинт больше не нужен (в оригинале getColorFromItemStack
    // возвращал 0xffffff для кастомных иконок).

    /**
     * Цветовые тинты предметов и блоков через Architectury ColorHandlerRegistry — единый путь
     * для Forge/NeoForge вместо дублированных @SubscribeEvent-обработчиков
     * RegisterColorHandlersEvent. Вызывается из {@link #registerCommonClientContent()} (фаза
     * конструктора мода): на NeoForge 1.21.1 Register-события стреляют раньше
     * FMLClientSetupEvent, а Architectury буферизует supplier'ы и резолвит их на событии лоадера,
     * поэтому здесь нельзя ни вызывать .get(), ни откладывать регистрацию до client setup.
     */
    private static void registerColors() {
        // Мета-предметы вкладки Parts (PartTabMetaItems): тинт базовой текстуры цветом
        // материала/красителя (аппроксимация ItemAutogen/ItemChemicalDye оригинала).
        // Двухслойные (dye/crayon) тинтуруются только на layer1 (оверлей).
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) ->
                opaqueTint(com.hbm_m.item.PartTabMetaItems.tintFor(stack.getItem(), tintIndex)),
                com.hbm_m.item.PartTabMetaItems.tintedSuppliers());
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> {
            if (tintIndex == 0) return opaqueTint(0xFFFFFF);
            return opaqueTint(com.hbm_m.item.liquids.FluidIdentifierItem.getTintColor(stack));
        }, ModItems.FLUID_IDENTIFIER);
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> {
            if (tintIndex == 0) return opaqueTint(0xFFFFFF);
            return opaqueTint(com.hbm_m.item.liquids.FluidBarrelItem.getTintColor(stack));
        }, ModItems.FLUID_BARREL);
        // Fluid Duct - tint overlay layer with fluid color
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> {
            if (tintIndex == 0) return opaqueTint(0xFFFFFF);
            return opaqueTint(com.hbm_m.item.liquids.FluidDuctItem.getTintColor(stack));
        }, ModItems.FLUID_DUCT, ModItems.FLUID_DUCT_COLORED, ModItems.FLUID_DUCT_SILVER);
        // Mineral Pipes - tint layer0 with the pipe's mineral color
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> {
            if (stack.getItem() instanceof com.hbm_m.item.MineralPipeItem pipe) {
                return opaqueTint(pipe.getTintColor());
            }
            return opaqueTint(0xFFFFFF);
        }, ModItems.PIPE_IRON, ModItems.PIPE_COPPER, ModItems.PIPE_GOLD,
           ModItems.PIPE_LEAD, ModItems.PIPE_STEEL, ModItems.PIPE_TUNGSTEN,
           ModItems.PIPE_TITANIUM, ModItems.PIPE_ALUMINUM, ModItems.PIPE_DURA_STEEL);

        net.minecraft.client.color.block.BlockColor sellafiteTint = (state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFF;
            int levelValue = state.getValue(com.hbm_m.block.generic.BlockSellafieldSlaked.COLOR_LEVEL);
            return java.awt.Color.HSBtoRGB(0F, 0F, 1F - levelValue / 15F);
        };
        ColorHandlerRegistry.registerBlockColors(sellafiteTint,
                com.hbm_m.block.ModBlocks.SELLAFIELD_BEDROCK,
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_DIAMOND,
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_EMERALD,
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED,
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM,
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_RADGEM);

        // Fluid Duct block - tint with the fluid's color from the BlockEntity
        ColorHandlerRegistry.registerBlockColors((state, level, pos, tintIndex) -> {
            if (tintIndex == 0) return opaqueTint(0xFFFFFF);
            if (tintIndex != 1 || level == null || pos == null) return opaqueTint(0xFFFFFF);
            var be = level.getBlockEntity(pos);
            if (be instanceof com.hbm_m.blockentity.machines.FluidDuctBlockEntity ductBe) {
                var fluid = ductBe.getFluidType();
                if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    return opaqueTint(com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(fluid));
                }
            }
            return opaqueTint(0xFFFFFF);
        }, com.hbm_m.block.ModBlocks.FLUID_DUCT,
                com.hbm_m.block.ModBlocks.FLUID_DUCT_COLORED,
                com.hbm_m.block.ModBlocks.FLUID_DUCT_SILVER);
    }

    /**
     * Рендереры сущностей через Architectury EntityRendererRegistry — единый путь для
     * Forge/NeoForge вместо @SubscribeEvent-обработчика EntityRenderersEvent.
     * Вызывается из {@link #registerCommonClientContent()} (фаза конструктора мода):
     * supplier'ы резолвятся Architectury лениво, на событии лоадера.
     */
    private static void registerEntityRenderersCommon() {
        EntityRendererRegistry.register(ModEntities.AIRNUKEBOMB_PROJECTILE, AirNukeBombProjectileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.AIRBOMB_PROJECTILE, AirBombProjectileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.AIRSTRIKE_NUKE_ENTITY, AirstrikeNukeEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.AIRSTRIKE_ENTITY, AirstrikeEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.AIRSTRIKE_AGENT_ENTITY, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.NUKE_FALLOUT_RAIN, RenderFallout::new);
        EntityRendererRegistry.register(ModEntities.NUKE_MK3, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.SOLINIUM_EXPLOSION, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.BALEFIRE_EXPLOSION, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.TOM_METEOR, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.TOM_BLAST, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.CLOUD_FLEIJA, RenderCloudFleija::new);
        EntityRendererRegistry.register(ModEntities.NUKE_MK5, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.FALLING_SELLAFIT_ENTITY_TYPE, FallingBlockRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_TEST, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_ANTI_BALLISTIC, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_MICRO, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_SCHRABIDIUM, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_BHOLE, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_TAINT, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_EMP, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_GENERIC, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_INCENDIARY, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_CLUSTER, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_BUSTER, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_DECOY, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_STEALTH, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_STRONG, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_INCENDIARY_STRONG, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_CLUSTER_STRONG, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_BUSTER_STRONG, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_EMP_STRONG, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_BURST, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_INFERNO, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_RAIN, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_DRILL, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_SHUTTLE, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_NUCLEAR, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_NUCLEAR_CLUSTER, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_VOLCANO, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_DOOMSDAY, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_DOOMSDAY_RUSTED, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CLUSTER_ROCKET, com.hbm_m.client.render.projectile.ClusterRocketEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHRAPNEL, com.hbm_m.client.render.effect.ShrapnelRenderer::new);
        EntityRendererRegistry.register(ModEntities.EMP_PULSE, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.BLACK_HOLE, RenderBlackHole::new);
        EntityRendererRegistry.register(ModEntities.VORTEX, RenderBlackHole::new);
        EntityRendererRegistry.register(ModEntities.RAGING_VORTEX, RenderBlackHole::new);
        EntityRendererRegistry.register(ModEntities.DIGAMMA_QUASAR, RenderQuasar::new);
        EntityRendererRegistry.register(ModEntities.DIGAMMA_SPEAR, com.hbm_m.client.render.effect.SpearRenderer::new);
        EntityRendererRegistry.register(ModEntities.RUBBLE, RubbleEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.RAD_BEAST, com.hbm_m.client.render.mob.RADBeastRenderer::new);
        EntityRendererRegistry.register(ModEntities.BOT_PRIME_HEAD, com.hbm_m.client.render.mob.BOTPrimeRenderer::head);
        EntityRendererRegistry.register(ModEntities.BOT_PRIME_BODY, com.hbm_m.client.render.mob.BOTPrimeRenderer::body);
        EntityRendererRegistry.register(ModEntities.UFO, com.hbm_m.client.render.mob.UFORenderer::new);
        EntityRendererRegistry.register(ModEntities.BOMBER, com.hbm_m.client.render.plane.BomberRenderer::new);
        EntityRendererRegistry.register(ModEntities.BOMBLET_ZETA, com.hbm_m.client.render.projectile.BombletThetaRenderer::new);
        EntityRendererRegistry.register(ModEntities.MASKMAN, com.hbm_m.client.render.mob.MaskManRenderer::new);
        EntityRendererRegistry.register(ModEntities.NOLO, NoloEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ENTITY_MOB_TAINTED_CREEPER, RenderCreeperUniversal::tainted);
        EntityRendererRegistry.register(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, RenderCreeperUniversal::volatileCreeper);
        EntityRendererRegistry.register(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, RenderCreeperUniversal::phosgene);
        EntityRendererRegistry.register(ModEntities.ENTITY_MIST, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.ENTITY_MOB_GOLD_CREEPER, RenderCreeperUniversal::goldCreeper);
        EntityRendererRegistry.register(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER, RenderCreeperUniversal::nuclear);

        // Renderer registrations restored after the FFA merge (2026-10-24)
        EntityRendererRegistry.register(ModEntities.MISSILE_9M723, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_TOPOL, MissileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CHLORINE_FX, ctx -> new com.hbm_m.client.render.entity.MultiCloudRenderer<>(ctx, "chlorine"));
        EntityRendererRegistry.register(ModEntities.ORANGE_FX, ctx -> new com.hbm_m.client.render.entity.MultiCloudRenderer<>(ctx, "orange"));
        EntityRendererRegistry.register(ModEntities.COG, com.hbm_m.client.render.effect.CogRenderer::new);
        EntityRendererRegistry.register(ModEntities.ARTILLERY_ROCKET, com.hbm_m.client.render.projectile.RenderArtilleryRocket::new);
        EntityRendererRegistry.register(ModEntities.ARTILLERY_SHELL, com.hbm_m.client.render.projectile.RenderArtilleryShell::new);
        EntityRendererRegistry.register(ModEntities.B92_BEAM, com.hbm_m.client.render.projectile.B92BeamRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLOCK_SPIDER, com.hbm_m.client.render.mob.MiscMobRenderers.BlockSpider::new);
        EntityRendererRegistry.register(ModEntities.BOAT_RUBBER, com.hbm_m.client.render.implementations.BoatRubberRenderer::new);
        EntityRendererRegistry.register(ModEntities.BOBMAZON, com.hbm_m.client.render.implementations.BobmazonRenderer::new);
        EntityRendererRegistry.register(ModEntities.BOXCAR, com.hbm_m.client.render.implementations.BoxcarRenderer::new);
        EntityRendererRegistry.register(ModEntities.BUILDING, com.hbm_m.client.render.implementations.FallingProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.BULLET, com.hbm_m.client.render.projectile.BulletRenderer::new);
        EntityRendererRegistry.register(ModEntities.BULLET_MK4, com.hbm_m.client.weapon.BulletRenderers.MK4::new);
        EntityRendererRegistry.register(ModEntities.BULLET_MK4_CL, com.hbm_m.client.weapon.BulletRenderers.MK4::new);
        EntityRendererRegistry.register(ModEntities.BULLET_BASE_NT, com.hbm_m.client.render.projectile.RenderBulletNT::new);
        EntityRendererRegistry.register(ModEntities.BULLET_BEAM, com.hbm_m.client.weapon.BulletRenderers.Beam::new);
        EntityRendererRegistry.register(ModEntities.BURNING_FOEQ, com.hbm_m.client.render.projectile.BurningFOEQRenderer::new);
        EntityRendererRegistry.register(ModEntities.C130, com.hbm_m.client.render.entity.C130Renderers.C130Renderer::new);
        EntityRendererRegistry.register(ModEntities.CHOPPER_MINE, com.hbm_m.client.render.projectile.ChopperMineRenderer::new);
        EntityRendererRegistry.register(ModEntities.CLOUD_FLEIJA_RAINBOW, com.hbm_m.client.render.effect.RenderCloudRainbow::new);
        EntityRendererRegistry.register(ModEntities.CLOUD_SOLINIUM, com.hbm_m.client.render.effect.RenderCloudSolinium::new);
        EntityRendererRegistry.register(ModEntities.CLOUD_TOM, com.hbm_m.client.render.effect.TomRenderers.CloudTom::new);
        EntityRendererRegistry.register(ModEntities.CYBER_CRAB, com.hbm_m.client.render.mob.CrabRenderers.Cyber::new);
        EntityRendererRegistry.register(ModEntities.CHEMICAL, com.hbm_m.client.render.projectile.ChemicalRenderer::new);
        EntityRendererRegistry.register(ModEntities.DEATH_BLAST, com.hbm_m.client.render.effect.OrbitalStrikeRenderer::deathBlast);
        EntityRendererRegistry.register(ModEntities.DUCHESS_GAMBIT, com.hbm_m.client.render.implementations.FallingProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.DUCK, com.hbm_m.client.render.mob.DuckRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEST_DUMMY, com.hbm_m.client.render.mob.MiscMobRenderers.Dummy::new);
        EntityRendererRegistry.register(ModEntities.EMP_BLAST, com.hbm_m.client.render.entity.EMPBlastRenderer::new);
        EntityRendererRegistry.register(ModEntities.FBI, com.hbm_m.client.render.mob.FBIRenderers.FBI::new);
        EntityRendererRegistry.register(ModEntities.FBI_DRONE, com.hbm_m.client.render.mob.FBIRenderers.Drone::new);
        EntityRendererRegistry.register(ModEntities.FALLING_NUKE, com.hbm_m.client.render.projectile.FallingNukeRenderer::new);
        EntityRendererRegistry.register(ModEntities.FIREWORKS, ctx -> new EmptyEntityRenderer<>(ctx));
        EntityRendererRegistry.register(ModEntities.FOG_FX, com.hbm_m.client.render.entity.FogRenderer::new);
        EntityRendererRegistry.register(ModEntities.GHOST, com.hbm_m.client.render.mob.MiscMobRenderers.Ghost::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_BEHEMOTH, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_BLASTER, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_BOMBARDIER, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_BRAWLER, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_BRENDA, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_DIGGER, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_NUCLEAR, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLYPHID_SCOUT, com.hbm_m.client.render.mob.GlyphidRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRENADE_UNIVERSAL, com.hbm_m.client.weapon.render.RenderGrenadeUniversal::new);
        EntityRendererRegistry.register(ModEntities.HUNTER_CHOPPER, com.hbm_m.client.render.mob.HunterChopperRenderer::new);
        EntityRendererRegistry.register(ModEntities.ITEM_BUOYANT, net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.METEOR, com.hbm_m.client.render.implementations.MeteorRenderer::new);
        EntityRendererRegistry.register(ModEntities.CART_CRATE, com.hbm_m.client.render.entity.NeoCartRenderer::new);
        EntityRendererRegistry.register(ModEntities.CART_DESTROYER, com.hbm_m.client.render.entity.NeoCartRenderer::new);
        EntityRendererRegistry.register(ModEntities.CART_ORE, com.hbm_m.client.render.entity.NeoCartRenderer::new);
        EntityRendererRegistry.register(ModEntities.CART_POWDER, com.hbm_m.client.render.entity.NeoCartRenderer::new);
        EntityRendererRegistry.register(ModEntities.CART_SEMTEX, com.hbm_m.client.render.entity.NeoCartRenderer::new);
        EntityRendererRegistry.register(ModEntities.MINECART_TEST, com.hbm_m.client.render.entity.RenderMinecartTest::new);
        EntityRendererRegistry.register(ModEntities.MINER_ROCKET, com.hbm_m.client.render.implementations.MinerRocketRenderer::new);
        EntityRendererRegistry.register(ModEntities.PARACHUTE_CRATE, com.hbm_m.client.render.entity.C130Renderers.ParachuteCrateRenderer::new);
        EntityRendererRegistry.register(ModEntities.PARASITE_MAGGOT, com.hbm_m.client.render.mob.ParasiteMaggotRenderer::new);
        EntityRendererRegistry.register(ModEntities.PIGEON, com.hbm_m.client.render.mob.MiscMobRenderers.Pigeon::new);
        EntityRendererRegistry.register(ModEntities.PLASTIC_BAG, com.hbm_m.client.render.mob.MiscMobRenderers.PlasticBag::new);
        EntityRendererRegistry.register(ModEntities.QUACKOS, com.hbm_m.client.render.mob.MiscMobRenderers.Quackos::new);
        EntityRendererRegistry.register(ModEntities.SIEGE_CRAFT, com.hbm_m.client.render.mob.SiegeCraftRenderer::new);
        EntityRendererRegistry.register(ModEntities.SIEGE_LASER, com.hbm_m.client.render.projectile.SiegeLaserRenderer::new);
        EntityRendererRegistry.register(ModEntities.SIEGE_TUNNELER, com.hbm_m.client.render.mob.SiegeTunnelerRenderer::new);
        EntityRendererRegistry.register(ModEntities.TAINT_CRAB, com.hbm_m.client.render.mob.CrabRenderers.Taint::new);
        EntityRendererRegistry.register(ModEntities.TESLA_CRAB, com.hbm_m.client.render.mob.CrabRenderers.Tesla::new);
        EntityRendererRegistry.register(ModEntities.TORPEDO, com.hbm_m.client.render.implementations.FallingProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.UNDEAD_SOLDIER, com.hbm_m.client.render.entity.UndeadSoldierRenderer::new);
        EntityRendererRegistry.register(ModEntities.WASTE_PEARL, com.hbm_m.client.render.entity.WhiteBoxRenderer::new);
        EntityRendererRegistry.register(ModEntities.ROCKET_LAMBDA, com.hbm_m.client.render.entity.LambdaRocketRenderer::new);
        EntityRendererRegistry.register(ModEntities.SATELLITE_POD, com.hbm_m.client.render.entity.SatellitePodRenderer::new);
        EntityRendererRegistry.register(ModEntities.SAWBLADE, com.hbm_m.client.render.effect.SawbladeRenderer::new);
        EntityRendererRegistry.register(ModEntities.TRAIN_CARGO_TRAM, com.hbm_m.client.render.entity.RenderTrainCargoTram::new);
        EntityRendererRegistry.register(ModEntities.TRAIN_CARGO_TRAM_TRAILER, com.hbm_m.client.render.entity.RenderTrainCargoTramTrailer::new);
        // Follow-up one-liners (same merge loss): renderer classes that already live in the tree
        EntityRendererRegistry.register(ModEntities.MISSILE_CUSTOM, com.hbm_m.client.render.entity.MissileCustomRenderer::new);
        EntityRendererRegistry.register(ModEntities.TNT_PRIMED_BASE, com.hbm_m.client.render.entity.TNTPrimedBaseRenderer::new);
        EntityRendererRegistry.register(ModEntities.ORBITAL_LASER, com.hbm_m.client.render.effect.OrbitalStrikeRenderer::orbitalLaser);
        EntityRendererRegistry.register(ModEntities.PINK_CLOUD_FX, ctx -> new com.hbm_m.client.render.entity.MultiCloudRenderer<>(ctx, "pc"));
        EntityRendererRegistry.register(ModEntities.ACID_BOMB, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.DISPERSER_CANISTER, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRENADE_BOUNCY_GENERIC, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.COIN, EmptyEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FIRE_LINGERING, EmptyEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WAYPOINT, EmptyEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TRAIN_BOUNDING_DUMMY, EmptyEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TRAIN_SEAT_DUMMY, EmptyEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ShaderReloadListener());
        event.registerReloadListener(HbmThermalHandler.INSTANCE);
        event.registerReloadListener(com.hbm_m.client.model.variant.DoorModelRegistry.getInstance());
        event.registerReloadListener(new com.hbm_m.client.loader.dae.DaeModelReloader());
        event.registerReloadListener((preparationBarrier, resourceManager,
                preparationsProfiler, reloadProfiler,
                backgroundExecutor, gameExecutor) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                FleijaSphereMesh.reload(resourceManager);
                com.hbm_m.client.render.projectile.ClusterSubmunitionMesh.reload(resourceManager);
                // Единая точка инвалидации: см. RenderCacheManager
                com.hbm_m.client.render.cache.RenderCacheManager.invalidateAll(
                        com.hbm_m.client.render.cache.RenderCacheManager.Reason.RESOURCE_RELOAD);
            }, gameExecutor);
        });
    }

    public static void onClientDisconnect(
            //? if forge {
            net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event
            //?} elif neoforge {
            /*net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event
            *///?}
    ) {
        clearClientCachesDeferred();
    }

    //? if forge {
    @SubscribeEvent
    public static void onRegisterGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        MainRegistry.LOGGER.info("Registering GUI overlays...");
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "geiger_counter_hud", OverlayGeiger.GEIGER_HUD_OVERLAY);
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.ARMOR_LEVEL.id(), "power_armor_hud", OverlayPowerArmor.POWER_ARMOR_OVERLAY);
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.PORTAL.id(), "radiation_pixels", OverlayRadiationVisuals.RADIATION_PIXELS_OVERLAY);
        event.registerAboveAll("info_toast", OverlayInfoToast.OVERLAY);
        // Оверлей маски рисуется ПОД HUD (1.7.10: GuiIngameForge рисует helmet-хов после
        // виньетки, до прицела и хотбара — хотбар остаётся поверх мутности).
        event.registerBelow(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id(), "gas_mask_overlay", com.hbm_m.client.overlay.OverlayGasMask.OVERLAY);
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRegisterGuiOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        MainRegistry.LOGGER.info("Registering GUI overlays...");
        
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "geiger_counter_hud"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayGeiger.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
        
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.ARMOR_LEVEL, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "power_armor_hud"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayPowerArmor.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
        
        event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "radiation_pixels"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayRadiationVisuals.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
        
        event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "info_toast"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayInfoToast.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
        
        // Оверлей маски рисуется ПОД HUD (1.7.10: после виньетки, до прицела и хотбара).
        event.registerBelow(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "gas_mask_overlay"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                com.hbm_m.client.overlay.OverlayGasMask.render(guiGraphics);
            }
        });
        
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }
    *///?}
    
    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CrateContentsTooltipComponent.class, CrateContentsTooltipComponentRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterShaders(
            //? if forge {
            net.minecraftforge.client.event.RegisterShadersEvent event
            //?} elif neoforge {
            /*net.neoforged.neoforge.client.event.RegisterShadersEvent event
            *///?}
    ) throws IOException {
        MainRegistry.LOGGER.info("Registering optimized shaders...");

        //? if < 1.21.1 {
        VertexFormat blockLitSimpleFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .build()
        );
        //?} else {
        /*VertexFormat blockLitSimpleFormat = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("Normal",   com.mojang.blaze3d.vertex.VertexFormatElement.NORMAL)
                .add("UV0",      com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .build();
        *///?}

        //? if < 1.21.1 {
        VertexFormat blockLitInstancedFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("BoneId", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .put("InstPos", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBboxMin", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstBboxSize", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC01", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC23", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC45", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC67", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build()
        );
        //?} else {
        /*VertexFormat blockLitInstancedFormat = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("Normal",   com.mojang.blaze3d.vertex.VertexFormatElement.NORMAL)
                .add("UV0",      com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .add("BoneId", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .add("InstPos", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .add("InstRot", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstBboxMin", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .add("InstBboxSize", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC01", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC23", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC45", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC67", RenderHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build();
        *///?}

        ResourceLocation virtualInstancedVsh = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shaders/core/block_lit_instanced.vsh");

        com.hbm_m.client.render.shader.modification.ShaderModification instancingDefine =
            com.hbm_m.client.render.shader.modification.ShaderModification.builder()
                .define("USE_INSTANCING")
                .define("USE_VERTEX_BONE_ID");

        // Патчим РЕАЛЬНЫЙ файл block_lit_instanced.vsh defines'ами. Исторический
        // wrapRedirect(... -> block_lit.vsh) остался с времён, когда файла
        // block_lit_instanced.vsh не существовало; теперь он полноценный исходник
        // (InstUvRect/InstColor/GradParams + фоллофф), и редирект на block_lit.vsh
        // молча выкидывал все пер-инстанс атрибуты из компиляции — на фордже из-за
        // этого не работал тинт частей и шеринг скинов дверей (атрибут uvRect
        // отсутствовал в программе).
        net.minecraft.server.packs.resources.ResourceProvider instancedProvider =
            com.hbm_m.client.render.shader.modification.ShaderPreDefinitions.wrap(
                event.getResourceProvider(), virtualInstancedVsh, instancingDefine);

        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block_lit_simple"),
                blockLitSimpleFormat
            ),
            ModShaders::setBlockLitSimpleShader
        );
        MainRegistry.LOGGER.info("Successfully registered block_lit_simple shader");

        // Weapon viewmodel mesh shader (WeaponPartVboRenderer / WeaponRenderRegistry, audit firearms parity §7.4 Phase 1):
        // same Position/Normal/UV0 layout as block_lit_simple, but Sampler0 = weapon
        // texture, uColor = per-part tint, constant lightmap UV from packedLight.
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "weapon_lit"),
                blockLitSimpleFormat
            ),
            ModShaders::setWeaponLitShader
        );
        MainRegistry.LOGGER.info("Successfully registered weapon_lit shader");

        event.registerShader(
            new ShaderInstance(
                instancedProvider,
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block_lit_instanced"),
                blockLitInstancedFormat
            ),
            ModShaders::setBlockLitInstancedShader
        );
        MainRegistry.LOGGER.info("Successfully registered block_lit_instanced shader");

        //? if < 1.21.1 {
        VertexFormat nukeCloudFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("Color",    DefaultVertexFormat.ELEMENT_COLOR)
                .build()
        );
        //?} else {
        /*VertexFormat nukeCloudFormat = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("UV0",      com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .add("Color",    com.mojang.blaze3d.vertex.VertexFormatElement.COLOR)
                .build();
        *///?}

        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "nuke_cloud"),
                nukeCloudFormat
            ),
            ModShaders::setNukeCloudShader
        );
        MainRegistry.LOGGER.info("Successfully registered nuke_cloud shader");

        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "nuke_add"),
                nukeCloudFormat
            ),
            ModShaders::setNukeAddShader
        );
        MainRegistry.LOGGER.info("Successfully registered nuke_add shader");

        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "gun_additive"),
                DefaultVertexFormat.NEW_ENTITY
            ),
            ModShaders::setGunAdditiveShader
        );
        MainRegistry.LOGGER.info("Successfully registered gun_additive shader");

        // Vanilla position_color_tex_lightmap clone: NT particle render types
        // (gas flame, cooling tower, ashes) bind OUR instance so the pack's
        // translucent material treatment never replaces the vanilla look.
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "nt_color_tex_lightmap"),
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
            ),
            ModShaders::setNtColorTexLightmapShader
        );
        MainRegistry.LOGGER.info("Successfully registered nt_color_tex_lightmap shader");

        // Копия DH-глубины в главный z-buffer (окклюзия дальних мешей против LOD).
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "dh_depth_blit"),
                DefaultVertexFormat.POSITION
            ),
            ModShaders::setDhDepthBlitShader
        );
        MainRegistry.LOGGER.info("Successfully registered dh_depth_blit shader");
    }

    private static class LeavesModelWrapper extends BakedModelWrapper<BakedModel> {

        public LeavesModelWrapper(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            GraphicsStatus graphics = Minecraft.getInstance().options.graphicsMode().get();
            if (graphics == GraphicsStatus.FANCY || graphics == GraphicsStatus.FABULOUS) {
                return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
            }
            return ChunkRenderTypeSet.of(RenderType.solid());
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.POWER_ARMOR, PowerArmorEmptyModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.GAS_MASK, com.hbm_m.client.model.GasMaskModels::createGasMask);
        event.registerLayerDefinition(com.hbm_m.client.render.armor.ArmorAccessoryModels.GOGGLES, com.hbm_m.client.render.armor.ArmorAccessoryModels::createGoggles);
        event.registerLayerDefinition(com.hbm_m.client.render.armor.ArmorAccessoryModels.JETPACK, com.hbm_m.client.render.armor.ArmorAccessoryModels::createJetpack);
        event.registerLayerDefinition(ModModelLayers.M65, com.hbm_m.client.model.GasMaskModels::createM65);
    }
}