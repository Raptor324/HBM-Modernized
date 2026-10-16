package com.hbm_m.client.render.machine;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.IrisShadowBatchCollector;
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

    // ── Кэш дельт анимации (Шаг 1: аниматор исполняется один раз на кадр) ──
    //
    // Поза части зависит только от (state BE, gameTime, partialTick), а shadow-
    // и main-проход одного кадра получают ОДИНАКОВЫЕ значения всех трёх — лямбды
    // (lerp+trig+цепочки translate, ~10-12% CPU на ферме ассемблеров по профилю)
    // исполняются на первом проходе, второй берёт готовую дельту. Кэшируется
    // ДЕЛЬТА S⁻¹·F, а не финальная матрица: базисы проходов разные (тень —
    // shadowMV, main — R_cam·T(be−cam)); на хите F' = S·delta — один 4×4-mul.
    private static final boolean ANIM_CACHE_ON =
            !"false".equalsIgnoreCase(System.getProperty("hbm.animCache", "true"));
    private static final int ANIM_SLOTS = 4096;
    private static final int ANIM_SLOT_MASK = ANIM_SLOTS - 1;
    private final long[] animPosKey = new long[ANIM_SLOTS];
    private final long[] animTimeKey = new long[ANIM_SLOTS];
    private final Matrix4f[][] animDelta = new Matrix4f[ANIM_SLOTS][];
    private final boolean[][] animSkip = new boolean[ANIM_SLOTS][];
    // Scratch (рендер однопоточный): S/Sinv текущего BE на время прохода частей.
    private final Matrix4f animBase = new Matrix4f();
    private final Matrix4f animBaseInv = new Matrix4f();
    private final Matrix4f animComposed = new Matrix4f();

    private void animStore(int slot, int partIdx, int partCount, long posKey, long timeKey,
                           Matrix4f baseInv, Matrix4f finalPose, boolean draw) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) STORE_SHADOW++;
        else STORE_MAIN++;
        if (animDelta[slot] == null || animDelta[slot].length != partCount) {
            animDelta[slot] = new Matrix4f[partCount];
            animSkip[slot] = new boolean[partCount];
        }
        if (animDelta[slot][partIdx] == null) {
            animDelta[slot][partIdx] = new Matrix4f();
        }
        // Миссы бывают на первом проходе каждого BE каждого кадра (и после
        // смены timeKey на тике) — alloc-free: матрицы переиспользуются.
        animDelta[slot][partIdx].set(baseInv).mul(finalPose);
        animSkip[slot][partIdx] = !draw;
        animPosKey[slot] = posKey;
        animTimeKey[slot] = timeKey;
    }

    private void animInvalidate(int slot) {
        if (animDelta[slot] != null) {
            java.util.Arrays.fill(animDelta[slot], null);
        }
    }

    // Hit/miss-счётчики кэша анимаций (диагностика «кэш не срабатывает», профиль 0914).
    private static volatile long ANIM_HITS = 0;
    private static volatile long ANIM_MISSES = 0;
    private static volatile long MISS_OFF = 0, MISS_NO_SLOT = 0, MISS_POS = 0, MISS_FRAME = 0, MISS_DELTA = 0;
    // Расщепление по проходам: где пишем и где теряем (лог 0914 03:31: delta-миссы
    // 90% при живых ключах — записи отсутствуют; нужно знать, ЧЕЙ store не дошёл).
    private static volatile long STORE_SHADOW = 0, STORE_MAIN = 0;
    private static volatile long DELTA_MISS_SHADOW = 0, DELTA_MISS_MAIN = 0;
    private static final boolean DEBUG_ANIM_CACHE = Boolean.getBoolean("hbm.debugAnimCache");
    private static volatile long ANIM_LAST_LOG = 0L;

    private static void maybeLogAnimCache() {
        long now = System.currentTimeMillis();
        if (now - ANIM_LAST_LOG > 5000L) {
            ANIM_LAST_LOG = now;
            // Кэш анимаций устраняет дублирование вычислений между shadow pass и main pass в рамках одного кадра.
            // Если теневой проход не выполнялся (STORE_SHADOW == 0), второго прохода в кадре нет — 0% попаданий
            // является нормальным и ожидаемым поведением. Логируем только при STORE_SHADOW > 0 или флаге -Dhbm.debugAnimCache=true.
            if (DEBUG_ANIM_CACHE || STORE_SHADOW > 0) {
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] anim cache: hits={} misses={} ({}% hit) | off={} noSlot={} pos={} frame={} "
                                + "delta={} (deltaShadow={} deltaMain={}) | stores: shadow={} main={}",
                        ANIM_HITS, ANIM_MISSES,
                        (ANIM_HITS + ANIM_MISSES) == 0 ? 0
                                : ANIM_HITS * 100 / (ANIM_HITS + ANIM_MISSES),
                        MISS_OFF, MISS_NO_SLOT, MISS_POS, MISS_FRAME, MISS_DELTA,
                        DELTA_MISS_SHADOW, DELTA_MISS_MAIN, STORE_SHADOW, STORE_MAIN);
            }
            ANIM_HITS = 0;
            ANIM_MISSES = 0;
            MISS_OFF = 0;
            MISS_NO_SLOT = 0;
            MISS_POS = 0;
            MISS_FRAME = 0;
            MISS_DELTA = 0;
            STORE_SHADOW = 0;
            STORE_MAIN = 0;
            DELTA_MISS_SHADOW = 0;
            DELTA_MISS_MAIN = 0;
        }
    }

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

    /**
     * Сбор машины БЕЗ ванильного диспетчера — вызывается плоским обходом
     * {@link com.hbm_m.client.render.NucleusDispatcherBypass} (байпас
     * Sodium/Iris-диспетчеризации). poseStack уже несёт base·T(be−cam) — тот же
     * контракт, что давал диспетчер. {@code applyFrustum} — только для main
     * (shadow-проход не режется фрустумом основной камеры).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void collectRender(BlockEntity be, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight, boolean applyFrustum) {
        T blockEntity = (T) be;
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            SHADOW_BER_INVOCATIONS++;
        } else if (applyFrustum && !isInViewFrustum(blockEntity)) {
            return;
        }
        currentModelViewMatrix.set(poseStack.last().pose());

        BakedModel rawModel = getModel(blockEntity);
        rawModel = unwrapFabricForwardingModels(rawModel);
        BakedModel model = getModelType(rawModel);
        if (model == null) return;

        LegacyAnimator animator = LegacyAnimator.create(poseStack);
        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);
        poseStack.pushPose();
        try {
            setupBlockTransform(animator, blockEntity);
            renderParts(blockEntity, model, animator, partialTick, packedLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource);
        } finally {
            poseStack.popPose();
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
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
        // fade для анимированного контента (min обеих зон); статика гаснет ТОЛЬКО
        // по статической зоне — чисто статичные машины не должны мигать в кольце
        // анимационного фейда и возвращаться за ним (fade = staticFade = 1).
        float fade = animatedVisible ? Math.min(staticFade, animFade) : staticFade;

        BlockPos blockPos = blockEntity.getBlockPos();
        Matrix4f blockPose = new Matrix4f(poseStack.last().pose());

        FrameCtx ctx = frameCtx;
        ctx.fadeAlpha = fade;
        ctx.blockPos = blockPos;
        ctx.transforms.clear();

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
            // В shadow begin(true)+close = apply()/clear() пака НА КАЖДЫЙ BER — на
            // ферме ассемблеров это ~14% кадра. Когда включён глобальный shadow-батч
            // (все части уходят в него записями, см. addInstanceGpuBones/addInstance),
            // немедленный companion-дроук не нужен и батч-обёртка не открывается.
            // Выпавшие из батча части корректно деградируют в putBulkData
            // (bufferSource) или в запись — оба пути без активного батча.
            if (shadowPass && IrisShadowBatchCollector.isBatchingEnabled()) {
                renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                        poseStack, bufferSource, blockPose, blockPos, ctx, animatedVisible, staticFade, fade);
            } else {
                try (IrisRenderBatch ignored = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                    renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                            poseStack, bufferSource, blockPose, blockPos, ctx, animatedVisible, staticFade, fade);
                }
            }
        } else {
            renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                    poseStack, bufferSource, blockPose, blockPos, ctx, animatedVisible, staticFade, fade);
        }
    }

    private void renderAll(T blockEntity, BakedModel model, float partialTick,
                           int packedLight, int packedOverlay, PoseStack poseStack,
                           MultiBufferSource bufferSource, Matrix4f blockPose, BlockPos blockPos,
                           FrameCtx ctx, boolean animatedVisible, float staticFade, float fade) {
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;

        // Shared light: при батчинге один 8-corner сэмпл на машину за кадр,
        // все части переиспользуют его (экономия getLightColor-вызовов на фермах).
        // В shadow-проходе с включённым глобальным батчем свет не нужен вообще
        // (теневой FB — глубина/простой цвет): сэмпл скипаем целиком.
        float[] sharedLight = null;
        boolean shadowNoLight = ShaderCompatibilityDetector.isRenderingShadowPass()
                && IrisShadowBatchCollector.isBatchingEnabled();
        if (!shadowNoLight && ClientRenderFlags.useInstancedBatching()) {
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
        // Кэш ВКЛЮЧЁН ВСЕГДА (без external-гейта): детект isExternalShaderActive()
        // во время теневого re-entry Iris нестабилен — с гейтом shadow не писал
        // кэш, и каждое первое main-чтение было delta-миссом (hits 3.07M ≈
        // «вторые» main-отрисовки, delta 3.5M ≈ «первые», лог 0914 03:08).
        // Без дублирования проходов запись просто не читается (ключ по кадру) —
        // копеечный оверхед, корректность не зависит от состояния детектора.
        boolean animCacheOn = ANIM_CACHE_ON;
        int cacheSlot = -1;
        long timeKey = 0L;
        boolean baseReady = false;
        java.util.List<MachineSpec.PartDef<T>> parts = spec.parts();
        for (int partIdx = 0; partIdx < parts.size(); partIdx++) {
            MachineSpec.PartDef<T> part = parts.get(partIdx);
            // Скипается только анимированный КОНТЕНТ; статические части с
            // трансформом-«аниматором» (легаси-офсеты) живут до статической отсечки.
            if (!animatedVisible && part.animated()) continue;
            BakedModel partModel = spec.partModel(part, model);
            // Ленивое построение: dynQuads вычисляются только если VBO ещё не собран
            // (раньше resolver дергался каждый кадр для каждого BE — см. MachineSpec.partRendererLazy).
            if (partModel == null && !part.dynamic()) continue;
            String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, blockEntity) : null;

            MachinePartRenderer renderer = spec.partRendererLazy(part, partModel, blockEntity, dynKey);
            if (!renderer.hasGeometry()) continue;

            // Кэш анимаций: инициализация на первой части с аниматором.
            // Ключ = пер-кадровый счётчик (общий для shadow/main одного кадра).
            // СЛОТ: asLong() держит X в битах 0..25, Z в 26..51 — маска по
            // младшим битам видела ТОЛЬКО X (ряд машин по Z с общим X = полная
            // коллизия, 90% delta-миссов, лог 0914 03:08). Смешиваем умножением
            // и берём СТАРШИЕ биты — равномерное распределение по X/Y/Z.
            if (part.animator() != null && animCacheOn && cacheSlot < 0) {
                long posKey0 = blockPos == null ? 0L : blockPos.asLong();
                long mixed = posKey0 * 0x9E3779B97F4A7C15L;
                cacheSlot = (int) (mixed >>> (64 - 12)) & ANIM_SLOT_MASK;
                timeKey = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
                if (animPosKey[cacheSlot] != posKey0 || animTimeKey[cacheSlot] != timeKey) {
                    animInvalidate(cacheSlot);
                }
            }

            poseStack.pushPose();
            try {
                boolean draw = true;
                if (part.animator() != null) {
                    long posKey = blockPos == null ? 0L : blockPos.asLong();
                    boolean hit;
                    if (!animCacheOn) {
                        hit = false;
                        MISS_OFF++;
                    } else if (cacheSlot < 0) {
                        hit = false;
                        MISS_NO_SLOT++;
                    } else if (animPosKey[cacheSlot] != posKey) {
                        hit = false;
                        MISS_POS++;
                    } else if (animTimeKey[cacheSlot] != timeKey) {
                        hit = false;
                        MISS_FRAME++;
                    } else if (animDelta[cacheSlot] == null || animDelta[cacheSlot][partIdx] == null) {
                        hit = false;
                        MISS_DELTA++;
                        if (ShaderCompatibilityDetector.isRenderingShadowPass()) DELTA_MISS_SHADOW++;
                        else DELTA_MISS_MAIN++;
                    } else {
                        hit = true;
                    }
                    if (!baseReady) {
                        animBase.set(poseStack.last().pose());
                        animBaseInv.set(animBase).invert();
                        baseReady = true;
                    }
                    if (hit) {
                        ANIM_HITS++;
                        if (animSkip[cacheSlot][partIdx]) {
                            draw = false;
                        } else {
                            // F' = S · delta — базис текущего прохода поверх
                            // кэшированной локальной анимации.
                            animComposed.set(animBase).mul(animDelta[cacheSlot][partIdx]);
                            poseStack.last().pose().set(animComposed);
                        }
                    } else {
                        ANIM_MISSES++;
                        maybeLogAnimCache();
                        draw = part.animator().animate(blockEntity, partialTick, gameTime, poseStack);
                        if (animCacheOn && cacheSlot >= 0) {
                            animStore(cacheSlot, partIdx, parts.size(), posKey, timeKey,
                                    animBaseInv, poseStack.last().pose(), draw);
                        }
                    }
                }
                if (draw) {
                    // Матрицы нужны только хукам (MachineRenderApi.partTransform);
                    // без хуков не аллоцируем ничего.
                    if (!spec.hooks().isEmpty()) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                    // Per-part fade: fade пишется в per-instance данные в момент
                    // addInstance — статику гасит только статическая зона,
                    // анимированные части дополнительно анимационная.
                    SingleMeshVboRenderer.setFadeAlpha(part.animated() ? fade : staticFade);
                    // Форсированный свет части (порт fullbright: InnerBurning печей, lightmap 240/240)
                    int partLight = packedLight;
                    if (part.lightOverride() != null) {
                        Integer forced = part.lightOverride().apply(blockEntity);
                        if (forced != null && forced >= 0) partLight = forced;
                    }
                    renderer.enqueue(poseStack, blockPose, basePoseStack, partLight, blockPos,
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
