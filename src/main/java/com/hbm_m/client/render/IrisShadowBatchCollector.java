package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.IrisShaderApply;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.ShaderInstance;

@OnlyIn(Dist.CLIENT)
/**
 * Глобальный батч shadow-прохода под Iris/Oculus.
 * <p>
 * {@code addInstance} в shadow только ЗАПИСЫВАЕТ инстанс (30-флоатная запись —
 * поза инстанса в shadow-space: Iris передаёт BER'у PoseStack
 * {@code shadowModelView · T(bePos - camPos)}, а запись — точная декомпозиция
 * T(pos)·R(rot) этой позы), а в конце shadow BE-фазы миксин в Iris
 * {@code ShadowRenderer.renderShadows} (перед {@code copyPreTranslucentDepth})
 * вызывает {@link #flushGlobalShadowBatch()}:
 * {@link IrisRenderBatch#begin(boolean, Matrix4f)} ОДИН раз применяет
 * pack-тень (биндинг shadow-FB + PROJ = P_shadow + MV = identity — ровно тот же
 * контракт, что у прежнего per-BE пути), затем — {@code drawCompanion} на каждую
 * запись с pack-программой SHADOW_* (глубина + shadowcolor пишет САМА программа
 * пака в своём формате).
 * <p>
 * Почему НЕ наш инстансный ExtendedShader (см. {@code IrisInstancedShaders}):
 * однотаргетный FSH не воспроизводит формат gbuffer/shadow-вывода пака
 * (Photon, например, пакует albedo+нормаль+свет через pack_unorm_2x8 в
 * colortex1) — при corretной геометрии содержимое получалось «мусорным».
 * <p>
 * Точка флаша выбрана ПОСЛЕ {@code bufferSource.endBatch()} и ДО копирования
 * translucent-глубины: непрозрачная геометрия машин успевает записать глубину
 * до полупрозрачных теневых кастеров.
 * <p>
 * Отказобезопасность: если миксин не применился или pack-тень недоступна —
 * {@link #onMainPassFrameStart()} замечает незафлушенные данные и после двух
 * кадров возвращает машины на немедленный путь ({@code batchingEnabled=false}).
 */
public final class IrisShadowBatchCollector {

    /** Флоатов на инстанс — тот же layout, что у ванильного инстансного пути. */
    private static final int FLOATS_PER_INSTANCE = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
    private static final int GROW_INSTANCES = 256;

    static final class Entry {
        final InstancedStaticPartRenderer renderer;
        FloatBuffer data;
        int count;

