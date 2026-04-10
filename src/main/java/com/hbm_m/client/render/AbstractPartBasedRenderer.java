package com.hbm_m.client.render;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractPartBasedRenderer<T extends BlockEntity, M extends BakedModel>
        implements BlockEntityRenderer<T> {

    /**
     * Получает модель для рендеринга. По умолчанию — из blockstate.
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

    private Matrix4f currentModelViewMatrix = new Matrix4f();
    private boolean gpuStateSetup = false;
    
    private static final net.minecraft.client.renderer.RenderType RT_SOLID = 
        net.minecraft.client.renderer.RenderType.solid();
    
    //  Убрать статический getter
    public Matrix4f getCurrentModelViewMatrix() {
        return new Matrix4f(currentModelViewMatrix);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Фикс: храним снимок матрицы, а не ссылку на мутабельный объект PoseStack.
        currentModelViewMatrix = new Matrix4f(poseStack.last().pose());
        
        if (!isInViewFrustum(blockEntity)) {
            return;
        }

        BakedModel rawModel = getModel(blockEntity);
        // Continuity (через Connector/FFAPI) оборачивает все blockstate-модели в CtmBakedModel/
        // EmissiveBakedModel, которые расширяют ForwardingBakedModel (Fabric FRAPI).
        // Разворачиваем, чтобы instanceof-проверка в getModelType() корректно работала.
        rawModel = unwrapFabricForwardingModels(rawModel);
        M model = getModelType(rawModel);
        
        if (model == null) return;

        LegacyAnimator animator = LegacyAnimator.create(poseStack, bufferSource,
                packedLight, packedOverlay);

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
     * Кешируется при первом успешном поиске, null — если FRAPI недоступен.
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
