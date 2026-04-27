package com.hbm_m.client;
// Основной класс клиентской настройки мода. Здесь регистрируются все клиентские обработчики событий,
// GUI, рендереры, модели и т.д.
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.client.loader.DoorModelLoader;
import com.hbm_m.client.loader.HeatingOvenModelLoader;
import com.hbm_m.client.loader.MachineAdvancedAssemblerModelLoader;
import com.hbm_m.client.loader.MachineAssemblerModelLoader;
import com.hbm_m.client.loader.MachineBatterySocketModelLoader;
import com.hbm_m.client.loader.MachineChemicalPlantModelLoader;
import com.hbm_m.client.loader.MachineFluidTankModelLoader;
import com.hbm_m.client.loader.MachineHydraulicFrackiningTowerModelLoader;
import com.hbm_m.client.loader.PressModelLoader;
import com.hbm_m.client.loader.TemplateModelLoader;
import com.hbm_m.client.overlay.OverlayGeiger;
import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.client.overlay.OverlayRadiationVisuals;
import com.hbm_m.client.render.BatterySocketCreativeRenderer;
import com.hbm_m.client.render.EmptyEntityRenderer;
import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.HeatingOvenRenderer;
import com.hbm_m.client.render.IndustrialTurbineRenderer;
import com.hbm_m.client.render.ModShaders;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.effect.RenderFallout;
import com.hbm_m.client.render.implementations.AirBombProjectileEntityRenderer;
import com.hbm_m.client.render.implementations.AirNukeBombProjectileEntityRenderer;
import com.hbm_m.client.render.implementations.AirstrikeEntityRenderer;
import com.hbm_m.client.render.implementations.AirstrikeNukeEntityRenderer;
import com.hbm_m.client.render.implementations.DoorRenderer;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.client.render.implementations.MissileTestEntityRenderer;
import com.hbm_m.client.render.shader.ShaderReloadListener;
import com.hbm_m.client.tooltip.CrateContentsTooltipComponent;
import com.hbm_m.client.tooltip.CrateContentsTooltipComponentRenderer;
import com.hbm_m.client.tooltip.ItemTooltipComponent;
import com.hbm_m.client.tooltip.ItemTooltipComponentRenderer;
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
import com.hbm_m.inventory.gui.GUIMachineChemicalPlant;
import com.hbm_m.inventory.gui.GUIMachineFluidTank;
import com.hbm_m.inventory.gui.GUIMachineFrackingTower;
import com.hbm_m.inventory.gui.GUIMachinePress;
import com.hbm_m.inventory.gui.GUIMachineShredder;
import com.hbm_m.inventory.gui.GUIMachineWoodBurner;
import com.hbm_m.inventory.gui.GUISteelCrate;
import com.hbm_m.inventory.gui.GUITemplateCrate;
import com.hbm_m.inventory.gui.GUITungstenCrate;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.DarkParticle;
import com.hbm_m.particle.custom.RadFogParticle;
import com.hbm_m.powerarmor.layer.AbstractObjArmorLayer;
import com.hbm_m.powerarmor.layer.ModModelLayers;
import com.hbm_m.powerarmor.layer.PowerArmorEmptyModel;
import com.hbm_m.powerarmor.overlay.HbmThermalHandler;
import com.hbm_m.powerarmor.overlay.OverlayPowerArmor;
import com.hbm_m.recipe.AssemblerRecipe;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
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
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.server.packs.PackType;

