package com.hbm_m.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
//? if fabric {
/*import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import static dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register;
*///?}

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import dev.architectury.registry.registries.RegistrySupplier;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.loader.DoorModelLoader;
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
import com.hbm_m.datagen.assets.MissileItemModelDefinitions;
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
import com.hbm_m.inventory.gui.GUIMachineIndustrialBoiler;
import com.hbm_m.inventory.gui.GUIMachineSolarBoiler;
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
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.SchrabfogParticle;
import com.hbm_m.particle.custom.TownauraParticle;
import com.hbm_m.particle.custom.MissileContrailParticle;
import com.hbm_m.particle.custom.RadFogParticle;
import com.hbm_m.particle.explosions.basic.CameraShakeHandler;
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
import net.minecraft.world.level.block.Block;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?}

//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
@SuppressWarnings("UnstableApiUsage")
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

    //? if fabric {
    /*private static volatile boolean fabricShadersLoaded = false;

    private static void registerFabricRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(),
                ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get(),
                ModBlocks.MACHINE_ASSEMBLER.get(),
                ModBlocks.CHEMICAL_PLANT.get(),
                ModBlocks.CRYSTALLIZER.get(),
                // Multipart base (solid) + overlay (cutout) в Forge; на Fabric без слоя cutout оверлей с альфой не рисуется.
                ModBlocks.FLUID_DUCT.get(),
                ModBlocks.FLUID_DUCT_COLORED.get(),
                ModBlocks.FLUID_DUCT_SILVER.get());
    }

    private static void registerFabricShaders() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                @Override
                public net.minecraft.resources.ResourceLocation getFabricId() {
                    return new ResourceLocation(MainRegistry.MOD_ID, "shader_loader");
                }

                @Override
                public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                    loadFabricShaders(manager);
                }
            }
        );
    }

    private static void loadFabricShaders(net.minecraft.server.packs.resources.ResourceManager manager) {
        MainRegistry.LOGGER.info("Registering optimized shaders (Fabric)...");

        VertexFormat blockLitSimpleFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .build()
        );

        VertexFormat blockLitInstancedFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("BoneId", new VertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .put("InstPos", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBboxMin", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstBboxSize", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC45", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC67", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build()
        );

        VertexFormat blockLitInstancedSlicedFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("BoneId", new VertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .put("InstPos", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBboxMin", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstBboxSize", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS0C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS0C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS1C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS1C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS2C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS2C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS3C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS3C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build()
        );

        ResourceLocation realVsh = new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit.vsh");
        ResourceLocation virtualInstancedVsh = new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit_instanced.vsh");
        ResourceLocation virtualSlicedVsh = new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit_sliced.vsh");
        ResourceLocation virtualInstancedSlicedVsh = new ResourceLocation(MainRegistry.MOD_ID, "shaders/core/block_lit_instanced_sliced.vsh");
        net.minecraft.server.packs.resources.ResourceProvider hbmCoreShaderProvider = location -> {
            if ("minecraft".equals(location.getNamespace()) && location.getPath().startsWith("shaders/core/block_lit")) {
                ResourceLocation modLocation = new ResourceLocation(MainRegistry.MOD_ID, location.getPath());
                return manager.getResource(modLocation);
            }
            return manager.getResource(location);
        };

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
                hbmCoreShaderProvider, virtualInstancedVsh, realVsh, instancingDefine);

        net.minecraft.server.packs.resources.ResourceProvider slicedProvider =
            com.hbm_m.client.render.shader.modification.ShaderPreDefinitions.wrapRedirect(
                hbmCoreShaderProvider, virtualSlicedVsh, realVsh, slicedDefine);

        net.minecraft.server.packs.resources.ResourceProvider instancedSlicedProvider =
            com.hbm_m.client.render.shader.modification.ShaderPreDefinitions.wrapRedirect(
                hbmCoreShaderProvider, virtualInstancedSlicedVsh, realVsh, instancedSlicedDefine);

        try {
            ShaderInstance simpleShader = new ShaderInstance(
                hbmCoreShaderProvider,
                "block_lit_simple",
                blockLitSimpleFormat
            );
            ModShaders.setBlockLitSimpleShader(simpleShader);
            MainRegistry.LOGGER.info("Successfully registered block_lit_simple shader (Fabric)");

            ShaderInstance instancedShader = new ShaderInstance(
                instancedProvider,
                "block_lit_instanced",
                blockLitInstancedFormat
            );
            ModShaders.setBlockLitInstancedShader(instancedShader);
            MainRegistry.LOGGER.info("Successfully registered block_lit_instanced shader (Fabric)");

            ShaderInstance slicedShader = new ShaderInstance(
                slicedProvider,
                "block_lit_simple_sliced",
                blockLitSimpleFormat
            );
            ModShaders.setBlockLitSimpleSlicedShader(slicedShader);
            MainRegistry.LOGGER.info("Successfully registered block_lit_simple_sliced shader (Fabric)");

            ShaderInstance instancedSlicedShader = new ShaderInstance(
                instancedSlicedProvider,
                "block_lit_instanced_sliced",
                blockLitInstancedSlicedFormat
            );
            ModShaders.setBlockLitInstancedSlicedShader(instancedSlicedShader);
            MainRegistry.LOGGER.info("Successfully registered block_lit_instanced_sliced shader (Fabric)");

            fabricShadersLoaded = true;
        } catch (IOException e) {
            MainRegistry.LOGGER.error("Failed to register shaders on Fabric", e);
        }
    }
    *///?}

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

        event.enqueueWork(ClientSetup::registerRadAbsorberItemProperties);
        event.enqueueWork(ClientSetup::registerRbmkPelletItemProperties);
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

    private static void registerRadAbsorberItemProperties() {
        net.minecraft.client.renderer.item.ItemProperties.register(
                ModBlocks.RAD_ABSORBER.get().asItem(),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "tier"),
                (stack, level, entity, seed) -> BlockAbsorberItem.readTier(stack).ordinal()
        );
    }
    //?}

    private static void registerScreens() {
        MenuScreens.register(ModMenuTypes.CRYSTALLIZER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCrystallizer::new);
        MenuScreens.register(ModMenuTypes.BREEDER_MENU.get(), GUIMachineBreeder::new);
        MenuScreens.register(ModMenuTypes.LARGE_PYLON_MENU.get(), GUIMachineLargePylon::new);
        MenuScreens.register(ModMenuTypes.CYCLOTRON_MENU.get(), GUIMachineCyclotron::new);
        MenuScreens.register(ModMenuTypes.ZIRNOX_MENU.get(), GUIMachineZirnox::new);
        MenuScreens.register(ModMenuTypes.WATZ_POWERPLANT_MENU.get(), com.hbm_m.inventory.gui.GUIMachineWatzPowerplant::new);
        MenuScreens.register(ModMenuTypes.PWR_CONTROLLER_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePWRController::new);
        MenuScreens.register(ModMenuTypes.ARC_WELDER_MENU.get(), GUIMachineArcWelder::new);
        MenuScreens.register(ModMenuTypes.SOLDERING_STATION_MENU.get(), GUIMachineSolderingStation::new);
        MenuScreens.register(ModMenuTypes.MIXER_MENU.get(), GUIMachineMixer::new);
        MenuScreens.register(ModMenuTypes.DERRICK_MENU.get(), GUIMachineDerrick::new);
        MenuScreens.register(ModMenuTypes.COKER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoker::new);
        MenuScreens.register(ModMenuTypes.PYROOVEN_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePyroOven::new);
        MenuScreens.register(ModMenuTypes.SOLIDIFIER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSolidifier::new);
        MenuScreens.register(ModMenuTypes.ASHPIT_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAshpit::new);
        MenuScreens.register(ModMenuTypes.REACTOR_RESEARCH_MENU.get(), com.hbm_m.inventory.gui.GUIMachineReactorResearch::new);
        MenuScreens.register(ModMenuTypes.RADGEN_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRadGen::new);
        MenuScreens.register(ModMenuTypes.CRANE_INSERTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneInserter::new);
        MenuScreens.register(ModMenuTypes.CRANE_EXTRACTOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneExtractor::new);
        MenuScreens.register(ModMenuTypes.CRANE_GRABBER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneGrabber::new);
        MenuScreens.register(ModMenuTypes.CRANE_ROUTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneRouter::new);
        MenuScreens.register(ModMenuTypes.CRANE_BOXER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneBoxer::new);
        MenuScreens.register(ModMenuTypes.CRANE_UNBOXER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCraneUnboxer::new);
        MenuScreens.register(ModMenuTypes.DRONE_CRATE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneCrate::new);
        MenuScreens.register(ModMenuTypes.DRONE_PROVIDER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneProvider::new);
        MenuScreens.register(ModMenuTypes.DRONE_REQUESTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneRequester::new);
        MenuScreens.register(ModMenuTypes.DRONE_DOCK_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDroneDock::new);
        MenuScreens.register(ModMenuTypes.RADIO_TORCH_COUNTER_MENU.get(), com.hbm_m.inventory.gui.radio.GUIRadioTorchCounter::new);
        MenuScreens.register(ModMenuTypes.MACHINE_STORAGE_DRUM_MENU.get(), com.hbm_m.inventory.gui.GUIMachineStorageDrum::new);
        MenuScreens.register(ModMenuTypes.MACHINE_SIREN_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSiren::new);
        MenuScreens.register(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFirebox::new);
        MenuScreens.register(ModMenuTypes.MACHINE_KEYFORGE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineKeyforge::new);
        MenuScreens.register(ModMenuTypes.MACHINE_MASS_STORAGE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineMassStorage::new);
        MenuScreens.register(ModMenuTypes.RBMK_CONSOLE_MENU.get(), GUIMachineRbmkConsole::new);
        MenuScreens.register(ModMenuTypes.RBMK_ROD_MENU.get(), GUIRBMKRod::new);
        MenuScreens.register(ModMenuTypes.RBMK_CONTROL_MENU.get(), GUIRBMKControl::new);
        MenuScreens.register(ModMenuTypes.RBMK_CONTROL_AUTO_MENU.get(), com.hbm_m.inventory.gui.GUIRBMKControlAuto::new);
        MenuScreens.register(ModMenuTypes.RBMK_BOILER_MENU.get(), GUIRBMKBoiler::new);
        MenuScreens.register(ModMenuTypes.RBMK_HEATER_MENU.get(), GUIRBMKHeater::new);
        MenuScreens.register(ModMenuTypes.RBMK_STORAGE_MENU.get(), GUIRBMKStorage::new);
        MenuScreens.register(ModMenuTypes.RBMK_AUTOLOADER_MENU.get(), GUIRBMKAutoloader::new);
        MenuScreens.register(ModMenuTypes.RBMK_OUTGASSER_MENU.get(), GUIRBMKOutgasser::new);
        MenuScreens.register(ModMenuTypes.FLARE_STACK_MENU.get(), GUIMachineFlareStack::new);
        MenuScreens.register(ModMenuTypes.CORE_EMITTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoreEmitter::new);
        MenuScreens.register(ModMenuTypes.CORE_RECEIVER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoreReceiver::new);
        MenuScreens.register(ModMenuTypes.OILBURNER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineOilburner::new);
        MenuScreens.register(ModMenuTypes.HEATEX_MENU.get(), com.hbm_m.inventory.gui.GUIMachineHeatex::new);
        MenuScreens.register(ModMenuTypes.PUMPJACK_MENU.get(), GUIMachinePumpjack::new);
        MenuScreens.register(ModMenuTypes.RADAR_MENU.get(), GUIMachineRadarNT::new);
        MenuScreens.register(ModMenuTypes.RADAR_SLOTS_MENU.get(), GUIMachineRadarNTSlots::new);
        MenuScreens.register(ModMenuTypes.CRACKING_TOWER_MENU.get(), GUIMachineCrackingTower::new);
        MenuScreens.register(ModMenuTypes.FRACTION_TOWER_MENU.get(), GUIMachineFractionTower::new);
        MenuScreens.register(ModMenuTypes.MINING_DRILL_MENU.get(), GUIMachineMiningDrill::new);
        MenuScreens.register(ModMenuTypes.FEL_MENU.get(), GUIMachineFel::new);
        MenuScreens.register(ModMenuTypes.SILEX_MENU.get(), GUIMachineSilex::new);
        MenuScreens.register(ModMenuTypes.GAS_CENTRIFUGE_MENU.get(), GUIMachineGasCentrifuge::new);
        MenuScreens.register(ModMenuTypes.INDUSTRIAL_BOILER_MENU.get(), GUIMachineIndustrialBoiler::new);
        MenuScreens.register(ModMenuTypes.SOLAR_BOILER_MENU.get(), GUIMachineSolarBoiler::new);
        MenuScreens.register(ModMenuTypes.TURBINE_MENU.get(), GUIMachineTurbine::new);
        MenuScreens.register(ModMenuTypes.LARGE_TURBINE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineLargeTurbine::new);
        MenuScreens.register(ModMenuTypes.TURBINEGAS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineTurbineGas::new);
        MenuScreens.register(ModMenuTypes.STEAM_TURBINE_MENU.get(), GUIMachineSteamTurbine::new);
        MenuScreens.register(ModMenuTypes.SUBSTATION_MENU.get(), GUIMachineSubstation::new);
        MenuScreens.register(ModMenuTypes.CRUCIBLE_MENU.get(), GUIMachineCrucible::new);
        MenuScreens.register(ModMenuTypes.ARMOR_TABLE_MENU.get(), GUIArmorTable::new);
        MenuScreens.register(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), GUIMachineAssembler::new);
        MenuScreens.register(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), GUIMachineAdvancedAssembler::new);
        MenuScreens.register(ModMenuTypes.MACHINE_PRECASS_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePrecAss::new);
        MenuScreens.register(ModMenuTypes.MACHINE_DIFURNACE_RTG_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDifurnaceRtg::new);
        MenuScreens.register(ModMenuTypes.MACHINE_BATTERY_MENU.get(), GUIMachineBattery::new);
        MenuScreens.register(ModMenuTypes.BATTERY_SOCKET_MENU.get(), GUIBatterySocket::new);
        MenuScreens.register(ModMenuTypes.BLAST_FURNACE_MENU.get(), GUIBlastFurnace::new);
        MenuScreens.register(ModMenuTypes.HEATING_OVEN_MENU.get(), GUIHeatingOven::new);
        MenuScreens.register(ModMenuTypes.PRESS_MENU.get(), GUIMachinePress::new);
        MenuScreens.register(ModMenuTypes.SHREDDER_MENU.get(), GUIMachineShredder::new);
        MenuScreens.register(ModMenuTypes.ORE_SLOPPER_MENU.get(), GUIMachineOreSlopper::new);
        MenuScreens.register(ModMenuTypes.COMBINATION_OVEN_MENU.get(), GUIMachineCombinationOven::new);
        MenuScreens.register(ModMenuTypes.ARC_FURNACE_MENU.get(), GUIMachineArcFurnace::new);
        MenuScreens.register(ModMenuTypes.ANNIHILATOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAnnihilator::new);
        MenuScreens.register(ModMenuTypes.MINING_LASER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineMiningLaser::new);
        MenuScreens.register(ModMenuTypes.AMMO_PRESS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAmmoPress::new);
        MenuScreens.register(ModMenuTypes.EPRESS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineEPress::new);
        MenuScreens.register(ModMenuTypes.AUTOCRAFTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineAutocrafter::new);
        MenuScreens.register(ModMenuTypes.INDUSTRIAL_GENERATOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineIndustrialGenerator::new);
        MenuScreens.register(ModMenuTypes.DIESEL_GENERATOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineDieselGenerator::new);
        MenuScreens.register(ModMenuTypes.COMBUSTION_ENGINE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCombustionEngine::new);
        MenuScreens.register(ModMenuTypes.TURBOFAN_MENU.get(), com.hbm_m.inventory.gui.GUIMachineTurbofan::new);
        MenuScreens.register(ModMenuTypes.FUNNEL_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFunnel::new);
        MenuScreens.register(ModMenuTypes.PUREX_MENU.get(), com.hbm_m.inventory.gui.GUIMachinePUREX::new);
        MenuScreens.register(ModMenuTypes.WOOD_BURNER_MENU.get(), GUIMachineWoodBurner::new);
        MenuScreens.register(ModMenuTypes.TURRET_MENU.get(), com.hbm_m.inventory.gui.GUITurret::new);
        MenuScreens.register(ModMenuTypes.MISSILE_ASSEMBLY_MENU.get(), com.hbm_m.inventory.gui.GUIMissileAssembly::new);
        MenuScreens.register(ModMenuTypes.ANVIL_MENU.get(), GUIAnvil::new);
        MenuScreens.register(ModMenuTypes.CENTRIFUGE_MENU.get(), GUIMachineCentrifuge::new);
        MenuScreens.register(ModMenuTypes.IRON_CRATE_MENU.get(), GUIIronCrate::new);
        MenuScreens.register(ModMenuTypes.STEEL_CRATE_MENU.get(), GUISteelCrate::new);
        MenuScreens.register(ModMenuTypes.DESH_CRATE_MENU.get(), GUIDeshCrate::new);
        MenuScreens.register(ModMenuTypes.TUNGSTEN_CRATE_MENU.get(), GUITungstenCrate::new);
        MenuScreens.register(ModMenuTypes.TEMPLATE_CRATE_MENU.get(), GUITemplateCrate::new);
        MenuScreens.register(ModMenuTypes.FLUID_TANK_MENU.get(), GUIMachineFluidTank::new);
        MenuScreens.register(ModMenuTypes.BAT9000_MENU.get(), com.hbm_m.inventory.gui.GUIBat9000::new);
        MenuScreens.register(ModMenuTypes.ORBUS_MENU.get(), com.hbm_m.inventory.gui.GUIOrbus::new);
        MenuScreens.register(ModMenuTypes.MACHINE_RTG_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRtg::new);
        MenuScreens.register(ModMenuTypes.MACHINE_WASTE_DRUM_MENU.get(), com.hbm_m.inventory.gui.GUIMachineWasteDrum::new);
        MenuScreens.register(ModMenuTypes.BARREL_IRON_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelIron::new);
        MenuScreens.register(ModMenuTypes.BARREL_STEEL_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelSteel::new);
        MenuScreens.register(ModMenuTypes.BARREL_TCALLOY_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelTcalloy::new);
        MenuScreens.register(ModMenuTypes.BARREL_CORRODED_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelCorroded::new);
        MenuScreens.register(ModMenuTypes.BARREL_PLASTIC_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelPlastic::new);
        MenuScreens.register(ModMenuTypes.BARREL_ANTIMATTER_MENU.get(), com.hbm_m.inventory.gui.GUIBarrelAntimatter::new);
        MenuScreens.register(ModMenuTypes.CHEMICAL_PLANT_MENU.get(), GUIMachineChemicalPlant::new);
        MenuScreens.register(ModMenuTypes.CHEMICAL_FACTORY_MENU.get(), GUIMachineChemicalFactory::new);
        MenuScreens.register(ModMenuTypes.SOYUZ_LAUNCHER_MENU.get(), com.hbm_m.inventory.gui.GUISoyuzLauncher::new);
        MenuScreens.register(ModMenuTypes.MACHINE_SATLINKER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineSatLinker::new);
        MenuScreens.register(ModMenuTypes.FRACTURING_TOWER_MENU.get(), GUIMachineFrackingTower::new);
        MenuScreens.register(ModMenuTypes.REFINERY_MENU.get(), GUIMachineRefinery::new);
        MenuScreens.register(ModMenuTypes.LAUNCH_PAD_LARGE_MENU.get(), GUILaunchPadLarge::new);
        MenuScreens.register(ModMenuTypes.LAUNCH_PAD_RUSTED_MENU.get(), GUILaunchPadRusted::new);
        MenuScreens.register(ModMenuTypes.NUKE_FAT_MAN_MENU.get(), com.hbm_m.inventory.gui.GUINukeFatMan::new);
        MenuScreens.register(ModMenuTypes.NUKE_PROTOTYPE_MENU.get(), com.hbm_m.inventory.gui.GUINukePrototype::new);
        MenuScreens.register(ModMenuTypes.VACUUM_DISTILL_MENU.get(), com.hbm_m.inventory.gui.GUIMachineVacuumDistill::new);
        MenuScreens.register(ModMenuTypes.HYDROTREATER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineHydrotreater::new);
        MenuScreens.register(ModMenuTypes.CORE_INJECTOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCoreInjector::new);
        MenuScreens.register(ModMenuTypes.CATALYTIC_REFORMER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCatalyticReformer::new);
        MenuScreens.register(ModMenuTypes.LIQUEFACTOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineLiquefactor::new);
        MenuScreens.register(ModMenuTypes.FURNACE_IRON_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFurnaceIron::new);
        MenuScreens.register(ModMenuTypes.FURNACE_STEEL_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFurnaceSteel::new);
        MenuScreens.register(ModMenuTypes.ROTARY_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRotaryFurnace::new);
        MenuScreens.register(ModMenuTypes.STRAND_CASTER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineStrandCaster::new);
        MenuScreens.register(ModMenuTypes.MICROWAVE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineMicrowave::new);
        MenuScreens.register(ModMenuTypes.EXPOSURE_CHAMBER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineExposureChamber::new);
        MenuScreens.register(ModMenuTypes.RADIOLYSIS_MENU.get(), com.hbm_m.inventory.gui.GUIMachineRadiolysis::new);
        MenuScreens.register(ModMenuTypes.ELECTROLYSER_MENU.get(), com.hbm_m.inventory.gui.GUIMachineElectrolyser::new);
        MenuScreens.register(ModMenuTypes.COMPRESSOR_MENU.get(), com.hbm_m.inventory.gui.GUIMachineCompressor::new);
        MenuScreens.register(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), com.hbm_m.inventory.gui.GUIMachineElectricFurnace::new);
        MenuScreens.register(ModMenuTypes.FURNACE_BRICK_MENU.get(), com.hbm_m.inventory.gui.GUIMachineFurnaceBrick::new);
    }

    private static void registerRenderersCommon() {
        //? if forge {
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
        ModEntities.DIGAMMA_SPEAR.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.effect.SpearRenderer::new));
        ModEntities.RUBBLE.ifPresent(entityType -> EntityRenderers.register(entityType, RubbleEntityRenderer::new));

        BlockEntityRenderers.register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
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
        BlockEntityRenderers.register(ModBlockEntities.CARGO_ELEVATOR_BE.get(),
                com.hbm_m.client.render.implementations.CargoElevatorRenderer::new);
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
        BlockEntityRenderers.register(ModBlockEntities.RBMK_LOADER_BE.get(),        RBMKColumnRenderer::new);
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
        //?}

        //? if fabric {
        /*ModEntities.TURRET_BULLET.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.TURRET_ROCKET.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new ThrownItemRenderer<>(ctx)));
        ModEntities.GRENADE_NUC_PROJECTILE.ifPresent(entityType ->
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
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_ABM.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_MICRO.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_SCHRABIDIUM.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BHOLE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_TAINT.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_EMP.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_GENERIC.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_INCENDIARY.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_CLUSTER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BUSTER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DECOY.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_STEALTH.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_STRONG.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_INCENDIARY_STRONG.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_CLUSTER_STRONG.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BUSTER_STRONG.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_EMP_STRONG.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_BURST.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_INFERNO.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_RAIN.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DRILL.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_SHUTTLE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_NUCLEAR.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_NUCLEAR_CLUSTER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_VOLCANO.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DOOMSDAY.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.MISSILE_DOOMSDAY_RUSTED.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, MissileEntityRenderer::new));
        ModEntities.CLUSTER_ROCKET.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, com.hbm_m.client.render.projectile.ClusterRocketEntityRenderer::new));
        ModEntities.EMP_PULSE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, EmptyEntityRenderer::new));
        ModEntities.BLACK_HOLE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderBlackHole::new));
        ModEntities.VORTEX.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderBlackHole::new));
        ModEntities.RAGING_VORTEX.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderBlackHole::new));
        ModEntities.DIGAMMA_QUASAR.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderQuasar::new));
        ModEntities.RUBBLE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RubbleEntityRenderer::new));
        ModEntities.RAD_BEAST.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.mob.RADBeastRenderer::new));
        ModEntities.BOT_PRIME_HEAD.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.mob.BOTPrimeRenderer::head));
        ModEntities.BOT_PRIME_BODY.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.mob.BOTPrimeRenderer::body));
        ModEntities.UFO.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.mob.UFORenderer::new));
        ModEntities.BOMBER.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.plane.BomberRenderer::new));
        ModEntities.BOMBLET_ZETA.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.EmptyEntityRenderer::new));
        ModEntities.MASKMAN.ifPresent(entityType -> EntityRenderers.register(entityType, com.hbm_m.client.render.mob.MaskManRenderer::new));
        ModEntities.NOLO.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, NoloEntityRenderer::new));
        ModEntities.ENTITY_MOB_TAINTED_CREEPER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderCreeperUniversal::tainted));
        ModEntities.ENTITY_MOB_VOLATILE_CREEPER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderCreeperUniversal::volatileCreeper));
        ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderCreeperUniversal::phosgene));
        ModEntities.ENTITY_MIST.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, EmptyEntityRenderer::new));
        ModEntities.ENTITY_MOB_GOLD_CREEPER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderCreeperUniversal::goldCreeper));
        ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderCreeperUniversal::nuclear));

        // Airstrike + авиационные бомбы (иначе на Fabric entityRenderer == null → краш при рендере)
        ModEntities.AIRNUKEBOMB_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, AirNukeBombProjectileEntityRenderer::new));
        ModEntities.AIRBOMB_PROJECTILE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, AirBombProjectileEntityRenderer::new));
        ModEntities.AIRSTRIKE_NUKE_ENTITY.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, AirstrikeNukeEntityRenderer::new));
        ModEntities.AIRSTRIKE_ENTITY.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, AirstrikeEntityRenderer::new));
        ModEntities.AIRSTRIKE_AGENT_ENTITY.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new EmptyEntityRenderer<>(ctx)));

        ModEntities.NUKE_FALLOUT_RAIN.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderFallout::new));
        ModEntities.NUKE_MK3.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new EmptyEntityRenderer<>(ctx)));
        ModEntities.CLOUD_FLEIJA.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, RenderCloudFleija::new));
        ModEntities.NUKE_MK5.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, ctx -> new EmptyEntityRenderer<>(ctx)));
        ModEntities.FALLING_SELLAFIT_ENTITY_TYPE.ifPresent(entityType ->
                EntityRendererRegistry.register(entityType, FallingBlockRenderer::new));

        register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
        register(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerRenderer::new);
        register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
        register(ModBlockEntities.PRESS_BE.get(), MachinePressRenderer::new);
        register(ModBlockEntities.CHEMICAL_PLANT_BE.get(), MachineChemicalPlantRenderer::new);
        register(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), MachineHydraulicFrackiningTowerRenderer::new);
        register(ModBlockEntities.CRYSTALLIZER.get(), MachineCrystallizerRenderer::new);
        register(ModBlockEntities.HEATING_OVEN_BE.get(), HeatingOvenRenderer::new);
        register(ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), IndustrialTurbineRenderer::new);
        register(ModBlockEntities.BATTERY_SOCKET_BE.get(), BatterySocketCreativeRenderer::new);
        register(ModBlockEntities.COOLING_TOWER_BE.get(), MachineCoolingTowerRenderer::new);
        register(ModBlockEntities.GAS_CENTRIFUGE_BE.get(), GasCentrifugeRenderer::new);
        register(ModBlockEntities.MINING_DRILL_BE.get(), com.hbm_m.client.render.implementations.MachineMiningDrillRenderer::new);
        register(ModBlockEntities.ORE_SLOPPER_BE.get(), com.hbm_m.client.render.implementations.MachineOreSlopperRenderer::new);
        register(ModBlockEntities.ARC_FURNACE_BE.get(), com.hbm_m.client.render.implementations.MachineArcFurnaceRenderer::new);
        register(ModBlockEntities.TURRET_SENTRY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_CHEKHOV_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_FRIENDLY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_JEREMY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_TAUON_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_RICHARD_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_HOWARD_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_MAXWELL_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_FRITZ_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_ARTY_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.TURRET_HIMARS_BE.get(), com.hbm_m.client.render.implementations.MachineTurretRenderer::new);
        register(ModBlockEntities.RADAR_BE.get(), MachineRadarRenderer::new);
        register(ModBlockEntities.RADAR_SCREEN_BE.get(), com.hbm_m.client.render.implementations.MachineRadarScreenRenderer::new);
        register(ModBlockEntities.LAUNCH_PAD_BE.get(), LaunchPadMissileRenderer::new);
        register(ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), LaunchPadMissileRenderer::new);
        register(ModBlockEntities.CRUCIBLE_BE.get(), CrucibleRenderer::new);
        register(ModBlockEntities.FOUNDRY_BASIN_BE.get(), com.hbm_m.client.render.implementations.FoundryBasinRenderer::new);
        register(ModBlockEntities.FOUNDRY_CHANNEL_BE.get(), com.hbm_m.client.render.implementations.FoundryChannelRenderer::new);
        register(ModBlockEntities.FLUID_TANK_BE.get(), MachineFluidTankRenderer::new);
        *///?}
    }

    private static void registerParticlesCommon() {
//        Particles registered via Forge event below.
        //? if forge {

        //?}

        //? if fabric {
        /*ParticleFactoryRegistry.getInstance().register(ModParticleTypes.TOWNAURA.get(), TownauraParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.SCHRABFOG.get(), SchrabfogParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.RAD_FOG_PARTICLE.get(), RadFogParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.RBMK_FLAME.get(), com.hbm_m.particle.custom.RBMKFlameParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.RBMK_STEAM.get(), com.hbm_m.particle.custom.RBMKSteamParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.RBMK_MUSH.get(), com.hbm_m.particle.custom.RBMKMushParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.DIGAMMA_SMOKE.get(), com.hbm_m.particle.custom.DigammaSmokeParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticleTypes.MISSILE_CONTRAIL.get(), MissileContrailParticle.Provider::new);
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
           ModItems.PIPE_TITANIUM.get(), ModItems.PIPE_ALUMINUM.get(), ModItems.PIPE_DURA_STEEL.get());

        ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> {
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
        *///?}
    }

    private static void registerReloadListenersCommon() {
        //? if fabric {
        /*// Критично: без вызова registerFabricShaders на Fabric не выполнялся loadFabricShaders — instanced shader == null,
        // flushBatchVanilla молча сбрасывал батч (машины пропадали), renderSingle уходил в putBulkData с WARN на каждый quad.
        registerFabricRenderLayers();
        registerFabricShaders();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
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
        // Forge: ClientModEvents.onRenderLevelStage(AFTER_BLOCK_ENTITIES)
        //? if fabric {
        /*ClientRenderHandlerFabric.register();
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
            com.hbm_m.client.render.culling.InstancedRenderFrame.clear();
            MachineAdvancedAssemblerRenderer.clearCaches();
            MachineAssemblerRenderer.clearCaches();
            MachineHydraulicFrackiningTowerRenderer.clearCaches();
            DoorRenderer.clearAllCaches();
            MachinePressRenderer.clearCaches();
            MachineChemicalPlantRenderer.clearCaches();
            MachineCrystallizerRenderer.clearCaches();
            MachineRadarRenderer.clearCaches();
            MeshRenderCache.clearAll();
            com.hbm_m.client.render.MdiGeometryAtlas.resetForResourceLifecycle();
            AbstractObjArmorLayer.clearAllCaches();
        });
    }

    public static void addTemplatesClient(java.util.function.Consumer<ItemStack> acceptor) {
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<AssemblerRecipe> recipes = recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);

            // Собираем уникальные blueprint_pool из сборочной машины и химзавода
            Set<String> blueprintPools = new HashSet<>();
            for (AssemblerRecipe recipe : recipes) {
                String pool = recipe.getBlueprintPool();
                if (pool != null && !pool.isEmpty()) {
                    blueprintPools.add(pool);
                }
            }
            for (ChemicalPlantRecipe chem : recipeManager.getAllRecipesFor(ChemicalPlantRecipe.Type.INSTANCE)) {
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
                ItemAssemblyTemplate.writeRecipeOutput(templateStack, recipe.getResultItem(null));
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
    /**
     * After bake (and after Item Transform Helper, if present): install Forge-safe display wrappers for
     * {@code isCustomRenderer} {@code hbm_m} item models so JSON {@code display} matches with/without ITH.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBakingCompletedDisplayGuards(ModelEvent.BakingCompleted event) {
        com.hbm_m.client.compat.ItemTransformHelperCompat.installDisplayTransformGuards(
                event.getModelBakery().getBakedTopLevelModels());
    }

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

        wrapConnectedDecoCtTerrainModels(models);
        com.hbm_m.client.compat.ItemTransformHelperCompat.installDisplayTransformGuards(models);
    }

    /**
     * Подменяет cube-модели деко-CT на {@link ConnectedDecoBlockBakedModel} (Forge ModelData + getQuads).
     * Делается после снятия Continuity-обёртки, иначе CT не получает корректный пайплайн.
     */
    private static void wrapConnectedDecoCtTerrainModels(Map<ResourceLocation, BakedModel> models) {
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
            ModelResourceLocation loc = new ModelResourceLocation(e.block.getId(), "");
            BakedModel baked = models.get(loc);
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
        // Регистрируем модели вариантов дверей, чтобы они загружались в ModelManager
        // round_airlock_door
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/doors/round_airlock_door_legacy"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_legacy"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/crystallizer_fluid"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_fluid"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/crystallizer_spinner"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_spinner"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/mining_drill_bit"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_bit"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/mining_drill_shaft"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_shaft"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/mining_drill_crusher1"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_crusher1"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/mining_drill_crusher2"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_crusher2"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/ore_slopper_fan"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_fan"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/ore_slopper_blades_left"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_blades_left"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/ore_slopper_blades_right"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/ore_slopper_blades_right"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/arc_furnace_electrodes_cold"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/arc_furnace_electrodes_cold"));
        //?}

        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/machines/arc_furnace_electrodes_hot"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/arc_furnace_electrodes_hot"));
        //?}

        // Soyuz rocket mesh - fetched directly via ModelManager for the launcher's mounted-rocket
        // preview and the SoyuzEntity/SoyuzCapsuleEntity renderers (see SoyuzLauncherRenderer).
        //? if fabric && < 1.21.1 {
        /*event.register(new ResourceLocation(RefStrings.MODID, "block/deco_soyuz_rocket"));
        *///?} else {
                event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket"));
        //?}

        // Turret-Animationsteile (siehe TurretModel / MachineTurretRenderer) - lose Modelle, per OBJ-"visibility"
        // aus den geteilten turret_*.obj-Dateien herausgeloest, direkt per ModelManager im BER abgerufen.
        for (String part : new String[] {
                "chekhov_carriage", "chekhov_carriage_friendly", "chekhov_body", "chekhov_barrels",
                "jeremy_gun", "tauon_cannon", "tauon_rotor", "richard_launcher",
                "howard_carriage", "howard_body", "howard_barrelstop", "howard_barrelsbottom",
                "fritz_gun", "maxwell_microwave",
                "arty_carriage", "arty_cannon", "arty_barrel",
                "himars_carriage", "himars_launcher", "himars_crane",
                "sentry_pivot", "sentry_body", "sentry_drum", "sentry_barrell", "sentry_barrelr"
        }) {
            event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/turret_parts/" + part));
        }

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


        for (MissileItemModelDefinitions.Definition definition : MissileItemModelDefinitions.all()) {
            ResourceLocation meshId = MissileRenderHelper.meshModelId(
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, definition.itemPath()));
            //? if fabric && < 1.21.1 {
            /*event.register(meshId);
            *///?} else {
            event.register(meshId);
            //?}
        }

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
        event.register("missile_loader", new MissileModelLoader());
        event.register("heating_oven_loader", new HeatingOvenModelLoader());
        event.register("cooling_tower_loader", new MachineCoolingTowerModelLoader());
        event.register("radar_loader", new MachineRadarModelLoader());
        event.register("soyuz_launcher_loader", new com.hbm_m.client.loader.SoyuzLauncherModelLoader());
        event.register("soyuz_rocket_loader", new com.hbm_m.client.loader.SoyuzRocketModelLoader());

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
        event.registerEntityRenderer(ModEntities.DIGAMMA_SPEAR.get(), com.hbm_m.client.render.effect.SpearRenderer::new);
        event.registerEntityRenderer(ModEntities.RUBBLE.get(), RubbleEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.RAD_BEAST.get(), com.hbm_m.client.render.mob.RADBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.BOT_PRIME_HEAD.get(), com.hbm_m.client.render.mob.BOTPrimeRenderer::head);
        event.registerEntityRenderer(ModEntities.BOT_PRIME_BODY.get(), com.hbm_m.client.render.mob.BOTPrimeRenderer::body);
        event.registerEntityRenderer(ModEntities.UFO.get(), com.hbm_m.client.render.mob.UFORenderer::new);
        event.registerEntityRenderer(ModEntities.BOMBER.get(), com.hbm_m.client.render.plane.BomberRenderer::new);
        // The bomblet is a 0.5-block object falling at terminal velocity; the original renders a
        // small model, but it is on screen for a fraction of a second either way.
        event.registerEntityRenderer(ModEntities.BOMBLET_ZETA.get(), com.hbm_m.client.render.EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MASKMAN.get(), com.hbm_m.client.render.mob.MaskManRenderer::new);
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

    public static void onClientDisconnect(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientCachesDeferred();
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Связываем наш ТИП частицы с ее ФАБРИКОЙ.
        event.registerSpriteSet(ModParticleTypes.TOWNAURA.get(), TownauraParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SCHRABFOG.get(), SchrabfogParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RAD_FOG_PARTICLE.get(), RadFogParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_FLAME.get(), com.hbm_m.particle.custom.RBMKFlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_STEAM.get(), com.hbm_m.particle.custom.RBMKSteamParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RBMK_MUSH.get(), com.hbm_m.particle.custom.RBMKMushParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.DIGAMMA_SMOKE.get(), com.hbm_m.particle.custom.DigammaSmokeParticle.Provider::new);
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

        // Instanced variant: per-vertex BoneId + InstPos/InstRot/… (см. InstancedStaticPartRenderer VAO).
        VertexFormat blockLitInstancedFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("BoneId", new VertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .put("InstPos", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBboxMin", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstBboxSize", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC45", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightC67", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .build()
        );

        VertexFormat blockLitInstancedSlicedFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("BoneId", new VertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1))
                .put("InstPos", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstRot", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstBboxMin", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3))
                .put("InstBboxSize", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS0C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS0C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS1C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS1C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS2C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS2C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS3C01", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
                .put("InstLightS3C23", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4))
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
    //?}
}