        Entry(InstancedStaticPartRenderer renderer) {
            this.renderer = renderer;
            this.data = MemoryUtil.memAllocFloat(GROW_INSTANCES * FLOATS_PER_INSTANCE);
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>(64);
    private static boolean batchingEnabled = true;
    private static int staleFrames = 0;
    private static boolean disabledLogged = false;
    private static long disabledAtGeneration = -1L;

    private IrisShadowBatchCollector() {}

    public static boolean isBatchingEnabled() {
        return batchingEnabled;
    }

    // ── Пер-кадровый счётчик для кэша анимаций MachineBer ────────────────
    // Ключ (gameTime, partialTick) НЕ работает: partialTick у shadow- и main-
    // вызовов BER различается → 0% попаданий (лог 0914 02:34, 8.6M промахов/5с).
    // Вместо него — счётчик кадров: инкремент на первом shadow-record кадра
    // (тень рисуется первой) или на каждом main-старте, если теней не было.
    private static long renderFrame = 0;
    private static boolean shadowSinceMain = false;

    public static long renderFrame() {
        return renderFrame;
    }

    static void noteShadowRecord() {
        if (!shadowSinceMain) {
            shadowSinceMain = true;
            renderFrame++;
            com.hbm_m.client.render.NucleusDispatcherBypass.noteShadowPassStart();
        }
    }

    private static void noteMainFrameStart() {
        com.hbm_m.client.render.NucleusDispatcherBypass.noteMainFrameStart();
        if (!shadowSinceMain) {
            renderFrame++;
        }
        shadowSinceMain = false;
    }

    // Shadow-проекция, снятая при записи инстансов (RenderSystem-состояние
    // shadow-прохода, константа в течение фазы; флаш идёт до restorePlayerProjection).
    private static final Matrix4f stashProj = new Matrix4f();
    private static boolean stashValid = false;

    static void stashShadowMatrices(Matrix4f proj) {
        stashProj.set(proj);
        stashValid = true;
    }

    private static void clearStash() {
        stashValid = false;
    }

    /**
     * Записывает один инстанс: поза (pos/quat) — точная декомпозиция PoseStack'а,
     * полученного BER'ом в shadow-проходе; bbox и corner-light идут в запас
     * (свет в shadow не используется). fade = 1.
     */
    static void record(InstancedStaticPartRenderer renderer, Vector3f pos, Quaternionf rot,
                       float[] bboxMin, float[] cornerUV16) {
        if (IrisExtendedShaderAccess.getPipelineGeneration() != disabledAtGeneration
                && !batchingEnabled) {
            // Пайплайн пересоздался (F3+R/смена пака) — пробуем батч снова.
            batchingEnabled = true;
        }
        noteShadowRecord();
        Entry e = null;
        for (int i = 0; i < ENTRIES.size(); i++) {
            if (ENTRIES.get(i).renderer == renderer) {
                e = ENTRIES.get(i);
                break;
            }
        }
        if (e == null) {
            e = new Entry(renderer);
            ENTRIES.add(e);
        }
        if ((e.count + 1) * FLOATS_PER_INSTANCE > e.data.capacity()) {
            FloatBuffer grown = MemoryUtil.memAllocFloat(e.data.capacity() * 2);
            e.data.flip();
            grown.put(e.data);
            MemoryUtil.memFree(e.data);
            e.data = grown;
        }
        FloatBuffer d = e.data;
        d.put(pos.x).put(pos.y).put(pos.z);
        d.put(rot.x).put(rot.y).put(rot.z).put(rot.w);
        d.put(bboxMin[0]).put(bboxMin[1]).put(bboxMin[2]);
        d.put(bboxMin[3] - bboxMin[0]).put(bboxMin[4] - bboxMin[1]).put(bboxMin[5] - bboxMin[2]).put(1.0f);
        for (int i = 0; i < 16; i++) {
            d.put(cornerUV16[i]);
        }
        e.count++;
    }

    /**
     * Флаш в конце shadow BE-фазы (вызывается из Iris-миксина).
     * <p>
     * <b>Основной путь — инстансный</b> (универсален для любого пака): pack-тень
     * применяется ради биндинга shadow-FB, затем наш vanilla-parity ExtendedShader
     * (теневому FB не нужен pack-формат — только глубина и простой цвет) и ОДИН
     * {@code glDrawElementsInstanced} на part-renderer с ModelViewMat = identity
     * (записи — позы в shadow-space). Никаких per-record glDrawElements/matrices.
     * <p>
     * Фолбэк (наш шейдер недоступен) — per-record {@code drawCompanion} через
     * pack-программу SHADOW_*: один apply на фазу, но N дроуков и матриц.
     */
    public static void flushGlobalShadowBatch() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        try {
            if (!stashValid) {
                disableBatching("no shadow matrices stashed at record time");
                return;
            }
            if (IrisRenderBatch.active() != null) {
                // Утечка чужого батча (исключение в BER): begin вернул бы NOOP_NESTED,
                // чьи drawCompanion молча но-опятся — тени кадра пропали бы молча.
                disableBatching("leaked render batch is active at shadow flush");
                return;
            }
            // P_shadow: RenderSystem-проекция на момент записи (Iris подменяет её
            // через setShadowProjection и восстанавливает только ПОСЛЕ
            // copyPreTranslucentDepth — наш флаш идёт раньше).
            Matrix4f shadowProj = new Matrix4f(stashProj);

            ShaderInstance packShadow = IrisExtendedShaderAccess.getBlockShader(true);
            if (packShadow == null) {
                disableBatching("shadow pack shader unavailable");
                return;
            }
            // Pack-тень биндит актуальный shadow-FB — единственный гарантированный
            // момент снять снапшот для фабрики инстансного шейдера.
            if (!IrisShaderApply.tryApply(packShadow)) {
                disableBatching("shadow pack shader apply failed");
                return;
            }

            // ── Tier 1: GPU Compute Bake ───────────────────────────────
            // Один glDrawElements на part-renderer РОДНОЙ программой пака —
            // пак сам применяет свою дисторсию (никакой репликации схем).
            // Любой сбой → false → прозрачный возврат на Tier 2/3 ниже.
            if (NucleusGpuBaker.isEnabled()) {
                boolean baked = false;
                try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                    baked = NucleusGpuBaker.bakeAndDrawShadow(ENTRIES, packShadow, shadowProj);
                }
                if (baked) {
                    NucleusDebug.recordDraw(1, totalInstances(), "GPU bake shadow flush");
                    return;
                }
            }

            com.hbm_m.client.render.shader.IrisInstancedShaders.captureLiveFramebuffer(true);
            ShaderInstance ours = com.hbm_m.client.render.shader.IrisInstancedShaders.getOrCreate(true);

            int drawCalls = 0;
            int total = 0;
            // Инстансный теневой путь безопасен только когда воспроизводима
            // дисторсия пака ("рыбий глаз" вокруг игрока — иначе тени «ползают»)
            // или когда дисторсии нет вовсе. Kill-switch: -Dhbm.shadowInstancing=false.
            boolean instancedSafe = !"false".equalsIgnoreCase(System.getProperty("hbm.shadowInstancing", "true"))
                    && com.hbm_m.client.render.shader.IrisShadowDistortion.isCompatible();
            if (ours != null && instancedSafe) {
                try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                    for (int i = 0; i < ENTRIES.size(); i++) {
                        Entry e = ENTRIES.get(i);
                        if (e.count <= 0) {
                            continue;
                        }
                        if (e.renderer.drawShadowInstances(e.data, e.count, ours, shadowProj)) {
                            drawCalls++;
                            total += e.count;
                        }
                    }
                    // После наших дроуков программа = наша; возвращаем pack-программу,
                    // чтобы трекинг Iris (lastApplied) остался консистентным.
                    GL20.glUseProgram(packShadow.getId());
                }
                NucleusDebug.recordDraw(drawCalls, total, "Iris shadow instanced");
            } else {
                // Фолбэк: per-record drawCompanion через pack-программу SHADOW_*.
                try (IrisRenderBatch batch = IrisRenderBatch.begin(true, shadowProj)) {
                    if (batch == null) {
                        disableBatching("shadow pack shader unavailable");
                        return;
                    }
                    final Matrix4f recordPose = new Matrix4f();
                    final Quaternionf recordRot = new Quaternionf();
                    for (int i = 0; i < ENTRIES.size(); i++) {
                        Entry e = ENTRIES.get(i);
                        if (e.count <= 0) {
                            continue;
                        }
                        IrisCompanionMesh companion = e.renderer.getOrBuildCompanionForShadowBatch();
                        if (companion == null) {
                            continue;
                        }
                        for (int r = 0; r < e.count; r++) {
                            int base = r * FLOATS_PER_INSTANCE;
                            float px = e.data.get(base);
                            float py = e.data.get(base + 1);
                            float pz = e.data.get(base + 2);
                            float qx = e.data.get(base + 3);
                            float qy = e.data.get(base + 4);
                            float qz = e.data.get(base + 5);
                            float qw = e.data.get(base + 6);
                            recordRot.set(qx, qy, qz, qw);
                            // Точная рекомпозиция записанной позы: T(pos)·R(rot).
                            recordPose.translationRotate(px, py, pz, recordRot);
                            // Свет в shadow не нужен (глубина/shadowcolor) — UV2 = 0.
                            batch.drawCompanion(companion, recordPose, 0);
                            drawCalls++;
                        }
                    }
                    NucleusDebug.recordDraw(drawCalls, totalInstances(), "Iris shadow batch");
                }
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M] Iris shadow batch flush failed", t);
        } finally {
            clearEntries();
            clearStash();
        }
    }

    private static int totalInstances() {
        int total = 0;
        for (int i = 0; i < ENTRIES.size(); i++) {
            total += ENTRIES.get(i).count;
        }
        return total;
    }

    private static void disableBatching(String reason) {
        clearEntries();
        clearStash();
        if (batchingEnabled) {
            batchingEnabled = false;
            disabledAtGeneration = IrisExtendedShaderAccess.getPipelineGeneration();
            MainRegistry.LOGGER.warn(
                    "[HBM-M] Iris shadow batch disabled ({}): {} - machines fall back "
                            + "to per-BE immediate shadow draws until the pipeline is rebuilt",
                    reason,
                    "records discarded for this frame");
        }
    }


    /** Полная очистка с освобождением нативных буферов записей. */
    private static void clearEntries() {
        for (int i = 0; i < ENTRIES.size(); i++) {
            MemoryUtil.memFree(ENTRIES.get(i).data);
            ENTRIES.get(i).data = null;
        }
        ENTRIES.clear();
        stashValid = false;
    }

    /**
     * Начало main-прохода (тень уже отработала). Незафлашенные данные = миксин
     * не сработал: два таких кадра подряд → возврат на немедленный shadow-путь.
     */
    public static void onMainPassFrameStart() {
        noteMainFrameStart();
        if (!ENTRIES.isEmpty()) {
            clearEntries();
            staleFrames++;
            if (batchingEnabled && staleFrames >= 2) {
                batchingEnabled = false;
                disabledAtGeneration = IrisExtendedShaderAccess.getPipelineGeneration();
                if (!disabledLogged) {
                    disabledLogged = true;
                    MainRegistry.LOGGER.warn(
                            "[HBM-M] Iris shadow BE flush hook never fired (mixin not applied?) — "
                                    + "falling back to per-BE immediate shadow draws");
                }
            }
        } else {
            staleFrames = 0;
        }
    }
}
