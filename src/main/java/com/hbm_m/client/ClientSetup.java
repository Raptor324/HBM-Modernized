package com.hbm_m.client;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.particle.custom.MissileContrailParticle;
import com.hbm_m.particle.custom.RadFogParticle;
import com.hbm_m.particle.custom.SchrabfogParticle;
import com.hbm_m.particle.custom.TownauraParticle;
import com.hbm_m.particle.ModParticleTypes;
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
import com.hbm_m.client.render.implementations.CrucibleRenderer;
import com.hbm_m.client.render.implementations.SoyuzRocketRenderer;
import com.hbm_m.client.render.implementations.MachineCrystallizerRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.SoyuzLauncherRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.client.render.implementations.MachineRadarRenderer;
import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.client.render.implementations.MissileEntityRenderer;
import com.hbm_m.client.render.entity.mob.RenderCreeperUniversal;
import com.hbm_m.client.render.implementations.NoloEntityRenderer;
// import com.hbm_m.client.render.implementations.TestBlockRenderer;
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
import com.hbm_m.inventory.gui.GUIRBMKHeater;
import com.hbm_m.inventory.gui.GUIRBMKStorage;
import com.hbm_m.inventory.gui.GUIRBMKAutoloader;
import com.hbm_m.inventory.gui.GUIRBMKOutgasser;
import com.hbm_m.inventory.gui.GUIMachineSilex;
import com.hbm_m.inventory.gui.GUIMachineSolderingStation;
import com.hbm_m.inventory.gui.GUIMachineSubstation;
import com.hbm_m.inventory.gui.GUIMachineSteamTurbine;
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
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.particle.explosions.basic.CameraShakeHandler;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.powerarmor.layer.AbstractObjArmorLayer;
import com.hbm_m.powerarmor.layer.ModModelLayers;
import com.hbm_m.powerarmor.layer.PowerArmorEmptyModel;
import com.hbm_m.powerarmor.overlay.HbmThermalHandler;
import com.hbm_m.powerarmor.overlay.OverlayPowerArmor;
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

