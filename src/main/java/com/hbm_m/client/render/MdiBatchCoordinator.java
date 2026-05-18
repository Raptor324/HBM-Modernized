package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

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
 *       путь атласа; по умолчанию без {@code glMultiDrawElementsIndirect}.</li>
 *   <li>Otherwise: legacy per-renderer {@code glDrawElementsInstanced}
 *       (the {@code flushBatchVanilla} path stays as-is).</li>
 * </ol>
 * <p>
 * <b>Eligibility constraints for an individual flush:</b>
 * <ul>
 *   <li>Iris/Oculus NOT active (we only optimise the vanilla shader path).</li>
 *   <li>Unsliced light layout ({@code useSlicedLight == false}). Sliced parts
 *       use a different attribute layout (loc 7..14 + 15) and a different
 *       shader, so they fall through to the legacy path.</li>
 *   <li>Renderer initialised, instanceCount &gt; 0.</li>
 *   <li>{@link ModClothConfig#useMultiDrawIndirect} enabled.</li>
 * </ul>
 * <p>
 * <b>Iris path:</b> untouched. {@link InstancedStaticPartRenderer#flush(Matrix4f)}
 * still routes to {@code flushBatchIris} when an external shader is active —
 * the coordinator's eligibility test rejects those flushes up front.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
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
    private static final AtomicBoolean LOGGED_SEQUENTIAL_ACTIVE = new AtomicBoolean();
    private static final AtomicBoolean LOGGED_SEQUENTIAL_FALLBACK = new AtomicBoolean();

    private static final ThreadLocal<MdiBatchCoordinator> ACTIVE = new ThreadLocal<>();

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
        FloatBuffer instanceData;
        /** {@code memAllocFloat} в {@link #submit}; освобождать в {@link #endFrame}. */
        boolean instanceDataNativeOwned;
        Pending(InstancedStaticPartRenderer renderer) { this.renderer = renderer; }
    }

    private final Matrix4f projectionMatrix;
    private final List<Pending> pending = new ArrayList<>(16);
    private int totalInstances = 0;

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
        ModClothConfig cfg;
        try { cfg = ModClothConfig.get(); }
        catch (Throwable t) { return null; }
        if (cfg == null || !cfg.useMultiDrawIndirect) return null;
        if (!isMdiAvailable()) return null;

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
        MdiBatchCoordinator s = ACTIVE.get();
        if (s != null) {
            // #region agent log
            int psz = s.pending.size();
            if (psz > 0) {
                MdiDebugNdjson.log("H4", "MdiBatchCoordinator.discardActiveSessionNoDispatch",
                        "discarding non-empty mdi session", "{\"pending\":" + psz + "}");
            }
            // #endregion agent log
            for (Pending p : s.pending) {
                if (p.instanceDataNativeOwned && p.instanceData != null) {
                    MemoryUtil.memFree(p.instanceData);
                    p.instanceData = null;
                    p.instanceDataNativeOwned = false;
                }
            }
            s.pending.clear();
            s.totalInstances = 0;
            ACTIVE.remove();
        }
    }

    public void endFrame() {
        try {
            dispatch();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] dispatch failed; future flushes will use legacy path", t);
        } finally {
            for (Pending p : pending) {
                if (p.instanceDataNativeOwned && p.instanceData != null) {
                    MemoryUtil.memFree(p.instanceData);
                    p.instanceData = null;
                    p.instanceDataNativeOwned = false;
                }
            }
            pending.clear();
            totalInstances = 0;
            if (ACTIVE.get() == this) ACTIVE.remove();
        }
    }

    public boolean submit(InstancedStaticPartRenderer renderer,
                          int indexCount,
                          int instanceCount,
                          int instanceDataSize,
                          FloatBuffer instanceDataFlipped,
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
        FloatBuffer instanceSnapshot = MemoryUtil.memAllocFloat(instanceFloats);
        try {
            FloatBuffer srcView = instanceDataFlipped.duplicate();
            if (srcView.remaining() < instanceFloats) {
                MemoryUtil.memFree(instanceSnapshot);
                return false;
            }
            int srcStart = srcView.position();
            srcView.limit(srcStart + instanceFloats);
            instanceSnapshot.put(srcView);
            instanceSnapshot.flip();
        } catch (Throwable t) {
            MemoryUtil.memFree(instanceSnapshot);
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
        p.instanceData = instanceSnapshot;
        p.instanceDataNativeOwned = true;
        pending.add(p);
        totalInstances += instanceCount;
        return true;
    }

    private void dispatch() {
        if (pending.isEmpty()) return;

        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady()) return;

        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null) return;

        // Repack может произойти между ранним flush (assembler/door) и поздним
        // (chemical plant) в том же beginFrame/endFrame — Pending тогда держит
        // устаревшие baseVertex/firstIndexBytes. Перечитать Slot из атласа
        // непосредственно перед загрузкой инстансов и MDI.
        // Раньше при slot==null делали return на всём кадре — пропадали все части
        // (дверные створки, frame advanced assembler, cogs и т.д.).
        List<Pending> drawList = new ArrayList<>(pending.size());
        for (Pending p : pending) {
            MdiGeometryAtlas.Slot slot = atlas.getCurrentSlot(p.renderer);
            if (slot == null) {
                MainRegistry.LOGGER.warn(
                        "[HBM-M MDI] Atlas slot missing for renderer {}; dropping this MDI sub-draw (others still draw)",
                        System.identityHashCode(p.renderer));
                if (p.instanceDataNativeOwned && p.instanceData != null) {
                    MemoryUtil.memFree(p.instanceData);
                    p.instanceData = null;
                    p.instanceDataNativeOwned = false;
                }
                continue;
            }
            // #region agent log
            boolean slotDrift = p.submitBaseVertex != slot.baseVertex
                    || p.submitFirstIndexBytes != slot.firstIndexBytes
                    || p.submitIndexCount != slot.indexCount;
            if (slotDrift) {
                MdiDebugNdjson.log("H1", "MdiBatchCoordinator.dispatch",
                        "slot drift submit vs atlas",
                        "{\"rid\":" + System.identityHashCode(p.renderer)
                                + ",\"sbv\":" + p.submitBaseVertex + ",\"sfi\":" + p.submitFirstIndexBytes
                                + ",\"sic\":" + p.submitIndexCount + ",\"abv\":" + slot.baseVertex
                                + ",\"afi\":" + slot.firstIndexBytes + ",\"aic\":" + slot.indexCount + "}");
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
            // #endregion agent log
            p.baseVertex = slot.baseVertex;
            p.firstIndexBytes = slot.firstIndexBytes;
            p.indexCount = slot.indexCount;
            drawList.add(p);
        }
        int droppedNoSlot = pending.size() - drawList.size();
        if (drawList.isEmpty()) {
            return;
        }

        int drawTotalInstances = 0;
        for (Pending p : drawList) {
            drawTotalInstances += p.instanceCount;
        }
        if (!atlas.ensureInstanceCapacity(drawTotalInstances)) return;

        int baseInst = 0;
        for (Pending p : drawList) {
            p.baseInstance = baseInst;
            baseInst += p.instanceCount;
        }

        int instanceFloatsPerInstance = atlas.getInstanceFloatsPerInstance();
        atlas.orphanInstanceBuffer(drawTotalInstances);
        int offsetFloats = 0;
        for (Pending p : drawList) {
            int floats = p.instanceCount * instanceFloatsPerInstance;
            if (p.instanceData == null || p.instanceData.remaining() < floats) {
                // #region agent log
                MdiDebugNdjson.log("H5", "MdiBatchCoordinator.dispatch",
                        "instance buffer remaining insufficient; abort mdi",
                        "{\"rid\":" + System.identityHashCode(p.renderer)
                                + ",\"needFloats\":" + floats + ",\"rem\":" + (p.instanceData == null ? -1 : p.instanceData.remaining())
                                + ",\"ic\":" + p.instanceCount + "}");
                // #endregion agent log
                return;
            }
            atlas.uploadInstanceWindow(offsetFloats, p.instanceData, floats);
            offsetFloats += floats;
        }

        boolean wantSequential = MdiRenderDiag.isForceSequentialDraw();
        boolean canSequential = false;
        if (wantSequential) {
            try {
                GLCapabilities c = GL.getCapabilities();
                canSequential = c != null && c.glDrawElementsInstancedBaseVertexBaseInstance != 0L;
            } catch (Throwable ignored) {
            }
            if (!canSequential && LOGGED_SEQUENTIAL_FALLBACK.compareAndSet(false, true)) {
                MainRegistry.LOGGER.warn(
                        "[HBM-M MDI] rendering.mdiForceSequentialDraw включён в Cloth, но нужен OpenGL 4.2 (glDrawElementsInstancedBaseVertexBaseInstance). Используется indirect-путь.");
            }
        }
        boolean useSequential = wantSequential && canSequential;
        if (useSequential && LOGGED_SEQUENTIAL_ACTIVE.compareAndSet(false, true)) {
            MainRegistry.LOGGER.info(
                    "[HBM-M MDI] Sequential sub-draw path (Cloth: mdiForceSequentialDraw): тот же атлас и instance VBO, без indirect buffer.");
        }

        // GL_DRAW_INDIRECT: uint32 поля в порядке байт процессора. memAlloc даёт BIG_ENDIAN по умолчанию —
        // на LE тогда «работает» только первая команда, остальные читаются как мусор (одна base, без створок/cogs).
        ByteBuffer cmdBuf = null;
        if (!useSequential) {
            int nCmd = drawList.size();
            cmdBuf = MemoryUtil.memAlloc(nCmd * INDIRECT_CMD_STRIDE_BYTES);
            cmdBuf.order(ByteOrder.nativeOrder());
            for (Pending p : drawList) {
                int rowStart = cmdBuf.position();
                cmdBuf.putInt(p.indexCount);
                cmdBuf.putInt(p.instanceCount);
                // DrawElementsIndirectCommand: firstIndex в элементах (× sizeof(type) → байты в драйвере).
                cmdBuf.putInt(p.firstIndexBytes >>> 2);
                cmdBuf.putInt(p.baseVertex);
                cmdBuf.putInt(p.baseInstance);
                while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                    cmdBuf.putInt(0);
                }
            }
            cmdBuf.flip();
        }

        try {
            int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int prevArrayBuf = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            int prevIndirectBuf = 0;
            try { prevIndirectBuf = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING); }
            catch (Throwable ignored) {}
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

            String dispatchDrawMode = "SEQ";

            try {
                GL30.glBindVertexArray(atlas.getVaoId());

                RenderSystem.setShader(() -> shader);
                applyCommonUniforms(shader);
                SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
                shader.apply();
                // apply() (Forge/Embeddium) может переключить VAO; без повторной привязки атласа
                // indirect/SEQ рисуют в чужой конфигурации массивов (пропажи частей, мусор).
                GL30.glBindVertexArray(atlas.getVaoId());
                // apply() мог отключить attrib arrays — VAO хранит pointers, enable нужен для каждого sub-draw.
                atlas.enableVertexAttribArraysOnBoundVao();

                float minFade = 1f;
                int fadeOffset = atlas.getInstanceFadeFloatOffset();
                for (Pending p : drawList) {
                    FloatBuffer buf = p.instanceData;
                    for (int i = 0; i < p.instanceCount; i++) {
                        float fa = buf.get(i * instanceFloatsPerInstance + fadeOffset);
                        if (fa < minFade) minFade = fa;
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

                // Chunk/батч-проходы могут оставить PRIMITIVE_RESTART — с MDI это даёт «рваную» геометрию.
                if (primitiveRestartWas) {
                    GL11.glDisable(GL31.GL_PRIMITIVE_RESTART);
                }

                if (useSequential) {
                    for (Pending p : drawList) {
                        GL42.glDrawElementsInstancedBaseVertexBaseInstance(
                                GL11.GL_TRIANGLES,
                                p.indexCount,
                                GL11.GL_UNSIGNED_INT,
                                (long) p.firstIndexBytes,
                                p.instanceCount,
                                p.baseVertex,
                                p.baseInstance);
                    }
                } else {
                    ModClothConfig pathCfg = null;
                    try {
                        pathCfg = ModClothConfig.get();
                    } catch (Throwable ignored) {
                    }
                    boolean wantTrueMulti = pathCfg != null && pathCfg.mdiUseTrueMultiDraw;

                    int cmdByteLen = drawList.size() * INDIRECT_CMD_STRIDE_BYTES;
                    GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, atlas.getIndirectBufferId());
                    atlas.ensureIndirectCommandByteCapacity(cmdByteLen);
                    // Только SubData в уже выделенный буфер (см. MdiGeometryAtlas#initialise /
                    // ensureIndirectCommandByteCapacity). Повторный glBufferData на весь cap каждый
                    // кадр давал одинаковый мусор для IND_LOOP и MULTI при рабочем SEQ — драйвер
                    // читал indirect из «свежего» хранилища в неожиданном порядке относительно записи.
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

                    if (wantTrueMulti && canMulti) {
                        if (caps2.glMultiDrawElementsIndirect != 0L) {
                            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                    GL11.GL_UNSIGNED_INT, 0L, drawList.size(), INDIRECT_CMD_STRIDE_BYTES);
                        } else {
                            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                    GL11.GL_UNSIGNED_INT, 0L, drawList.size(), INDIRECT_CMD_STRIDE_BYTES);
                        }
                        dispatchDrawMode = "MULTI";
                    } else if (canSingle) {
                        int n = drawList.size();
                        for (int i = 0; i < n; i++) {
                            long cmdOff = (long) i * INDIRECT_CMD_STRIDE_BYTES;
                            if (caps2.glDrawElementsIndirect != 0L) {
                                GL40.glDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, cmdOff);
                            } else {
                                ARBDrawIndirect.glDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, cmdOff);
                            }
                        }
                        dispatchDrawMode = "IND_LOOP";
                    } else if (canMulti) {
                        if (caps2.glMultiDrawElementsIndirect != 0L) {
                            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                    GL11.GL_UNSIGNED_INT, 0L, drawList.size(), INDIRECT_CMD_STRIDE_BYTES);
                        } else {
                            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                    GL11.GL_UNSIGNED_INT, 0L, drawList.size(), INDIRECT_CMD_STRIDE_BYTES);
                        }
                        dispatchDrawMode = "MULTI_FALLBACK";
                    } else {
                        MainRegistry.LOGGER.error(
                                "[HBM-M MDI] Нет ни glDrawElementsIndirect, ни glMultiDrawElementsIndirect — пропуск отрисовки атласа");
                        dispatchDrawMode = "NONE";
                    }
                }

                if (minFade < 0.99f) RenderSystem.disableBlend();
                logDispatchDiag(pending.size(), drawList, drawTotalInstances, atlas, droppedNoSlot, dispatchDrawMode);
            } finally {
                if (primitiveRestartWas) {
                    GL11.glEnable(GL31.GL_PRIMITIVE_RESTART);
                }
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, prevIndirectBuf);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuf);
                GL30.glBindVertexArray(prevVao);
                RenderSystem.depthMask(depthMaskWas);
                RenderSystem.depthFunc(prevDepthFunc);
                if (depthTestWas) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
                if (cullWas) RenderSystem.enableCull(); else RenderSystem.disableCull();
                RenderSystem.blendFuncSeparate(prevSrcRgb, prevDstRgb, prevSrcA, prevDstA);
                if (blendWas) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
                RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            }
        } finally {
            if (cmdBuf != null) {
                MemoryUtil.memFree(cmdBuf);
            }
        }
    }

    private void applyCommonUniforms(ShaderInstance shader) {
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projectionMatrix);
        if (shader.MODEL_VIEW_MATRIX != null)
            shader.MODEL_VIEW_MATRIX.set(new Matrix4f(RenderSystem.getModelViewMatrix()));
        var fogStart = shader.getUniform("FogStart");
        if (fogStart != null) fogStart.set(RenderSystem.getShaderFogStart());
        var fogEnd = shader.getUniform("FogEnd");
        if (fogEnd != null) fogEnd.set(RenderSystem.getShaderFogEnd());
        var fogColor = shader.getUniform("FogColor");
        if (fogColor != null) {
            float[] c = RenderSystem.getShaderFogColor();
            fogColor.set(c[0], c[1], c[2], c[3]);
        }
        var sampler0 = shader.getUniform("Sampler0");
        if (sampler0 != null) sampler0.set(0);
        var fade = shader.getUniform("FadeAlpha");
        if (fade != null) fade.set(1.0f);
    }

    private void logDispatchDiag(int pendingSize, List<Pending> drawList, int drawTotalInstances,
                                 MdiGeometryAtlas atlas, int droppedNoSlot, String drawMode) {
        if (!MdiRenderDiag.isDebugEnabled() && !MdiRenderDiag.isVerboseEnabled()
                && !MdiRenderDiag.isForceSequentialDraw() && !MainRegistry.LOGGER.isDebugEnabled()) {
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
        String summary = String.format(
                "[HBM-M MDI] dispatch gameTime=%s draws=%d/%d droppedNoSlot=%d instances=%d atlasParts=%d mode=%s",
                gameTime == -1L ? "?" : Long.toString(gameTime),
                drawList.size(), pendingSize, droppedNoSlot, drawTotalInstances, atlas.getRegisteredGeometryCount(), drawMode);
        if (MdiRenderDiag.isDebugEnabled() || MdiRenderDiag.isVerboseEnabled() || MdiRenderDiag.isForceSequentialDraw()) {
            MainRegistry.LOGGER.info(summary);
        } else {
            MainRegistry.LOGGER.debug(summary);
        }
        if (!MdiRenderDiag.isVerboseEnabled()) {
            return;
        }
        for (Pending p : drawList) {
            String tag = p.renderer.getMdiTraceTag();
            if (tag == null) {
                tag = "?";
            }
            MainRegistry.LOGGER.info(
                    "[HBM-M MDI]   sub tag={} rid=0x{} idxCount={} instanceCount={} baseInstance={} baseVertex={} firstIndexBytes={}",
                    tag,
                    Integer.toHexString(System.identityHashCode(p.renderer)),
                    p.indexCount,
                    p.instanceCount,
                    p.baseInstance,
                    p.baseVertex,
                    p.firstIndexBytes);
        }
    }
}
