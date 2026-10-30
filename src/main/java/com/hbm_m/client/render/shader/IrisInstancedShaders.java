package com.hbm_m.client.render.shader;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
public final class IrisInstancedShaders {

    private static volatile boolean reflectionTried = false;
    private static volatile boolean available = false;

    private static Object pipeline;              // live IrisRenderingPipeline (swapped on rebuild, see refreshPipelineIfRebuilt)
    /** Generation {@link #pipeline} was fetched at; -1 = never resolved. */
    private static long pipelineGenSeen = -1L;

    private static final ShaderInstance[] cachedShader = { null, null };
    private static final String[] cachedShaderName = { null, null };
    private static final Object[][] cachedFbPair = { null, null }; // [before, after] GlFramebuffer

    // Cache of CONSTRUCTION FAILURE: without it a failed (e.g. GLSL error) shader
    // would be retried on EVERY flush of EVERY renderer - shader compilation in a
    // loop means FPS death + log spam. The failure lives until a pipeline rebuild.
    private static final boolean[] constructionFailed = { false, false };
    private static final long[] failedAtGeneration = { -1L, -1L };

    // Per-pass snapshot of the gbuffer/shadow-FB (filled in captureLiveFramebuffer
    // from IrisRenderBatch.setupOuter - the only moment the correct FB is
    // guaranteed to be bound; by the time of AFTER_BLOCK_ENTITIES
    // closePersistentIfActive has already switched the binding to the main
    // RenderTarget).
    private static final int[] capturedFbo = { -1, -1 };
    private static final int[][] capturedColors = { new int[8], new int[8] };
    private static final int[] capturedColorCount = { 0, 0 };
    private static final int[] capturedDepth = { -1, -1 };

    private static boolean constructionLogged = false;

    private IrisInstancedShaders() {}