//? if forge {
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
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
import com.hbm_m.client.render.missile.MissileItemModelDefinitions;
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
import com.hbm_m.client.render.missile.MissileItemModelDefinitions;
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
        if (com.hbm_m.compat.dh.DhCompat.isModPresent()) {
            com.hbm_m.client.compat.dh.DhRenderBridge.tryRegister();
        }
        ClientModEvents.init();
        com.hbm_m.client.missile.track.MissileTrackClientEvents.register();
        CameraShakeHandler.initClient();

        // MOTD при входе в мир — решение принимает клиент (client.json -> enableMOTD).
        com.hbm_m.client.ClientMotdHandler.register();

        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            com.hbm_m.config.ModClothConfig.reloadServer();
            ClientRadiationData.clearAll();
        });

        // Экраны меню: на Forge регистрируются напрямую, на NeoForge 1.21.1+ — через RegisterMenuScreensEvent ниже.
        // neo-pendant: onRegisterMenuScreens (RegisterMenuScreensEvent)
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

    // Item-Client-Extensions (Power-Armor, FSB, Waffen-BEWLR ...) registrieren sich auf NeoForge wie auf Forge
    // ueber Item.initializeClient im neoforge-Zweig der Item-Klasse (siehe VERSIONPORT.md, Abschnitt 8).
    // Hier zusaetzlich zu registrieren wuerde auf NeoForge mit "Duplicate client extensions registration" abbrechen.
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
        // Original ClientCommandHandler: /ntmclient, /dumpthreadsandcrashgame
        com.hbm_m.commands.HbmClientCommandsNT.register(event.getDispatcher());
    }

    @SuppressWarnings("removal")
    private static void registerRenderLayers() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.TRANSITION_SEAL.get(), RenderType.cutout());
        // Original: BlockGasClorine.getRenderBlockPass() == 1 - Chlorgas ist der einzige Gasblock,
        // der ueberhaupt gerendert wird, und zwar durchscheinend.
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.CHLORINE_GAS.get(), RenderType.translucent());
        // Welt-Fluessigkeiten: BlockFluidBase.getRenderBlockPass() == 1 (durchscheinend), ausser den
        // Bloecken, die im Original auf Pass 0 ueberschreiben (Schrabidiumsaeure, beide Laven).
        for (var e : new com.hbm_m.inventory.fluid.WorldFluids.Entry[] {
                com.hbm_m.inventory.fluid.WorldFluids.MUD, com.hbm_m.inventory.fluid.WorldFluids.ACID,
                com.hbm_m.inventory.fluid.WorldFluids.TOXIC, com.hbm_m.inventory.fluid.WorldFluids.SULFURIC_ACID }) {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(e.getSource(), RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(e.getFlowing(), RenderType.translucent());
        }
    }

    /**
     * Original ItemAlexandrite.TextureAlexandrite: das Bild (Frame) = Blocklicht an der Spielerposition (0-15),
     * gilt fuer jeden Alexandrit gleichzeitig.
     */
    /**
     * R4: Coltan-Kompass (TextureColtass: Bild nach Nadelwinkel, hbm_m:angle = Bild / 32) und Werkzeugkiste
     * (getIcon: offene Kiste solange "isOpen").
     */
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
                (stack, level, entity, seed) -> StackNbt.has(stack) && StackNbt.read(stack).getBoolean("isOpen") ? 1F : 0F);
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
                    (stack, level, entity, seed) -> StackNbt.has(stack) && StackNbt.read(stack).getShort("fill") > 0 ? 1F : 0F);
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

    private static void registerRadAbsorberItemProperties() {
        net.minecraft.client.renderer.item.ItemProperties.register(
                ModBlocks.RAD_ABSORBER.get().asItem(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "tier"),
                (stack, level, entity, seed) -> BlockAbsorberItem.readTier(stack).ordinal()
        );
    }

    private static void registerScreens() {
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRYSTALLIZER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCrystallizer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RTG_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRTG::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FORCE_FIELD_MENU.get(), com.hbm_m.inventory.gui.GUIForceField::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_TUBE_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoTube::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_ACCESS_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageAccess::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_CLUTTER_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageClutter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_MONO_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageMono::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_IMPORTER_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageImporter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PNEUMO_STORAGE_EXPORTER_MENU.get(), com.hbm_m.inventory.gui.GUIPneumoStorageExporter::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DFC_CORE_MENU.get(), com.hbm_m.inventory.gui.GUIDFCCore::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DFC_STABILIZER_MENU.get(), com.hbm_m.inventory.gui.GUIDFCStabilizer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_ICF_MENU.get(), com.hbm_m.inventory.gui.GUIMachineICF::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_ICF_PRESS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineICFPress::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_SOURCE_MENU.get(), com.hbm_m.inventory.gui.GUIPASource::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_DETECTOR_MENU.get(), com.hbm_m.inventory.gui.GUIPADetector::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_QUADRUPOLE_MENU.get(), com.hbm_m.inventory.gui.GUIPAQuadrupole::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_DIPOLE_MENU.get(), com.hbm_m.inventory.gui.GUIPADipole::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PA_RFC_MENU.get(), com.hbm_m.inventory.gui.GUIPARFC::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REACTOR_CONTROL_MENU.get(), com.hbm_m.inventory.gui.GUIReactorControl::new);
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
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFirebox<com.hbm_m.inventory.menu.MachineFireboxMenu>::new);
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
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_TORUS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionTorus::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_KLYSTRON_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionKlystron::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_BREEDER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionBreeder::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FUSION_PLASMA_FORGE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFusionPlasmaForge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PUMPJACK_MENU.get(), GUIMachinePumpjack::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADAR_MENU.get(), GUIMachineRadarNT::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.RADAR_SLOTS_MENU.get(), GUIMachineRadarNTSlots::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRACKING_TOWER_MENU.get(), GUIMachineCrackingTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FRACTION_TOWER_MENU.get(), GUIMachineFractionTower::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MINING_DRILL_MENU.get(), GUIMachineMiningDrill::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FEL_MENU.get(), GUIMachineFel::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SILEX_MENU.get(), GUIMachineSilex::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.GAS_CENTRIFUGE_MENU.get(), GUIMachineGasCentrifuge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LARGE_TURBINE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineLargeTurbine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURBINEGAS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineTurbineGas::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.STEAM_TURBINE_MENU.get(), GUIMachineSteamTurbine::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SUBSTATION_MENU.get(), GUIMachineSubstation::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CRUCIBLE_MENU.get(), GUIMachineCrucible::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ARMOR_TABLE_MENU.get(), GUIArmorTable::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), GUIMachineAssembler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), GUIMachineAdvancedAssembler::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_DIFURNACE_RTG_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDifurnaceRtg::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MACHINE_RTG_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIRtgFurnace::new);
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
        MenuRegistry.registerScreenFactory(ModMenuTypes.PRECASS_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePrecAss::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.WOOD_BURNER_MENU.get(), GUIMachineWoodBurner::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TURRET_MENU.get(), com.hbm_m.inventory.gui.GUITurret::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.MISSILE_ASSEMBLY_MENU.get(), com.hbm_m.inventory.gui.GUIMissileAssembly::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CUSTOM_LAUNCHER_MENU.get(), com.hbm_m.inventory.gui.GUICustomLauncher::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CUSTOM_MACHINE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCustom::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ANVIL_MENU.get(), GUIAnvil::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CENTRIFUGE_MENU.get(), GUIMachineCentrifuge::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.IRON_CRATE_MENU.get(), GUIIronCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FILE_CABINET_MENU.get(), com.hbm_m.inventory.gui.GUIFileCabinet::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.STEEL_CRATE_MENU.get(), GUISteelCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CART_CRATE_MENU.get(), com.hbm_m.inventory.gui.GUICartScreens.Crate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.CART_DESTROYER_MENU.get(), com.hbm_m.inventory.gui.GUICartScreens.Destroyer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TRAIN_CARGO_TRAM_MENU.get(), com.hbm_m.inventory.gui.GUITrainCargoTram::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TRAIN_CARGO_TRAM_TRAILER_MENU.get(), com.hbm_m.inventory.gui.GUITrainCargoTramTrailer::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.DESH_CRATE_MENU.get(), GUIDeshCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TUNGSTEN_CRATE_MENU.get(), GUITungstenCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SAFE_MENU.get(), com.hbm_m.inventory.gui.GUISafe::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.TEMPLATE_CRATE_MENU.get(), GUITemplateCrate::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FLUID_TANK_MENU.get(), GUIMachineFluidTank::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BAT9000_MENU.get(), com.hbm_m.inventory.gui.GUIBat9000::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BOOK_MENU.get(), com.hbm_m.inventory.gui.GUIBook::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.HELD_ITEM_MENU.get(), com.hbm_m.inventory.gui.GUIHeldItem::new);
        // SEDNA: Waffenmodifikationstisch + Huelsen-Partikel ("casingNT")
        MenuRegistry.registerScreenFactory(ModMenuTypes.WEAPON_TABLE_MENU.get(), com.hbm_m.inventory.gui.GUIWeaponTable::new);
        com.hbm_m.client.particle.CasingClientCreator.register();
        MenuRegistry.registerScreenFactory(ModMenuTypes.REBAR_MENU.get(), com.hbm_m.inventory.gui.GUIRebar::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SAT_DOCK_MENU.get(), com.hbm_m.inventory.gui.GUISatDock::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.LEMEGETON_MENU.get(), com.hbm_m.inventory.gui.GUILemegeton::new);
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
        MenuRegistry.registerScreenFactory(ModMenuTypes.ASSEMBLY_FACTORY_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAssemblyFactory::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOYUZ_LAUNCHER_MENU.get(), com.hbm_m.inventory.gui.GUISoyuzLauncher::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.SOYUZ_CAPSULE_MENU.get(), com.hbm_m.inventory.gui.GUISoyuzCapsule::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.BATTERY_REDD_MENU.get(), com.hbm_m.inventory.gui.GUIBatteryREDD::new);
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
        MenuRegistry.registerScreenFactory(ModMenuTypes.ELECTROLYSER_FLUID_MENU.get(), com.hbm_m.inventory.gui.GUIElectrolyserFluid::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ELECTROLYSER_METAL_MENU.get(), com.hbm_m.inventory.gui.GUIElectrolyserMetal::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.COMPRESSOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCompressor::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineElectricFurnace::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.FURNACE_BRICK_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFurnaceBrick::new);
    }

    private static void registerRenderersCommon() {

        ModEntities.SOYUZ.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.SoyuzEntityRenderer::new));
        ModEntities.SOYUZ_CAPSULE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.SoyuzCapsuleEntityRenderer::new));
        ModEntities.ARTILLERY_SHELL.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.projectile.RenderArtilleryShell::new));
        ModEntities.ARTILLERY_ROCKET.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.projectile.RenderArtilleryRocket::new));
        ModEntities.ZIRNOX_DEBRIS.ifPresent(entityType -> EntityRenderers.register(entityType, ZirnoxDebrisRenderer::new));
        ModEntities.RBMK_DEBRIS.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.rbmk.RBMKDebrisRenderer::new));
        ModEntities.MOVING_CONVEYOR_ITEM.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.MovingConveyorRenderers.Item::new));
        ModEntities.MOVING_CONVEYOR_PACKAGE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.MovingConveyorRenderers.Package::new));
        ModEntities.DELIVERY_DRONE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.DeliveryDroneRenderer::new));
        ModEntities.REQUEST_DRONE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.DeliveryDroneRenderer::new));
        ModEntities.TURRET_BULLET.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.TURRET_ROCKET.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_NUC_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_FIRE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_SLIME_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_HE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_BOUNCY_GENERIC.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.DISPERSER_CANISTER.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.TNT_PRIMED_BASE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.TNTPrimedBaseRenderer::new));
        ModEntities.CHLORINE_FX.ifPresent(t -> EntityRenderers.register(t, c -> new com.hbm_m.client.render.entity.MultiCloudRenderer<>(c, "chlorine")));
        ModEntities.PINK_CLOUD_FX.ifPresent(t -> EntityRenderers.register(t, c -> new com.hbm_m.client.render.entity.MultiCloudRenderer<>(c, "pc")));
        ModEntities.CLOUD_FX.ifPresent(t -> EntityRenderers.register(t, c -> new com.hbm_m.client.render.entity.MultiCloudRenderer<>(c, "cloud")));
        ModEntities.ORANGE_FX.ifPresent(t -> EntityRenderers.register(t, c -> new com.hbm_m.client.render.entity.MultiCloudRenderer<>(c, "orange")));
        ModEntities.EMP_BLAST.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.EMPBlastRenderer::new));
        ModEntities.FIREWORKS.ifPresent(entityType -> EntityRenderers.register(entityType, EmptyEntityRenderer::new));
        ModEntities.DEATH_BLAST.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.OrbitalStrikeRenderer::deathBlast));
        ModEntities.ORBITAL_LASER.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.OrbitalStrikeRenderer::orbitalLaser));
        ModEntities.MINER_ROCKET.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.MinerRocketRenderer::new));
        ModEntities.BOBMAZON.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.BobmazonRenderer::new));
        ModEntities.METEOR.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.MeteorRenderer::new));
        ModEntities.BOXCAR.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.BoxcarRenderer::new));
        ModEntities.DUCHESS_GAMBIT.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.FallingProjectileRenderer::new));
        ModEntities.BUILDING.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.FallingProjectileRenderer::new));
        ModEntities.TORPEDO.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.FallingProjectileRenderer::new));
        ModEntities.SIEGE_LASER.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.projectile.SiegeLaserRenderer::new));
        ModEntities.FOG_FX.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.FogRenderer::new));
        ModEntities.WASTE_PEARL.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.WhiteBoxRenderer::new));
        ModEntities.UNDEAD_SOLDIER.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.UndeadSoldierRenderer::new));
        ModEntities.SIEGE_CRAFT.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.mob.SiegeCraftRenderer::new));
        ModEntities.SIEGE_TUNNELER.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.mob.SiegeTunnelerRenderer::new));
        ModEntities.BOAT_RUBBER.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.implementations.BoatRubberRenderer::new));
        ModEntities.ITEM_BUOYANT.ifPresent(entityType -> EntityRenderers.register(entityType, net.minecraft.client.renderer.entity.ItemEntityRenderer::new));
        ModEntities.GRENADEHE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADEFIRE_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADESMART_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADESLIME_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_PROJECTILE.ifPresent(entityType -> EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.MISSILE_CUSTOM.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.entity.MissileCustomRenderer::new));
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
        ModEntities.MISSILE_9M723.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_9M723_BUSTER.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_TOPOL.ifPresent(entityType -> EntityRenderers.register(entityType, MissileEntityRenderer::new));
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
        ModEntities.DIGAMMA_SPEAR.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.SpearRenderer::new));
        ModEntities.RUBBLE.ifPresent(entityType -> EntityRenderers.register(entityType, RubbleEntityRenderer::new));
        ModEntities.SHRAPNEL.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.ShrapnelRenderer::new));
        ModEntities.COG.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.CogRenderer::new));
        ModEntities.SAWBLADE.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.SawbladeRenderer::new));

        // Сеть длинной ЛЭП: один рендерер кабелей для всех пилонов/коннекторов.
        {
            net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<com.hbm_m.blockentity.network.PylonBaseBlockEntity> wireRenderer =
                    com.hbm_m.client.render.implementations.RedPylonWireRenderer::new;
            BlockEntityRenderers.register(ModBlockEntities.FORCE_FIELD_BE.get(),
                    com.hbm_m.client.render.implementations.ForceFieldRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.RAIL_SWITCH.get(),
                    com.hbm_m.client.render.implementations.RailSwitchRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PNEUMO_TUBE_PAINTABLE_BE.get(),
                    com.hbm_m.client.render.implementations.PneumoTubePaintableRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.RED_CONNECTOR_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.MISSILE_ASSEMBLY_BE.get(), com.hbm_m.client.render.implementations.MissileAssemblyRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.COMPACT_LAUNCHER_BE.get(), com.hbm_m.client.render.implementations.CustomLauncherRenderers.Compact::new);
            BlockEntityRenderers.register(ModBlockEntities.LAUNCH_TABLE_BE.get(), com.hbm_m.client.render.implementations.CustomLauncherRenderers.Table::new);
            BlockEntityRenderers.register(ModBlockEntities.STRUCT_LAUNCHER_CORE_BE.get(), com.hbm_m.client.render.implementations.CustomLauncherRenderers.Struct::new);
            BlockEntityRenderers.register(ModBlockEntities.CUSTOM_MACHINE_BE.get(), com.hbm_m.client.render.implementations.CustomMachineRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FLOODLIGHT.get(), com.hbm_m.client.render.implementations.FloodlightRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LANTERN.get(), com.hbm_m.client.render.implementations.LanternRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DECO_BLOCK.get(), com.hbm_m.client.render.implementations.DecoBlockRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DECO_EMITTER.get(), com.hbm_m.client.render.implementations.EmitterRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SKELETON_HOLDER.get(), com.hbm_m.client.render.implementations.SkeletonHolderRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.WAND_STRUCTURE.get(), com.hbm_m.client.render.implementations.WandStructureRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LOGIC_BLOCK.get(), com.hbm_m.client.render.implementations.LogicBlockRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.VENDING_MACHINE.get(), com.hbm_m.client.render.implementations.VendingMachineRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.TRINKET.get(), com.hbm_m.client.render.implementations.TrinketRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.OBJ_TESTER.get(), com.hbm_m.client.render.implementations.ObjTesterRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FILE_CABINET.get(), com.hbm_m.client.render.implementations.FileCabinetRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.STATUE_ELB_F.get(), com.hbm_m.client.render.implementations.StatueRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CHARGE.get(), com.hbm_m.client.render.implementations.ChargeRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DEMON_LAMP.get(), com.hbm_m.client.render.implementations.DemonLampRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.THRESHER_BE.get(), com.hbm_m.client.render.implementations.ThresherRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.AUTOSAW_BE.get(), com.hbm_m.client.render.implementations.AutosawRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PYROOVEN_BE.get(), com.hbm_m.client.render.implementations.PyroOvenRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ANNIHILATOR_BE.get(), com.hbm_m.client.render.implementations.AnnihilatorRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.BOILER_BE.get(), com.hbm_m.client.render.implementations.BoilerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SOLAR_BOILER_BE.get(), com.hbm_m.client.render.implementations.SolarBoilerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SOLAR_MIRROR_BE.get(), com.hbm_m.client.render.implementations.SolarMirrorRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.COMBUSTION_ENGINE_BE.get(), com.hbm_m.client.render.implementations.CombustionEngineRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.COMPRESSOR_BE.get(), com.hbm_m.client.render.implementations.CompressorRenderer.Large::new);
            BlockEntityRenderers.register(ModBlockEntities.COMPRESSOR_COMPACT_BE.get(), com.hbm_m.client.render.implementations.CompressorRenderer.Compact::new);
            BlockEntityRenderers.register(ModBlockEntities.CONDENSER_POWERED_BE.get(), com.hbm_m.client.render.implementations.CondenserPoweredRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.EXPOSURE_CHAMBER_BE.get(), com.hbm_m.client.render.implementations.ExposureChamberRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FURNACE_STEEL_BE.get(), com.hbm_m.client.render.implementations.FurnaceSteelRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.HEPHAESTUS_BE.get(), com.hbm_m.client.render.implementations.HephaestusRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LARGE_TURBINE_BE.get(), com.hbm_m.client.render.implementations.LargeTurbineRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LPW2_BE.get(), com.hbm_m.client.render.implementations.Lpw2Renderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MINING_LASER_BE.get(), com.hbm_m.client.render.implementations.MiningLaserRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MACHINE_PUMP_STEAM_BE.get(), com.hbm_m.client.render.implementations.PumpRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MACHINE_PUMP_ELECTRIC_BE.get(), com.hbm_m.client.render.implementations.PumpRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PUREX_BE.get(), com.hbm_m.client.render.implementations.PurexRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PRECASS_BE.get(), com.hbm_m.client.render.implementations.MachinePrecAssRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SOYUZ_STRUCT_BE.get(), com.hbm_m.client.render.implementations.StructSoyuzCoreRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.RADGEN_BE.get(), com.hbm_m.client.render.implementations.RadGenRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.RADIOLYSIS_BE.get(), com.hbm_m.client.render.implementations.RadiolysisRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SOLIDIFIER_BE.get(), com.hbm_m.client.render.implementations.SolidifierRenderer::solidifier);
            BlockEntityRenderers.register(ModBlockEntities.LIQUEFACTOR_BE.get(), com.hbm_m.client.render.implementations.SolidifierRenderer::liquefactor);
            BlockEntityRenderers.register(ModBlockEntities.STEAM_ENGINE_BE.get(), com.hbm_m.client.render.implementations.SteamEngineRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.TURBINEGAS_BE.get(), com.hbm_m.client.render.implementations.TurbineGasRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CRACKING_TOWER_BE.get(), com.hbm_m.client.render.implementations.CatalyticCrackerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FRACTION_TOWER_BE.get(), com.hbm_m.client.render.implementations.FractionTowerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.VACUUM_DISTILL_BE.get(), com.hbm_m.client.render.implementations.VacuumDistillRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FEL_BE.get(), com.hbm_m.client.render.implementations.FelRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SILEX_BE.get(), com.hbm_m.client.render.implementations.SilexRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CHIMNEY_BE.get(), com.hbm_m.client.render.implementations.ChimneyRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.AMMO_PRESS_BE.get(), com.hbm_m.client.render.implementations.AmmoPressRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.COMBINATION_OVEN_BE.get(), com.hbm_m.client.render.implementations.CombinationOvenRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ORE_SLOPPER_BE.get(), com.hbm_m.client.render.implementations.MachineOreSlopperRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.REACTOR_RESEARCH_BE.get(), com.hbm_m.client.render.implementations.ReactorResearchRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SAWMILL_BE.get(), com.hbm_m.client.render.implementations.SawmillRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.TURBOFAN_BE.get(), com.hbm_m.client.render.implementations.TurbofanRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.RADIOBOX_BE.get(), com.hbm_m.client.render.implementations.RadioboxRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MINING_DRILL_BE.get(), com.hbm_m.client.render.implementations.MachineMiningDrillRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SLAG_BE.get(), com.hbm_m.client.render.implementations.SlagRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ROTARY_FURNACE_BE.get(), com.hbm_m.client.render.implementations.RotaryFurnaceRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ELECTROLYSER_BE.get(), com.hbm_m.client.render.implementations.ElectrolyserRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ARC_FURNACE_BE.get(), com.hbm_m.client.render.implementations.ArcFurnaceRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FAN_BE.get(), com.hbm_m.client.render.implementations.FanRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.BATTERY_REDD_BE.get(), com.hbm_m.client.render.implementations.BatteryREDDRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MACHINE_FENSU_BE.get(), com.hbm_m.client.render.implementations.FENSURenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ASSEMBLY_FACTORY_BE.get(), com.hbm_m.client.render.implementations.AssemblyFactoryRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.INTAKE_BE.get(), com.hbm_m.client.render.implementations.IntakeRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.NUKE_FSTBMB_BE.get(), com.hbm_m.client.render.implementations.NukeFstbmbRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SOYUZ_CAPSULE_BE.get(), com.hbm_m.client.render.implementations.SoyuzCapsuleRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PISTON_INSERTER_BE.get(), com.hbm_m.client.render.implementations.PistonInserterRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.REFUELER_BE.get(), com.hbm_m.client.render.implementations.RefuelerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LANTERN_BEHEMOTH_BE.get(), com.hbm_m.client.render.implementations.LanternBehemothRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.STRAND_CASTER_BE.get(), com.hbm_m.client.render.implementations.StrandCasterRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CRASHED_BOMB.get(), com.hbm_m.client.render.implementations.CrashedBombRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.RED_CONNECTOR_SUPER_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_PYLON_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_PYLON_MEDIUM_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_PYLON_LARGE_BE.get(), wireRenderer);
            BlockEntityRenderers.register(ModBlockEntities.RED_CABLE_PAINTABLE_BE.get(), com.hbm_m.client.render.implementations.RedCablePaintableRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FLUID_DUCT_PAINTABLE.get(), com.hbm_m.client.render.implementations.PaintableDuctRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PIPE_ANCHOR.get(), com.hbm_m.client.render.implementations.PipeAnchorRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.BLAST_DOOR.get(), com.hbm_m.client.render.implementations.BlastDoorRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.STRUCT_WATZ_CORE.get(), com.hbm_m.client.render.implementations.StructWatzCoreRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ICF_STRUCT_BE.get(), com.hbm_m.client.render.implementations.ICFStructRenderer::new);
        }

        MachineAdvancedAssemblerRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.CARGO_ELEVATOR_BE.get(), com.hbm_m.client.render.implementations.CargoElevatorRenderer::new);
        MachineAssemblerRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TRANSITION_SEAL_BE.get(), TransitionSealRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PEDESTAL_BE.get(), com.hbm_m.client.render.implementations.PedestalRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DECO_LOOT_BE.get(), com.hbm_m.client.render.implementations.DecoLootRenderer::new);
        MachinePressRenderer.register();
        MachineChemicalPlantRenderer.register();
        MachineHydraulicFrackiningTowerRenderer.register();
        SoyuzLauncherRenderer.register();
        SoyuzRocketRenderer.register();
        com.hbm_m.client.render.implementations.DynamicMachineRenderers.register(); // Audit 6: vorher nur statische Modelle
        BlockEntityRenderers.register(ModBlockEntities.HEATING_OVEN_BE.get(), HeatingOvenRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FIREBOX_BE.get(), com.hbm_m.client.render.implementations.FireboxRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CHEMICAL_FACTORY_BE.get(), com.hbm_m.client.render.implementations.ChemicalFactoryRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_CHUNGUS_BE.get(), com.hbm_m.client.render.implementations.ChungusRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MIXER_BE.get(), com.hbm_m.client.render.implementations.MixerRenderer::new);
        // BlockEntityRenderers.register(ModBlockEntities.TEST_BE.get(), TestBlockRenderer::new);
        MachineCrystallizerRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), IndustrialTurbineRenderer::new);
        // Audit 6: 1:1 RenderBatterySocket (Stuetzen, Sonnenpony, drehende Batterie) statt der freien Spielerfigur
        BlockEntityRenderers.register(ModBlockEntities.BATTERY_SOCKET_BE.get(), com.hbm_m.client.render.implementations.BatterySocketRenderer::new);
        MachineFluidTankRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_PAD_BE.get(), LaunchPadMissileRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MOBILE_LAUNCH_PAD_BE.get(), com.hbm_m.client.render.implementations.MobileLaunchPadMissileRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TOPOL_LAUNCH_PAD_BE.get(), com.hbm_m.client.render.implementations.TopolLauncherRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), LaunchPadMissileRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.COOLING_TOWER_BE.get(), com.hbm_m.client.render.implementations.CoolingTowerRenderer::large);
        BlockEntityRenderers.register(ModBlockEntities.TOWER_SMALL_BE.get(), com.hbm_m.client.render.implementations.CoolingTowerRenderer::small);
        BlockEntityRenderers.register(ModBlockEntities.GAS_CENTRIFUGE_BE.get(), GasCentrifugeRenderer::new);


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
        BlockEntityRenderers.register(ModBlockEntities.TURRET_SENTRY_DAMAGED_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TURRET_HOWARD_DAMAGED_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        MachineRadarRenderer.register();
        BlockEntityRenderers.register(ModBlockEntities.RADAR_SCREEN_BE.get(), com.hbm_m.client.render.implementations.MachineRadarScreenRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CRUCIBLE_BE.get(), CrucibleRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FOUNDRY_BASIN_BE.get(), com.hbm_m.client.render.implementations.FoundryBasinRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FOUNDRY_MOLD_BE.get(), com.hbm_m.client.render.implementations.FoundryBasinRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FOUNDRY_CHANNEL_BE.get(), com.hbm_m.client.render.implementations.FoundryChannelRenderer::new);
        // ─── RBMK column renderers ─────
        // ===== Fusionsreaktor =====
        BlockEntityRenderers.register(ModBlockEntities.FUSION_TORUS_BE.get(),
                com.hbm_m.client.render.implementations.FusionTorusRenderer::new);
        // Bauplan des Torus: zeichnet das 15x15x5-Muster als durchscheinende Miniwuerfel um den
        // Kern, damit man den Reaktor ueberhaupt von Hand bauen kann (RenderFusionTorusMultiblock).
        BlockEntityRenderers.register(ModBlockEntities.STRUCT_TORUS_CORE_BE.get(),
                com.hbm_m.client.render.implementations.StructTorusCoreRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SATLINK_BE.get(),
                com.hbm_m.client.render.implementations.SatLinkRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.REBAR_BE.get(),
                com.hbm_m.client.render.implementations.RebarRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FUSION_KLYSTRON_BE.get(),
                com.hbm_m.client.render.implementations.FusionKlystronRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FUSION_KLYSTRON_CREATIVE_BE.get(),
                com.hbm_m.client.render.implementations.FusionKlystronCreativeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FUSION_MHDT_BE.get(),
                com.hbm_m.client.render.implementations.FusionMhdtRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FUSION_PLASMA_FORGE_BE.get(),
                com.hbm_m.client.render.implementations.FusionPlasmaForgeRenderer::new);

        BlockEntityRenderers.register(ModBlockEntities.RBMK_ROD_BE.get(),          RBMKColumnRenderer::new);
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
        BlockEntityRenderers.register(ModBlockEntities.RBMK_AUTOLOADER_BE.get(),    com.hbm_m.client.render.rbmk.RBMKAutoloaderRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), com.hbm_m.client.render.rbmk.RBMKCraneConsoleRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_PANEL_BE.get(),         RBMKColumnRenderer::new);
        // The 7 RTTY panel devices each get their own renderer, ported 1:1 from the original's
        // RenderRBMK* tile entity special renderers.
        BlockEntityRenderers.register(ModBlockEntities.RBMK_DISPLAY_BE.get(),   com.hbm_m.client.render.rbmk.RBMKDisplayRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_GAUGE_BE.get(),     com.hbm_m.client.render.rbmk.RBMKGaugeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_INDICATOR_BE.get(), com.hbm_m.client.render.rbmk.RBMKIndicatorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_LEVER_BE.get(),     com.hbm_m.client.render.rbmk.RBMKLeverRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_KEYPAD_BE.get(),    com.hbm_m.client.render.rbmk.RBMKKeyPadRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_NUMITRON_BE.get(),  com.hbm_m.client.render.rbmk.RBMKNumitronRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_GRAPH_BE.get(),     com.hbm_m.client.render.rbmk.RBMKGraphRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_TERMINAL_BE.get(),  com.hbm_m.client.render.rbmk.RBMKTerminalRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RBMK_CONSOLE_BE.get(),
                com.hbm_m.client.render.implementations.MachineRbmkConsoleRenderer::new);
        // Steam inlet/outlet are floor blocks (not columns) — rendered via MODEL + JSON
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
                // Original ItemBlueprints.getSubItems: geheime Pools nicht im Reiter
                if (pool.startsWith(com.hbm_m.item.industrial.BlueprintPools.POOL_PREFIX_SECRET)) continue;
                ItemStack folderStack = com.hbm_m.item.industrial.ItemBlueprints.make(pool);
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
        if (ModClothConfig.get().renderRebarSimple) {
            BakedModel simple = (BakedModel) modelRegistry.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/rebar_simple"));
            if (simple != null) modelRegistry.put(PlatformHooks.createModelLocation(ModBlocks.REBAR.getId(), ""), simple);
        }
        Object leavesLocation = PlatformHooks.createModelLocation(ModBlocks.WASTE_LEAVES.getId(), "");
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
    //?} elif neoforge {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBakingCompletedDisplayGuards(ModelEvent.BakingCompleted event) {
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.installDisplayTransformGuards(
                event.getModelBakery().getBakedTopLevelModels());
    }
    *///?}
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModelBakeUnwrapContinuity(ModelEvent.ModifyBakingResult event) {
        java.util.Map models = event.getModels();
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

        wrapConnectedDecoCtTerrainModels(models);
        wrapPileCtTerrainModels(models);
        wrapIcfCtTerrainModels(models);
        wrapPwrCtTerrainModels(models);
        wrapHadronCoilCtTerrainModels(models);
        wrapBoxDuctModels(models);
        // R6e: RenderReeds - Schilf bis zum Gewaessergrund
        {
            Object loc = PlatformHooks.createModelLocation(ModBlocks.PLANT_REEDS.getId(), "");
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked != null && !(baked instanceof com.hbm_m.client.model.ReedsBakedModel)) models.put(loc, new com.hbm_m.client.model.ReedsBakedModel(baked));
        }
        // checkTilt: gekippte Maschinen (Absenkung wie in den Originalrenderern)
        wrapTilted(models, ModBlocks.MACHINE_BIGASSTANK.get(), 1D);
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
                new CtEntry(ModBlocks.GLASS_BORON, "glass_boron"),
                new CtEntry(ModBlocks.GLASS_LEAD, "glass_lead"),
                new CtEntry(ModBlocks.GLASS_URANIUM, "glass_uranium"),
                new CtEntry(ModBlocks.GLASS_TRINITITE, "glass_trinitite"),
                new CtEntry(ModBlocks.GLASS_POLONIUM, "glass_polonium"),
                new CtEntry(ModBlocks.GLASS_ASH, "glass_ash"),
                new CtEntry(ModBlocks.GLASS_QUARTZ, "glass_quartz"),
                new CtEntry(ModBlocks.GLASS_POLARIZED, "glass_polarized"),
                new CtEntry(ModBlocks.REINFORCED_LAMINATE, "reinforced_laminate"),
                new CtEntry(ModBlocks.PLATEMETAL_BASE, "platemetal_base"),
                new CtEntry(ModBlocks.PLATEMETAL_BLACK, "platemetal_black"),
                new CtEntry(ModBlocks.PLATEMETAL_WHITE, "platemetal_white"),
                new CtEntry(ModBlocks.PLATEMETAL_RED, "platemetal_red"),
                new CtEntry(ModBlocks.PLATEMETAL_GREEN, "platemetal_green"),
                new CtEntry(ModBlocks.PLATEMETAL_LIGHT_GRAY, "platemetal_light_gray"),
                new CtEntry(ModBlocks.PLATEMETAL_BLUE, "platemetal_blue"),
                new CtEntry(ModBlocks.PLATEMETAL_PURPLE, "platemetal_purple"),
                new CtEntry(ModBlocks.PLATEMETAL_CYAN, "platemetal_cyan"),
                new CtEntry(ModBlocks.PLATEMETAL_PINK, "platemetal_pink"),
                new CtEntry(ModBlocks.PLATEMETAL_LIME, "platemetal_lime"),
                new CtEntry(ModBlocks.PLATEMETAL_YELLOW, "platemetal_yellow"),
                new CtEntry(ModBlocks.PLATEMETAL_LIGHT_BLUE, "platemetal_light_blue"),
                new CtEntry(ModBlocks.PLATEMETAL_MAGENTA, "platemetal_magenta"),
                new CtEntry(ModBlocks.PLATEMETAL_ORANGE, "platemetal_orange"),
                // Original BlockNTMGlassCT reinforced_glass und WireCoated red_wire_coated (IBlockCT)
                new CtEntry(ModBlocks.REINFORCED_GLASS, "reinforced_glass"),
                new CtEntry(ModBlocks.RED_WIRE_COATED, "red_wire_coated"),
        };

        for (CtEntry e : entries) {
            Object loc = PlatformHooks.createModelLocation(e.block.getId(), "");
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) {
                continue;
            }
            
            ResourceLocation full = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/" + e.textureBase);
            ResourceLocation ct = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/" + e.textureBase + "_ct");
            models.put(loc, new ConnectedDecoBlockBakedModel(baked, full, ct));
        }
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

            Object loc = PlatformHooks.createModelLocation(ModBlocks.PILE_BLOCK.getId(),
                    "type=" + type.getSerializedName());
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) continue;

            models.put(loc, new ConnectedDecoBlockBakedModel(baked, side[0], side[1], perSide, connects));
        }
    }

    /**
     * 1:1-Port von {@code BlockICF.getFragments}: die Aussenhaut und die Anschlussstellen tragen
     * jeweils ein eigenes Paar, und beide verbinden sich auch mit dem Steuerblock.
     */
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
        for (RegistrySupplier<Block> sup : java.util.List.of(ModBlocks.FLUID_DUCT_BOX, ModBlocks.FLUID_DUCT_EXHAUST, ModBlocks.RED_CABLE_BOX)) {
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

    /** 1:1 {@code BlockHadronCoil.canConnect}: jede Spule verbindet sich mit jeder anderen Spule. */
    private static void wrapHadronCoilCtTerrainModels(java.util.Map models) {
        java.util.function.BiPredicate<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> connects =
                (a, b) -> b instanceof com.hbm_m.block.machines.HadronCoilBlock;
        for (RegistrySupplier<Block> coil : java.util.List.of(ModBlocks.HADRON_COIL_ALLOY, ModBlocks.HADRON_COIL_GOLD, ModBlocks.HADRON_COIL_NEODYMIUM,
                ModBlocks.HADRON_COIL_MAGTUNG, ModBlocks.HADRON_COIL_SCHRABIDIUM, ModBlocks.HADRON_COIL_SCHRABIDATE, ModBlocks.HADRON_COIL_STARMETAL,
                ModBlocks.HADRON_COIL_CHLOROPHYTE, ModBlocks.HADRON_COIL_MESE)) {
            Object loc = PlatformHooks.createModelLocation(coil.getId(), "");
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

            Object loc = PlatformHooks.createModelLocation(ModBlocks.PWR_BLOCK.getId(), "port=" + port);
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

            Object loc = PlatformHooks.createModelLocation(ModBlocks.ICF_BLOCK.getId(), "port=" + port);
            BakedModel baked = (BakedModel) models.get(loc);
            if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) continue;

            models.put(loc, new ConnectedDecoBlockBakedModel(baked, full, ct, null, connects));
        }
    }

    @SubscribeEvent
    public static void onModelRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Power armor: общие multipart-модели сетов (рендер на entity через OBJ-слои)
        PlatformHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.T51_MODEL_ID);
        PlatformHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.AJR_MODEL_ID);
        PlatformHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.AJRO_MODEL_ID);
        PlatformHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.BISMUTH_MODEL_ID);
        PlatformHooks.registerItemModel(event, com.hbm_m.powerarmor.render.ClientPowerArmorRender.DNT_MODEL_ID);

        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_legacy"));
        // Original SoyuzPronter.SoyuzSkin LUNA / AUTHENTIC
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_luna"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_authentic"));
        // Restport: flaches Inventar-Icon der B92 (GunB92ItemRenderer, GUI-Zweig)
        PlatformHooks.registerAdditionalModel(event, com.hbm_m.client.render.item.GunB92ItemRenderer.ICON_MODEL);
        // ClientConfig.RENDER_REBAR_SIMPLE: nur drei Staebe
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/rebar_simple"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_fluid"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_spinner"));

        // mining_drill
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_bit"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_shaft"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_crusher1"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_crusher2"));

        // ore_slopper
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_fan"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_blades_left"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_blades_right"));

        // arc_furnace

        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket"));

        for (String part : new String[] {
                "chekhov_carriage", "chekhov_carriage_friendly", "chekhov_body", "chekhov_barrels",
                "jeremy_gun", "tauon_cannon", "tauon_rotor", "richard_launcher",
                "howard_carriage", "howard_body", "howard_barrelstop", "howard_barrelsbottom",
                "fritz_gun", "maxwell_microwave",
                "arty_carriage", "arty_cannon", "arty_barrel",
                "himars_carriage", "himars_launcher", "himars_crane",
                "sentry_pivot", "sentry_body", "sentry_drum", "sentry_barrell", "sentry_barrelr",
                "sentry_damaged_pivot", "sentry_damaged_body", "sentry_damaged_drum", "sentry_damaged_barrell", "sentry_damaged_barrelr",
                "howard_damaged_carriage", "howard_damaged_body", "howard_damaged_barrelstop", "howard_damaged_barrelsbottom",
                // Sockel der Mehrblock-Tuerme (zeichnet der BER am Turm-Zentrum)
                "chekhov_base", "chekhov_base_friendly", "chekhov_base_rusted", "arty_base", "chekhov_connectors"
        }) {
            PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/turret_parts/" + part));
        }

        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_clean"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_green"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_rad"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_clean"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_black"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_orange"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_trefoil"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_yellow"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_gray"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_yellow"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_black"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_clean"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil_yellow"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant1"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant2"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_legacy"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_modern"));

        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_2"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_81"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_87"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_99"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_101"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_106"));
        PlatformHooks.registerAdditionalModel(event, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_111"));

        for (MissileItemModelDefinitions.Definition definition : MissileItemModelDefinitions.all()) {
            ResourceLocation meshId = MissileRenderHelper.meshModelId(
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, definition.itemPath()));
            PlatformHooks.registerAdditionalModel(event, meshId);
        }

        MainRegistry.LOGGER.debug("Registered door and missile variant models for loading");
    }

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterGeometryLoaders event) {
        MainRegistry.LOGGER.info("DoorDeclRegistry initialized with {} doors", DoorDeclRegistry.getAll().size());

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
        // PlatformHooks.registerGeometryLoader(event, "test", new TestModelLoader());

        //? if neoforge {
        /*event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "composite"), net.neoforged.neoforge.client.model.CompositeModel.Loader.INSTANCE);
        event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "obj"), net.neoforged.neoforge.client.model.obj.ObjLoader.INSTANCE);
        *///?}
        
        PlatformHooks.registerGeometryLoader(event, "radar_loader", new MachineRadarModelLoader());
        PlatformHooks.registerGeometryLoader(event, "soyuz_launcher_loader", new com.hbm_m.client.loader.SoyuzLauncherModelLoader());
        PlatformHooks.registerGeometryLoader(event, "soyuz_rocket_loader", new com.hbm_m.client.loader.SoyuzRocketModelLoader());

        // Power armor OBJ loaders
        PlatformHooks.registerGeometryLoader(event, "t51_armor_parts", new com.hbm_m.powerarmor.render.T51ArmorModelLoader());
        PlatformHooks.registerGeometryLoader(event, "ajr_armor_parts", new com.hbm_m.powerarmor.render.AJRArmorModelLoader());
        PlatformHooks.registerGeometryLoader(event, "bismuth_armor_parts", new com.hbm_m.powerarmor.render.BismuthArmorModelLoader());
        PlatformHooks.registerGeometryLoader(event, "dnt_armor_parts", new com.hbm_m.powerarmor.render.DNTArmorModelLoader());

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

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // Original ItemCassette.getColorFromItemStack: Overlay in Trackfarbe
        event.register((stack, tintIndex) -> com.hbm_m.item.machine.ItemCassette.getColor(stack, tintIndex),
                ModItems.CASSETTES.stream().map(r -> (net.minecraft.world.level.ItemLike) r.get()).toArray(net.minecraft.world.level.ItemLike[]::new));
        // R6e: getRenderColor - Gras-Farbe (0.5, 1.0) fuer Tabak/Hanf
        event.register((stack, tintIndex) -> tintIndex == 0 ? net.minecraft.world.level.GrassColor.get(0.5D, 1.0D) : 0xFFFFFF,
                ModBlocks.PLANT_FLOWER_TOBACCO.get(), ModBlocks.PLANT_FLOWER_WEED.get(), ModBlocks.PLANT_TALL_WEED.get());
        event.register((stack, tintIndex) -> tintIndex >= 0 && tintIndex < 6
                ? com.hbm_m.block.machines.MachineCraneRouterBlock.SIDE_COLORS[tintIndex] : 0xffffff, ModBlocks.CRANE_ROUTER.get());
        // Original ItemPipette.getColorFromItemStack (Pass 1 = Fluessigkeitsfarbe)
        event.register((stack, tintIndex) -> opaqueTint(StackNbt.has(stack) && stack.getItem() instanceof com.hbm_m.item.tool.ItemPipette p ? p.getColor(stack, tintIndex) : 0xFFFFFF),
                ModItems.PIPETTE.get(), ModItems.PIPETTE_BORON.get(), ModItems.PIPETTE_LABORATORY.get());
        // Мета-предметы вкладки Parts (PartTabMetaItems): тинт базовой текстуры цветом
        // материала/красителя (аппроксимация ItemAutogen/ItemChemicalDye оригинала).
        // Двухслойные (dye/crayon) тинтуруются только на layer1 (оверлей).
        // Original ItemBookLore.getColorFromItemStack: Schicht 1 Einband, Schicht 2 Titel
        event.register((stack, tintIndex) -> opaqueTint(com.hbm_m.item.special.ItemBookLore.getColor(stack, tintIndex)), ModItems.BOOK_LORE.get());
        // Original ItemKitCustom.getColorFromItemStack: Pass 1/2 aus dem NBT
        event.register((stack, tintIndex) -> opaqueTint(com.hbm_m.item.special.ItemKitCustom.getRenderColor(stack, tintIndex)), ModItems.KIT_CUSTOM.get());
        event.register((stack, tintIndex) ->
                opaqueTint(com.hbm_m.item.PartTabMetaItems.tintFor(stack.getItem(), tintIndex)),
                com.hbm_m.item.PartTabMetaItems.tintedItems());
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) return opaqueTint(0xFFFFFF);
            return opaqueTint(com.hbm_m.item.liquids.FluidIdentifierItem.getTintColor(stack));
        }, ModItems.FLUID_IDENTIFIER.get());
        // Original ItemFluidTank/ItemCanister/ItemGasTank/ItemDisperser.getColorFromItemStack
        event.register((stack, tintIndex) -> opaqueTint(((com.hbm_m.item.liquids.ItemFluidTank) stack.getItem()).getColor(stack, tintIndex)),
                ModItems.FLUID_TANK_FULL.get(), ModItems.FLUID_TANK_LEAD_FULL.get(), ModItems.FLUID_BARREL_FULL.get(), ModItems.FLUID_PACK_FULL.get(),
                ModItems.CANISTER_FULL.get(), ModItems.GAS_FULL.get(), ModItems.DISPERSER_CANISTER.get(), ModItems.GLYPHID_GLAND.get());
        // Fluid Duct - tint overlay layer with fluid color
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) return opaqueTint(0xFFFFFF);
            return opaqueTint(com.hbm_m.item.liquids.FluidDuctItem.getTintColor(stack));
        }, ModItems.FLUID_DUCT.get(), ModItems.FLUID_DUCT_COLORED.get(), ModItems.FLUID_DUCT_SILVER.get());
        // ICF-Pellet: untere Lage (icf_pellet_bg) in der Mischfarbe beider Brennstoffe, obere
        // Lage unveraendert - 1:1 aus getColorFromItemStack/getIconFromDamageForRenderPass.
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return opaqueTint(0xFFFFFF);
            return opaqueTint(com.hbm_m.item.machine.ItemICFPellet.getMixedColor(stack));
        }, ModItems.ICF_PELLET.get());

        // Mineral Pipes - tint layer0 with the pipe's mineral color
        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof com.hbm_m.item.MineralPipeItem pipe) {
                return opaqueTint(pipe.getTintColor());
            }
            return opaqueTint(0xFFFFFF);
        }, ModItems.PIPE_IRON.get(), ModItems.PIPE_COPPER.get(), ModItems.PIPE_GOLD.get(),
           ModItems.PIPE_LEAD.get(), ModItems.PIPE_STEEL.get(), ModItems.PIPE_TUNGSTEN.get(),
           ModItems.PIPE_TITANIUM.get(), ModItems.PIPE_ALUMINUM.get(), ModItems.PIPE_DURA_STEEL.get());
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // R6e: BlockNTMFlower/BlockTallPlant.colorMultiplier - Tabak und Hanf laubfarben
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                ? net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(level, pos)
                : net.minecraft.world.level.GrassColor.get(0.5D, 1.0D),
                ModBlocks.PLANT_FLOWER_TOBACCO.get(), ModBlocks.PLANT_FLOWER_WEED.get(), ModBlocks.PLANT_TALL_WEED.get());
        // 1:1 CraneRouter.colorMultiplier: Seite 0-5 rot, orange, gelb, gruen, blau, violett
        event.register((state, level, pos, tintIndex) -> tintIndex >= 0 && tintIndex < 6
                ? com.hbm_m.block.machines.MachineCraneRouterBlock.SIDE_COLORS[tintIndex] : 0xffffff, ModBlocks.CRANE_ROUTER.get());
        // R6c: Balefire.colorMultiplier (dunkler mit steigendem Alter)
        event.register((state, level, pos, tintIndex) -> com.hbm_m.block.bomb.BalefireBlock.color(state), com.hbm_m.block.ModBlocks.BALEFIRE.get());
        // R7i: FluidDuctBox.colorMultiplier - weisse Kastenrohre in (aufgehellter) Fluessigkeitsfarbe
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null || state.getValue(com.hbm_m.block.network.BoxDuctBlock.META) % 3 != 2) return 0xFFFFFF;
            if (!(level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.network.PaintableDuctBlockEntity pipe)) return 0xFFFFFF;
            net.minecraft.world.level.material.Fluid f = pipe.getFluidType();
            if (f == null || f == net.minecraft.world.level.material.Fluids.EMPTY) f = com.hbm_m.inventory.fluid.ModFluids.NONE.getSource();
            int c = com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(f);
            int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            r = (int) (r + (255 - r) * 0.25D); g = (int) (g + (255 - g) * 0.25D); b = (int) (b + (255 - b) * 0.25D);
            return (r << 16) | (g << 8) | b;
        }, com.hbm_m.block.ModBlocks.FLUID_DUCT_BOX.get());
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
        }, com.hbm_m.block.ModBlocks.FLUID_DUCT.get(),
                com.hbm_m.block.ModBlocks.FLUID_DUCT_COLORED.get(),
                com.hbm_m.block.ModBlocks.FLUID_DUCT_SILVER.get());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.AIRNUKEBOMB_PROJECTILE.get(), AirNukeBombProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRBOMB_PROJECTILE.get(), AirBombProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRSTRIKE_NUKE_ENTITY.get(), AirstrikeNukeEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRSTRIKE_ENTITY.get(), AirstrikeEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRSTRIKE_AGENT_ENTITY.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.NUKE_FALLOUT_RAIN.get(), RenderFallout::new);
        event.registerEntityRenderer(ModEntities.NUKE_MK3.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.SOLINIUM_EXPLOSION.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.BALEFIRE_EXPLOSION.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.TOM_METEOR.get(), com.hbm_m.client.render.effect.TomRenderers.Tom::new);
        event.registerEntityRenderer(ModEntities.B92_BEAM.get(), com.hbm_m.client.render.projectile.B92BeamRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET_BASE_NT.get(), com.hbm_m.client.render.projectile.RenderBulletNT::new);
        event.registerEntityRenderer(ModEntities.CLOUD_TOM.get(), com.hbm_m.client.render.effect.TomRenderers.CloudTom::new);
        event.registerEntityRenderer(ModEntities.CART_CRATE.get(), com.hbm_m.client.render.entity.NeoCartRenderer::new);
        event.registerEntityRenderer(ModEntities.CART_DESTROYER.get(), com.hbm_m.client.render.entity.NeoCartRenderer::new);
        event.registerEntityRenderer(ModEntities.CART_ORE.get(), com.hbm_m.client.render.entity.NeoCartRenderer::new);
        event.registerEntityRenderer(ModEntities.CART_POWDER.get(), com.hbm_m.client.render.entity.NeoCartRenderer::new);
        event.registerEntityRenderer(ModEntities.CART_SEMTEX.get(), com.hbm_m.client.render.entity.NeoCartRenderer::new);
        // Zugsystem + Test-Lore (Original ClientProxy: RenderMinecartTest, RenderEmpty fuer Sitz-/Hitbox-Dummys)
        event.registerEntityRenderer(ModEntities.MINECART_TEST.get(), com.hbm_m.client.render.entity.RenderMinecartTest::new);
        event.registerEntityRenderer(ModEntities.TRAIN_SEAT_DUMMY.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAIN_BOUNDING_DUMMY.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAIN_CARGO_TRAM.get(), com.hbm_m.client.render.entity.RenderTrainCargoTram::new);
        event.registerEntityRenderer(ModEntities.TRAIN_CARGO_TRAM_TRAILER.get(), com.hbm_m.client.render.entity.RenderTrainCargoTramTrailer::new);
        event.registerEntityRenderer(ModEntities.TOM_BLAST.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.CLOUD_FLEIJA.get(), RenderCloudFleija::new);
        event.registerEntityRenderer(ModEntities.CLOUD_FLEIJA_RAINBOW.get(), com.hbm_m.client.render.effect.RenderCloudRainbow::new);
        event.registerEntityRenderer(ModEntities.CLOUD_SOLINIUM.get(), com.hbm_m.client.render.effect.RenderCloudSolinium::new);
        event.registerEntityRenderer(ModEntities.FALLING_NUKE.get(), com.hbm_m.client.render.projectile.FallingNukeRenderer::new);
        event.registerEntityRenderer(ModEntities.BURNING_FOEQ.get(), com.hbm_m.client.render.projectile.BurningFOEQRenderer::new);
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
        event.registerEntityRenderer(ModEntities.MISSILE_9M723.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_9M723_BUSTER.get(), MissileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE_TOPOL.get(), MissileEntityRenderer::new);
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
        event.registerEntityRenderer(ModEntities.DIGAMMA_SPEAR.get(), com.hbm_m.client.render.effect.SpearRenderer::new);
        event.registerEntityRenderer(ModEntities.RUBBLE.get(), RubbleEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SHRAPNEL.get(), com.hbm_m.client.render.effect.ShrapnelRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET_MK4.get(), com.hbm_m.client.weapon.BulletRenderers.MK4::new);
        event.registerEntityRenderer(ModEntities.BULLET_MK4_CL.get(), com.hbm_m.client.weapon.BulletRenderers.MK4::new);
        event.registerEntityRenderer(ModEntities.BULLET_BEAM.get(), com.hbm_m.client.weapon.BulletRenderers.Beam::new);
        event.registerEntityRenderer(ModEntities.COIN.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_LINGERING.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADE_UNIVERSAL.get(), com.hbm_m.client.weapon.render.RenderGrenadeUniversal::new);
        event.registerEntityRenderer(ModEntities.RAD_BEAST.get(), com.hbm_m.client.render.mob.RADBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.BOT_PRIME_HEAD.get(), com.hbm_m.client.render.mob.BOTPrimeRenderer::head);
        event.registerEntityRenderer(ModEntities.BOT_PRIME_BODY.get(), com.hbm_m.client.render.mob.BOTPrimeRenderer::body);
        event.registerEntityRenderer(ModEntities.UFO.get(), com.hbm_m.client.render.mob.UFORenderer::new);
        event.registerEntityRenderer(ModEntities.BOMBER.get(), com.hbm_m.client.render.plane.BomberRenderer::new);
        // 1:1 RenderBombletTheta (Zeta: halbe Groesse, eigene Textur)
        event.registerEntityRenderer(ModEntities.BOMBLET_ZETA.get(), com.hbm_m.client.render.projectile.BombletThetaRenderer::new);
        event.registerEntityRenderer(ModEntities.MASKMAN.get(), com.hbm_m.client.render.mob.MaskManRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.HUNTER_CHOPPER.get(), com.hbm_m.client.render.mob.HunterChopperRenderer::new);
        event.registerEntityRenderer(ModEntities.C130.get(), com.hbm_m.client.render.entity.C130Renderers.C130Renderer::new);
        event.registerEntityRenderer(ModEntities.PARACHUTE_CRATE.get(), com.hbm_m.client.render.entity.C130Renderers.ParachuteCrateRenderer::new);
        event.registerEntityRenderer(ModEntities.DUCK.get(), com.hbm_m.client.render.mob.DuckRenderer::new);
        event.registerEntityRenderer(ModEntities.FBI.get(), com.hbm_m.client.render.mob.FBIRenderers.FBI::new);
        event.registerEntityRenderer(ModEntities.FBI_DRONE.get(), com.hbm_m.client.render.mob.FBIRenderers.Drone::new);
        event.registerEntityRenderer(ModEntities.QUACKOS.get(), com.hbm_m.client.render.mob.MiscMobRenderers.Quackos::new);
        event.registerEntityRenderer(ModEntities.PIGEON.get(), com.hbm_m.client.render.mob.MiscMobRenderers.Pigeon::new);
        event.registerEntityRenderer(ModEntities.PLASTIC_BAG.get(), com.hbm_m.client.render.mob.MiscMobRenderers.PlasticBag::new);
        event.registerEntityRenderer(ModEntities.TEST_DUMMY.get(), com.hbm_m.client.render.mob.MiscMobRenderers.Dummy::new);
        event.registerEntityRenderer(ModEntities.GHOST.get(), com.hbm_m.client.render.mob.MiscMobRenderers.Ghost::new);
        event.registerEntityRenderer(ModEntities.BLOCK_SPIDER.get(), com.hbm_m.client.render.mob.MiscMobRenderers.BlockSpider::new);
        event.registerEntityRenderer(ModEntities.CYBER_CRAB.get(), com.hbm_m.client.render.mob.CrabRenderers.Cyber::new);
        event.registerEntityRenderer(ModEntities.TESLA_CRAB.get(), com.hbm_m.client.render.mob.CrabRenderers.Tesla::new);
        event.registerEntityRenderer(ModEntities.TAINT_CRAB.get(), com.hbm_m.client.render.mob.CrabRenderers.Taint::new);
        event.registerEntityRenderer(ModEntities.CHOPPER_MINE.get(), com.hbm_m.client.render.projectile.ChopperMineRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET.get(), com.hbm_m.client.render.projectile.BulletRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_BRAWLER.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_BEHEMOTH.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_BRENDA.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_BOMBARDIER.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_BLASTER.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_SCOUT.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_NUCLEAR.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPHID_DIGGER.get(), com.hbm_m.client.render.mob.GlyphidRenderer::new);
        event.registerEntityRenderer(ModEntities.PARASITE_MAGGOT.get(), com.hbm_m.client.render.mob.ParasiteMaggotRenderer::new);
        event.registerEntityRenderer(ModEntities.WAYPOINT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ACID_BOMB.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CHEMICAL.get(), com.hbm_m.client.render.projectile.ChemicalRenderer::new);
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
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Связываем наш ТИП частицы с ее ФАБРИКОЙ.
        event.registerSpriteSet(ModParticleTypes.TOWNAURA.get(), TownauraParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SCHRABFOG.get(), SchrabfogParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RAD_FOG_PARTICLE.get(), RadFogParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.HADRON.get(), com.hbm_m.particle.custom.HadronParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_FLAME.get(), com.hbm_m.particle.custom.RBMKFlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_STEAM.get(), com.hbm_m.particle.custom.RBMKSteamParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_MUSH.get(), com.hbm_m.particle.custom.RBMKMushParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.DIGAMMA_SMOKE.get(), com.hbm_m.particle.custom.DigammaSmokeParticle.Provider::new);
        MainRegistry.LOGGER.info("Registered custom particle providers.");
    }

    /** Original ItemRendererHot: Gluehen der ItemHot-Gegenstaende im Inventar. */
    @SubscribeEvent
    public static void onRegisterItemDecorations(net.minecraftforge.client.event.RegisterItemDecorationsEvent event) {
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.hbm_m.item.special.ItemHot) event.register(item, HotItemDecorator.INSTANCE);
        }
        event.register(com.hbm_m.item.ModItems.BROKEN_ITEM.get(), BrokenItemDecorator.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        MainRegistry.LOGGER.info("Registering GUI overlays...");
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "geiger_counter_hud", OverlayGeiger.GEIGER_HUD_OVERLAY);
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.ARMOR_LEVEL.id(), "power_armor_hud", OverlayPowerArmor.POWER_ARMOR_OVERLAY);
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.ARMOR_LEVEL.id(), "jetpack_fuel_hud", com.hbm_m.client.overlay.OverlayJetpackFuel.OVERLAY);
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.PORTAL.id(), "radiation_pixels", OverlayRadiationVisuals.RADIATION_PIXELS_OVERLAY);
        event.registerAboveAll("info_toast", OverlayInfoToast.OVERLAY);
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id(), "tool_ability_indicator", ToolAbilityClient.OVERLAY);
        event.registerAboveAll("gas_mask_overlay", com.hbm_m.client.overlay.OverlayGasMask.OVERLAY);
        // Original RenderScreenOverlay.renderDashBar / renderShieldBar
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "dash_bar", com.hbm_m.client.overlay.OverlayDashShield.DASH_OVERLAY);
        event.registerBelow(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.ARMOR_LEVEL.id(), "shield_bar", com.hbm_m.client.overlay.OverlayDashShield.SHIELD_OVERLAY);
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }
    //?} elif neoforge {
    /*// NeoForge-Gegenstueck zu onRegisterParticleProviders (Forge): gleiche Fabriken.
    @SubscribeEvent
    public static void onRegisterParticleProviders(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.TOWNAURA.get(), TownauraParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SCHRABFOG.get(), SchrabfogParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RAD_FOG_PARTICLE.get(), RadFogParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.HADRON.get(), com.hbm_m.particle.custom.HadronParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_FLAME.get(), com.hbm_m.particle.custom.RBMKFlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_STEAM.get(), com.hbm_m.particle.custom.RBMKSteamParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_MUSH.get(), com.hbm_m.particle.custom.RBMKMushParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.DIGAMMA_SMOKE.get(), com.hbm_m.particle.custom.DigammaSmokeParticle.Provider::new);
        MainRegistry.LOGGER.info("Registered custom particle providers.");
    }

    /^* Original ItemRendererHot: Gluehen der ItemHot-Gegenstaende im Inventar (NeoForge-Gegenstueck). ^/
    @SubscribeEvent
    public static void onRegisterItemDecorations(net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent event) {
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.hbm_m.item.special.ItemHot) event.register(item, HotItemDecorator.INSTANCE);
        }
        event.register(com.hbm_m.item.ModItems.BROKEN_ITEM.get(), BrokenItemDecorator.INSTANCE);
    }

    private static int hbmGuiWidth() { return Minecraft.getInstance().getWindow().getGuiScaledWidth(); }
    private static int hbmGuiHeight() { return Minecraft.getInstance().getWindow().getGuiScaledHeight(); }
    private static net.minecraft.resources.ResourceLocation hbmLayer(String id) { return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id); }

    // Gleiche Ebenen und Reihenfolge wie der Forge-Zweig; PORTAL gibt es auf 1.21.1 nicht mehr einzeln
    // (Teil von CAMERA_OVERLAYS). leftHeight liegt auf NeoForge direkt an Minecraft.gui.
    @SubscribeEvent
    public static void onRegisterGuiOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        MainRegistry.LOGGER.info("Registering GUI overlays...");
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR, hbmLayer("geiger_counter_hud"), (g, dt) ->
                OverlayGeiger.render(g, dt.getGameTimeDeltaPartialTick(true), hbmGuiWidth(), hbmGuiHeight()));
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.ARMOR_LEVEL, hbmLayer("power_armor_hud"), (g, dt) ->
                OverlayPowerArmor.renderNeo(g, dt.getGameTimeDeltaPartialTick(true), hbmGuiWidth(), hbmGuiHeight()));
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.ARMOR_LEVEL, hbmLayer("jetpack_fuel_hud"), (g, dt) ->
                com.hbm_m.client.overlay.OverlayJetpackFuel.renderNeo(g, hbmGuiWidth(), hbmGuiHeight()));
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CAMERA_OVERLAYS, hbmLayer("radiation_pixels"), (g, dt) ->
                OverlayRadiationVisuals.render(g, dt.getGameTimeDeltaPartialTick(true), hbmGuiWidth(), hbmGuiHeight()));
        event.registerAboveAll(hbmLayer("info_toast"), (g, dt) ->
                OverlayInfoToast.render(g, dt.getGameTimeDeltaPartialTick(true), hbmGuiWidth(), hbmGuiHeight()));
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR, hbmLayer("tool_ability_indicator"), (g, dt) ->
                ToolAbilityClient.renderHUD(g, hbmGuiWidth(), hbmGuiHeight()));
        event.registerAboveAll(hbmLayer("gas_mask_overlay"), (g, dt) -> com.hbm_m.client.overlay.OverlayGasMask.render(g));
        // Original RenderScreenOverlay.renderDashBar / renderShieldBar
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR, hbmLayer("dash_bar"), (g, dt) ->
                com.hbm_m.client.overlay.OverlayDashShield.renderDashNeo(g, hbmGuiHeight()));
        event.registerBelow(net.neoforged.neoforge.client.gui.VanillaGuiLayers.ARMOR_LEVEL, hbmLayer("shield_bar"), (g, dt) ->
                com.hbm_m.client.overlay.OverlayDashShield.renderShieldNeo(g, hbmGuiWidth(), hbmGuiHeight()));
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

        ResourceLocation realVsh = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shaders/core/block_lit.vsh");
        ResourceLocation virtualInstancedVsh = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shaders/core/block_lit_instanced.vsh");

        com.hbm_m.client.render.shader.modification.ShaderModification instancingDefine =
            com.hbm_m.client.render.shader.modification.ShaderModification.builder()
                .define("USE_INSTANCING")
                .define("USE_VERTEX_BONE_ID");

        net.minecraft.server.packs.resources.ResourceProvider instancedProvider =
            com.hbm_m.client.render.shader.modification.ShaderPreDefinitions.wrapRedirect(
                event.getResourceProvider(), virtualInstancedVsh, realVsh, instancingDefine);

        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block_lit_simple"),
                blockLitSimpleFormat
            ),
            ModShaders::setBlockLitSimpleShader
        );
        MainRegistry.LOGGER.info("Successfully registered block_lit_simple shader");

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
        event.registerLayerDefinition(ModModelLayers.POWER_ARMOR, PowerArmorEmptyModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.GAS_MASK, com.hbm_m.client.model.GasMaskModels::createGasMask);
        event.registerLayerDefinition(com.hbm_m.client.render.armor.ArmorAccessoryModels.GOGGLES, com.hbm_m.client.render.armor.ArmorAccessoryModels::createGoggles);
        event.registerLayerDefinition(com.hbm_m.client.render.armor.ArmorAccessoryModels.JETPACK, com.hbm_m.client.render.armor.ArmorAccessoryModels::createJetpack);
        event.registerLayerDefinition(ModModelLayers.M65, com.hbm_m.client.model.GasMaskModels::createM65);
    }
}