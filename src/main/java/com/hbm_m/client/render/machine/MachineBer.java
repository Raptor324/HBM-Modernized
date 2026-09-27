package com.hbm_m.client.render.machine;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.IrisShadowBatchCollector;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
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
@OnlyIn(Dist.CLIENT)
public final class MachineBer<T extends BlockEntity> extends AbstractPartBasedRenderer<T, BakedModel> {

    private final MachineSpec<T> spec;

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
            // Диагностика кэша анимаций — только по явному флагу -Dhbm.debugAnimCache=true
            // (расследование 0914 закрыто; автобревно каждые 5с под тенями фонит).
            if (DEBUG_ANIM_CACHE) {
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

    /** Scratch для fast-path assert (фаза 1 → фаза 2); однопоточный рендер. */
    private final java.util.ArrayList<InstancedStaticPartRenderer> fastAssertScratch = new java.util.ArrayList<>();

    /**
     * Частичный fast-path (машина внутри анимационной зоны): статические части
     * подтверждаются roster'ом прямо в {@link #renderAll}, анимированные части и
     * хуки идут полным путём. Выставляется в {@link #tryFastAssertRender},
     * консьюмится первым же {@code collectRender} (сброс в его finally —
     * ранние выходы фрустума/куллинга не должны протечкиать на соседний BE).
     */
    private BlockEntity partialAssertBE;
    private boolean animatedPartsResolved;
    private boolean hasAnimatedPartsCache;

    private boolean hasAnimatedParts() {
        if (!animatedPartsResolved) {
            boolean any = false;
            for (MachineSpec.PartDef<T> part : spec.parts()) {
                if (part.animated()) {
                    any = true;
                    break;
                }
            }
            hasAnimatedPartsCache = any;
            animatedPartsResolved = true;
        }
        return hasAnimatedPartsCache;
    }

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
        try {
            doCollectRender(be, partialTick, poseStack, bufferSource, packedLight, applyFrustum);
        } finally {
            // Частичный fast-path живёт только в пределах одного collectRender:
            // ранние выходы (фрустум/CPU-окклюзия) не переносят флаг на соседний BE.
            partialAssertBE = null;
            // Dirty-трекер: сбор состоялся (включая ранние выходы фрустума/куллинга —
            // машина остаётся в MDI-батче и её догоняет GPU-culler, повторный CPU-обход
            // до TTL не нужен).
            if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker) {
                var lvl = be.getLevel();
                tracker.onRenderCollected(lvl != null ? lvl.getGameTime() : 0L,
                        com.hbm_m.client.render.NucleusRenderVersion.worldGen());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doCollectRender(BlockEntity be, float partialTick, PoseStack poseStack,
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

    /**
     * Fast-path dirty-skip: чистая машина БЕЗ пересборки подтверждает своё
     * присутствие в part-рендерерах (roster-assert). Возврат true означает, что
     * все статические части подтверждены, {@code collectRender} звать не нужно —
     * CPU-затраты на машину: distance-чек + сравнение long-ключей на part.
     * <p>
     * Гейты: MDI/GPU-cull путь активен, Iris выключен, трекер не протух
     * (dirty-флаг, worldGen, TTL света/fade). За анимационной зоной (и для
     * машин без анимированных частей/хуков — в том числе внутри неё) чистая
     * машина подтверждает ВСЕ статические части roster-assert'ом — полный путь
     * не нужен. Внутри зоны при наличии анимированных частей или хуков
     * включается частичный fast-path ({@link #partialAssertBE}): статические
     * части подтверждаются в renderAll, пер-кадровый контент идёт полным путём.
     * <p>
     * Безопасность: фаза 1 — только чтение (поиск рендереров + roster-проверка);
     * при любом миссе — false, состояние не тронуто, вызывающий делает полный
     * {@code collectRender}.
     */
    @SuppressWarnings("unchecked")
    public boolean tryFastAssertRender(BlockEntity beRaw, long gameTick) {
        if (!ClientRenderFlags.nucleusDirtySkip()) {
            return false;
        }
        // Спеки с time-varying динамическими частями (двери: animated=false, но
        // аниматор зависит от openTicks, который двигает клиентский тик БЕЗ
        // render-dirty) — roster-assert заморозил бы позу («бинарная» анимация).
        // Такие части всегда идут полным путём с исполнением аниматора.
        if (spec.hasDynamicAnimators()) {
            return false;
        }
        if (ShaderCompatibilityDetector.isExternalShaderActive()
                || ShaderCompatibilityDetector.canUseIrisExtendedShader()
                || ShaderCompatibilityDetector.isRenderingShadowPass()
                || ClientRenderFlags.forceVanillaImmediate()) {
            return false;
        }
        // CPU-фрустум/окклюзия пропускаются — их обязан взять на себя GPU-culler.
        if (ModClothConfig.get().getEffectiveOcclusionCullingMode()
                != ModClothConfig.OcclusionCullingMode.GPU
                || !com.hbm_m.client.render.culling.GpuCullingCapability.isSupported()) {
            return false;
        }
        if (!(beRaw instanceof com.hbm_m.api.render.RenderDirtyTracker tracker)) {
            return false;
        }
        if (tracker.isRenderStale(gameTick, com.hbm_m.client.render.NucleusRenderVersion.worldGen())) {
            return false;
        }
        // Анимационная зона: полный путь обязателен только для пер-кадрового
        // контента — аниматоров анимированных частей и immediate-хуков
        // (жидкости/предметы/алмазы). Машина без них проходит полным fast-path
        // и внутри зоны; иначе — частичный: статические части подтвердятся
        // roster'ом в renderAll (без матриц/света/сравнения флоатов), что
        // снимает основную часть ежекадровой стоимости зоны.
        double animDist = RenderDistanceHelper.getAnimatedDistanceBlocks();
        if (animDist > 0) {
            double distSq = RenderDistanceHelper.distanceSqToCamera(beRaw.getBlockPos());
            if (distSq <= animDist * animDist) {
                if (!spec.hooks().isEmpty() || hasAnimatedParts()) {
                    partialAssertBE = beRaw;
                    return false;
                }
            }
        }

        // Дистанционный fade — чистая функция позиции камеры и меняется каждый кадр
        // движения без какого-либо dirty-события: roster-assert обязан сверять
        // квантованный fade записи буфера (canAssertInstance(posKey, fade)), иначе
        // машина в кольце фейда «замораживает» альфу до случайного полного пересбора
        // — pop вместо растворения при отдалении, полупрозрачный траней при
        // приближении. За отсечкой (fade<0) подтверждать нечего — полный сбор
        // (тот же ранний выход по fade, что и всегда).
        float staticFade = RenderDistanceHelper.computeStaticFade(beRaw);
        if (staticFade < 0) {
            return false;
        }
        float quantizedFade = InstancedStaticPartRenderer.quantizeFade(staticFade);

        BlockPos pos = beRaw.getBlockPos();
        long posKey = pos.asLong();

        // Фаза 1 (read-only): резолв рендереров + roster-проверка.
        fastAssertScratch.clear();
        for (MachineSpec.PartDef<T> part : spec.parts()) {
            // Здесь animatedVisible == false (гейт выше) — анимированные части скипаются,
            // ровно как в renderAll.
            if (part.animated()) continue;
            String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, (T) beRaw) : null;
            MachinePartRenderer r = spec.findExistingRenderer(part, dynKey);
            if (r == null) {
                return false; // рендерера нет (кеш снесён) — полный сбор построит
            }
            InstancedStaticPartRenderer inst = r.instanced();
            if (inst == null) {
                if (r.hasGeometry()) {
                    return false; // single-VBO/immediate часть — не инстансная
                }
                continue; // пустая часть — renderAll её тоже скипает
            }
            if (!inst.isInitialized() || !inst.canAssertInstance(posKey, quantizedFade)) {
                return false;
            }
            fastAssertScratch.add(inst);
        }

        // Фаза 2 (коммит): состояние между фазами не менялось (однопоточный рендер) —
        // assert не может провалиться.
        for (InstancedStaticPartRenderer inst : fastAssertScratch) {
            inst.assertCleanInstance(pos);
        }
        // ШТАМП ТРЕКЕРА ЗДЕСЬ НЕ ОБНОВЛЯЕТСЯ: onRenderCollected от каждого удавшегося
        // assert держал бы штамп свежим каждый кадр, и TTL-пересбор (единственный
        // владелец обновления света/fade без событийной модели) не срабатывал бы
        // никогда. Свет/fade обновляются полным сбором по TTL (isRenderStale),
        // смене worldGen, dirty-флагу или смене кванта fade (проверка выше).
        return true;
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

        FrameCtx ctx = frameCtx;
        ctx.fadeAlpha = fade;
        ctx.blockPos = blockPos;
        ctx.transforms.clear();

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
            // В shadow begin(true)+close = apply()/clear() пака НА КАЖДЫЙ BER — на
            // ферме ассемблеров это ~14% кадра. Когда включён глобальный shadow-батч
            // (все части уходят в него записями, см. addInstance),
            // немедленный companion-дроук не нужен и батч-обёртка не открывается.
            // Выпавшие из батча части корректно деградируют в putBulkData
            // (bufferSource) или в запись — оба пути без активного батча.
            if (shadowPass && IrisShadowBatchCollector.isBatchingEnabled()) {
                renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                        poseStack, bufferSource, blockPos, ctx, animatedVisible, staticFade, fade);
            } else {
                try (IrisRenderBatch ignored = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                    renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                            poseStack, bufferSource, blockPos, ctx, animatedVisible, staticFade, fade);
                }
            }
        } else {
            renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                    poseStack, bufferSource, blockPos, ctx, animatedVisible, staticFade, fade);
        }
    }

    private void renderAll(T blockEntity, BakedModel model, float partialTick,
                           int packedLight, int packedOverlay, PoseStack poseStack,
                           MultiBufferSource bufferSource, BlockPos blockPos,
                           FrameCtx ctx, boolean animatedVisible, float staticFade, float fade) {
        // Частичный fast-path: подтверждён в tryFastAssertRender для ЭТОГО BE.
        // Читаем с консьюмом — повторный вызов renderAll не должен задвоить assert.
        boolean partialAssert = partialAssertBE == blockEntity;
        partialAssertBE = null;
        long rosterKey = blockPos.asLong();
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;

        // Shared light: при батчинге один 8-corner сэмпл на машину за кадр,
        // все части переиспользуют его (экономия getLightColor-вызовов на фермах).
        // В shadow-проходе с включённым глобальным батчем свет не нужен вообще
        // (теневой FB — глубина/простой цвет): сэмпл скипаем целиком.
        float[] sharedLight = null;
        boolean shadowNoLight = ShaderCompatibilityDetector.isRenderingShadowPass()
                && IrisShadowBatchCollector.isBatchingEnabled();
        if (!shadowNoLight && ClientRenderFlags.useInstancedBatching()) {
            net.minecraft.world.phys.AABB bb = com.hbm_m.platform.RenderHooks.getRenderBoundingBox(blockEntity);
            if (bb != null && bb.maxX > bb.minX && bb.maxY > bb.minY && bb.maxZ > bb.minZ) {
                sharedLightBbox[0] = (float) (bb.minX - blockPos.getX());
                sharedLightBbox[1] = (float) (bb.minY - blockPos.getY());
                sharedLightBbox[2] = (float) (bb.minZ - blockPos.getZ());
                sharedLightBbox[3] = (float) (bb.maxX - blockPos.getX());
                sharedLightBbox[4] = (float) (bb.maxY - blockPos.getY());
                sharedLightBbox[5] = (float) (bb.maxZ - blockPos.getZ());
            } else {
                sharedLightBbox[0] = -0.5f;
                sharedLightBbox[1] = 0f;
                sharedLightBbox[2] = -0.5f;
                sharedLightBbox[3] = 1.5f;
                sharedLightBbox[4] = 2f;
                sharedLightBbox[5] = 1.5f;
            }
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
                // Частичный fast-path (машина внутри анимационной зоны): статическая
                // часть уже лежит в instance-буфере прошлого кадра — подтверждаем
                // roster-assert'ом вместо полной пересборки (матрица/свет/30-float
                // сравнение). Гард по fade — равенство кванта записи буфера текущему
                // кванту staticFade: выцветающая часть пересобирается только при
                // смене кванта альфы (1/255), симметрично полному fast-path за
                // зоной. Семантика позы — как у полного fast-path за зоной:
                // аниматор статической части (легаси-офсеты, константная поза)
                // не исполняется, запись буфера не меняется.
                boolean fastAsserted = false;
                if (partialAssert && !part.animated()
                        && !(part.animator() != null && part.dynamic())) {
                    InstancedStaticPartRenderer inst = renderer.instanced();
                    if (inst != null && inst.isInitialized()
                            && inst.canAssertInstance(rosterKey,
                                    InstancedStaticPartRenderer.quantizeFade(staticFade))) {
                        int beforeCount = inst.getInstanceCount();
                        inst.assertCleanInstance(blockPos);
                        fastAsserted = inst.getInstanceCount() > beforeCount;
                    }
                }
                if (!fastAsserted && part.animator() != null) {
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
                if (fastAsserted) {
                    // Поза части нужна хукам (MachineRenderApi.partTransform) даже
                    // при скипе пересборки — базовая (аниматор статической части
                    // константный), сохраняем без enqueue.
                    if (!spec.hooks().isEmpty()) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                } else if (draw) {
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
                    float[] partSharedLight = sharedLight;
                    if (part.lightOverride() != null) {
                        Integer forced = part.lightOverride().apply(blockEntity);
                        if (forced != null && forced >= 0) {
                            partLight = forced;
                            // Общий 8-corner сэмпл машины затирал бы форсированный
                            // свет (расплав тигля темнел ночью) — на fullbright-частях
                            // его не передаём.
                            partSharedLight = null;
                        }
                    }
                    renderer.enqueue(poseStack, partLight, blockPos,
                            blockEntity, bufferSource, partSharedLight,
                            spec.resolveUvRect(part, blockEntity),
                            spec.resolveTint(part, blockEntity),
                            part.tintFalloff());
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
