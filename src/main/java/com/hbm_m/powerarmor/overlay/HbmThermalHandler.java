package com.hbm_m.powerarmor.overlay;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.PowerArmorClientState;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Тепловизор (FULL_SHADER): общий обработчик состояния/шейдера.
 *
 * Хуки на рендер-ивенты делаются в loader-specific коде (Forge event / Fabric callback),
 * но логика шейдера и проверок живёт здесь, чтобы класс существовал во всех таргетах.
 */
public class HbmThermalHandler implements ResourceManagerReloadListener {

    public static final HbmThermalHandler INSTANCE = new HbmThermalHandler();

    //? if fabric && < 1.21.1 {
    private static final ResourceLocation THERMAL_EFFECT = new ResourceLocation(MainRegistry.MOD_ID, "shaders/post/thermal.json");
    //?} else {
    /*private static final ResourceLocation THERMAL_EFFECT = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "shaders/post/thermal.json");
    *///?}

    private static @Nullable PostChain thermalChain;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean hasHotEntitiesThisFrame = false;

    private HbmThermalHandler() {}

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        cleanup();
    }

    public static void cleanup() {
        if (thermalChain != null) {
            thermalChain.close();
            thermalChain = null;
        }
    }

    public static boolean isThermalShaderModeActive() {
        return PowerArmorClientState.isThermalActive()
            && ModClothConfig.get().thermalRenderMode == ModClothConfig.ThermalRenderMode.FULL_SHADER;
    }

    /** Вызывать после рендера сущностей (аналоги AFTER_ENTITIES). */
    public static void onAfterEntities(PoseStack poseStack, float partialTick) {
        if (!isThermalShaderModeActive()) return;
        prepareAndRenderEntities(poseStack, partialTick);
    }

    /** Вызывать после рендера мира (аналоги AFTER_LEVEL). */
    public static void onAfterLevel(float partialTick) {
        if (!isThermalShaderModeActive()) return;
        applyPostProcess(partialTick);
    }

    private static boolean ensureChain(Minecraft mc) {
        if (thermalChain == null) {
            try {
                thermalChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), THERMAL_EFFECT);
                thermalChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                lastWidth = mc.getWindow().getWidth();
                lastHeight = mc.getWindow().getHeight();
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to load thermal shader", e);
                PowerArmorClientState.deactivateThermal();
                return false;
            }
        }

        if (lastWidth != mc.getWindow().getWidth() || lastHeight != mc.getWindow().getHeight()) {
            lastWidth = mc.getWindow().getWidth();
            lastHeight = mc.getWindow().getHeight();
            thermalChain.resize(lastWidth, lastHeight);
        }
        return true;
    }

    private static boolean hasAnyHotEntities(Minecraft mc) {
        hasHotEntitiesThisFrame = false;
        if (mc.level == null) return false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isHotEntity(entity)) {
                hasHotEntitiesThisFrame = true;
                return true;
            }
        }
        return false;
    }

    private static void prepareAndRenderEntities(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Fast pre-scan: if there are no hot entities at all, skip the entire thermal pass
        if (!hasAnyHotEntities(mc)) {
            hasHotEntitiesThisFrame = false;
            return;
        }

        if (!ensureChain(mc)) return;

        RenderTarget thermalBuffer = thermalChain.getTempTarget("thermal_buffer");
        if (thermalBuffer == null) return;

        // Save current shadow setting
        boolean oldShadowOption = mc.options.entityShadows().get();
        // Disable shadows globally to ensure no renderer tries to draw them
        mc.options.entityShadows().set(false);
        mc.getEntityRenderDispatcher().setRenderShadow(false);
        boolean pushedPose = false;

        try {
            thermalBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            thermalBuffer.clear(Minecraft.ON_OSX);

            // if (mc.getMainRenderTarget().isStencilEnabled() && !thermalBuffer.isStencilEnabled()) {
            //     thermalBuffer.enableStencil();
            // }
            try {
                thermalBuffer.copyDepthFrom(mc.getMainRenderTarget());
            } catch (Throwable depthCopyError) {
                // Никогда не рисуем "сквозь стены" в FULL_SHADER режиме: если depth не скопирован,
                // пропускаем thermal entities в этом кадре.
                MainRegistry.LOGGER.debug("Thermal depth copy failed, skipping thermal entity pass this frame", depthCopyError);
                hasHotEntitiesThisFrame = false;
                return;
            }
            
            thermalBuffer.bindWrite(true);
            RenderSystem.enableDepthTest();

            var camera = mc.gameRenderer.getMainCamera();
            var cameraPos = camera.getPosition();

            poseStack.pushPose();
            pushedPose = true;
            net.minecraft.client.renderer.MultiBufferSource.BufferSource originalBufferSource =
                    net.minecraft.client.renderer.MultiBufferSource.immediate(new BufferBuilder(256));
            ShadowIgnoringBufferSource bufferSource = new ShadowIgnoringBufferSource(originalBufferSource);

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (isHotEntity(entity)) {
                    double lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
                    double lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
                    double lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

                    mc.getEntityRenderDispatcher().render(
                            entity,
                            lerpX - cameraPos.x,
                            lerpY - cameraPos.y,
                            lerpZ - cameraPos.z,
                            entity.getViewYRot(partialTick),
                            partialTick,
                            poseStack,
                            bufferSource,
                            15728880
                    );
                }
            }

            originalBufferSource.endBatch();
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error rendering thermal entities", e);
        } finally {
            if (pushedPose) {
                poseStack.popPose();
            }
            // Restore shadows
            mc.options.entityShadows().set(oldShadowOption);
            mc.getEntityRenderDispatcher().setRenderShadow(oldShadowOption);
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void applyPostProcess(float partialTick) {
        // Если в этом кадре не было ни одной "горячей" сущности, то thermal_buffer пустой
        // и нет смысла выполнять фуллскрин-постпроцесс.
        if (thermalChain == null || !hasHotEntitiesThisFrame) {
            hasHotEntitiesThisFrame = false;
            return;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            thermalChain.process(partialTick);
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
        }
        RenderSystem.disableBlend();
        
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        // Сбрасываем флаг до следующего кадра
        hasHotEntitiesThisFrame = false;
    }

    private static boolean isHotEntity(Entity entity) {
        if (entity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return false;
        }
        return entity instanceof LivingEntity;
    }

    private static class ShadowIgnoringBufferSource implements net.minecraft.client.renderer.MultiBufferSource {
        private final net.minecraft.client.renderer.MultiBufferSource delegate;
        private final com.mojang.blaze3d.vertex.VertexConsumer noOp = new NoOpVertexConsumer();

        public ShadowIgnoringBufferSource(net.minecraft.client.renderer.MultiBufferSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(net.minecraft.client.renderer.RenderType type) {
            String name = type.toString().toLowerCase();

            // Тени/шлейфы/глинт и прочее лишнее нам не нужно в тепловизоре.
            // Возвращаем noOp, чтобы ItemRenderer не создавал дублирующиеся VertexMultiConsumer'ы
            if (name.contains("shadow") || name.contains("glint") || name.contains("foil")) {
                return noOp;
            }

            return delegate.getBuffer(type);
        }
    }

    private static class NoOpVertexConsumer implements com.mojang.blaze3d.vertex.VertexConsumer {
        @Override public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer uv(float u, float v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer uv2(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() {}
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}
    }

    // ======================================================================
    // Forge hook (optional): keeps old behaviour when Forge events available
    // ======================================================================
    //? if forge {
    /*@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.hbm_m.lib.RefStrings.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeHooks {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRenderLevel(net.minecraftforge.client.event.RenderLevelStageEvent event) {
            if (!HbmThermalHandler.isThermalShaderModeActive()) return;
            if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                HbmThermalHandler.onAfterEntities(event.getPoseStack(), event.getPartialTick());
            } else if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                HbmThermalHandler.onAfterLevel(event.getPartialTick());
            }
        }
    }
    *///?}
}