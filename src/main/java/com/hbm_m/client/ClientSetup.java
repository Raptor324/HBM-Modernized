package com.hbm_m.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.menu.MenuRegistry;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.loader.CargoElevatorModelLoader;
import com.hbm_m.client.loader.DoorModelLoader;
import com.hbm_m.client.loader.DaeModelLoader;
import com.hbm_m.client.loader.HeatingOvenModelLoader;
import com.hbm_m.client.loader.MachineAdvancedAssemblerModelLoader;
import com.hbm_m.client.loader.MachineAssemblerModelLoader;
import com.hbm_m.client.loader.MachineBatterySocketModelLoader;
import com.hbm_m.client.loader.MachineChemicalPlantModelLoader;
import com.hbm_m.client.loader.MachineFluidTankModelLoader;
import com.hbm_m.client.loader.MachineHydraulicFrackiningTowerModelLoader;
import com.hbm_m.client.loader.MachineRadarModelLoader;
import com.hbm_m.client.loader.MissileModelLoader;
import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.hbm_m.client.loader.PressModelLoader;
import com.hbm_m.client.loader.TemplateModelLoader;
import com.hbm_m.client.model.ConnectedDecoBlockBakedModel;
import com.hbm_m.client.overlay.OverlayGeiger;
import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.client.overlay.OverlayRadiationVisuals;
import com.hbm_m.client.render.EmptyEntityRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.ModShaders;
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
import com.hbm_m.client.render.implementations.BatterySocketCreativeRenderer;
import com.hbm_m.client.render.implementations.DoorRenderer;
import com.hbm_m.client.render.implementations.TransitionSealRenderer;
import com.hbm_m.client.render.implementations.GasCentrifugeRenderer;
import com.hbm_m.client.render.implementations.HeatingOvenRenderer;
import com.hbm_m.client.render.implementations.MachineFluidTankRenderer;
import com.hbm_m.client.render.implementations.IndustrialTurbineRenderer;
import com.hbm_m.client.render.implementations.LaunchPadMissileRenderer;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.loader.MachineCoolingTowerModelLoader;
import com.hbm_m.client.render.implementations.CrucibleRenderer;
import com.hbm_m.client.render.implementations.MachineCoolingTowerRenderer;
import com.hbm_m.client.render.implementations.MachineCrystallizerRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.SoyuzLauncherRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.client.render.implementations.MachineRadarRenderer;
import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
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
import com.hbm_m.inventory.gui.GUIMachineGasCentrifuge;
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
import com.hbm_m.inventory.gui.GUIRBMKStorage;
import com.hbm_m.inventory.gui.GUIRBMKOutgasser;
import com.hbm_m.inventory.gui.GUIMachineSilex;
import com.hbm_m.inventory.gui.GUIMachineSolderingStation;
import com.hbm_m.inventory.gui.GUIMachineSubstation;
import com.hbm_m.inventory.gui.GUIMachineSteamTurbine;
import com.hbm_m.inventory.gui.GUIMachineTurbine;
import com.hbm_m.inventory.gui.GUIMachineZirnox;
import com.hbm_m.inventory.gui.GUIMachineChemicalPlant;
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
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.SchrabfogParticle;
import com.hbm_m.particle.custom.TownauraParticle;
import com.hbm_m.particle.custom.MissileContrailParticle;
import com.hbm_m.particle.custom.RadFogParticle;
import com.hbm_m.particle.explosions.basic.CameraShakeHandler;
import com.hbm_m.platform.PlatformHooks;
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
import com.hbm_m.platform.recipe.RecipeHooks;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm_m.datagen.assets.MissileItemModelDefinitions;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?} elif neoforge {
/*@EventBusSubscriber(modid = RefStrings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?}
@SuppressWarnings({"UnstableApiUsage", "removal"})
public class ClientSetup {

    private static boolean initialized = false;

    /**
     * Loader-agnostic клиентская инициализация.
     *
     * Вызывается:
     * - на Forge: из {@link #onClientSetup(FMLClientSetupEvent)} (MOD bus)
     * - на Fabric: из {@code FabricClientEntrypoint} (см. src/main/java/.../FabricClientEntrypoint.java)
     */
    public static synchronized void initClient() {
        if (initialized) return;
        initialized = true;

        ModPacketHandler.registerClientReceivers();

        // Key mappings регистрируются в ModConfigKeybindHandler.init() через Architectury/обвязку,
        // но на некоторых таргетах удобно иметь fallback в одном месте.
        ModConfigKeybindHandler.init();
        ClientModEvents.init();
        com.hbm_m.client.missile.track.MissileTrackClientEvents.register();
        CameraShakeHandler.initClient();
        PowerArmorHardLandingCameraShakeClient.initClient();
        PowerArmorSounds.register();
        PowerArmorStepSoundHandler.initClient();

        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
                com.hbm_m.config.ModClothConfig.reloadServer();
                ClientRadiationData.clearAll();
        });


        // Экраны меню.
        // На Forge/Fabric MenuRegistry.registerScreenFactory работает напрямую из любого момента.
        // На NeoForge 1.21.1+ регистрация экранов обязана произойти ДО RegisterMenuScreensEvent
        // (он стреляет во время Minecraft construction, раньше FMLClientSetupEvent) — там
        // используется отдельный @SubscribeEvent onRegisterMenuScreens ниже, а сюда мы не заходим.
        //? if forge || fabric {
        registerScreens();
        //?}

        // Рендереры (entity + block entity) - loader-specific registration.
        registerRenderersCommon();

        // Частицы - loader-specific registration.
        registerParticlesCommon();

        // Цвета предметов/блоков - loader-specific registration.
        registerColorsCommon();

        // Reload listeners + очистка кэшей.
        registerReloadListenersCommon();

        // Overlays/HUD.
        registerHudCommon();

        // World render hooks (debug, stage flushes, etc.).
        registerWorldRenderHooksCommon();

        // Disconnect handler - чистим VBO/модельные кэши.
        registerDisconnectHandlerCommon();

        // Клиентские тэги/настройки рендера, общие для обоих.
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
        event.enqueueWork(ClientSetup::registerRenderLayers);
    }

    // Регистрация биндов: RegisterKeyMappingsEvent стреляет РАНЬШЕ FMLClientSetupEvent
    // (на Forge 1.20.1 — уже до него, на NeoForge 1.21.1 — во время Minecraft construction),
    // поэтому бинды нельзя регистрировать из initClient()/Architectury KeyMappingRegistry —
    // они «регистрируются после события», не попадают в таблицу ввода и не видны в настройках.
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
    /*/// NeoForge 1.21.1+: RegisterMenuScreensEvent стреляет во время Minecraft construction,
    /// РАНЬШЕ FMLClientSetupEvent. Поэтому экраны надо регистрировать здесь, а не в initClient().
    /// Architectury MenuRegistry.registerScreenFactory на NeoForge подписывается на это же событие
    /// через bus.addListener — вызов внутри @SubscribeEvent (зарегистрированного через
    /// @EventBusSubscriber(Bus.MOD) во время mod loading) происходит до события, поэтому listener успевает.
    @SubscribeEvent
    public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        MainRegistry.LOGGER.info("RegisterMenuScreensEvent fired. Registering HBM screens.");
        registerScreens();
    }
    *///?}

    private static void registerDebugClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            net.minecraft.commands.Commands.literal("debug_ntm_m_transition_seal")
                .executes(context -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                            Component.literal(TransitionSealRenderer.getDebugInfo()), false);
                    }
                    return 1;
                })
        );
    }

    @SuppressWarnings("removal")
    private static void registerRenderLayers() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.TRANSITION_SEAL.get(), RenderType.cutout());
    }

    private static void registerRadAbsorberItemProperties() {
        // RefStrings.resourceLocation инкапсулирует платформо-зависимый конструктор
        // (new ResourceLocation на 1.20.1, ResourceLocation.fromNamespaceAndPath на 1.21.1+).
        net.minecraft.client.renderer.item.ItemProperties.register(
                ModBlocks.RAD_ABSORBER.get().asItem(),
                RefStrings.resourceLocation("tier"),
                (stack, level, entity, seed) -> BlockAbsorberItem.readTier(stack).ordinal()
        );
    }

    private static void registerScreens() {
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRYSTALLIZER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCrystallizer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BREEDER_MENU.get(), GUIMachineBreeder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LARGE_PYLON_MENU.get(), GUIMachineLargePylon::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CYCLOTRON_MENU.get(), GUIMachineCyclotron::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ZIRNOX_MENU.get(), GUIMachineZirnox::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ARC_WELDER_MENU.get(), GUIMachineArcWelder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOLDERING_STATION_MENU.get(), GUIMachineSolderingStation::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MIXER_MENU.get(), GUIMachineMixer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DERRICK_MENU.get(), GUIMachineDerrick::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_CONSOLE_MENU.get(), GUIMachineRbmkConsole::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_ROD_MENU.get(), GUIRBMKRod::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_CONTROL_MENU.get(), GUIRBMKControl::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_BOILER_MENU.get(), GUIRBMKBoiler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_STORAGE_MENU.get(), GUIRBMKStorage::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RBMK_OUTGASSER_MENU.get(), GUIRBMKOutgasser::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FLARE_STACK_MENU.get(), GUIMachineFlareStack::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PUMPJACK_MENU.get(), GUIMachinePumpjack::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADAR_MENU.get(), GUIMachineRadarNT::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADAR_SLOTS_MENU.get(), GUIMachineRadarNTSlots::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRACKING_TOWER_MENU.get(), GUIMachineCrackingTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FRACTION_TOWER_MENU.get(), GUIMachineFractionTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MINING_DRILL_MENU.get(), GUIMachineMiningDrill::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FEL_MENU.get(), GUIMachineFel::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SILEX_MENU.get(), GUIMachineSilex::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.GAS_CENTRIFUGE_MENU.get(), GUIMachineGasCentrifuge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURBINE_MENU.get(), GUIMachineTurbine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.STEAM_TURBINE_MENU.get(), GUIMachineSteamTurbine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SUBSTATION_MENU.get(), GUIMachineSubstation::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRUCIBLE_MENU.get(), GUIMachineCrucible::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ARMOR_TABLE_MENU.get(), GUIArmorTable::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), GUIMachineAssembler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), GUIMachineAdvancedAssembler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_BATTERY_MENU.get(), GUIMachineBattery::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BATTERY_SOCKET_MENU.get(), GUIBatterySocket::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BLAST_FURNACE_MENU.get(), GUIBlastFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.HEATING_OVEN_MENU.get(), GUIHeatingOven::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PRESS_MENU.get(), GUIMachinePress::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SHREDDER_MENU.get(), GUIMachineShredder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ORE_SLOPPER_MENU.get(), GUIMachineOreSlopper::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.COMBINATION_OVEN_MENU.get(), GUIMachineCombinationOven::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ARC_FURNACE_MENU.get(), GUIMachineArcFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.WOOD_BURNER_MENU.get(), GUIMachineWoodBurner::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURRET_MENU.get(), com.hbm_m.inventory.gui.GUITurret::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MISSILE_ASSEMBLY_MENU.get(), com.hbm_m.inventory.gui.GUIMissileAssembly::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ANVIL_MENU.get(), GUIAnvil::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CENTRIFUGE_MENU.get(), GUIMachineCentrifuge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.IRON_CRATE_MENU.get(), GUIIronCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.STEEL_CRATE_MENU.get(), GUISteelCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DESH_CRATE_MENU.get(), GUIDeshCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TUNGSTEN_CRATE_MENU.get(), GUITungstenCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TEMPLATE_CRATE_MENU.get(), GUITemplateCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FLUID_TANK_MENU.get(), GUIMachineFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BAT9000_MENU.get(), com.hbm_m.inventory.gui.GUIBat9000::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_IRON_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelIron::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BARREL_STEEL_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelSteel::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CHEMICAL_PLANT_MENU.get(), GUIMachineChemicalPlant::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOYUZ_LAUNCHER_MENU.get(), com.hbm_m.inventory.gui.GUISoyuzLauncher::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_SATLINKER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSatLinker::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FRACTURING_TOWER_MENU.get(), GUIMachineFrackingTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REFINERY_MENU.get(), GUIMachineRefinery::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LAUNCH_PAD_LARGE_MENU.get(), GUILaunchPadLarge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LAUNCH_PAD_RUSTED_MENU.get(), GUILaunchPadRusted::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_FAT_MAN_MENU.get(), com.hbm_m.inventory.gui.GUINukeFatMan::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.NUKE_PROTOTYPE_MENU.get(), com.hbm_m.inventory.gui.GUINukePrototype::new);
    }

    private static void registerRenderersCommon() {
        // Forge + NeoForge используют один и тот же vanilla API (EntityRenderers.register /
        // BlockEntityRenderers.register) — тело идентично, только imports пакетов различаются.
        ModEntities.SOYUZ.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.SoyuzEntityRenderer::new));
        ModEntities.SOYUZ_CAPSULE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.SoyuzCapsuleEntityRenderer::new));
        ModEntities.ZIRNOX_DEBRIS.ifPresent(entityType -> EntityRenderers.register(entityType, ZirnoxDebrisRenderer::new));
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
        ModEntities.MISSILE_ABM.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
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
        ModEntities.EMP_PULSE.ifPresent(entityType -> EntityRenderers.register(entityType, EmptyEntityRenderer::new));
        ModEntities.BLACK_HOLE.ifPresent(entityType -> EntityRenderers.register(entityType, RenderBlackHole::new));
        ModEntities.VORTEX.ifPresent(entityType -> EntityRenderers.register(entityType, RenderBlackHole::new));
        ModEntities.RAGING_VORTEX.ifPresent(entityType -> EntityRenderers.register(entityType, RenderBlackHole::new));
        ModEntities.DIGAMMA_QUASAR.ifPresent(entityType -> EntityRenderers.register(entityType, RenderQuasar::new));
        ModEntities.RUBBLE.ifPresent(entityType -> EntityRenderers.register(entityType, RubbleEntityRenderer::new));

        BlockEntityRenderers.register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CARGO_ELEVATOR_BE.get(), com.hbm_m.client.render.implementations.CargoElevatorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TRANSITION_SEAL_BE.get(), TransitionSealRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PRESS_BE.get(), MachinePressRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CHEMICAL_PLANT_BE.get(), MachineChemicalPlantRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), MachineHydraulicFrackiningTowerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SOYUZ_LAUNCHER_BE.get(), SoyuzLauncherRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DECO_SOYUZ_ROCKET_BE.get(), com.hbm_m.client.render.implementations.SoyuzRocketRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HEATING_OVEN_BE.get(), HeatingOvenRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CRYSTALLIZER.get(), MachineCrystallizerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), IndustrialTurbineRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BATTERY_SOCKET_BE.get(), BatterySocketCreativeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FLUID_TANK_BE.get(), MachineFluidTankRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BAT9000_BE.get(), MachineFluidTankRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_PAD_BE.get(), LaunchPadMissileRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), LaunchPadMissileRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.COOLING_TOWER_BE.get(), MachineCoolingTowerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.GAS_CENTRIFUGE_BE.get(), GasCentrifugeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MINING_DRILL_BE.get(), com.hbm_m.client.render.implementations.MachineMiningDrillRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ORE_SLOPPER_BE.get(), com.hbm_m.client.render.implementations.MachineOreSlopperRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ARC_FURNACE_BE.get(), com.hbm_m.client.render.implementations.MachineArcFurnaceRenderer::new);
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
        BlockEntityRenderers.register(ModBlockEntities.RADAR_BE.get(), MachineRadarRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RADAR_SCREEN_BE.get(), com.hbm_m.client.render.implementations.MachineRadarScreenRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CRUCIBLE_BE.get(), CrucibleRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FOUNDRY_BASIN_BE.get(), com.hbm_m.client.render.implementations.FoundryBasinRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FOUNDRY_CHANNEL_BE.get(), com.hbm_m.client.render.implementations.FoundryChannelRenderer::new);
        // ─── RBMK column renderers (all use the same generic column renderer) ─────
        BlockEntityRenderers.register(ModBlockEntities.RBMK_ROD_BE.get(),          RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SU47_TROPHY_BE.get(),
                com.hbm_m.client.render.implementations.SU47TrophyRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_BLANK_BE.get(),        RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_ABSORBER_BE.get(),     RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_REFLECTOR_BE.get(),    RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_COOLER_BE.get(),       RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_BOILER_BE.get(),       RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_HEATER_BE.get(),       RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_MODERATOR_BE.get(),    RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_OUTGASSER_BE.get(),    RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_STORAGE_BE.get(),      RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_CONTROL_BE.get(),      RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_CONTROL_AUTO_BE.get(), RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_LOADER_BE.get(),        RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_AUTOLOADER_BE.get(),    RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), RBMKColumnRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_PANEL_BE.get(),         RBMKColumnRenderer::new);
        // Steam inlet/outlet are floor blocks (not columns) — rendered via MODEL + JSON
    }

    private static void registerParticlesCommon() {
        //Particles registered via Forge event below.
        //? if forge {

        //?}

    }

    private static void registerColorsCommon() {
//        Colors registered via Forge events below.
        //? if forge {

        //?}

        
    }

    private static void registerReloadListenersCommon() {
        
    }

    private static void registerHudCommon() {
//        Overlays registered via Forge event below.
        //? if forge {

        //?}

    
    }

    private static void registerWorldRenderHooksCommon() {
        // Forge: ClientModEvents.onRenderLevelStage(AFTER_BLOCK_ENTITIES)

    }

    private static void registerDisconnectHandlerCommon() {
//        Forge uses ClientPlayerNetworkEvent.LoggingOut (see onClientDisconnect).
        //? if forge {

        //?}

        
    }

    private static void clearClientCachesDeferred() {
        com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> {
            com.hbm_m.client.render.culling.InstancedRenderFrame.clear();
            // Свет/окклюжен: ключи включают позицию (без измерения) и identityHashCode
            // рендерера — после выхода из мира это мусор, а occlusion-данные другого
            // измерения по той же позиции дают ложный куллинг до истечения TTL.
            com.hbm_m.client.render.LightSampleCache.invalidateAll();
            com.hbm_m.client.render.culling.OcclusionCullingHelper.clearCache();
            MachineAdvancedAssemblerRenderer.clearCaches();
            MachineAssemblerRenderer.clearCaches();
            MachineHydraulicFrackiningTowerRenderer.clearCaches();
            DoorRenderer.clearAllCaches();
            MachinePressRenderer.clearCaches();
            MachineChemicalPlantRenderer.clearCaches();
            MachineCrystallizerRenderer.clearCaches();
            MachineRadarRenderer.clearCaches();
            MachineFluidTankRenderer.clearCaches();
            MeshRenderCache.clearAll();
            com.hbm_m.client.render.MdiGeometryAtlas.resetForResourceLifecycle();
            AbstractObjArmorLayer.clearAllCaches();
        });
    }

    public static void addTemplatesClient(java.util.function.Consumer<ItemStack> acceptor) {
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(recipeManager, AssemblerRecipe.Type.INSTANCE);

            // Собираем уникальные blueprint_pool из сборочной машины и химзавода
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

            // Создаём папку для каждого уникального пула
            for (String pool : blueprintPools) {
                ItemStack folderStack = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
                ItemBlueprintFolder.writeBlueprintPool(folderStack, pool);
                acceptor.accept(folderStack);
            }

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added {} blueprint folders to NTM Templates tab", blueprintPools.size());
            }

            // Добавляем шаблоны
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
        
        // Получаем объект локации для нашего блока листвы
        Object leavesLocation = PlatformHooks.createModelLocation(ModBlocks.WASTE_LEAVES.getId(), "");

        // Находим оригинальную, "запеченную" модель в регистре
        BakedModel originalModel = (BakedModel) modelRegistry.get(leavesLocation);
        
        // Если модель найдена, заменяем ее на нашу обертку
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

    /*
     * Continuity (через Connector/FFAPI) оборачивает все blockstate-модели в CtmBakedModel
     * (extends ForwardingBakedModel). Это ломает два поведения при активном шейдере:
     *
     * 1. Skin switching - FRAPI emitBlockQuads() не передаёт Forge ModelData, поэтому
     *    DoorBakedModel.getPartsForModelData() не видит выбранного скина.
     * 2. JSON transforms - FRAPI-путь на некоторых версиях Connector не применяет
     *    blockstate-ротации корректно.
     *
     * Решение: в LOWEST-приоритете (после Continuity) разворачиваем обёртки обратно
     * для всех моделей нашего мода, чтобы terrain-рендер использовал vanilla/Forge-путь.
     */

    /*
     * After bake (and after Item Transform Helper, if present): install Forge-safe display wrappers for
     * {@code isCustomRenderer} {@code hbm_m} item models so JSON {@code display} matches with/without ITH.
     */
    //? if forge {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBakingCompletedDisplayGuards(ModelEvent.BakingCompleted event) {
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.installDisplayTransformGuards(
                event.getModelBakery().getBakedTopLevelModels());
    }
    //?}
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModelBakeUnwrapContinuity(ModelEvent.ModifyBakingResult event) {
        // Используем raw Map, чтобы обойти конфликт дженериков ключей между 1.20.1 и 1.21.1
        java.util.Map models = event.getModels();

        // Собираем замены отдельно - не модифицируем map во время итерации
        java.util.Map replacements = new java.util.HashMap<>();

        for (Object entryObj : models.entrySet()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) entryObj;
            ResourceLocation keyLoc = PlatformHooks.getModelId(entry.getKey());
            
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

        // CT-обёртка деко-блоков работает и на forge, и на neoforge
        // (ConnectedDecoBlockBakedModel имеет ветки под оба лоадера).
        wrapConnectedDecoCtTerrainModels(models);

        //? if forge {
        @SuppressWarnings("unchecked")
        Map<ResourceLocation, BakedModel> typedModels = (Map<ResourceLocation, BakedModel>) models;
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.installDisplayTransformGuards(typedModels);
        //?}
    }

    /**
     * Подменяет cube-модели деко-CT на {@link ConnectedDecoBlockBakedModel} (ModelData + getQuads).
     * Делается после снятия Continuity-обёртки, иначе CT не получает корректный пайплайн.
     */
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
        };

        for (CtEntry e : entries) {
            Object loc = PlatformHooks.createModelLocation(e.block.getId(), "");
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) {
                continue;
            }
            
            ResourceLocation full = RefStrings.resourceLocation("block/" + e.textureBase);
            ResourceLocation ct = RefStrings.resourceLocation("block/" + e.textureBase + "_ct");
            models.put(loc, new ConnectedDecoBlockBakedModel(baked, full, ct));
        }
    }

    @SubscribeEvent
    public static void onModelRegisterAdditional(ModelEvent.RegisterAdditional event) {
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/round_airlock_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/crystallizer_fluid"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/crystallizer_spinner"));

        // mining_drill
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/mining_drill_bit"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/mining_drill_shaft"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/mining_drill_crusher1"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/mining_drill_crusher2"));

        // ore_slopper
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/ore_slopper_fan"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/ore_slopper_blades_left"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/ore_slopper_blades_right"));

        // arc_furnace
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/arc_furnace_electrodes_cold"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/machines/arc_furnace_electrodes_hot"));

        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/deco_soyuz_rocket"));

        for (String part : new String[] {
                "chekhov_carriage", "chekhov_carriage_friendly", "chekhov_body", "chekhov_barrels",
                "jeremy_gun", "tauon_cannon", "tauon_rotor", "richard_launcher",
                "howard_carriage", "howard_body", "howard_barrelstop", "howard_barrelsbottom",
                "fritz_gun", "maxwell_microwave",
                "arty_carriage", "arty_cannon", "arty_barrel",
                "himars_carriage", "himars_launcher", "himars_crane",
                "sentry_pivot", "sentry_body", "sentry_drum", "sentry_barrell", "sentry_barrelr"
        }) {
            PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/turret_parts/" + part));
        }

        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/round_airlock_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/round_airlock_door_modern_clean"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/round_airlock_door_modern_green"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/large_vehicle_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/large_vehicle_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/large_vehicle_door_modern_rad"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/large_vehicle_door_modern_clean"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/fire_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/fire_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/fire_door_modern_black"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/fire_door_modern_orange"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/fire_door_modern_trefoil"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/fire_door_modern_yellow"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/secure_access_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/secure_access_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/secure_access_door_modern_gray"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/secure_access_door_modern_yellow"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/secure_access_door_modern_black"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/water_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/water_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/water_door_clean"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/qe_containment_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/qe_containment_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/qe_containment_door_modern_trefoil"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/qe_containment_door_modern_trefoil_yellow"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/qe_sliding_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/qe_sliding_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/sliding_blast_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/sliding_blast_door_modern"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/sliding_blast_door_modern_variant1"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/sliding_blast_door_modern_variant2"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/sliding_seal_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/sliding_seal_door_modern"));

        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/vault_door_skin_2"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/vault_door_skin_81"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/vault_door_skin_87"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/vault_door_skin_99"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/vault_door_skin_101"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/vault_door_skin_106"));
        PlatformHooks.registerAdditionalModel(event, RefStrings.resourceLocation("block/doors/vault_door_skin_111"));

        //? if forge {
        for (MissileItemModelDefinitions.Definition definition : MissileItemModelDefinitions.all()) {
            ResourceLocation meshId = MissileRenderHelper.meshModelId(
                    RefStrings.resourceLocation(definition.itemPath()));
            PlatformHooks.registerAdditionalModel(event, meshId);
        }
        //?}

        MainRegistry.LOGGER.debug("Registered door variant models for loading");
    }

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterGeometryLoaders event) {
        MainRegistry.LOGGER.info("DoorDeclRegistry initialized with {} doors", DoorDeclRegistry.getAll().size());

        // Заменяем все event.register(..) на PlatformHooks.registerGeometryLoader(event, ..)
        PlatformHooks.registerGeometryLoader(event, "advanced_assembly_machine_loader", new MachineAdvancedAssemblerModelLoader());
        PlatformHooks.registerGeometryLoader(event, "chemical_plant_loader", new MachineChemicalPlantModelLoader());
        PlatformHooks.registerGeometryLoader(event, "machine_assembler_loader", new MachineAssemblerModelLoader());
 
        PlatformHooks.registerGeometryLoader(event, "hydraulic_frackining_tower_loader", new MachineHydraulicFrackiningTowerModelLoader());
        PlatformHooks.registerGeometryLoader(event, "fluid_tank_loader", new MachineFluidTankModelLoader());
        PlatformHooks.registerGeometryLoader(event, "battery_socket_loader", new MachineBatterySocketModelLoader());
        PlatformHooks.registerGeometryLoader(event, "door", new DoorModelLoader());
        PlatformHooks.registerGeometryLoader(event, "cargo_elevator", new CargoElevatorModelLoader());
        PlatformHooks.registerGeometryLoader(event, "dae", new DaeModelLoader());
        PlatformHooks.registerGeometryLoader(event, "template_loader", new TemplateModelLoader());
        PlatformHooks.registerGeometryLoader(event, "press_loader", new PressModelLoader());
        PlatformHooks.registerGeometryLoader(event, "missile_loader", new MissileModelLoader());
        PlatformHooks.registerGeometryLoader(event, "heating_oven_loader", new HeatingOvenModelLoader());

        //? if neoforge {
        /*event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "composite"), net.neoforged.neoforge.client.model.CompositeModel.Loader.INSTANCE);
        event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "obj"), net.neoforged.neoforge.client.model.obj.ObjLoader.INSTANCE);
        *///?}
        
        PlatformHooks.registerGeometryLoader(event, "cooling_tower_loader", new MachineCoolingTowerModelLoader());
        PlatformHooks.registerGeometryLoader(event, "radar_loader", new MachineRadarModelLoader());
        PlatformHooks.registerGeometryLoader(event, "soyuz_launcher_loader", new com.hbm_m.client.loader.SoyuzLauncherModelLoader());
        PlatformHooks.registerGeometryLoader(event, "soyuz_rocket_loader", new com.hbm_m.client.loader.SoyuzRocketModelLoader());

        MainRegistry.LOGGER.info("Registered geometry loaders: advanced_assembly_machine_loader, chemical_plant_loader, machine_assembler_loader, hydraulic_frackining_tower_loader, template_loader, door, press_loader, heating_oven_loader, cooling_tower_loader, radar_loader, soyuz_launcher_loader");
    }

    // Key mappings регистрируются в ModConfigKeybindHandler.init() через Architectury.

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            return com.hbm_m.item.liquids.FluidIdentifierItem.getTintColor(stack);
        }, ModItems.FLUID_IDENTIFIER.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            return com.hbm_m.item.liquids.FluidBarrelItem.getTintColor(stack);
        }, ModItems.FLUID_BARREL.get());
        // Fluid Duct - tint overlay layer with fluid color
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            return com.hbm_m.item.liquids.FluidDuctItem.getTintColor(stack);
        }, ModItems.FLUID_DUCT.get(), ModItems.FLUID_DUCT_COLORED.get(), ModItems.FLUID_DUCT_SILVER.get());
        // Mineral Pipes - tint layer0 with the pipe's mineral color
        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof com.hbm_m.item.MineralPipeItem pipe) {
                return pipe.getTintColor();
            }
            return 0xFFFFFF;
        }, ModItems.PIPE_IRON.get(), ModItems.PIPE_COPPER.get(), ModItems.PIPE_GOLD.get(),
           ModItems.PIPE_LEAD.get(), ModItems.PIPE_STEEL.get(), ModItems.PIPE_TUNGSTEN.get(),
           ModItems.PIPE_TITANIUM.get(), ModItems.PIPE_ALUMINUM.get(), ModItems.PIPE_DURA_STEEL.get());
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        net.minecraft.client.color.block.BlockColor sellafiteTint = (state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFF;
            int levelValue = state.getValue(com.hbm_m.block.generic.BlockSellafieldSlaked.COLOR_LEVEL);
            return java.awt.Color.HSBtoRGB(0F, 0F, 1F - levelValue / 15F);
        };
        event.register(sellafiteTint,
                com.hbm_m.block.ModBlocks.SELLAFIELD_BEDROCK.get(),
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_DIAMOND.get(),
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_EMERALD.get(),
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get(),
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get(),
                com.hbm_m.block.ModBlocks.ORE_SELLAFIELD_RADGEM.get());

        // Fluid Duct block - tint with the fluid's color from the BlockEntity
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            if (tintIndex != 1 || level == null || pos == null) return 0xFFFFFF;
            var be = level.getBlockEntity(pos);
            if (be instanceof com.hbm_m.blockentity.machines.FluidDuctBlockEntity ductBe) {
                var fluid = ductBe.getFluidType();
                if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    return com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(fluid);
                }
            }
            return 0xFFFFFF;
        }, com.hbm_m.block.ModBlocks.FLUID_DUCT.get(),
                com.hbm_m.block.ModBlocks.FLUID_DUCT_COLORED.get(),
                com.hbm_m.block.ModBlocks.FLUID_DUCT_SILVER.get());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.AIRNUKEBOMB_PROJECTILE.get(),
                AirNukeBombProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRBOMB_PROJECTILE.get(),
                AirBombProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRSTRIKE_NUKE_ENTITY.get(), AirstrikeNukeEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRSTRIKE_ENTITY.get(), AirstrikeEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRSTRIKE_AGENT_ENTITY.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.NUKE_FALLOUT_RAIN.get(), RenderFallout::new);
        event.registerEntityRenderer(ModEntities.NUKE_MK3.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.TOM_METEOR.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.TOM_BLAST.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.CLOUD_FLEIJA.get(), RenderCloudFleija::new);
        event.registerEntityRenderer(ModEntities.NUKE_MK5.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.FALLING_SELLAFIT_ENTITY_TYPE.get(), FallingBlockRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_TEST.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_ABM.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_MICRO.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_SCHRABIDIUM.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_BHOLE.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_TAINT.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_EMP.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_GENERIC.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_INCENDIARY.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_CLUSTER.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_BUSTER.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_DECOY.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_STEALTH.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_STRONG.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_INCENDIARY_STRONG.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_CLUSTER_STRONG.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_BUSTER_STRONG.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_EMP_STRONG.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_BURST.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_INFERNO.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_RAIN.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_DRILL.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_SHUTTLE.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_NUCLEAR.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_NUCLEAR_CLUSTER.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_VOLCANO.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_DOOMSDAY.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_DOOMSDAY_RUSTED.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CLUSTER_ROCKET.get(), com.hbm_m.client.render.projectile.ClusterRocketEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.EMP_PULSE.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), RenderBlackHole::new);
        event.registerEntityRenderer(ModEntities.VORTEX.get(), RenderBlackHole::new);
        event.registerEntityRenderer(ModEntities.RAGING_VORTEX.get(), RenderBlackHole::new);
        event.registerEntityRenderer(ModEntities.DIGAMMA_QUASAR.get(), RenderQuasar::new);
        event.registerEntityRenderer(ModEntities.RUBBLE.get(), RubbleEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.NOLO.get(), NoloEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ENTITY_MOB_TAINTED_CREEPER.get(), RenderCreeperUniversal::tainted);
        event.registerEntityRenderer(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(), RenderCreeperUniversal::volatileCreeper);
        event.registerEntityRenderer(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(), RenderCreeperUniversal::phosgene);
        event.registerEntityRenderer(ModEntities.ENTITY_MIST.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(), RenderCreeperUniversal::goldCreeper);
        event.registerEntityRenderer(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.get(), RenderCreeperUniversal::nuclear);
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
                // КРИТИЧНО: Откладываем очистку кэшей на render thread, чтобы избежать
                // race condition с активным рендером (EXCEPTION_ACCESS_VIOLATION при
                // включении шейдера - clearCaches вызывался во время render pass).
                com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> {
                    try {
                        com.hbm_m.client.render.MdiBatchCoordinator.discardActiveSessionNoDispatch();
                        MachineAdvancedAssemblerRenderer.clearCaches();
                        MachineAssemblerRenderer.clearCaches();
                        MachineHydraulicFrackiningTowerRenderer.clearCaches();
                        DoorRenderer.clearAllCaches();
                        MachinePressRenderer.clearCaches();
                        MachineChemicalPlantRenderer.clearCaches();
                        MachineCrystallizerRenderer.clearCaches();
                        MachineRadarRenderer.clearCaches();
                        MeshRenderCache.clearAll();
                        // F3+T пересоздаёт рендереры с новыми identityHashCode — ключи
                        // CACHE8/CACHE16 от старых рендереров становятся вечным мусором.
                        com.hbm_m.client.render.LightSampleCache.invalidateAll();
                        com.hbm_m.client.render.MdiGeometryAtlas.resetForResourceLifecycle();
                        AbstractObjArmorLayer.clearAllCaches();
                        MainRegistry.LOGGER.info("VBO cache cleanup completed (deferred to render thread)");
                    } catch (Exception e) {
                        MainRegistry.LOGGER.error("Error during deferred VBO cache cleanup", e);
                    }
                });
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
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRegisterGuiOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        MainRegistry.LOGGER.info("Registering GUI overlays...");
        
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR, com.hbm_m.lib.RefStrings.resourceLocation("geiger_counter_hud"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayGeiger.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
        
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.ARMOR_LEVEL, com.hbm_m.lib.RefStrings.resourceLocation("power_armor_hud"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayPowerArmor.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
        
        event.registerAboveAll(com.hbm_m.lib.RefStrings.resourceLocation("radiation_pixels"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayRadiationVisuals.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
        
        event.registerAboveAll(com.hbm_m.lib.RefStrings.resourceLocation("info_toast"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                OverlayInfoToast.render(guiGraphics, tickDelta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
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
                .put("BoneId", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .put("InstPos", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBboxMin", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstBboxSize", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC45", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC67", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build()
        );
        //?} else {
        /*VertexFormat blockLitInstancedFormat = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("Normal",   com.mojang.blaze3d.vertex.VertexFormatElement.NORMAL)
                .add("UV0",      com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .add("BoneId", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .add("InstPos", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .add("InstRot", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstBboxMin", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .add("InstBboxSize", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC45", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightC67", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build();
        *///?}

        //? if < 1.21.1 {
        VertexFormat blockLitInstancedSlicedFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("BoneId", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .put("InstPos", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBboxMin", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstBboxSize", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS0C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS0C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS1C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS1C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS2C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS2C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS3C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS3C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build()
        );
        //?} else {
        /*VertexFormat blockLitInstancedSlicedFormat = VertexFormat.builder()
                .add("Position", com.mojang.blaze3d.vertex.VertexFormatElement.POSITION)
                .add("Normal",   com.mojang.blaze3d.vertex.VertexFormatElement.NORMAL)
                .add("UV0",      com.mojang.blaze3d.vertex.VertexFormatElement.UV0)
                .add("BoneId", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .add("InstPos", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .add("InstRot", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstBboxMin", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .add("InstBboxSize", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS0C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS0C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS1C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS1C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS2C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS2C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS3C01", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .add("InstLightS3C23", PlatformHooks.createVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build();
        *///?}

        // Both variants share the same .vsh source on disk, but vanilla Program.getOrCreate
        // caches the compiled GL program by NAME ("vertex"/"fragment" string from JSON). If both
        // JSONs reference "hbm_m:block_lit", the first compiled (un-patched) Program would win
        // and the instanced shader would silently be missing #define USE_INSTANCING. To avoid
        // this we expose a separate VIRTUAL vsh name for the instanced variant and let our
        // ResourceProvider wrapper synthesize it from the real source + the define injection.
        ResourceLocation realVsh =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit.vsh");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "shaders/core/block_lit.vsh");
            //?}

        ResourceLocation virtualInstancedVsh =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit_instanced.vsh");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "shaders/core/block_lit_instanced.vsh");
            //?}

        ResourceLocation virtualSlicedVsh =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit_sliced.vsh");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "shaders/core/block_lit_sliced.vsh");
            //?}

        ResourceLocation virtualInstancedSlicedVsh =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit_instanced_sliced.vsh");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "shaders/core/block_lit_instanced_sliced.vsh");
            //?}


        com.hbm_m.client.render.shader.modification.ShaderModification instancingDefine =
            com.hbm_m.client.render.shader.modification.ShaderModification.builder()
                .define("USE_INSTANCING")
                .define("USE_VERTEX_BONE_ID");

        com.hbm_m.client.render.shader.modification.ShaderModification slicedDefine =
            com.hbm_m.client.render.shader.modification.ShaderModification.builder()
                .define("USE_SLICED_LIGHT");

        com.hbm_m.client.render.shader.modification.ShaderModification instancedSlicedDefine =
            com.hbm_m.client.render.shader.modification.ShaderModification.builder()
                .define("USE_INSTANCING")
                .define("USE_SLICED_LIGHT")
                .define("USE_VERTEX_BONE_ID");

        net.minecraft.server.packs.resources.ResourceProvider instancedProvider =
            com.hbm_m.client.render.shader.modification.ShaderPreDefinitions.wrapRedirect(
                event.getResourceProvider(), virtualInstancedVsh, realVsh, instancingDefine);

        net.minecraft.server.packs.resources.ResourceProvider slicedProvider =
            com.hbm_m.client.render.shader.modification.ShaderPreDefinitions.wrapRedirect(
                event.getResourceProvider(), virtualSlicedVsh, realVsh, slicedDefine);

        net.minecraft.server.packs.resources.ResourceProvider instancedSlicedProvider =
            com.hbm_m.client.render.shader.modification.ShaderPreDefinitions.wrapRedirect(
                event.getResourceProvider(), virtualInstancedSlicedVsh, realVsh, instancedSlicedDefine);

        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                //? if fabric && < 1.21.1 {
                /*new ResourceLocation(MainRegistry.MOD_ID, "block_lit_simple"),
                blockLitSimpleFormat
            ),
            ModShaders::setBlockLitSimpleShader
        );
                *///?} else {
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block_lit_simple"),
                blockLitSimpleFormat
            ),
            ModShaders::setBlockLitSimpleShader
        );
                //?}

        MainRegistry.LOGGER.info("Successfully registered block_lit_simple shader");

        event.registerShader(
            new ShaderInstance(
                instancedProvider,
                //? if fabric && < 1.21.1 {
                /*new ResourceLocation(MainRegistry.MOD_ID, "block_lit_instanced"),
                blockLitInstancedFormat
            ),
            ModShaders::setBlockLitInstancedShader
        );
                *///?} else {
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block_lit_instanced"),
                blockLitInstancedFormat
            ),
            ModShaders::setBlockLitInstancedShader
        );
                //?}

        MainRegistry.LOGGER.info("Successfully registered block_lit_instanced shader");

        event.registerShader(
            new ShaderInstance(
                slicedProvider,
                //? if fabric && < 1.21.1 {
                /*new ResourceLocation(MainRegistry.MOD_ID, "block_lit_simple_sliced"),
                blockLitSimpleFormat
            ),
            ModShaders::setBlockLitSimpleSlicedShader
        );
                *///?} else {
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block_lit_simple_sliced"),
                blockLitSimpleFormat
            ),
            ModShaders::setBlockLitSimpleSlicedShader
        );
                //?}

        MainRegistry.LOGGER.info("Successfully registered block_lit_simple_sliced shader");

        event.registerShader(
            new ShaderInstance(
                instancedSlicedProvider,
                //? if fabric && < 1.21.1 {
                /*new ResourceLocation(MainRegistry.MOD_ID, "block_lit_instanced_sliced"),
                blockLitInstancedFormat
            ),
            ModShaders::setBlockLitInstancedSlicedShader
        );
                *///?} else {
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block_lit_instanced_sliced"),
                blockLitInstancedSlicedFormat
            ),
            ModShaders::setBlockLitInstancedSlicedShader
        );
                //?}

        MainRegistry.LOGGER.info("Successfully registered block_lit_instanced_sliced shader");
        
        // Register thermal vision shader for post-processing
        // VertexFormat thermalVisionFormat = new VertexFormat(
        //     ImmutableMap.<String, VertexFormatElement>builder()
        //         .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
        //         .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
        //         .build()
        // );
        
        // ResourceLocation shaderLocation = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "thermal_vision");
        // MainRegistry.LOGGER.info("Attempting to register thermal_vision shader at: {}", shaderLocation);
        
        // ShaderInstance shaderInstance = null;
        // try {
        //     shaderInstance = new ShaderInstance(
        //         event.getResourceProvider(),
        //         shaderLocation,
        //         thermalVisionFormat
        //     );
        //     MainRegistry.LOGGER.info("ShaderInstance created successfully for thermal_vision");
        // } catch (Exception e) {
        //     MainRegistry.LOGGER.error("Exception while creating ShaderInstance for thermal_vision: {}", e.getMessage(), e);
        //     return; // Don't register if creation failed
        // }
        
        // if (shaderInstance != null) {
        //     event.registerShader(shaderInstance, ModShaders::setThermalVisionShader);
        //     MainRegistry.LOGGER.info("thermal_vision shader registered with event handler");
            
        //     // Note: The callback ModShaders::setThermalVisionShader is called asynchronously,
        //     // so we can't verify it here immediately. The shader will be available after reload.
        // } else {
        //     MainRegistry.LOGGER.error("ShaderInstance is null after creation - cannot register thermal_vision shader!");
        // }
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
        // Generic dummy armor model for all power armor items.
        event.registerLayerDefinition(ModModelLayers.POWER_ARMOR, PowerArmorEmptyModel::createBodyLayer);
    }
    
}
