package com.hbm_m.client.render.machine;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * BER, генерируемый фабрикой {@link MachineRenderers}. Содержит весь общий
 * пайплайн (куллинг/fade/Iris-батч/деградация путей), специфична для машины
 * только спека: части + аниматоры + хуки.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class MachineBer<T extends BlockEntity> extends AbstractPartBasedRenderer<T, BakedModel> {

    private final MachineSpec<T> spec;

    // Вспомогательный стек для bone-пути (last().pose() = блочный трансформ).
    private final PoseStack basePoseStack = new PoseStack();

    // Shared light: один 8-corner сэмпл на машину за кадр (вместо per-part).
    private final float[] sharedLight8 = new float[16];
    private final float[] sharedLightBbox = new float[6];
    private final Matrix4f sharedLightPose = new Matrix4f();

    // Переиспользуемый контекст кадра для хуков (рендер однопоточный).
    private final FrameCtx frameCtx = new FrameCtx();

    private final class FrameCtx implements MachineRenderApi {
        private float fadeAlpha = 1f;
        private BlockPos blockPos = BlockPos.ZERO;
        private final Map<String, Matrix4f> transforms = new HashMap<>();

        @Override public float fadeAlpha() { return fadeAlpha; }
        @Override public BlockPos blockPos() { return blockPos; }
        @Override public @Nullable Matrix4f partTransform(String partName) {
            // Живой экземпляр без defensive copy: матрица мутируется через saveTransform
            // раз за кадр, хуки живут внутри того же кадра.
            return transforms.get(partName);
        }

        void saveTransform(String partName, Matrix4f pose) {
            transforms.computeIfAbsent(partName, k -> new Matrix4f()).set(pose);
        }
    }

    public MachineBer(MachineSpec<T> spec) {
        this.spec = spec;
    }

    @Override
    protected BakedModel getModelType(BakedModel rawModel) {
        return rawModel;
    }

    @Override
    protected BakedModel getModel(T blockEntity) {
        return spec.modelResolver().apply(blockEntity);
    }

    @Override
    protected Direction getFacing(T blockEntity) {
        return spec.facingResolver().apply(blockEntity);
    }

    @Override
    protected void setupBlockTransform(LegacyAnimator animator, T blockEntity) {
        var custom = spec.blockTransform();
        if (custom != null) {
            custom.apply(blockEntity, animator);
            return;
        }
        super.setupBlockTransform(animator, blockEntity);
    }

    @Override
    public int getViewDistance() {
        return spec.viewDistance() >= 0 ? spec.viewDistance() : RenderDistanceHelper.getStaticViewDistanceBlocks();
    }

    @Override
    protected void renderParts(T blockEntity, BakedModel model, LegacyAnimator animator, float partialTick,
                               int packedLight, int packedOverlay, PoseStack poseStack,
                               MultiBufferSource bufferSource) {
        // ── Куллинг + fade (автоматически) ─────────────────────────────
        // Контрапшен: BE.getLevel() — VirtualRenderWorld, shouldRender() пропускает
        // frustum/ray-march куллинг (см. AbstractPartBasedRenderer).
        float staticFade = applyCullingAndStaticFade(blockEntity);
        if (staticFade < 0) return;
        // За анимационной дистанцией (modelUpdateDistance) показываем только статику.
        float animFade = RenderDistanceHelper.computeAnimatedFade(blockEntity);
        boolean animatedVisible = animFade >= 0;
        float fade = animatedVisible ? Math.min(staticFade, animFade) : staticFade;
        SingleMeshVboRenderer.setFadeAlpha(fade);

        BlockPos blockPos = blockEntity.getBlockPos();
        Matrix4f blockPose = new Matrix4f(poseStack.last().pose());

        FrameCtx ctx = frameCtx;
        ctx.fadeAlpha = fade;
        ctx.blockPos = blockPos;
        ctx.transforms.clear();

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
            try (IrisRenderBatch ignored = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                        poseStack, bufferSource, blockPose, blockPos, ctx, animatedVisible);
            }
        } else {
            renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                    poseStack, bufferSource, blockPose, blockPos, ctx, animatedVisible);
        }
    }

    private void renderAll(T blockEntity, BakedModel model, float partialTick,
                           int packedLight, int packedOverlay, PoseStack poseStack,
                           MultiBufferSource bufferSource, Matrix4f blockPose, BlockPos blockPos,
                           FrameCtx ctx, boolean animatedVisible) {
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;

        // Shared light: при батчинге один 8-corner сэмпл на машину за кадр,
        // все части переиспользуют его (экономия getLightColor-вызовов на фермах).
        float[] sharedLight = null;
        if (ClientRenderFlags.useInstancedBatching()) {
            // Фиксированный bbox 1×2×1 вокруг блока BE вместо renderBounds(): new AABB()
            // на каждую машину каждый кадр — чистый мусор для GC, на свет LOD-сэмпла
            // влияет мало (сэмпл кешируется в LightSampleCache).
            sharedLightBbox[0] = -0.5f;
            sharedLightBbox[1] = 0f;
            sharedLightBbox[2] = -0.5f;
            sharedLightBbox[3] = 1.5f;
            sharedLightBbox[4] = 2f;
            sharedLightBbox[5] = 1.5f;
            sharedLightPose.identity();
            com.hbm_m.client.render.LightSampleCache.getOrSample8Lod(blockEntity, spec.lightSampleKey,
                    sharedLightBbox, blockPos, sharedLightPose, packedLight, sharedLight8,
                    com.hbm_m.client.render.RenderDistanceHelper.distanceSqToCamera(blockPos));
            sharedLight = sharedLight8;
        }

        // ── Части: статика и анимация через VBO-пайплайн ───────────────
        for (MachineSpec.PartDef<T> part : spec.parts()) {
            if (!animatedVisible && part.animator() != null) continue;
            BakedModel partModel = spec.partModel(part, model);
            // Ленивое построение: dynQuads вычисляются только если VBO ещё не собран
            // (раньше resolver дергался каждый кадр для каждого BE — см. MachineSpec.partRendererLazy).
            if (partModel == null && !part.dynamic()) continue;
            String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, blockEntity) : null;

            MachinePartRenderer renderer = spec.partRendererLazy(part, partModel, blockEntity, dynKey);
            if (!renderer.hasGeometry()) continue;

            poseStack.pushPose();
            try {
                boolean draw = true;
                if (part.animator() != null) {
                    // Аниматор применяет трансформы поверх блока; движок снимает матрицу и откатывает стек.
                    draw = part.animator().animate(blockEntity, partialTick, gameTime, poseStack);
                }
                if (draw) {
                    // Матрицы нужны только хукам (MachineRenderApi.partTransform);
                    // без хуков не аллоцируем ничего.
                    if (!spec.hooks().isEmpty()) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                    renderer.enqueue(poseStack, blockPose, basePoseStack, packedLight, blockPos,
                            blockEntity, bufferSource, sharedLight);
                }
            } catch (Throwable t) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] part '{}' render failed",
                        spec.id(), part.name(), t);
            } finally {
                poseStack.popPose();
            }
        }

        // ── Хуки: жидкости/предметы/алмазы (immediate); за анимационной
        // дистанцией не рисуются (иконки/алмазы — косметика анимации) ─────
        if (!animatedVisible) return;
        for (MachineRenderHook<T> hook : spec.hooks()) {
            try {
                hook.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, ctx);
            } catch (Throwable t) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] hook render failed", spec.id(), t);
            }
        }
    }
}
