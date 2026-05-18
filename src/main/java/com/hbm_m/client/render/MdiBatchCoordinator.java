package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

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
 * <b>one</b> {@code glMultiDrawElementsIndirect} call against a shared atlas
 * VAO holding all part geometries + a unified per-instance VBO.
 * <p>
 * <b>Fallback chain (in order):</b>
 * <ol>
 *   <li>{@code hasMDI && hasBaseInstance} (GL 4.0 + ARB_base_instance / GL 4.2):
 *       MDI path. Atlas-eligible part flushes are deferred and aggregated.</li>
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

    /** Bytes per {@code DrawElementsIndirectCommand} (5 × uint32). */
    static final int CMD_BYTES = 20;

    private static volatile boolean capsResolved = false;
    private static volatile boolean hasMDI = false;
    private static volatile boolean hasBaseInstance = false;
    private static volatile boolean loggedOnce = false;

    private static final ThreadLocal<MdiBatchCoordinator> ACTIVE = new ThreadLocal<>();

    static final class Pending {
        final InstancedStaticPartRenderer renderer;
        int baseVertex;
        /** Byte offset into the EBO for {@code DrawElementsIndirectCommand.firstIndex}. */
        int firstIndexBytes;
        int indexCount;
        /** Snapshot at submit time — сравнение с {@link #dispatch} (H1: repack между submit и draw). */
        int submitBaseVertex = -1;
        int submitFirstIndexBytes = -1;
        int submitIndexCount = -1;
        int baseInstance;
        int instanceCount;
        FloatBuffer instanceData;
        Pending(InstancedStaticPartRenderer renderer) { this.renderer = renderer; }
    }

    private final Matrix4f projectionMatrix;
    private final List<Pending> pending = new ArrayList<>(16);
    private int totalInstances = 0;

    private MdiBatchCoordinator(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }

    public static void ensureCapsResolved() {
        if (capsResolved) return;
        synchronized (MdiBatchCoordinator.class) {
            if (capsResolved) return;
            try {
                GLCapabilities caps = GL.getCapabilities();
                if (caps != null) {
                    hasMDI = caps.glMultiDrawElementsIndirect != 0L
                            || caps.GL_ARB_multi_draw_indirect;
                    hasBaseInstance = caps.glDrawElementsInstancedBaseInstance != 0L
                            || caps.GL_ARB_base_instance;
                } else {
                    hasMDI = false;
                    hasBaseInstance = false;
                }
            } catch (Throwable t) {
                hasMDI = false;
                hasBaseInstance = false;
            }
            capsResolved = true;
        }
    }

    public static boolean isMdiAvailable() {
        ensureCapsResolved();
        boolean ok = hasMDI && hasBaseInstance;
        if (!loggedOnce) {
            loggedOnce = true;
            try {
                String vendor = GL11.glGetString(GL11.GL_VENDOR);
                String renderer = GL11.glGetString(GL11.GL_RENDERER);
                String version = GL11.glGetString(GL11.GL_VERSION);
                if (ok) {
                    MainRegistry.LOGGER.info(
                            "[HBM-M MDI] Multi-Draw Indirect available. GL_VENDOR='{}', GL_RENDERER='{}', GL_VERSION='{}'",
                            vendor, renderer, version);
                } else {
                    MainRegistry.LOGGER.info(
                            "[HBM-M MDI] MDI/base_instance NOT available (hasMDI={}, hasBaseInstance={}) - vanilla instanced path will be used. GL_VENDOR='{}', GL_RENDERER='{}', GL_VERSION='{}'",
                            hasMDI, hasBaseInstance, vendor, renderer, version);
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

        Pending p = new Pending(renderer);
        p.baseVertex = slot.baseVertex;
        p.firstIndexBytes = slot.firstIndexBytes;
        p.indexCount = slot.indexCount;
        p.submitBaseVertex = slot.baseVertex;
        p.submitFirstIndexBytes = slot.firstIndexBytes;
        p.submitIndexCount = slot.indexCount;
        p.baseInstance = totalInstances;
        p.instanceCount = instanceCount;
        p.instanceData = instanceDataFlipped;
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

        if (!atlas.ensureInstanceCapacity(totalInstances)) return;

        // Repack может произойти между ранним flush (assembler/door) и поздним
        // (chemical plant) в том же beginFrame/endFrame — Pending тогда держит
        // устаревшие baseVertex/firstIndexBytes. Перечитать Slot из атласа
        // непосредственно перед загрузкой инстансов и MDI.
        for (Pending p : pending) {
            MdiGeometryAtlas.Slot slot = atlas.getCurrentSlot(p.renderer);
            if (slot == null) {
                MainRegistry.LOGGER.warn(
                        "[HBM-M MDI] Atlas slot missing for renderer {}; aborting MDI dispatch this frame",
                        System.identityHashCode(p.renderer));
                return;
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
            }
            // #endregion agent log
            p.baseVertex = slot.baseVertex;
            p.firstIndexBytes = slot.firstIndexBytes;
            p.indexCount = slot.indexCount;
        }

        int instanceFloatsPerInstance = atlas.getInstanceFloatsPerInstance();
        atlas.orphanInstanceBuffer(totalInstances);
        int offsetFloats = 0;
        for (Pending p : pending) {
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

        ByteBuffer cmdBuf = MemoryUtil.memAlloc(pending.size() * CMD_BYTES);
        try {
            for (Pending p : pending) {
                cmdBuf.putInt(p.indexCount);
                cmdBuf.putInt(p.instanceCount);
                cmdBuf.putInt(p.firstIndexBytes);
                cmdBuf.putInt(p.baseVertex);
                cmdBuf.putInt(p.baseInstance);
            }
            cmdBuf.flip();

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

            try {
                GL30.glBindVertexArray(atlas.getVaoId());

                RenderSystem.setShader(() -> shader);
                applyCommonUniforms(shader);
                SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
                shader.apply();

                float minFade = 1f;
                int fadeOffset = atlas.getInstanceFadeFloatOffset();
                for (Pending p : pending) {
                    FloatBuffer buf = p.instanceData;
                    int basePos = buf.position();
                    for (int i = 0; i < p.instanceCount; i++) {
                        float fa = buf.get(basePos + i * instanceFloatsPerInstance + fadeOffset);
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

                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, atlas.getIndirectBufferId());
                GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, cmdBuf, GL15.GL_STREAM_DRAW);

                if (GL.getCapabilities().glMultiDrawElementsIndirect != 0L) {
                    GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                            GL11.GL_UNSIGNED_INT, 0L, pending.size(), 0);
                } else {
                    ARBMultiDrawIndirect.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                            GL11.GL_UNSIGNED_INT, 0L, pending.size(), 0);
                }

                if (minFade < 0.99f) RenderSystem.disableBlend();
            } finally {
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
            MemoryUtil.memFree(cmdBuf);
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
}
