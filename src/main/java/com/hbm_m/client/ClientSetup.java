package com.hbm_m.client;

// Основной класс клиентской настройки мода. Здесь регистрируются все клиентские обработчики событий,
// GUI, рендереры, модели и т.д.
import com.hbm_m.client.overlay.*;
import com.hbm_m.client.loader.*;
import com.hbm_m.client.overlay.crates.*;
import com.hbm_m.client.render.*;
import com.hbm_m.client.render.shader.*;
import com.hbm_m.config.*;
import com.hbm_m.client.tooltip.*;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.custom.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.custom.industrial.ItemBlueprintFolder;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.ModMenuTypes;
// import com.hbm_m.multiblock.DoorPartAABBRegistry;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.recipe.AssemblerRecipe;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.google.common.collect.ImmutableMap;
import com.hbm_m.particle.custom.*;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.doors.DoorDeclRegistry;
import com.hbm_m.block.entity.ModBlockEntities;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.client.GraphicsStatus;
import net.minecraftforge.client.ChunkRenderTypeSet;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MainRegistry.LOGGER.info("FMLClientSetupEvent fired. Registering client-side FORGE event handlers.");
        // Обработчики, которые должны работать каждый тик/каждое событие в игре,
        // регистрируются на шине MinecraftForge.EVENT_BUS.
        MinecraftForge.EVENT_BUS.register(ModConfigKeybindHandler.class);
        MinecraftForge.EVENT_BUS.register(DarkParticleHandler.class);
        MinecraftForge.EVENT_BUS.register(ChunkRadiationDebugRenderer.class);
        MinecraftForge.EVENT_BUS.register(ClientRenderHandler.class);
        // MinecraftForge.EVENT_BUS.register(DoorOutlineRenderer.class);
        // MinecraftForge.EVENT_BUS.register(DoorDebugRenderer.class);
        // MinecraftForge.EVENT_BUS.register(ClientSetup.class);

        // Register Entity Renders
        ModEntities.GRENADE_NUC_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_FIRE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_SLIME_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_HE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADEHE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADEFIRE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADESMART_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADESLIME_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );

        // MinecraftForge.EVENT_BUS.register(new ClientTickHandler());

        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.ORE_ACIDIZER_MENU.get(), com.hbm_m.client.screen.OreAcidizerScreen::new);
            MenuScreens.register(ModMenuTypes.ARMOR_TABLE_MENU.get(), GUIArmorTable::new);
            MenuScreens.register(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), GUIMachineAssembler::new);
            MenuScreens.register(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), GUIMachineAdvancedAssembler::new);
            MenuScreens.register(ModMenuTypes.MACHINE_BATTERY_MENU.get(), GUIMachineBattery::new);
            MenuScreens.register(ModMenuTypes.BLAST_FURNACE_MENU.get(), GUIBlastFurnace::new);
            MenuScreens.register(ModMenuTypes.PRESS_MENU.get(), GUIMachinePress::new);
            MenuScreens.register(ModMenuTypes.SHREDDER_MENU.get(), GUIMachineShredder::new);
            MenuScreens.register(ModMenuTypes.WOOD_BURNER_MENU.get(), GUIMachineWoodBurner::new);
            MenuScreens.register(ModMenuTypes.ANVIL_MENU.get(), GUIAnvil::new);
            MenuScreens.register(ModMenuTypes.CENTRIFUGE_MENU.get(), GUIMachineCentrifuge::new);
            MenuScreens.register(ModMenuTypes.IRON_CRATE_MENU.get(), GUIIronCrate::new);
            MenuScreens.register(ModMenuTypes.STEEL_CRATE_MENU.get(), GUISteelCrate::new);
            MenuScreens.register(ModMenuTypes.DESH_CRATE_MENU.get(), GUIDeshCrate::new);
            MenuScreens.register(ModMenuTypes.FLUID_TANK_MENU.get(), GUIMachineFluidTank::new);

            // Register BlockEntity renderers
            BlockEntityRenderers.register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PRESS_BE.get(), MachinePressRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CHEMICAL_PLANT_BE.get(), ChemicalPlantRenderer::new);

            OcclusionCullingHelper.setTransparentBlocksTag(ModTags.Blocks.NON_OCCLUDING);
            MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientDisconnect);
            MainRegistry.LOGGER.info("VBO render system initialized successfully");
        });
    }



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

    @SubscribeEvent
    public static void onModelRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Регистрируем модели вариантов дверей, чтобы они загружались в ModelManager
        // round_airlock_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_clean"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/round_airlock_door_modern_green"));
        // large_vehicle_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/large_vehicle_door_modern"));
        // fire_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_black"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/fire_door_modern_orange"));
        // secure_access_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/secure_access_door_modern_gray"));
        // water_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/water_door_modern"));
        // qe_containment_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_containment_door_modern"));
        // qe_sliding_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/qe_sliding_door_modern"));
        // sliding_blast_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant1"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_blast_door_modern_variant2"));
        // sliding_seal_door
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_legacy"));
        event.register(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/sliding_seal_door_modern"));
        MainRegistry.LOGGER.debug("Registered door variant models for loading");
    }

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterGeometryLoaders event) {
        // DoorDeclRegistry.init();
        MainRegistry.LOGGER.info("DoorDeclRegistry initialized with {} doors", DoorDeclRegistry.getAll().size());

        event.register("procedural_wire", new ProceduralWireLoader());
        event.register("advanced_assembly_machine_loader", new MachineAdvancedAssemblerModelLoader());
        event.register("door", new DoorModelLoader());
        event.register("template_loader", new TemplateModelLoader());
        event.register("press_loader", new PressModelLoader());

        MainRegistry.LOGGER.info("Registered geometry loaders: procedural_wire, advanced_assembly_machine_loader, template_loader, door, press_loader");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ModConfigKeybindHandler.onRegisterKeyMappings(event);
        MainRegistry.LOGGER.info("Registered key mappings.");
    }
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.AIRNUKEBOMB_PROJECTILE.get(),
                AirNukeBombProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRBOMB_PROJECTILE.get(),
                AirBombProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRSTRIKE_NUKE_ENTITY.get(), AirstrikeNukeEntityRenderer::new);

        event.registerEntityRenderer(ModEntities.AIRSTRIKE_ENTITY.get(), AirstrikeEntityRenderer::new);
    }
    @SubscribeEvent
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ShaderReloadListener());
        event.registerReloadListener(com.hbm_m.client.model.variant.DoorModelRegistry.getInstance());
        event.registerReloadListener((preparationBarrier, resourceManager,
                preparationsProfiler, reloadProfiler,
                backgroundExecutor, gameExecutor) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                // КРИТИЧНО: Откладываем очистку кэшей на render thread, чтобы избежать
                // race condition с активным рендером (EXCEPTION_ACCESS_VIOLATION при
                // включении шейдера — clearCaches вызывался во время render pass).
                com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> {
                    try {
                        MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
                        MachineAdvancedAssemblerRenderer.clearCaches();
                        DoorRenderer.clearAllCaches();
                        MachinePressRenderer.clearCaches();
                        GlobalMeshCache.clearAll();
                        MainRegistry.LOGGER.info("VBO cache cleanup completed (deferred to render thread)");
                    } catch (Exception e) {
                        MainRegistry.LOGGER.error("Error during deferred VBO cache cleanup", e);
                    }
                });
            }, gameExecutor);
        });
    }

    public static void onClientDisconnect(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        MainRegistry.LOGGER.info("Client disconnecting, clearing VBO caches...");
        DoorRenderer.clearAllCaches();
        MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
        MachinePressRenderer.clearCaches();
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

        event.registerAbove(VanillaGuiOverlay.PORTAL.id(), "radiation_pixels", OverlayRadiationVisuals.RADIATION_PIXELS_OVERLAY);
        
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }
    
    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ItemTooltipComponent.class, ItemTooltipComponentRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        MainRegistry.LOGGER.info("Registering optimized shaders...");
        
        VertexFormat blockLitFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION) // Loc 0
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)   // Loc 1
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)      // Loc 2
                
                // Новые атрибуты:
                .put("InstPos", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3)) // Loc 3
                .put("InstRot", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4)) // Loc 4
                .put("InstBrightness", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1)) // Loc 5
                
                .build()
        );
        
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block_lit"),
                blockLitFormat
            ),
            ModShaders::setBlockLitShader
        );
        MainRegistry.LOGGER.info("Successfully registered block_lit shader");
    }

    private static class LeavesModelWrapper extends BakedModelWrapper<BakedModel> {

        public LeavesModelWrapper(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(@Nonnull BlockState state, @Nonnull RandomSource rand, @Nonnull ModelData data) {
            
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
}