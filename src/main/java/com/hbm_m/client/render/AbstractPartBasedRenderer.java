package com.hbm_m.client.render;

import java.lang.reflect.Field;

import org.joml.Matrix4f;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public abstract class AbstractPartBasedRenderer<T extends BlockEntity, M extends BakedModel>
        implements com.hbm_m.client.render.HbmBerBounds<T> {

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
    
    /** Defensive copy - callers may not mutate the renderer's snapshot field. */
    public Matrix4f getCurrentModelViewMatrix() {
        return new Matrix4f(currentModelViewMatrix);
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Frustum cull FIRST for the main pass. Shadow pass uses light-space
        // bounds; the main-camera frustum here would drop off-screen casters.
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            // ── Диагностика 1.21.1 (машины не отбрасывают теней под Iris):
            // счётчик вызовов BER внутри теневого прохода; логируется из
            // ClientModEvents (AFTER_SKY основного прохода). 0 вызовов =
            // Iris вообще не зовёт BER в shadow (пустой список теневых BE,
            // интероп terrain-мода) — тогда проблема не в нашем draw-пути.
            SHADOW_BER_INVOCATIONS++;
        } else if (!isInViewFrustum(blockEntity)) {
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
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
    }

    protected final Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    /** Диагностика shadow pass: вызовы BER с прошлого сброса. См. render(). */
    private static int SHADOW_BER_INVOCATIONS = 0;

    /** Читает и обнуляет счётчик вызовов BER в shadow pass (раз в кадр из AFTER_SKY). */
    public static int drainShadowBerInvocations() {
        int v = SHADOW_BER_INVOCATIONS;
        SHADOW_BER_INVOCATIONS = 0;
        return v;
    }

    protected boolean isInViewFrustum(T blockEntity) {
        // Контрапшен (Create train и т.п.): BE висит на фейковом уровне и
        // getRenderBoundingBox() возвращает AABB в локальных координатах, который
        // world-space фрустум отбраковывает -> модель невидима (тень рисуется,
        // т.к. shadow-pass этот чек пропускает). Пропускаем position-based кулл.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(blockEntity)) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) {
            return true;
        }
        Frustum frustum = mc.levelRenderer.getFrustum();
        if (frustum == null) {
            return true;
        }
        AABB box = frustumCullBounds(blockEntity);
        return frustum.isVisible(box);
    }

    /**
     * AABB для frustum-теста до дорогого occlusion ray-march.
     * Forge: {@link net.minecraftforge.common.extensions.IForgeBlockEntity#getRenderBoundingBox()}.
     * Fabric: только известные подклассы с явным методом (остальные — 1 блок + запас).
     */
    private static AABB frustumCullBounds(BlockEntity blockEntity) {
        //? if forge {
        return ((net.minecraftforge.common.extensions.IForgeBlockEntity) blockEntity).getRenderBoundingBox();
        //?}
        //? if fabric {
        /*if (blockEntity instanceof com.hbm_m.blockentity.BaseMachineBlockEntity b) {
            return b.getRenderBoundingBox();
        }
        if (blockEntity instanceof com.hbm_m.block.entity.doors.DoorBlockEntity d) {
            return d.getRenderBoundingBox();
        }
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D);
        *///?}

        //? if neoforge {
        /*// На 1.21.1 у BlockEntity есть ванильный getRenderBoundingBox(), но для HBM-машин
        // используем явные переопределения (мультиблоки с увеличенным AABB), как на Fabric.
        if (blockEntity instanceof com.hbm_m.blockentity.BaseMachineBlockEntity b) {
            return b.getRenderBoundingBox();
        }
        if (blockEntity instanceof com.hbm_m.block.entity.doors.DoorBlockEntity d) {
            return d.getRenderBoundingBox();
        }
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D);
        *///?}
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
