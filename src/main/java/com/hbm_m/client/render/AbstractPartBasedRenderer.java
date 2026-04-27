package com.hbm_m.client.render;


import java.lang.reflect.Field;

import org.joml.Matrix4f;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public abstract class AbstractPartBasedRenderer<T extends BlockEntity, M extends BakedModel>
        implements BlockEntityRenderer<T> {

    /**
     * Получает модель для рендеринга. По умолчанию - из blockstate.
     * Можно переопределить для выбора модели по данным BlockEntity (например, двери с разными скинами).
     */
    protected BakedModel getModel(T blockEntity) {
        return Minecraft.getInstance().getBlockRenderer()
            .getBlockModel(blockEntity.getBlockState());
    }

    protected abstract M getModelType(BakedModel rawModel);
    protected abstract Direction getFacing(T blockEntity);
    protected abstract void renderParts(T blockEntity, M model, LegacyAnimator animator, float partialTick,
                                        int packedLight, int packedOverlay, PoseStack poseStack, MultiBufferSource bufferSource);

    /** Поворот/сдвиг блока в локальных координатах перед {@link #renderParts}. */
    protected void setupBlockTransform(LegacyAnimator animator, T blockEntity) {
        animator.setupBlockTransform(getFacing(blockEntity));
    }

    /**
     * Snapshot of the most-recent {@code poseStack.last().pose()} captured at the
     * start of {@link #render}. Reused (mutated in place) rather than reallocated
     * to keep this hot per-BE method allocation-free; downstream callers that
     * need a stable copy go through {@link #getCurrentModelViewMatrix()} which
     * does the defensive copy on demand.
     */
    private final Matrix4f currentModelViewMatrix = new Matrix4f();
    private boolean gpuStateSetup = false;
    
    private static final net.minecraft.client.renderer.RenderType RT_SOLID = 
        net.minecraft.client.renderer.RenderType.solid();
    
    /** Defensive copy - callers may not mutate the renderer's snapshot field. */
    public Matrix4f getCurrentModelViewMatrix() {
        return new Matrix4f(currentModelViewMatrix);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Frustum cull FIRST - every cycle spent on the matrix snapshot,
        // model lookup, FRAPI unwrap and LegacyAnimator construction below is
        // wasted on a BE that the camera cannot see. With dense Iris shadow
        // dispatch this method is invoked on every visible AND every shadow-
        // frustum-visible BE per frame, so this short-circuit is the single
        // cheapest cull we have.
        if (!isInViewFrustum(blockEntity)) {
            return;
        }

        // Mutate the persistent snapshot in place - no Matrix4f allocation per
        // BE per pass. The field is private and only read by getCurrentModelViewMatrix
        // (which makes its own defensive copy), so the in-place update is safe.
        currentModelViewMatrix.set(poseStack.last().pose());

        BakedModel rawModel = getModel(blockEntity);
        // Continuity (через Connector/FFAPI) оборачивает все blockstate-модели в CtmBakedModel/
        // EmissiveBakedModel, которые расширяют ForwardingBakedModel (Fabric FRAPI).
        // Разворачиваем, чтобы instanceof-проверка в getModelType() корректно работала.
        rawModel = unwrapFabricForwardingModels(rawModel);
        M model = getModelType(rawModel);
        
        if (model == null) return;

        LegacyAnimator animator = LegacyAnimator.create(poseStack, bufferSource,
                packedLight, packedOverlay);

        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);

        poseStack.pushPose();
        try {
            setupBlockTransform(animator, blockEntity);
            renderParts(blockEntity, model, animator, partialTick, packedLight, packedOverlay, poseStack, bufferSource);
        } finally {
            poseStack.popPose();
            if (gpuStateSetup) {
                RT_SOLID.clearRenderState();
                Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
                gpuStateSetup = false;
            }
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
    }

    protected final Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    protected boolean isInViewFrustum(T blockEntity) {
        return true;
    }

    // -----------------------------------------------------------------------
    // Совместимость с Fabric FRAPI (Continuity, Emissive и т.д.)
    // -----------------------------------------------------------------------

    /**
     * Поле 'wrapped' в ForwardingBakedModel (Fabric FRAPI).
     * Кешируется при первом успешном поиске, null - если FRAPI недоступен.
     */
    private static Field fabricWrappedField;
    private static boolean fabricWrappedFieldChecked = false;

    /**
     * Разворачивает цепочку {@code ForwardingBakedModel} обёрток (Continuity CtmBakedModel,
     * EmissiveBakedModel и т.п.) до исходной модели.
     *
     * <p>Continuity через Connector оборачивает все blockstate-модели в {@code CtmBakedModel}
     * (extends {@code ForwardingBakedModel}), из-за чего instanceof-проверки в {@link #getModelType}
     * возвращают null и блок становится невидимым.
     */
    public static BakedModel unwrapFabricForwardingModels(BakedModel model) {
        if (model == null) return null;

        if (!fabricWrappedFieldChecked) {
            fabricWrappedFieldChecked = true;
            try {
                Class<?> cls = Class.forName("net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel");
                Field f = cls.getDeclaredField("wrapped");
                f.setAccessible(true);
                fabricWrappedField = f;
            } catch (ClassNotFoundException ignored) {
                // FRAPI недоступен в окружении
            } catch (Exception e) {
                MainRegistry.LOGGER.warn("[HBM] Не удалось получить поле ForwardingBakedModel.wrapped: {}", e.toString());
            }
        }

        if (fabricWrappedField == null) return model;

        int depth = 0;
        while (depth++ < 8) {
            Class<?> cls = model.getClass();
            boolean isFrapi = false;
            while (cls != null && cls != Object.class) {
                if (cls.getName().equals("net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel")) {
                    isFrapi = true;
                    break;
                }
                cls = cls.getSuperclass();
            }
            if (!isFrapi) break;

            try {
                BakedModel inner = (BakedModel) fabricWrappedField.get(model);
                if (inner == null || inner == model) break;
                if (depth == 1) {
                    MainRegistry.LOGGER.debug("[HBM] Разворачиваем {} → {}",
                            model.getClass().getSimpleName(), inner.getClass().getSimpleName());
                }
                model = inner;
            } catch (Exception e) {
                break;
            }
        }
        return model;
    }
}