    /**
     * One-time snapshot of the live FB (only needed as an ExtendedShader
     * constructor argument; these FBs are not used at runtime). Called from
     * IrisRenderBatch.setupOuter after the pack apply.
     */
    public static void captureLiveFramebuffer(boolean shadowPass) {
        if (!ensureReflected()) {
            return;
        }
        int slot = shadowPass ? 1 : 0;
        int liveFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (capturedFbo[slot] == liveFbo) {
            return;
        }
        if (!snapshotAttachments(slot)) {
            MainLog.warn("IrisInstancedShaders: capture(slot=" + slot + ") failed — liveFbo=" + liveFbo);
            return;
        }
        capturedFbo[slot] = liveFbo;
        com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM-M] IrisInstancedShaders: captured slot={}, liveFbo={}, colors=[{}..{}] (n={}), depth={}",
                slot, liveFbo,
                capturedColorCount[slot] > 0 ? capturedColors[slot][0] : -1,
                capturedColorCount[slot] > 0 ? capturedColors[slot][capturedColorCount[slot]-1] : -1,
                capturedColorCount[slot],
                capturedDepth[slot] == -1 ? "none" : String.valueOf(capturedDepth[slot]));
    }

    /**
     * Lazily builds our ExtendedShader (one per pass for the whole session).
     * The CONSTRUCTOR needs FB arguments - we hand it the one-time snapshot from
     * {@link #captureLiveFramebuffer}; at runtime we NEVER call this shader's
     * apply()/clear() (they would bind those FBs) - the pack shader does the
     * framebuffer binding, we only swap the program. The FB arguments therefore
     * play no role after construction, and the pack's attachment resize/
     * ping-pong cannot hurt us.
     *
     * @return our shader, or null (no snapshot yet / reflection unavailable).
     */
    public static ShaderInstance getOrCreate(boolean shadowPass) {
        if (!ensureReflected()) {
            return null;
        }
        if (!refreshPipelineIfRebuilt()) {
            return null;
        }
        // Defense in depth against routing drift: the MAIN-pass ExtendedShader
        // must only exist where a verified gbuffer encoder is selected (the
        // packed packing scheme - see IrisInstancedEncoders). Without it our
        // program would write content the pack's deferred composite cannot
        // decode, rendering black machines - consumers fall back to the
        // companion path (the pack's own program). The shadow slot is
        // universal (depth + vanilla-parity color).
        if (!shadowPass && !IrisInstancedEncoders.hasEncoder()) {
            return null;
        }
        int slot = shadowPass ? 1 : 0;
        if (cachedShader[slot] != null) {
            // The profile (pack gbuffer scheme) changed - rebuild with a different FSH.
            String wanted = IrisInstancedEncoders.shaderName(shadowPass);
            if (!wanted.equals(cachedShaderName[slot])) {
                disposeSlot(slot);
            } else {
                return cachedShader[slot];
            }
        }
        if (capturedFbo[slot] == -1) {
            return null; // FB snapshot for constructor arguments not taken yet
        }
        long generation = IrisExtendedShaderAccess.getPipelineGeneration();
        if (constructionFailed[slot] && failedAtGeneration[slot] == generation) {
            return null; // already failed on this pipeline build - do not retry every frame
        }
        String shaderName = IrisInstancedEncoders.shaderName(shadowPass);
        Object[] fbs = null;
        try {
            // Built before the FB pair so a failure here cannot orphan FBOs.
            Object customUniforms = IrisReflectionBridge.createEmptyCustomUniforms();
            fbs = buildFramebuffers(slot);
            if (fbs == null) {
                return null;
            }
            VertexFormat format = requireVertexFormat();
            ShaderInstance shader = IrisReflectionBridge.createExtendedShader(
                    Minecraft.getInstance().getResourceManager(),
                    shaderName,
                    format,
                    Boolean.FALSE,
                    fbs[0], fbs[1],
                    null,
                    IrisReflectionBridge.getAlphaTestOff(),
                    (java.util.function.Consumer<Object>) o -> { },
                    (java.util.function.BiConsumer<Object, Object>) (a, b) -> { },
                    Boolean.FALSE,
                    pipeline,
                    null,
                    customUniforms);
            cachedShader[slot] = shader;
            cachedShaderName[slot] = shaderName;
            cachedFbPair[slot] = fbs;
            constructionFailed[slot] = false;
            if (!constructionLogged) {
                constructionLogged = true;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] IrisInstancedShaders: ExtendedShader built (slot={}, programId={}, captureFbo={})",
                        slot, shader.getId(), capturedFbo[slot]);
            }
            return cachedShader[slot];
        } catch (Exception | LinkageError t) {
            // The pair is owned by cachedFbPair only AFTER a successful
            // construction - a throw from requireVertexFormat or the ctor up to
            // here would orphan both live GL FBOs; destroy them.
            if (fbs != null) {
                IrisReflectionBridge.destroyGlFramebuffer(fbs[0]);
                IrisReflectionBridge.destroyGlFramebuffer(fbs[1]);
            }
            constructionFailed[slot] = true;
            failedAtGeneration[slot] = generation;
            MainLog.warn("IrisInstancedShaders: ExtendedShader construction failed (slot=" + slot
                    + ") - disabled until pipeline rebuild - legacy path", t);
            // Oculus 1.8.x's causeException mixin destroys the driver's compile log
            // (unconditional cancel() on a non-cancellable injection) - recompile the
            // stages ourselves to recover the real GLSL error (never throws).
            ShaderCompileDiagnostics.reportFailedConstruction(shaderName, shadowPass, t);
            return null;
        }
    }

    private static VertexFormat requireVertexFormat() {
        ShaderInstance vanilla = ModShaders.getBlockLitInstancedShader();
        if (vanilla == null) {
            throw new IllegalStateException("vanilla block_lit_instanced shader not registered yet");
        }
        return vanilla.getVertexFormat();
    }

    private static Object[] buildFramebuffers(int slot) {
        Object before = null;
        Object after = null;
        try {
            before = IrisReflectionBridge.createGlFramebuffer();
            after = IrisReflectionBridge.createGlFramebuffer();
            fillAttachments(slot, before);
            fillAttachments(slot, after);
            return new Object[] { before, after };
        } catch (Exception t) {
            IrisReflectionBridge.destroyGlFramebuffer(before);
            IrisReflectionBridge.destroyGlFramebuffer(after);
            MainLog.warn("IrisInstancedShaders: framebuffer capture failed", t);
            return null;
        }
    }

    private static void fillAttachments(int slot, Object framebuffer) throws Exception {
        for (int i = 0; i < capturedColorCount[slot]; i++) {
            IrisReflectionBridge.addFramebufferColorAttachment(framebuffer, i, capturedColors[slot][i]);
        }
        if (capturedDepth[slot] != -1) {
            IrisReflectionBridge.addFramebufferDepthAttachment(framebuffer, capturedDepth[slot]);
        }
        IrisReflectionBridge.setFramebufferDrawBuffers(framebuffer, capturedColorCount[slot] > 0 ? 1 : 0);
    }

    /** Snapshots the texture names of the current GL_DRAW_FRAMEBUFFER into staging. */
    private static boolean snapshotAttachments(int slot) {
        for (int i = 0; i < capturedColors[slot].length; i++) {
            capturedColors[slot][i] = 0;
        }
        capturedColorCount[slot] = 0;
        capturedDepth[slot] = -1;
        for (int i = 0; i < 8; i++) {
            int name = queryAttachment(GL30.GL_COLOR_ATTACHMENT0 + i);
            if (name != 0) {
                capturedColors[slot][capturedColorCount[slot]++] = name;
            }
        }
        int depthType = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        if (depthType == GL11.GL_TEXTURE) {
            capturedDepth[slot] = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        }
        if (capturedColorCount[slot] == 0) {
            MainLog.warn("IrisInstancedShaders: live framebuffer has no color attachments — skipping");
            return false;
        }
        return true;
    }

    private static int queryAttachment(int attachment) {
        int type = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER, attachment, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        if (type == GL11.GL_TEXTURE) {
            return GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_DRAW_FRAMEBUFFER, attachment, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        }
        return 0;
    }

    private static synchronized boolean ensureReflected() {
        if (reflectionTried) {
            return available;
        }
        reflectionTried = true;
        if (!IrisReflectionBridge.isIrisPresent() || !IrisReflectionBridge.isReflectionAvailable()) {
            available = false;
            return false;
        }
        Object nullable = IrisReflectionBridge.getLivePipeline();
        if (nullable == null) {
            MainLog.warn("IrisInstancedShaders: Iris pipeline is null — instanced path disabled", null);
            available = false;
            return false;
        }
        pipeline = nullable;
        pipelineGenSeen = IrisExtendedShaderAccess.getPipelineGeneration();
        available = true;
        return true;
    }

    /**
     * Keeps the static {@code pipeline} pointing at the LIVE
     * {@code IrisRenderingPipeline}: after any rebuild (pack swap, settings
     * apply, F3+T) the previously pinned object is destroyed, yet it stayed
     * strongly reachable here AND through {@code ExtendedShader.parent} of the
     * cached shader. When the generation counter from
     * {@link IrisExtendedShaderAccess} moved, swap in the live pipeline and drop
     * BOTH cached slots first - their shaders are parented to the dead pipeline
     * and their FB pairs pin the old gbuffer textures. Must run before any
     * cached-shader fast path in {@link #getOrCreate}.
     */
    private static boolean refreshPipelineIfRebuilt() {
        long generation = IrisExtendedShaderAccess.getPipelineGeneration();
        if (generation == pipelineGenSeen) {
            return true;
        }
        Object nullable = IrisReflectionBridge.getLivePipeline();
        if (nullable == null) {
            MainLog.warn("IrisInstancedShaders: Iris pipeline is null — instanced path disabled", null);
            available = false;
            pipelineGenSeen = -1L;
            return false;
        }
        pipeline = nullable;
        pipelineGenSeen = generation;
        disposeSlot(0);
        disposeSlot(1);
        return true;
    }

    private static void disposeFramebuffers(int slot) {
        if (cachedFbPair[slot] != null) {
            for (Object fb : cachedFbPair[slot]) {
                IrisReflectionBridge.destroyGlFramebuffer(fb);
            }
        }
        cachedFbPair[slot] = null;
    }

    private static void disposeSlot(int slot) {
        if (cachedShader[slot] != null) {
            try {
                cachedShader[slot].close();
            } catch (Exception ignored) {
            }
            cachedShader[slot] = null;
        }
        disposeFramebuffers(slot);
        cachedShaderName[slot] = null;
        constructionFailed[slot] = false;
        failedAtGeneration[slot] = -1L;
    }

    /** Local logger - avoid dragging MainRegistry into every reflection site. */
    private static final class MainLog {
        static void warn(String message) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(message);
        }

        static void warn(String message, Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(message, t);
        }
    }
}