import static dev.architectury.registry.client.rendering.fabric.BlockEntityRendererRegistryImpl.register;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
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

        // Key mappings регистрируются в ModConfigKeybindHandler.init() через Architectury/обвязку,
        // но на некоторых таргетах удобно иметь fallback в одном месте.
        ModConfigKeybindHandler.init();
        DarkParticleHandler.init();

        // Экраны меню - vanilla API, одинаково работает на обоих лоадерах.
        registerScreens();

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

    //? if forge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MainRegistry.LOGGER.info("FMLClientSetupEvent fired. Initializing client.");
        initClient();

        // Forge-only: шина событий для тик/рендер-хендлеров.
        MinecraftForge.EVENT_BUS.register(ChunkRadiationDebugRenderer.class);
        MinecraftForge.EVENT_BUS.register(ClientRenderHandler.class);
        MinecraftForge.EVENT_BUS.register(HbmThermalHandler.INSTANCE);

        // Forge-only: дисконнект (на Fabric есть свой хук).
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientDisconnect);
    }
    //?}

    private static void registerScreens() {
        MenuScreens.register(ModMenuTypes.CRYSTALLIZER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCrystallizer::new);
        MenuScreens.register(ModMenuTypes.ARMOR_TABLE_MENU.get(), GUIArmorTable::new);
        MenuScreens.register(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), GUIMachineAssembler::new);
        MenuScreens.register(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), GUIMachineAdvancedAssembler::new);
        MenuScreens.register(ModMenuTypes.MACHINE_BATTERY_MENU.get(), GUIMachineBattery::new);
        MenuScreens.register(ModMenuTypes.BATTERY_SOCKET_MENU.get(), GUIBatterySocket::new);
        MenuScreens.register(ModMenuTypes.BLAST_FURNACE_MENU.get(), GUIBlastFurnace::new);
        MenuScreens.register(ModMenuTypes.HEATING_OVEN_MENU.get(), GUIHeatingOven::new);
        MenuScreens.register(ModMenuTypes.PRESS_MENU.get(), GUIMachinePress::new);
        MenuScreens.register(ModMenuTypes.SHREDDER_MENU.get(), GUIMachineShredder::new);
        MenuScreens.register(ModMenuTypes.WOOD_BURNER_MENU.get(), GUIMachineWoodBurner::new);
        MenuScreens.register(ModMenuTypes.ANVIL_MENU.get(), GUIAnvil::new);
        MenuScreens.register(ModMenuTypes.CENTRIFUGE_MENU.get(), GUIMachineCentrifuge::new);
        MenuScreens.register(ModMenuTypes.IRON_CRATE_MENU.get(), GUIIronCrate::new);
        MenuScreens.register(ModMenuTypes.STEEL_CRATE_MENU.get(), GUISteelCrate::new);
        MenuScreens.register(ModMenuTypes.DESH_CRATE_MENU.get(), GUIDeshCrate::new);
        MenuScreens.register(ModMenuTypes.TUNGSTEN_CRATE_MENU.get(), GUITungstenCrate::new);
        MenuScreens.register(ModMenuTypes.TEMPLATE_CRATE_MENU.get(), GUITemplateCrate::new);
        MenuScreens.register(ModMenuTypes.FLUID_TANK_MENU.get(), GUIMachineFluidTank::new);
        MenuScreens.register(ModMenuTypes.CHEMICAL_PLANT_MENU.get(), GUIMachineChemicalPlant::new);
        MenuScreens.register(ModMenuTypes.FRACTURING_TOWER_MENU.get(), GUIMachineFrackingTower::new);
        MenuScreens.register(ModMenuTypes.LAUNCH_PAD_LARGE_MENU.get(), GUILaunchPadLarge::new);
        MenuScreens.register(ModMenuTypes.LAUNCH_PAD_RUSTED_MENU.get(), GUILaunchPadRusted::new);
        MenuScreens.register(ModMenuTypes.NUKE_FAT_MAN_MENU.get(), com.hbm_m.inventory.gui.GUINukeFatMan::new);
    }

    private static void registerRenderersCommon() {
        //? if forge {
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
        ModEntities.MISSILE_TEST.ifPresent(entityType -> EntityRenderers.register(entityType, MissileTestEntityRenderer::new));

        BlockEntityRenderers.register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PRESS_BE.get(), MachinePressRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CHEMICAL_PLANT_BE.get(), MachineChemicalPlantRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), MachineHydraulicFrackiningTowerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HEATING_OVEN_BE.get(), HeatingOvenRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), IndustrialTurbineRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BATTERY_SOCKET_BE.get(), BatterySocketCreativeRenderer::new);
        //?}

        //? if fabric {
        /*ModEntities.GRENADE_NUC_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADE_IF_FIRE_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADE_IF_SLIME_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADE_IF_HE_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADE_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADEHE_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADEFIRE_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADESMART_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADESLIME_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADE_IF_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.MISSILE_TEST.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileTestEntityRenderer::new));

        register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
        register(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerRenderer::new);
        register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
        register(ModBlockEntities.PRESS_BE.get(), MachinePressRenderer::new);
        register(ModBlockEntities.CHEMICAL_PLANT_BE.get(), MachineChemicalPlantRenderer::new);
        register(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), MachineHydraulicFrackiningTowerRenderer::new);
        register(ModBlockEntities.HEATING_OVEN_BE.get(), HeatingOvenRenderer::new);
        register(ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), IndustrialTurbineRenderer::new);
        register(ModBlockEntities.BATTERY_SOCKET_BE.get(), BatterySocketCreativeRenderer::new);
        *///?}
    }

    private static void registerParticlesCommon() {
//        Particles registered via Forge event below.
        //? if forge {

        //?}

        //? if fabric {
        /*ParticleFactoryRegistry.getInstance().register(ModParticleTypes.DARK_PARTICLE.get(), DarkParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.RAD_FOG_PARTICLE.get(), RadFogParticle.Provider::new);
        *///?}
    }

    private static void registerColorsCommon() {
//        Colors registered via Forge events below.
        //? if forge {

        //?}

        //? if fabric {
        /*ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            return com.hbm_m.item.liquids.FluidIdentifierItem.getTintColor(stack);
        }, ModItems.FLUID_IDENTIFIER.get());
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            return com.hbm_m.item.liquids.FluidBarrelItem.getTintColor(stack);
        }, ModItems.FLUID_BARREL.get());
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            return com.hbm_m.item.liquids.FluidDuctItem.getTintColor(stack);
        }, ModItems.FLUID_DUCT.get(), ModItems.FLUID_DUCT_COLORED.get(), ModItems.FLUID_DUCT_SILVER.get());
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof com.hbm_m.item.MineralPipeItem pipe) {
                return pipe.getTintColor();
            }
            return 0xFFFFFF;
        }, ModItems.PIPE_IRON.get(), ModItems.PIPE_COPPER.get(), ModItems.PIPE_GOLD.get(),
           ModItems.PIPE_LEAD.get(), ModItems.PIPE_STEEL.get(), ModItems.PIPE_TUNGSTEN.get(),
           ModItems.PIPE_TITANIUM.get(), ModItems.PIPE_ALUMINUM.get());

        ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            if (tintIndex != 1 || level == null || pos == null) return 0xFFFFFF;
            var be = level.getBlockEntity(pos);
            if (be instanceof com.hbm_m.block.entity.machines.FluidDuctBlockEntity ductBe) {
                var fluid = ductBe.getFluidType();
                if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    return com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(fluid);
                }
            }
            return 0xFFFFFF;
        }, com.hbm_m.block.ModBlocks.FLUID_DUCT.get(),
                com.hbm_m.block.ModBlocks.FLUID_DUCT_COLORED.get(),
                com.hbm_m.block.ModBlocks.FLUID_DUCT_SILVER.get());
        *///?}
    }

    private static void registerReloadListenersCommon() {
//        Reload listeners registered via Forge event below.
        //? if forge {

        //?}

        //? if fabric {
        /*ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new com.hbm_m.client.reload.IdentifiableReloadListenerAdapter(
                        new ResourceLocation(RefStrings.MODID, "shader_reload_listener"),
                        new ShaderReloadListener()));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new com.hbm_m.client.reload.IdentifiableReloadListenerAdapter(
                        new ResourceLocation(RefStrings.MODID, "thermal_handler_reload_listener"),
                        HbmThermalHandler.INSTANCE));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new com.hbm_m.client.reload.IdentifiableReloadListenerAdapter(
                        new ResourceLocation(RefStrings.MODID, "door_model_registry_reload_listener"),
                        com.hbm_m.client.model.variant.DoorModelRegistry.getInstance()));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new com.hbm_m.client.reload.IdentifiableReloadListenerAdapter(
                        new ResourceLocation(RefStrings.MODID, "deferred_cache_cleanup_reload_listener"),
                        new com.hbm_m.client.reload.DeferredCacheCleanupReloadListener()));
        *///?}
    }

    private static void registerHudCommon() {
//        Overlays registered via Forge event below.
        //? if forge {

        //?}

        //? if fabric {
        /*HudRenderCallback.EVENT.register((gfx, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return;
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();

            OverlayGeiger.render(gfx, tickDelta, w, h);
            OverlayPowerArmor.render(gfx, tickDelta, w, h);
            OverlayRadiationVisuals.render(gfx, tickDelta, w, h);
            OverlayInfoToast.render(gfx, tickDelta, w, h);
            com.hbm_m.client.overlay.BlockLookOverlayHud.render(gfx);
        });
        *///?}
    }

    private static void registerWorldRenderHooksCommon() {
        // Forge: wired via Forge event subscribers (RenderLevelStageEvent) in separate classes.

        //? if fabric {
        /*// Closest equivalent to "after particles" stage for our debug text:
        // we're in world render pass and have camera + PoseStack.
        WorldRenderEvents.AFTER_ENTITIES.register(ctx ->
                ChunkRadiationDebugRenderer.render(ctx.matrixStack(), ctx.camera().getPosition()));

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            ClientRenderHandler.onRenderWorldLate(mc.renderBuffers().bufferSource(), ctx.matrixStack(), ctx.camera().getPosition());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientRenderHandler.onClientTickEnd());

        // Clear radiation cache when joining/leaving worlds/dimensions.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientRadiationData.clearAll());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientRadiationData.clearAll());
        *///?}
    }

    private static void registerDisconnectHandlerCommon() {
//        Forge uses ClientPlayerNetworkEvent.LoggingOut (see onClientDisconnect).
        //? if forge {

        //?}

        //? if fabric {
        /*ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearClientCachesDeferred());
        *///?}
    }

    private static void clearClientCachesDeferred() {
        com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> {
            MachineAdvancedAssemblerRenderer.clearCaches();
            MachineAssemblerRenderer.clearCaches();
            MachineHydraulicFrackiningTowerRenderer.clearCaches();
            DoorRenderer.clearAllCaches();
            MachinePressRenderer.clearCaches();
            MachineChemicalPlantRenderer.clearCaches();
            GlobalMeshCache.clearAll();
            AbstractObjArmorLayer.clearAllCaches();
        });
    }

    //? if forge {
    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();
        
        // Получаем ResourceLocation для нашего блока листвы
        ResourceLocation leavesLocation = new ModelResourceLocation(ModBlocks.WASTE_LEAVES.getId(), "");

        // Находим оригинальную, "запеченную" модель в регистре
        BakedModel originalModel = modelRegistry.get(leavesLocation);
        
        // Если модель найдена, заменяем ее на нашу обертку
        if (originalModel != null) {
            LeavesModelWrapper wrappedModel = new LeavesModelWrapper(originalModel);
            event.getModels().put(leavesLocation, wrappedModel);
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("Successfully wrapped waste_leaves model for dynamic render types.");
            }
        } else {
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.warn("Could not find model for waste_leaves to wrap.");
            }
        }
    }

    /**
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
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModelBakeUnwrapContinuity(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        // Собираем замены отдельно - не модифицируем map во время итерации
        Map<ResourceLocation, BakedModel> replacements = new java.util.HashMap<>();

        for (Map.Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
            if (!RefStrings.MODID.equals(entry.getKey().getNamespace())) continue;
            BakedModel original = entry.getValue();
            BakedModel unwrapped = com.hbm_m.client.render.AbstractPartBasedRenderer
                    .unwrapFabricForwardingModels(original);
            if (unwrapped != original) {
                replacements.put(entry.getKey(), unwrapped);
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug(
                            "[HBM] Unwrapped Continuity model: {} ({} → {})",
                            entry.getKey(),
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
    }

    @SubscribeEvent
    public static void onModelRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Регистрируем модели вариантов дверей, чтобы они загружались в ModelManager
        // round_airlock_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/round_airlock_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/round_airlock_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/round_airlock_door_modern_clean"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_clean"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/round_airlock_door_modern_green"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_green"));
        //?}

        // large_vehicle_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/large_vehicle_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/large_vehicle_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/large_vehicle_door_modern_rad"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern_rad"));
        //?}

        // fire_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/fire_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/fire_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/fire_door_modern_black"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_black"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/fire_door_modern_orange"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_orange"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/fire_door_modern_trefoil"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_trefoil"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/fire_door_modern_yellow"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_yellow"));
        //?}

        // secure_access_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/secure_access_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/secure_access_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/secure_access_door_modern_gray"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_gray"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/secure_access_door_modern_yellow"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_yellow"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/secure_access_door_modern_black"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_black"));
        //?}

        // water_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/water_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/water_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_modern"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/water_door_clean"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_clean"));
        //?}

        // qe_containment_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/qe_containment_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/qe_containment_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil_yellow"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern_trefoil_yellow"));
        //?}

        // qe_sliding_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/qe_sliding_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/qe_sliding_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern"));
        //?}

        // sliding_blast_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/sliding_blast_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/sliding_blast_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant1"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant1"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant2"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant2"));
        //?}

        // sliding_seal_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/sliding_seal_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/sliding_seal_door_modern"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_modern"));
        //?}


        // vault_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/vault_door_skin_2"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_2"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/vault_door_skin_81"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_81"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/vault_door_skin_87"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_87"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/vault_door_skin_99"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_99"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/vault_door_skin_101"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_101"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/vault_door_skin_106"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_106"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/vault_door_skin_111"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/vault_door_skin_111"));
        //?}

        
        MainRegistry.LOGGER.debug("Registered door variant models for loading");
    }

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterGeometryLoaders event) {
        // DoorDeclRegistry.init();
        MainRegistry.LOGGER.info("DoorDeclRegistry initialized with {} doors", DoorDeclRegistry.getAll().size());

        event.register("advanced_assembly_machine_loader", new MachineAdvancedAssemblerModelLoader());
        event.register("chemical_plant_loader", new MachineChemicalPlantModelLoader());
        event.register("machine_assembler_loader", new MachineAssemblerModelLoader());
        event.register("hydraulic_frackining_tower_loader", new MachineHydraulicFrackiningTowerModelLoader());
        event.register("fluid_tank_loader", new MachineFluidTankModelLoader());
        event.register("battery_socket_loader", new MachineBatterySocketModelLoader());
        event.register("door", new DoorModelLoader());
        event.register("template_loader", new TemplateModelLoader());
        event.register("press_loader", new PressModelLoader());
        event.register("heating_oven_loader", new HeatingOvenModelLoader());

        MainRegistry.LOGGER.info("Registered geometry loaders: advanced_assembly_machine_loader, chemical_plant_loader, machine_assembler_loader, hydraulic_frackining_tower_loader, template_loader, door, press_loader, heating_oven_loader");
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
           ModItems.PIPE_TITANIUM.get(), ModItems.PIPE_ALUMINUM.get());
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // Fluid Duct block - tint with the fluid's color from the BlockEntity
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFF;
            if (tintIndex != 1 || level == null || pos == null) return 0xFFFFFF;
            var be = level.getBlockEntity(pos);
            if (be instanceof com.hbm_m.block.entity.machines.FluidDuctBlockEntity ductBe) {
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
        event.registerEntityRenderer(ModEntities.NUKE_MK5.get(), ctx -> new EmptyEntityRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.FALLING_SELLAFIT_ENTITY_TYPE.get(), FallingBlockRenderer::new);
    }

    @SubscribeEvent
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ShaderReloadListener());
        event.registerReloadListener(HbmThermalHandler.INSTANCE);
        event.registerReloadListener(com.hbm_m.client.model.variant.DoorModelRegistry.getInstance());
        event.registerReloadListener((preparationBarrier, resourceManager,
                preparationsProfiler, reloadProfiler,
                backgroundExecutor, gameExecutor) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                // КРИТИЧНО: Откладываем очистку кэшей на render thread, чтобы избежать
                // race condition с активным рендером (EXCEPTION_ACCESS_VIOLATION при
                // включении шейдера - clearCaches вызывался во время render pass).
                com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> {
                    try {
                        MachineAdvancedAssemblerRenderer.clearCaches();
                        MachineAssemblerRenderer.clearCaches();
                        MachineHydraulicFrackiningTowerRenderer.clearCaches();
                        DoorRenderer.clearAllCaches();
                        MachinePressRenderer.clearCaches();
                        MachineChemicalPlantRenderer.clearCaches();
                        GlobalMeshCache.clearAll();
                        AbstractObjArmorLayer.clearAllCaches();
                        MainRegistry.LOGGER.info("VBO cache cleanup completed (deferred to render thread)");
                    } catch (Exception e) {
                        MainRegistry.LOGGER.error("Error during deferred VBO cache cleanup", e);
                    }
                });
            }, gameExecutor);
        });
    }

    public static void onClientDisconnect(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientCachesDeferred();
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Связываем наш ТИП частицы с ее ФАБРИКОЙ.
        event.registerSpriteSet(ModParticleTypes.DARK_PARTICLE.get(), DarkParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RAD_FOG_PARTICLE.get(), RadFogParticle.Provider::new);
        MainRegistry.LOGGER.info("Registered custom particle providers.");
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        MainRegistry.LOGGER.info("Registering GUI overlays...");
        
        // Регистрируем оверлей.
        // Мы говорим: "Нарисуй оверлей с ID 'geiger_counter_hud' НАД хотбаром,
        // используя логику из объекта GeigerOverlay.GEIGER_HUD_OVERLAY".
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "geiger_counter_hud", OverlayGeiger.GEIGER_HUD_OVERLAY);

        event.registerAbove(VanillaGuiOverlay.ARMOR_LEVEL.id(), "power_armor_hud", OverlayPowerArmor.POWER_ARMOR_OVERLAY);

        event.registerAboveAll("thermal_overlay", com.hbm_m.powerarmor.ModEventHandlerClient.THERMAL_OVERLAY);

        event.registerAbove(VanillaGuiOverlay.PORTAL.id(), "radiation_pixels", OverlayRadiationVisuals.RADIATION_PIXELS_OVERLAY);

        event.registerAboveAll("info_toast", OverlayInfoToast.OVERLAY);
        
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }
    
    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ItemTooltipComponent.class, ItemTooltipComponentRenderer::new);
        event.register(CrateContentsTooltipComponent.class, CrateContentsTooltipComponentRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        MainRegistry.LOGGER.info("Registering optimized shaders...");

        // Simple variant: no per-instance attributes, no USE_INSTANCING define.
        VertexFormat blockLitSimpleFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .build()
        );

        // Instanced variant: extended with InstPos/InstRot/InstBrightness attributes.
        VertexFormat blockLitInstancedFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("InstPos", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBrightness", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1))
                .build()
        );

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
                .define("USE_INSTANCING");

        com.hbm_m.client.render.shader.modification.ShaderModification slicedDefine =
            com.hbm_m.client.render.shader.modification.ShaderModification.builder()
                .define("USE_SLICED_LIGHT");

        com.hbm_m.client.render.shader.modification.ShaderModification instancedSlicedDefine =
            com.hbm_m.client.render.shader.modification.ShaderModification.builder()
                .define("USE_INSTANCING")
                .define("USE_SLICED_LIGHT");

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
                blockLitInstancedFormat
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
    public static void addTemplatesClient(BuildCreativeModeTabContentsEvent event) {
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<AssemblerRecipe> recipes = recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);
            
            // Собираем уникальные blueprintPool из всех рецептов
            Set<String> blueprintPools = new HashSet<>();
            for (AssemblerRecipe recipe : recipes) {
                String pool = recipe.getBlueprintPool();
                if (pool != null && !pool.isEmpty()) {
                    blueprintPools.add(pool);
                }
            }
            
            // Создаём папку для каждого уникального пула
            for (String pool : blueprintPools) {
                ItemStack folderStack = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
                ItemBlueprintFolder.writeBlueprintPool(folderStack, pool);
                event.accept(folderStack);
            }
            
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added {} blueprint folders to NTM Templates tab", blueprintPools.size());
            }
            
            // Добавляем шаблоны
            for (AssemblerRecipe recipe : recipes) {
                ItemStack templateStack = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
                ItemAssemblyTemplate.writeRecipeOutput(templateStack, recipe.getResultItem(null));
                event.accept(templateStack);
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
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Generic dummy armor model for all power armor items.
        event.registerLayerDefinition(ModModelLayers.POWER_ARMOR, PowerArmorEmptyModel::createBodyLayer);
    }
    //?}
}