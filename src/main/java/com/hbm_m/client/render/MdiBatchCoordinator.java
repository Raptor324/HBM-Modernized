package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

import com.hbm_m.client.render.shader.ModShaders;
import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBDrawIndirect;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Optional Multi-Draw Indirect aggregation path for {@link InstancedStaticPartRenderer}.
 * <p>
 * Today each {@code InstancedStaticPartRenderer.flushBatchVanilla} issues one
 * {@code glDrawElementsInstanced} call. A typical machine BER drives 8-11 part
 * renderers, so the per-frame draw call count on the vanilla path scales as
 * {@code partsPerMachine}. When all part renderers share the same vertex
 * format (pos vec3 / normal vec3 / uv vec2, stride 32) AND the same legacy
 * (unsliced) per-instance layout (loc 3..11), we can collapse those into
 * <b>one</b> indirect-command buffer + либо цикл {@code glDrawElementsIndirect},
 * либо (опционально) один {@code glMultiDrawElementsIndirect} — общий атлас
 * VAO и единый per-instance VBO.
 * <p>
 * <b>Fallback chain (in order):</b>
 * <ol>
 *   <li>{@code hasDrawIndirect && hasBaseInstance} (GL 4.0+ draw indirect + base instance в команде):
 *       путь атласа; один {@code glMultiDrawElementsIndirect} на flush.</li>
 *   <li>Otherwise: legacy per-renderer {@code glDrawElementsInstanced}
 *       (the {@code flushBatchVanilla} path stays as-is).</li>
 * </ol>
 * <p>
 * <b>Eligibility constraints for an individual flush:</b>
 * <ul>
 *   <li>Iris/Oculus NOT active (we only optimise the vanilla shader path).</li>
 *   <li>Renderer initialised, instanceCount &gt; 0.</li>
 * </ul>
 * <p>
 * <b>Fade ordering (variant G):</b> каждый снапшот инстансов партиционируется на
 * [opaque | fading back-to-front] ({@link #partitionOpaqueFirst}), и командный буфер
 * собирается как [все opaque-поддравы][все fading-поддравы] — один
 * {@code glMultiDrawElementsIndirect}, но непрозрачная геометрия гарантированно
 * запечатывает глубину до затухающей фазы. Без этого дальний непрозрачный станок
 * позади полупрозрачного отваливался по depth-тесту (полупрозрачные фрагменты
 * пишут глубину при {@code depthMask(true)}) и вместо него просвечивал чанк.
 * <p>
 * <b>Iris path:</b> untouched. {@link InstancedStaticPartRenderer#flush(Matrix4f)}
 * still routes to {@code flushBatchIris} when an external shader is active —
 * the coordinator's eligibility test rejects those flushes up front.
 */

@OnlyIn(Dist.CLIENT)
public final class MdiBatchCoordinator {

    /**
     * Полезная нагрузка {@code DrawElementsIndirectCommand} (5×uint32). В буфере каждая команда
     * выровнена до {@link #INDIRECT_CMD_STRIDE_BYTES}: иначе часть драйверов ломает multi/indirect fetch.
     */
    static final int INDIRECT_CMD_PACKED_BYTES = 20;
    /** Stride между командами в GL_DRAW_INDIRECT_BUFFER (кратно 4, ≥20). */
    static final int INDIRECT_CMD_STRIDE_BYTES = 32;

    private static volatile boolean capsResolved = false;
    /** Есть {@code glDrawElementsIndirect} и/или {@code glMultiDrawElementsIndirect} (или ARB-аналоги). */
    private static volatile boolean hasDrawIndirect = false;
    private static volatile boolean hasBaseInstance = false;
    private static volatile boolean loggedOnce = false;

    private static final ThreadLocal<MdiBatchCoordinator> ACTIVE = new ThreadLocal<>();

    /** Prepared on game-tick finalize; atlas upload + GL draw once in {@link #presentScheduledDraw()}. */
    private static volatile DeferredDraw scheduledDraw;

    /** GPU instance data from last tick dispatch; redrawn each render frame in {@link #redrawCachedIfAny}. */
    private static volatile PreparedMdi cachedRedraw;
    private static volatile long cachedRedrawGameTime = -1L;

    // ── Retained draw list (clean-frame reuse) ──────────────────────────
    // Снапшоты, партиция и fade-кэши переживают кадры: чистые рендереры
    // (submitClean, ни одной записи в буфер за кадр) переиспользуют прошлокадровую
    // запись без копирования снапшота и span-диффа; грязные сабмиты заменяют
    // запись на месте (стабильный порядок ⇒ стабильные оффсеты аплоада).

    /** Кадровый штамп assert-семантики: инкремент в {@link #beginFrame}. */
    private static long retainedFrameStamp = 0L;
    private static final List<Pending> retainedList = new ArrayList<>(64);
    private static final IdentityHashMap<InstancedStaticPartRenderer, Pending> retainedByRenderer = new IdentityHashMap<>();
    /** Пул нативных снапшот-буферов — заменяет memAllocFloat на каждый сабмит. */
    private static final ArrayDeque<FloatBuffer> snapshotPool = new ArrayDeque<>();
    private static final int SNAPSHOT_POOL_MAX_BUFFERS = 128;

    private static final class DeferredDraw {
        final List<Pending> drawList;
        final List<SubDraw> opaqueSubs;
        final List<FadeSlot> fadeSlots;
        final int drawTotalInstances;
        final int pendingSize;
        final int droppedNoSlot;
        final long gameTime;
        final Matrix4f projectionMatrix;

        DeferredDraw(List<Pending> drawList, List<SubDraw> opaqueSubs, List<FadeSlot> fadeSlots,
                     int drawTotalInstances, int pendingSize, int droppedNoSlot, long gameTime,
                     Matrix4f projectionMatrix) {
            this.drawList = drawList;
            this.opaqueSubs = opaqueSubs;
            this.fadeSlots = fadeSlots;
            this.drawTotalInstances = drawTotalInstances;
            this.pendingSize = pendingSize;
            this.droppedNoSlot = droppedNoSlot;
            this.gameTime = gameTime;
            this.projectionMatrix = new Matrix4f(projectionMatrix);
        }

        void free() {
            // Список — срез retained-записей (prepareMdiDraw): освобождение только
            // через uninstall (пул + снятие установки), иначе двойной recycle.
            for (Pending p : drawList) {
                uninstallRetained(p);
                retainedList.remove(p);
            }
            drawList.clear();
        }
    }

    static final class Pending {
        final InstancedStaticPartRenderer renderer;
        int baseVertex;
        /**
         * Byte offset into the EBO for {@link GL42#glDrawElementsInstancedBaseVertexBaseInstance}
         * (параметр {@code indices} при привязанном EBO — в байтах).
         * В {@link GL40#glDrawElementsIndirect} поле {@code firstIndex} команды — смещение в <b>элементах</b>
         * индекса ({@code GL_UNSIGNED_INT}: делить на 4).
         */
        int firstIndexBytes;
        int indexCount;
        /** Snapshot at submit time — сравнение с {@link #dispatch} (H1: repack между submit и draw). */
        int submitBaseVertex = -1;
        int submitFirstIndexBytes = -1;
        int submitIndexCount = -1;
        int baseInstance;
        int instanceCount;
        int[] instanceCullIndices;
        long[] instanceOcclusionKeys;
        FloatBuffer instanceData;
        /** {@code memAllocFloat} в {@link #submit}; освобождать в {@link #endFrame}. */
        boolean instanceDataNativeOwned;
        /**
         * Граница «непрозрачные | затухающие» после {@link #partitionOpaqueFirst}:
         * записи [0, opaqueCount) — fade≈1, [opaqueCount, instanceCount) — затухающие,
         * отсортированные back-to-front. -1 = partition ещё не выполнялся.
         * instanceCullIndices/instanceOcclusionKeys при перестановке НЕ переупорядочиваются:
         * в MDI они не читаются (куллинг lag-1 выполнен на стороне BER).
         */
        int opaqueCount = -1;
        // ── Retained / clean-frame reuse ───────────────────────────────
        /** Штамп последнего submit/submitClean (кадр, в котором рендерер заявил присутствие). */
        long assertedFrame;
        /** Штамп последнего ГРЯЗНОГО сабмита (для правила «максимальный сабмит» за кадр). */
        long dirtySubmittedFrame;
        /**
         * Снапшот актуален содержимому записей рендерера: партиция и cachedMinFade
         * посчитаны и не требуют повтора до следующего грязного сабмита.
         */
        boolean partitionValid;
        /** Оффсет (во флоатах) opaque-окна прошлой заливки — пропуск неизменившихся окон. */
        int lastOpaqueUploadOffsetFloats = -1;
        /** Минимальный fade снапшота (считается при партиции вместо per-кадрового скана). */
        float cachedMinFade = 1.0f;
        Pending(InstancedStaticPartRenderer renderer) { this.renderer = renderer; }
    }

    /**
     * Непрерывный диапазон opaque-инстансов одного {@link Pending} (фаза opaque).
     * Итоговый командный буфер: [opaque-окна…][fading-слоты по 1 инстансу…] в ОДНОМ
     * мульти-драве; opaque всегда раньше fading (запечатывает глубину до блендинга).
     */
    static final class SubDraw {
        final Pending owner;
        final int firstInstance;
        final int count;
        int baseInstance;
        SubDraw(Pending owner, int firstInstance, int count, int baseInstance) {
            this.owner = owner;
            this.firstInstance = firstInstance;
            this.count = count;
            this.baseInstance = baseInstance;
        }
    }

    /**
     * Один fading-инстанс в глобальном back-to-front порядке. Fading-фаза идёт
     * С depth-write: самоперекрытие внутри модели (шип внутри кожуха) обязан
     * работать через depth-test, а взаимный depth-reject машин исключается
     * глобальной сортировкой слотов по дистанции до камеры (дальние рисуются
     * первыми, ближние блендятся поверх). По одному инстансу на команду —
     * команды дешёвые (32 Б), fading-инстансов обычно десятки.
     */
    static final class FadeSlot {
        final Pending owner;
        final int srcInstance;
        final float distSq;
        /** Значение fade записи (для minFade-агрегации без чтения снапшота). */
        float fade = 1.0f;
        int baseInstance;
        FadeSlot(Pending owner, int srcInstance, float distSq, int baseInstance) {
            this.owner = owner;
            this.srcInstance = srcInstance;
            this.distSq = distSq;
            this.baseInstance = baseInstance;
        }
    }

    private final Matrix4f projectionMatrix;
    private final List<Pending> pending = new ArrayList<>(16);
    private int totalInstances = 0;

    /** Latest projection from the render event (camera may move within a game tick). */
    public void refreshProjection(Matrix4f projection) {
        if (projection != null) {
            projectionMatrix.set(projection);
        }
    }

    private MdiBatchCoordinator(Matrix4f projectionMatrix) {
        // Clone: Forge may reuse the same Matrix4f across stages; sharing the
        // reference risks ProjMat changing between beginFrame and dispatch.
        this.projectionMatrix = new Matrix4f(projectionMatrix);
    }

    public static void ensureCapsResolved() {
        if (capsResolved) return;
        synchronized (MdiBatchCoordinator.class) {
            if (capsResolved) return;
            try {
                GLCapabilities caps = GL.getCapabilities();
                if (caps != null) {
                    boolean multi = caps.glMultiDrawElementsIndirect != 0L || caps.GL_ARB_multi_draw_indirect;
                    boolean single = caps.glDrawElementsIndirect != 0L || caps.GL_ARB_draw_indirect;
                    hasDrawIndirect = multi || single;
                    hasBaseInstance = caps.glDrawElementsInstancedBaseVertexBaseInstance != 0L;
                } else {
                    hasDrawIndirect = false;
                    hasBaseInstance = false;
                }
            } catch (Throwable t) {
                hasDrawIndirect = false;
                hasBaseInstance = false;
            }
            capsResolved = true;
        }
    }

    public static boolean isMdiAvailable() {
        ensureCapsResolved();
        boolean ok = hasDrawIndirect && hasBaseInstance;
        if (!loggedOnce) {
            loggedOnce = true;
            try {
                String vendor = GL11.glGetString(GL11.GL_VENDOR);
                String renderer = GL11.glGetString(GL11.GL_RENDERER);
                String version = GL11.glGetString(GL11.GL_VERSION);
                if (ok) {
                    MainRegistry.LOGGER.info(
                            "[HBM-M MDI] Draw indirect + base_instance available (atlas batch path). GL_VENDOR='{}', GL_RENDERER='{}', GL_VERSION='{}'",
                            vendor, renderer, version);
                } else {
                    MainRegistry.LOGGER.info(
                            "[HBM-M MDI] Draw indirect/base_instance NOT available (hasDrawIndirect={}, hasBaseInstance={}) — vanilla instanced path. GL_VENDOR='{}', GL_RENDERER='{}', GL_VERSION='{}'",
                            hasDrawIndirect, hasBaseInstance, vendor, renderer, version);
                }
            } catch (Throwable ignored) {
                // GL string queries can fail very early; safe to skip the log line.
            }
        }
        return ok;
    }

    public static MdiBatchCoordinator beginFrame(Matrix4f projectionMatrix) {
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            return null;
        }
        if (!isMdiAvailable()) return null;

        retainedFrameStamp++;
        MdiBatchCoordinator session = new MdiBatchCoordinator(projectionMatrix);
        ACTIVE.set(session);
        MdiRenderDiag.logBannerOnce();
        return session;
    }

    public static MdiBatchCoordinator active() {
        return ACTIVE.get();
    }

    /**
     * Сброс активной MDI-сессии без {@link #dispatch} — при F3+T / очистке GPU-кэшей,
     * когда кадр уже не должен бить в атлас (см. {@link MdiGeometryAtlas#resetForResourceLifecycle}).
     */
    public static void discardActiveSessionNoDispatch() {
        cancelScheduledDraw();
        MdiBatchCoordinator s = ACTIVE.get();
        if (s != null) {
            // pending-записи валидно установлены в retained (см. submit): снапшоты
            // остаются живыми, следующий кадр сам переассертит или дропнет записи.
            s.pending.clear();
            s.totalInstances = 0;
            ACTIVE.remove();
        }
    }

    public void endFrame() {
        endFrame(false);
    }

    /**
     * Finishes accumulation for a game tick. When {@code deferDraw}, uploads instance data
     * and replaces {@link #scheduledDraw} — only the last deferred batch is drawn per render frame.
     */
    public void endFrame(boolean deferDraw) {
        try {
            if (deferDraw) {
                scheduleDeferredDraw();
            } else {
                dispatch();
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] dispatch failed; future flushes will use legacy path", t);
        } finally {
            // pending-записи валидно живут в retained-списке (см. submit) — здесь их
            // трогать нельзя: снапшоты ещё нужны следующему кадру. Только сброс сессии.
            pending.clear();
            totalInstances = 0;
            if (ACTIVE.get() == this) ACTIVE.remove();
        }
    }

    /**
     * GL draw for the scheduled batch. Call at start of a new client render frame (first
     * {@code AFTER_ENTITIES}), not {@code AFTER_LEVEL} — dirty texture units after level end.
     */
    public static void presentScheduledDraw(Matrix4f projection) {
        DeferredDraw draw = scheduledDraw;
        if (draw == null) {
            return;
        }
        scheduledDraw = null;
        try {
            executeDeferredDraw(draw, projection);
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] deferred present failed", t);
        } finally {
            draw.free();
        }
    }

    public static void cancelScheduledDraw() {
        DeferredDraw draw = scheduledDraw;
        scheduledDraw = null;
        if (draw != null) {
            draw.free();
        }
    }

    public static void clearCachedRedraw() {
        cachedRedraw = null;
        cachedRedrawGameTime = -1L;
        // Смена мира / ресурс-релоад / сброс атласа: retained-записи невалидны.
        dropAllRetained();
    }

    /**
     * Re-emit the last tick's MDI batch (atlas instance VBO already uploaded). Keeps static parts
     * visible every render frame between game-tick uploads (~20 Hz).
     */
    public static void redrawCachedIfAny(Matrix4f projection) {
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            return;
        }
        PreparedMdi snap = cachedRedraw;
        if (snap == null || snap.drawList.isEmpty() || projection == null) {
            return;
        }
        try {
            // Atlas could have repacked (eviction) between publish and this
            // inter-tick redraw: refresh baseVertex/firstIndexBytes against the
            // current atlas layout, else the cached draw lands on another
            // renderer's geometry. Entries whose renderer lost its atlas slot
            // are dropped by refreshDrawListAtlasSlots.
            MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
            if (atlas != null && atlas.isReady()) {
                refreshDrawListAtlasSlots(snap.drawList, atlas);
            }
            if (snap.drawList.isEmpty()) {
                return;
            }
            executeMdiGlDraw(snap, new Matrix4f(projection), cachedRedrawGameTime);
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] cached redraw failed", t);
        }
    }

    private static void publishCachedRedraw(PreparedMdi prepared, long gameTime) {
        // Cached redraw copies drop instanceData (kept null) to avoid holding
        // native snapshot buffers across frames. The fade scan in
        // executeMdiGlDraw reads instanceData — without seeding minFade here it
        // stays 1.0f and translucent/fading machines flash opaque between ticks.
        float minFade = 1f;
        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas != null && atlas.isReady()) {
            int instanceFloatsPerInstance = atlas.getInstanceFloatsPerInstance();
            int fadeOffset = atlas.getInstanceFadeFloatOffset();
            if (instanceFloatsPerInstance > 0 && fadeOffset >= 0) {
                for (Pending p : prepared.drawList) {
                    FloatBuffer buf = p.instanceData;
                    if (buf == null) continue;
                    for (int i = 0; i < p.instanceCount; i++) {
                        float fa = buf.get(i * instanceFloatsPerInstance + fadeOffset);
                        if (fa < minFade) minFade = fa;
                    }
                }
            }
        }
        List<Pending> snap = new ArrayList<>(prepared.drawList.size());
        for (Pending p : prepared.drawList) {
            Pending q = new Pending(p.renderer);
            q.baseVertex = p.baseVertex;
            q.firstIndexBytes = p.firstIndexBytes;
            q.indexCount = p.indexCount;
            q.baseInstance = p.baseInstance;
            q.instanceCount = p.instanceCount;
            snap.add(q);
        }
        // Cached-снапшоты не несут instanceData: партиция/сортировка невозможны,
        // весь диапазон трактуем как fading (минимальное изменение поведения dormant-пути).
        List<FadeSlot> snapSlots = new ArrayList<>(snap.size());
        int snapBase = 0;
        for (Pending q : snap) {
            if (q.instanceCount <= 0) continue;
            for (int i = 0; i < q.instanceCount; i++) {
                snapSlots.add(new FadeSlot(q, i, 0f, snapBase++));
            }
        }
        cachedRedraw = new PreparedMdi(
                snap, List.of(), snapSlots, prepared.drawTotalInstances, prepared.pendingSize, prepared.droppedNoSlot, minFade);
        cachedRedrawGameTime = gameTime;
    }

    private void scheduleDeferredDraw() {
        PreparedMdi prepared = prepareMdiDraw(false);
        if (prepared == null) {
            return;
        }
        long gameTime = -1L;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                gameTime = mc.level.getGameTime();
            }
        } catch (Throwable ignored) {
        }
        DeferredDraw next = new DeferredDraw(
                prepared.drawList,
                prepared.opaqueSubs(),
                prepared.fadeSlots(),
                prepared.drawTotalInstances,
                prepared.pendingSize,
                prepared.droppedNoSlot,
                gameTime,
                projectionMatrix);
        DeferredDraw prev = scheduledDraw;
        scheduledDraw = next;
        if (prev != null) {
            prev.free();
        }
    }

    private record PreparedMdi(List<Pending> drawList, List<SubDraw> opaqueSubs, List<FadeSlot> fadeSlots,
                               int drawTotalInstances, int pendingSize, int droppedNoSlot, float minFade) {}

    private static void freeDrawListInstanceBuffers(List<Pending> drawList) {
        for (Pending p : drawList) {
            if (p.instanceDataNativeOwned && p.instanceData != null) {
                MemoryUtil.memFree(p.instanceData);
                p.instanceData = null;
                p.instanceDataNativeOwned = false;
            }
        }
    }

    // ── Snapshot pool + retained list helpers ───────────────────────────

    private static FloatBuffer takeSnapshot(int floats) {
        FloatBuffer buf = snapshotPool.pollFirst();
        if (buf != null && buf.capacity() < floats) {
            MemoryUtil.memFree(buf);
            buf = null;
        }
        if (buf == null) {
            buf = MemoryUtil.memAllocFloat(floats);
        }
        buf.clear();
        return buf;
    }

    private static void recyclePooled(FloatBuffer buf) {
        if (buf == null) {
            return;
        }
        if (snapshotPool.size() < SNAPSHOT_POOL_MAX_BUFFERS) {
            buf.clear();
            snapshotPool.addFirst(buf);
        } else {
            MemoryUtil.memFree(buf);
        }
    }

    private static void recycleSnapshot(Pending p) {
        if (p.instanceDataNativeOwned && p.instanceData != null) {
            FloatBuffer buf = p.instanceData;
            p.instanceData = null;
            p.instanceDataNativeOwned = false;
            recyclePooled(buf);
        }
    }

    /** Убирает запись из retained-списка/карты и возвращает снапшот в пул. */
    private static void uninstallRetained(Pending p) {
        Pending mapped = retainedByRenderer.get(p.renderer);
        if (mapped == p) {
            retainedByRenderer.remove(p.renderer);
        }
        recycleSnapshot(p);
    }

    /** Retained-записи без assert за кадр — рендерер ушёл из виду/мира. */
    private static void dropUnassertedRetained() {
        for (Iterator<Pending> it = retainedList.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.assertedFrame != retainedFrameStamp) {
                uninstallRetained(p);
                it.remove();
            }
        }
    }

    /** Полный сброс retained-состояния (смена мира, ресурс-релоад, сброс атласа). */
    private static void dropAllRetained() {
        for (int i = 0; i < retainedList.size(); i++) {
            Pending mapped = retainedByRenderer.get(retainedList.get(i).renderer);
            if (mapped == retainedList.get(i)) {
                retainedByRenderer.remove(retainedList.get(i).renderer);
            }
            recycleSnapshot(retainedList.get(i));
        }
        retainedList.clear();
        retainedByRenderer.clear();
    }

    public boolean submit(InstancedStaticPartRenderer renderer,
                          int indexCount,
                          int instanceCount,
                          int instanceDataSize,
                          FloatBuffer instanceDataFlipped,
                          int[] sourceCullIndices,
                          long[] sourceOcclusionKeys,
                          ByteBuffer atlasVertexBytes,
                          IntBuffer atlasIndices,
                          int atlasIndexCount) {
        if (renderer == null || instanceCount <= 0 || indexCount <= 0) return false;

        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (!atlas.acceptsInstanceDataSize(instanceDataSize)) return false;

        MdiGeometryAtlas.Slot slot = atlas.registerGeometryIfAbsent(renderer,
                atlasVertexBytes, atlasIndices, atlasIndexCount);
        if (slot == null) return false;

        int instanceFloats = instanceCount * instanceDataSize;
        FloatBuffer instanceSnapshot = takeSnapshot(instanceFloats);
        try {
            FloatBuffer srcView = instanceDataFlipped.duplicate();
            if (srcView.remaining() < instanceFloats) {
                recyclePooled(instanceSnapshot);
                return false;
            }
            int srcStart = srcView.position();
            srcView.limit(srcStart + instanceFloats);
            instanceSnapshot.put(srcView);
            instanceSnapshot.flip();
        } catch (Throwable t) {
            recyclePooled(instanceSnapshot);
            MainRegistry.LOGGER.warn("[HBM-M MDI] instance snapshot alloc/copy failed: {}", t.toString());
            return false;
        }

        Pending p = new Pending(renderer);
        p.baseVertex = slot.baseVertex;
        p.firstIndexBytes = slot.firstIndexBytes;
        p.indexCount = slot.indexCount;
        p.submitBaseVertex = slot.baseVertex;
        p.submitFirstIndexBytes = slot.firstIndexBytes;
        p.submitIndexCount = slot.indexCount;
        p.baseInstance = totalInstances;
        p.instanceCount = instanceCount;
        p.dirtySubmittedFrame = retainedFrameStamp;
        p.instanceData = instanceSnapshot;
        p.instanceDataNativeOwned = true;
        if (sourceCullIndices != null && sourceCullIndices.length >= instanceCount) {
            p.instanceCullIndices = new int[instanceCount];
            System.arraycopy(sourceCullIndices, 0, p.instanceCullIndices, 0, instanceCount);
        } else {
            p.instanceCullIndices = null;
        }
        if (sourceOcclusionKeys != null && sourceOcclusionKeys.length >= instanceCount) {
            p.instanceOcclusionKeys = new long[instanceCount];
            System.arraycopy(sourceOcclusionKeys, 0, p.instanceOcclusionKeys, 0, instanceCount);
        } else {
            p.instanceOcclusionKeys = null;
        }

        // Retained-установка: грязный сабмит заменяет запись рендерера НА МЕСТЕ
        // (стабильный порядок списка ⇒ стабильные оффсеты аплоада у остальных).
        // Второй сабмит того же рендерера за кадр (Embeddium multi-pass): действует
        // правило «максимальный сабмит», как в coalescePendingByRenderer.
        Pending prev = retainedByRenderer.put(renderer, p);
        if (prev != null) {
            int idx = retainedList.indexOf(prev);
            if (prev.dirtySubmittedFrame == retainedFrameStamp
                    && prev.instanceCount >= instanceCount) {
                // Первый сабмит этого кадра полнее — оставляем его.
                retainedByRenderer.put(renderer, prev);
                if (idx >= 0) {
                    retainedList.set(idx, prev);
                }
                recycleSnapshot(p);
                p = prev;
            } else {
                if (idx >= 0) {
                    retainedList.set(idx, p);
                } else {
                    retainedList.add(p);
                }
                recycleSnapshot(prev);
            }
        } else {
            retainedList.add(p);
        }
        p.assertedFrame = retainedFrameStamp;
        p.partitionValid = false;

        pending.add(p);
        totalInstances += instanceCount;
        return true;
    }

    /**
     * Чистый re-assert присутствия: рендерер не сделал в этой фазе записей ни одной
     * записи (skip-write в {@code addInstance}), буфер синхронизирован со снапшотом.
     * Прошлокадровая запись (снапшот, партиция, cachedMinFade) переиспользуется —
     * без копирования снапшота, диффа и перезаливки окна.
     *
     * @return true — присутствие засчитано; false — нужен обычный {@link #submit}.
     */
    public boolean submitClean(InstancedStaticPartRenderer renderer, int indexCount, int instanceCount) {
        if (!ClientRenderFlags.mdiCleanFrameReuse()) {
            return false;
        }
        Pending p = retainedByRenderer.get(renderer);
        if (p == null || !p.partitionValid) {
            return false;
        }
        if (p.instanceCount != instanceCount || p.indexCount != indexCount) {
            return false;
        }
        if (!renderer.mdiBufferSynced || renderer.mdiRecordWriteHappened) {
            return false;
        }
        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady() || atlas.getCurrentSlot(renderer) == null) {
            return false;
        }
        p.assertedFrame = retainedFrameStamp;
        return true;
    }

    private void dispatch() {
        PreparedMdi prepared = prepareMdiDraw(true);
        if (prepared == null) {
            return;
        }
        long gameTime = resolveLevelGameTime();
        // Снапшоты НЕ освобождаются после диспатча: retained-список переживает кадры
        // (submitClean переиспользует их без копирования). Возврат в пул — при замене
        // (грязный сабмит), дропе (нет assert / слот атласа) и clearCachedRedraw().
        executeMdiGlDraw(prepared, projectionMatrix, gameTime);
    }

    private static long resolveLevelGameTime() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                return mc.level.getGameTime();
            }
        } catch (Throwable ignored) {
        }
        return -1L;
    }

    private static void executeDeferredDraw(DeferredDraw draw, Matrix4f projectionOverride) {
        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady()) {
            return;
        }
        refreshDrawListAtlasSlots(draw.drawList, atlas);
        if (draw.drawList.isEmpty()) {
            return;
        }
        if (!uploadInstancesToAtlas(draw.opaqueSubs, draw.fadeSlots, atlas, atlas.getInstanceFloatsPerInstance())) {
            return;
        }
        Matrix4f proj = projectionOverride != null
                ? new Matrix4f(projectionOverride)
                : new Matrix4f(draw.projectionMatrix);
        PreparedMdi prepared = new PreparedMdi(
                draw.drawList, draw.opaqueSubs, draw.fadeSlots, draw.drawTotalInstances, draw.pendingSize, draw.droppedNoSlot, 1f);
        executeMdiGlDraw(prepared, proj, draw.gameTime);
    }

    /**
     * @param uploadInstances when {@code false}, only builds/compacts draw list (deferred present uploads later).
     */
    private PreparedMdi prepareMdiDraw(boolean uploadInstances) {
        // Phase B: retained-записи без assert за кадр — рендерер ушёл из виду/мира.
        dropUnassertedRetained();

        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady()) {
            return null;
        }

        // Грязные сабмиты: слот-чек + дрейф-диагностика (как раньше). Лузеры
        // multi-pass уже отсеяны в submit; устанавливать в retained нечего.
        int pendingSize = pending.size();
        for (int i = pending.size() - 1; i >= 0; i--) {
            Pending p = pending.get(i);
            MdiGeometryAtlas.Slot slot = atlas.getCurrentSlot(p.renderer);
            if (slot == null) {
                MainRegistry.LOGGER.warn(
                        "[HBM-M MDI] Atlas slot missing for renderer {}; dropping this MDI sub-draw (others still draw)",
                        System.identityHashCode(p.renderer));
                uninstallRetained(p);
                retainedList.remove(p);
                pending.remove(i);
                continue;
            }
            boolean slotDrift = p.submitBaseVertex != slot.baseVertex
                    || p.submitFirstIndexBytes != slot.firstIndexBytes
                    || p.submitIndexCount != slot.indexCount;
            if (slotDrift) {
                if (MdiRenderDiag.isDebugEnabled() || MdiRenderDiag.isVerboseEnabled()) {
                    String tag = p.renderer.getMdiTraceTag();
                    MainRegistry.LOGGER.warn(
                            "[HBM-M MDI] slot drift tag={} rid=0x{} submit(bv,fiB,ic)=({},{},{}) atlas=({},{},{})",
                            tag != null ? tag : "?",
                            Integer.toHexString(System.identityHashCode(p.renderer)),
                            p.submitBaseVertex, p.submitFirstIndexBytes, p.submitIndexCount,
                            slot.baseVertex, slot.firstIndexBytes, slot.indexCount);
                }
            }
            p.baseVertex = slot.baseVertex;
            p.firstIndexBytes = slot.firstIndexBytes;
            p.indexCount = slot.indexCount;
        }
        pending.clear();
        totalInstances = 0;

        // Список кадра = retained-список (стабильный порядок ⇒ стабильные оффсеты
        // аплоада; переиспользованные записи не перезаливаются). Слот-чек с ленивой
        // перерегистрацией геометрии из retained-байт рендерера (репак/сброс атласа).
        List<Pending> drawList = new ArrayList<>(retainedList.size());
        int droppedNoSlot = 0;
        for (Iterator<Pending> it = retainedList.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            MdiGeometryAtlas.Slot slot = atlas.getCurrentSlot(p.renderer);
            if (slot == null && p.renderer.atlasVertexBytesRetained != null
                    && p.renderer.atlasIndicesRetained != null
                    && p.renderer.atlasIndexCountRetained > 0) {
                slot = atlas.registerGeometryIfAbsent(p.renderer, p.renderer.atlasVertexBytesRetained,
                        p.renderer.atlasIndicesRetained, p.renderer.atlasIndexCountRetained);
            }
            if (slot == null) {
                uninstallRetained(p);
                it.remove();
                droppedNoSlot++;
                continue;
            }
            p.baseVertex = slot.baseVertex;
            p.firstIndexBytes = slot.firstIndexBytes;
            p.indexCount = slot.indexCount;
            drawList.add(p);
        }
        if (drawList.isEmpty()) {
            return null;
        }

        int instanceFloatsPerInstance = atlas.getInstanceFloatsPerInstance();
        int instanceFadeOffset = atlas.getInstanceFadeFloatOffset();

        // Вариант G+: партиция [opaque | fading back-to-front] — ТОЛЬКО для записей
        // с новым снапшотом; чистые переиспользуют прошлокадровую партицию и кэши.
        int cleanRenderers = 0;
        int cleanInstances = 0;
        int drawTotalInstances = 0;
        for (Pending p : drawList) {
            if (!p.partitionValid) {
                partitionOpaqueFirst(p, instanceFloatsPerInstance, instanceFadeOffset);
                p.cachedMinFade = computeCachedMinFade(p, instanceFloatsPerInstance, instanceFadeOffset);
                p.partitionValid = true;
            } else {
                cleanRenderers++;
                cleanInstances += p.instanceCount;
            }
            drawTotalInstances += p.instanceCount;
        }
        NucleusDebug.recordMdiReuse(drawList.size(), cleanRenderers, cleanInstances);
        if (drawTotalInstances == 0) {
            dropAllRetained();
            return null;
        }

        // Непрозрачные окна; fading-инстансы — в ПЛОСКИЙ глобальный список,
        // отсортированный по дистанции до камеры (дальние первыми) — см. FadeSlot.
        List<SubDraw> opaqueSubs = new ArrayList<>(drawList.size());
        int baseInst = 0;
        for (Pending p : drawList) {
            if (p.opaqueCount > 0) {
                opaqueSubs.add(new SubDraw(p, 0, p.opaqueCount, baseInst));
                baseInst += p.opaqueCount;
            }
        }

        float camX = FrameViewState.camX();
        float camY = FrameViewState.camY();
        float camZ = FrameViewState.camZ();
        List<FadeSlot> fadeSlots = new ArrayList<>();
        for (Pending p : drawList) {
            if (p.instanceData == null) continue;
            int first = Math.max(0, p.opaqueCount);
            for (int i = first; i < p.instanceCount; i++) {
                int base = i * instanceFloatsPerInstance;
                float dx = p.instanceData.get(base) - camX;
                float dy = p.instanceData.get(base + 1) - camY;
                float dz = p.instanceData.get(base + 2) - camZ;
                FadeSlot s = new FadeSlot(p, i, dx * dx + dy * dy + dz * dz, 0);
                s.fade = p.instanceData.get(base + instanceFadeOffset);
                fadeSlots.add(s);
            }
        }
        fadeSlots.sort((a, b) -> Float.compare(b.distSq, a.distSq));
        for (FadeSlot s : fadeSlots) {
            s.baseInstance = baseInst++;
        }

        if (uploadInstances && !uploadInstancesToAtlas(opaqueSubs, fadeSlots, atlas, instanceFloatsPerInstance)) {
            freeDrawListInstanceBuffers(drawList);
            dropAllRetained();
            return null;
        }

        return new PreparedMdi(drawList, opaqueSubs, fadeSlots, drawTotalInstances, pendingSize, droppedNoSlot, 1f);
    }

    /** Минимальный fade снапшота (opaque-область ≥ порога по построению — достаточно хвоста fading). */
    private static float computeCachedMinFade(Pending p, int floatsPerInstance, int fadeOffset) {
        float min = 1.0f;
        if (p.instanceData == null) {
            return min;
        }
        int first = Math.max(0, p.opaqueCount);
        for (int i = first; i < p.instanceCount; i++) {
            float fa = p.instanceData.get(i * floatsPerInstance + fadeOffset);
            if (fa < min) {
                min = fa;
            }
        }
        return min;
    }

    private static void refreshDrawListAtlasSlots(List<Pending> drawList, MdiGeometryAtlas atlas) {
        for (int i = drawList.size() - 1; i >= 0; i--) {
            Pending p = drawList.get(i);
            MdiGeometryAtlas.Slot slot = atlas.getCurrentSlot(p.renderer);
            if (slot == null) {
                if (p.instanceDataNativeOwned && p.instanceData != null) {
                    MemoryUtil.memFree(p.instanceData);
                    p.instanceData = null;
                    p.instanceDataNativeOwned = false;
                }
                drawList.remove(i);
                continue;
            }
            p.baseVertex = slot.baseVertex;
            p.firstIndexBytes = slot.firstIndexBytes;
            p.indexCount = slot.indexCount;
        }
    }

    /**
     * Заливает инстансы в атлас СТРОГО в порядке [opaqueSubs…, fadeSlots…] — том же,
     * в котором командам назначены {@code baseInstance}.
     * Аплоад — span-дифф против CPU-копии атласа ({@link GpuSpanUploader}): статичная
     * сцена с мировыми координатами записей не даёт ни одного glBufferSubData/copy.
     * Чистые окна (тот же снапшот на том же оффсете при валидной тени) пропускаются
     * целиком — даже memcmp-скан диффа не выполняется.
     */
    private static boolean uploadInstancesToAtlas(List<SubDraw> opaqueSubs, List<FadeSlot> fadeSlots,
                                                  MdiGeometryAtlas atlas, int instanceFloatsPerInstance) {
        int drawTotalInstances = 0;
        for (SubDraw sub : opaqueSubs) {
            drawTotalInstances += sub.count;
        }
        drawTotalInstances += fadeSlots.size();
        if (drawTotalInstances == 0) {
            return false;
        }
        if (!atlas.ensureInstanceCapacity(drawTotalInstances)) {
            return false;
        }
        boolean shadowValid = atlas.isInstanceShadowValid();
        int offsetFloats = 0;
        for (SubDraw sub : opaqueSubs) {
            Pending p = sub.owner;
            if (p.instanceData == null) {
                return false;
            }
            int floats = sub.count * instanceFloatsPerInstance;
            if (p.instanceData.remaining() < (sub.firstInstance + sub.count) * instanceFloatsPerInstance) {
                return false;
            }
            if (!(shadowValid && p.partitionValid && p.lastOpaqueUploadOffsetFloats == offsetFloats)) {
                atlas.uploadInstanceWindowSpanned(offsetFloats, p.instanceData,
                        sub.firstInstance * instanceFloatsPerInstance, floats);
            }
            p.lastOpaqueUploadOffsetFloats = offsetFloats;
            offsetFloats += floats;
        }
        for (FadeSlot s : fadeSlots) {
            Pending p = s.owner;
            if (p.instanceData == null) {
                return false;
            }
            if (p.instanceData.remaining() < (s.srcInstance + 1) * instanceFloatsPerInstance) {
                return false;
            }
            atlas.uploadInstanceWindowSpanned(offsetFloats, p.instanceData,
                    s.srcInstance * instanceFloatsPerInstance, instanceFloatsPerInstance);
            offsetFloats += instanceFloatsPerInstance;
        }
        atlas.markInstanceShadowValid();
        return true;
    }

    private static void executeMdiGlDraw(PreparedMdi prepared, Matrix4f projection, long gameTime) {
        List<Pending> drawList = prepared.drawList;
        List<SubDraw> opaqueSubs = prepared.opaqueSubs();
        List<FadeSlot> fadeSlots = prepared.fadeSlots();
        if (drawList.isEmpty() || (opaqueSubs.isEmpty() && fadeSlots.isEmpty())) {
            return;
        }
        // Embeddium/Iris диспатчат stage-события и из теневых terrain-проходов —
        // там флашить нельзя (тот же контракт, что у flushBatchIris); заодно это
        // защищает FrameViewState от захвата камеры теневого прохода.
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }

        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady()) {
            return;
        }

        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            return;
        }

        int nCmd = opaqueSubs.size() + fadeSlots.size();
        ByteBuffer cmdBuf = MemoryUtil.memAlloc(nCmd * INDIRECT_CMD_STRIDE_BYTES);
        cmdBuf.order(ByteOrder.nativeOrder());
        // Порядок команд = порядок аплоада (baseInstance согласованы):
        // [opaque-окна…][fading-слоты, глобально back-to-front].
        for (SubDraw sub : opaqueSubs) {
            Pending p = sub.owner;
            int rowStart = cmdBuf.position();
            cmdBuf.putInt(p.indexCount);
            cmdBuf.putInt(sub.count);
            cmdBuf.putInt(p.firstIndexBytes >>> 2);
            cmdBuf.putInt(p.baseVertex);
            cmdBuf.putInt(sub.baseInstance);
            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                cmdBuf.putInt(0);
            }
        }
        for (FadeSlot s : fadeSlots) {
            Pending p = s.owner;
            int rowStart = cmdBuf.position();
            cmdBuf.putInt(p.indexCount);
            cmdBuf.putInt(1);
            cmdBuf.putInt(p.firstIndexBytes >>> 2);
            cmdBuf.putInt(p.baseVertex);
            cmdBuf.putInt(s.baseInstance);
            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                cmdBuf.putInt(0);
            }
        }
        cmdBuf.flip();

        try {
            int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int prevArrayBuf = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            int prevIndirectBuf = 0;
            try {
                prevIndirectBuf = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
            } catch (Throwable ignored) {
            }
            boolean cullWas = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean depthTestWas = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean depthMaskWas = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            boolean blendWas = GL11.glIsEnabled(GL11.GL_BLEND);
            int prevSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            int prevDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            int prevSrcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            int prevDstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            boolean primitiveRestartWas = false;
            try {
                primitiveRestartWas = GL11.glIsEnabled(GL31.GL_PRIMITIVE_RESTART);
            } catch (Throwable ignored) {
            }

            String dispatchDrawMode = "MULTI";

            try {
                GL30.glBindVertexArray(atlas.getVaoId());
                atlas.enableVertexAttribArraysOnBoundVao();

                var mc = Minecraft.getInstance();
                if (mc.gameRenderer != null) {
                    //? if < 1.21.1 {
                    mc.gameRenderer.lightTexture().updateLightTexture(mc.getFrameTime());
                    //?} else {
                    /*// 1.21.1: getPartialTick() удалён — частичное время тика через DeltaTracker.Timer.
                    mc.gameRenderer.lightTexture().updateLightTexture(mc.getTimer().getGameTimeDeltaPartialTick(true));
                    *///?}
                }

                RenderSystem.setShader(() -> shader);
                applyCommonUniforms(shader, projection);
                // РЕГРЕССИЯ-СТОП (MDI): тот же контракт block_lit, что VanillaInstancedBatchRenderer — иначе белые batch.
                SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
                shader.apply();
                InstancedStaticPartRenderer.bindBlockLitTexturesBeforeDraw(shader);

                float minFade = prepared.minFade();
                // Кэшированные минимумы: cachedMinFade считается при партиции снапшота,
                // fade отдельных слотов захвачен при перечислении — per-кадровый
                // стридед-скан всех инстансов не нужен.
                for (SubDraw sub : opaqueSubs) {
                    if (sub.owner.instanceData != null && sub.owner.cachedMinFade < minFade) {
                        minFade = sub.owner.cachedMinFade;
                    }
                }
                for (FadeSlot s : fadeSlots) {
                    if (s.fade < minFade) {
                        minFade = s.fade;
                    }
                }

                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();
                if (minFade < 0.99f) {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }

                if (primitiveRestartWas) {
                    GL11.glDisable(GL31.GL_PRIMITIVE_RESTART);
                }

                // РЕГРЕССИЯ-ФИКС: ёмкость обязана вмещать ВСЕ команды (nCmd =
                // opaque-поды + fading-слоты по 1 инстансу), а не только число
                // рендереров — иначе рост числа fading-инстансов недо-ensure'ил
                // буфер и glBufferSubData обрезался по GL_INVALID_VALUE.
                int cmdByteLen = nCmd * INDIRECT_CMD_STRIDE_BYTES;
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, atlas.getIndirectBufferId());
                atlas.ensureIndirectCommandByteCapacity(cmdByteLen);
                GL15.glBufferSubData(GL40.GL_DRAW_INDIRECT_BUFFER, 0, cmdBuf);
                try {
                    GLCapabilities capsBarrier = GL.getCapabilities();
                    if (capsBarrier != null && capsBarrier.glMemoryBarrier != 0L) {
                        GL42.glMemoryBarrier(GL42.GL_COMMAND_BARRIER_BIT);
                    }
                } catch (Throwable ignored) {
                }

                GLCapabilities caps2 = GL.getCapabilities();
                boolean canMulti = caps2 != null
                        && (caps2.glMultiDrawElementsIndirect != 0L || caps2.GL_ARB_multi_draw_indirect);
                boolean canSingle = caps2 != null
                        && (caps2.glDrawElementsIndirect != 0L || caps2.GL_ARB_draw_indirect);

                // Глубина пишется на ОБЕИХ фазах: opaque запечатывает объём, fading
                // обеспечивает самоперекрытие внутри модели (шип внутри кожуха).
                // Взаимный depth-reject fading-машин исключён глобальной back-to-front
                // сортировкой fadeSlots (дальние в командном буфере раньше ближних).
                if (canMulti) {
                    if (caps2.glMultiDrawElementsIndirect != 0L) {
                        GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                GL11.GL_UNSIGNED_INT, 0L, nCmd, INDIRECT_CMD_STRIDE_BYTES);
                    } else {
                        ARBMultiDrawIndirect.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                GL11.GL_UNSIGNED_INT, 0L, nCmd, INDIRECT_CMD_STRIDE_BYTES);
                    }
                    dispatchDrawMode = "MULTI";
                } else if (canSingle) {
                    for (int i = 0; i < nCmd; i++) {
                        long cmdOff = (long) i * INDIRECT_CMD_STRIDE_BYTES;
                        if (caps2.glDrawElementsIndirect != 0L) {
                            GL40.glDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, cmdOff);
                        } else {
                            ARBDrawIndirect.glDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, cmdOff);
                        }
                    }
                    dispatchDrawMode = "IND_LOOP";
                } else {
                    MainRegistry.LOGGER.error(
                            "[HBM-M MDI] Нет ни glDrawElementsIndirect, ни glMultiDrawElementsIndirect — пропуск отрисовки атласа");
                    dispatchDrawMode = "NONE";
                }

                if (minFade < 0.99f) {
                    RenderSystem.disableBlend();
                }
                if (!"NONE".equals(dispatchDrawMode)) {
                    NucleusDebug.recordDraw("MULTI".equals(dispatchDrawMode) ? 1 : nCmd,
                            prepared.drawTotalInstances(),
                            "MULTI".equals(dispatchDrawMode) ? "MDI (multi)" : "MDI (indirect loop)");
                }
                logDispatchDiagStatic(prepared.pendingSize, opaqueSubs, fadeSlots, prepared.drawTotalInstances,
                        atlas, prepared.droppedNoSlot, dispatchDrawMode, gameTime);
            } finally {
                if (primitiveRestartWas) {
                    GL11.glEnable(GL31.GL_PRIMITIVE_RESTART);
                }
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, prevIndirectBuf);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuf);
                GL30.glBindVertexArray(prevVao);
                RenderSystem.depthMask(depthMaskWas);
                RenderSystem.depthFunc(prevDepthFunc);
                if (depthTestWas) {
                    RenderSystem.enableDepthTest();
                } else {
                    RenderSystem.disableDepthTest();
                }
                if (cullWas) {
                    RenderSystem.enableCull();
                } else {
                    RenderSystem.disableCull();
                }
                RenderSystem.blendFuncSeparate(prevSrcRgb, prevDstRgb, prevSrcA, prevDstA);
                if (blendWas) {
                    RenderSystem.enableBlend();
                } else {
                    RenderSystem.disableBlend();
                }
                restoreVanillaSolidShader();
            }
        } finally {
            MemoryUtil.memFree(cmdBuf);
        }
    }

    private static void restoreVanillaSolidShader() {
        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
    }

    /**
     * Embeddium runs several {@code AFTER_BLOCK_ENTITIES} passes per frame; each flush can
     * re-submit the same {@link InstancedStaticPartRenderer}. Keep the submission with the
     * most instances (latest / fullest pass) instead of stacking duplicates into one dispatch.
     */
    private void coalescePendingByRenderer() {
        if (pending.size() <= 1) {
            return;
        }
        IdentityHashMap<InstancedStaticPartRenderer, Pending> best = new IdentityHashMap<>();
        List<Pending> dropped = new ArrayList<>();
        for (Pending p : pending) {
            Pending prev = best.get(p.renderer);
            if (prev == null) {
                best.put(p.renderer, p);
                continue;
            }
            if (p.instanceCount > prev.instanceCount) {
                dropped.add(prev);
                best.put(p.renderer, p);
            } else {
                dropped.add(p);
            }
        }
        if (dropped.isEmpty()) {
            return;
        }
        for (Pending p : dropped) {
            if (p.instanceDataNativeOwned && p.instanceData != null) {
                MemoryUtil.memFree(p.instanceData);
                p.instanceData = null;
                p.instanceDataNativeOwned = false;
            }
        }
        pending.clear();
        pending.addAll(best.values());
        totalInstances = 0;
        for (Pending p : pending) {
            totalInstances += p.instanceCount;
        }
    }

    /** Lag-1 culling at BER only; per-slice MDI compact caused {@code draws=X/13} flicker. */
    private static void compactVisibleInstances(Pending p, int floatsPerInstance) {
    }

    /**
     * Переставляет записи снапшота в порядке [непрозрачные в исходном порядке |
     * затухающие back-to-front] и выставляет {@link Pending#opaqueCount}.
     *
     * <p>Логика — общий хелпер {@link InstancedStaticPartRenderer#partitionInstancesOpaqueFirst}:
     * прямой путь (GPU-bones chain-части) партиционирует свои буферы тем же кодом,
     * чтобы фазовый порядок «opaque раньше fading» совпадал во всех путях рендера.
     */
    private static void partitionOpaqueFirst(Pending p, int floatsPerInstance, int fadeOffset) {
        p.opaqueCount = 0;
        if (p.instanceData == null || p.instanceCount <= 0) {
            return;
        }
        p.opaqueCount = InstancedStaticPartRenderer.partitionInstancesOpaqueFirst(
                p.instanceData, p.instanceCount, floatsPerInstance, fadeOffset);
    }

    private static void applyCommonUniforms(ShaderInstance shader, Matrix4f projection) {
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projection);
        // InstPos/InstRot мировые: ModelViewMat = R_cam·T(-cam) (FrameViewState).
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(FrameViewState.viewMatrix());
        var fogStart = shader.getUniform("FogStart");
        if (fogStart != null) fogStart.set(RenderSystem.getShaderFogStart());
        var fogEnd = shader.getUniform("FogEnd");
        if (fogEnd != null) fogEnd.set(RenderSystem.getShaderFogEnd());
        var fogColor = shader.getUniform("FogColor");
        if (fogColor != null) {
            float[] c = RenderSystem.getShaderFogColor();
            fogColor.set(c[0], c[1], c[2], c[3]);
        }
        var fade = shader.getUniform("FadeAlpha");
        if (fade != null) fade.set(1.0f);
    }

    private static void logDispatchDiagStatic(int pendingSize, List<SubDraw> opaqueSubs, List<FadeSlot> fadeSlots,
                                              int drawTotalInstances,
                                              MdiGeometryAtlas atlas, int droppedNoSlot, String drawMode, long gameTime) {
        if (!MdiRenderDiag.isDebugEnabled() && !MdiRenderDiag.isVerboseEnabled()) {
            return;
        }
        String summary = String.format(
                "[HBM-M MDI] dispatch gameTime=%s draws=%d/%d droppedNoSlot=%d instances=%d atlasParts=%d mode=%s",
                gameTime == -1L ? "?" : Long.toString(gameTime),
                opaqueSubs.size() + fadeSlots.size(), pendingSize, droppedNoSlot, drawTotalInstances,
                atlas.getRegisteredGeometryCount(), drawMode);
        if (MdiRenderDiag.isDebugEnabled() || MdiRenderDiag.isVerboseEnabled()) {
            MainRegistry.LOGGER.info(summary);
        } else {
            MainRegistry.LOGGER.debug(summary);
        }
        if (!MdiRenderDiag.isVerboseEnabled()) {
            return;
        }
        for (SubDraw sub : opaqueSubs) {
            Pending p = sub.owner;
            String tag = p.renderer.getMdiTraceTag();
            if (tag == null) {
                tag = "?";
            }
            MainRegistry.LOGGER.info(
                    "[HBM-M MDI]   sub tag={} rid=0x{} idxCount={} instances={} baseInstance={} baseVertex={} firstIndexBytes={} phase=OPAQUE",
                    tag,
                    Integer.toHexString(System.identityHashCode(p.renderer)),
                    p.indexCount,
                    sub.count,
                    sub.baseInstance,
                    p.baseVertex,
                    p.firstIndexBytes);
        }
        for (FadeSlot s : fadeSlots) {
            Pending p = s.owner;
            String tag = p.renderer.getMdiTraceTag();
            if (tag == null) {
                tag = "?";
            }
            MainRegistry.LOGGER.info(
                    "[HBM-M MDI]   sub tag={} rid=0x{} idxCount={} instances=1 baseInstance={} baseVertex={} firstIndexBytes={} phase=FADE distSq={}",
                    tag,
                    Integer.toHexString(System.identityHashCode(p.renderer)),
                    p.indexCount,
                    s.baseInstance,
                    p.baseVertex,
                    p.firstIndexBytes,
                    String.format(java.util.Locale.ROOT, "%.1f", s.distSq));
        }
    }
}
