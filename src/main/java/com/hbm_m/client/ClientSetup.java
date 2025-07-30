package com.hbm_m.client;

import com.hbm_m.client.overlay.ArmorTableGUI;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.client.overlay.GeigerOverlay;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.ModMenuTypes;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.DarkParticle;
import com.hbm_m.particle.custom.RadFogParticle;
import com.hbm_m.block.ModBlocks;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.GraphicsStatus;
import net.minecraftforge.client.ChunkRenderTypeSet;

import javax.annotation.Nonnull;
import java.util.Map;

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
        // MinecraftForge.EVENT_BUS.register(ModBlockEntities.class);

        event.enqueueWork(() -> {
            // Здесь мы связываем наш тип меню с классом экрана
            MenuScreens.register(ModMenuTypes.ARMOR_TABLE_MENU.get(), ArmorTableGUI::new);
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
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ModConfigKeybindHandler.onRegisterKeyMappings(event);
        MainRegistry.LOGGER.info("Registered key mappings.");
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
        
        // Регистрируем наш оверлей.
        // Мы говорим: "Нарисуй оверлей с ID 'geiger_counter_hud' НАД хотбаром,
        // используя логику из объекта GeigerOverlay.GEIGER_HUD_OVERLAY".
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "geiger_counter_hud", GeigerOverlay.GEIGER_HUD_OVERLAY);
        
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }


    private static class LeavesModelWrapper extends BakedModelWrapper<BakedModel> {

        public LeavesModelWrapper(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(@Nonnull BlockState state, @Nonnull RandomSource rand, @Nonnull ModelData data) {
            
            GraphicsStatus graphics = Minecraft.getInstance().options.graphicsMode().get();
            
            if (graphics == GraphicsStatus.FANCY || graphics == GraphicsStatus.FABULOUS) {
                // Используем ChunkRenderTypeSet.of()
                return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
            }
            
            return ChunkRenderTypeSet.of(RenderType.solid());
        }
    }
}