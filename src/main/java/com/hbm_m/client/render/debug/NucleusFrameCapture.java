package com.hbm_m.client.render.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hbm_m.client.render.FrameViewState;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.IrisShadowBatchCollector;
import com.hbm_m.client.render.shader.IrisReflectionBridge;
import com.hbm_m.client.render.shader.ShaderBindResync;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Nucleus "Mini RenderDoc" — Single-Frame State, Draw & Geometry Recorder.
 * Works identically on 1.20.1 Forge and 1.21.1 NeoForge.
 * Triggered via F9 (single frame) or Shift+F9 (auto-watchdog on vignette/state anomaly)
 * when enableDebugLogging is active in ModClothConfig.
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusFrameCapture {

    private enum CaptureState { IDLE, ARMED, CAPTURING }

    private static volatile CaptureState state = CaptureState.IDLE;
    private static volatile boolean autoWatchdog = false;
    private static String triggerReason = "Manual (F9)";

    private static FrameReport currentReport;
    private static GlSnapshot prevSnapshot;
    private static String lastWriterEvent = "NONE";
    private static String lastBlendMutatorEvent = "NONE";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");

    // ─── Reflection Handles for Internal Cache Inspection ──────────────────
    private static boolean reflInitialized = false;
    private static Field fBlendLastApplied;
    private static Field fBlendOpaque, fBlendSrcColor, fBlendDstColor, fBlendSrcAlpha, fBlendDstAlpha;
    private static Field fShaderLastProgramId;
    private static Field fGsmBlend, fGsmBlendEnabled, fGsmBlendSrcRgb, fGsmBlendDstRgb, fGsmBlendSrcA, fGsmBlendDstA;
    private static Field fGsmDepth, fGsmDepthEnabled, fGsmDepthMask, fGsmDepthFunc;
    private static Field fGsmCull, fGsmCullEnabled;
    private static Field fGsmColorMask, fGsmCmR, fGsmCmG, fGsmCmB, fGsmCmA;
    private static Field fGsmTextures, fGsmTexBinding;
    private static Field fIrisExtLastApplied;
    private static Field fTextureManagerByPath;

    private NucleusFrameCapture() {}

    public static boolean isDebugAllowed() {
        try {
            return ModClothConfig.get().enableDebugLogging || Boolean.getBoolean("hbm.frameCapture");
        } catch (Throwable t) {
            return Boolean.getBoolean("hbm.frameCapture");
        }
    }

    public static boolean isCapturing() {
        return state == CaptureState.CAPTURING;
    }

    /** Called when F9 is pressed. */
    public static void requestCapture(boolean toggleWatchdog) {
        if (!isDebugAllowed()) {
            notifyPlayer(Component.literal("[Nucleus RenderDoc] Enable enableDebugLogging in the mod config first!")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (toggleWatchdog) {
            autoWatchdog = !autoWatchdog;
            notifyPlayer(Component.literal("[Nucleus RenderDoc] Auto-Watchdog (auto-dump on vignette/state bug): "
                    + (autoWatchdog ? "ON" : "OFF")).withStyle(autoWatchdog ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            return;
        }
        triggerReason = "Manual (F9)";
        state = CaptureState.ARMED;
        notifyPlayer(Component.literal("[Nucleus RenderDoc] Armed for next frame capture...").withStyle(ChatFormatting.AQUA));
    }

    /** Called at the start of a render frame (RenderTickEvent.START on 1.20.1 / RenderFrameEvent.Pre on 1.21.1). */
    public static void onFrameStart() {
        if (!isDebugAllowed()) {
            state = CaptureState.IDLE;
            return;
        }
        if (state == CaptureState.ARMED) {
            beginCaptureSession(triggerReason);
        }
    }

    private static void beginCaptureSession(String reason) {
        ensureReflection();
        state = CaptureState.CAPTURING;
        currentReport = new FrameReport();
        currentReport.timestamp = LocalDateTime.now().format(TS_FMT);
        currentReport.triggerReason = reason;
        currentReport.renderFrame = IrisShadowBatchCollector.renderFrame();
        currentReport.irisActive = ShaderCompatibilityDetector.isExternalShaderActive();
        try {
            currentReport.anchorOrigin = FrameViewState.anchorOrigin().toShortString();
            currentReport.cameraPos = String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)",
                    FrameViewState.camX(), FrameViewState.camY(), FrameViewState.camZ());
        } catch (Throwable ignored) {}
        try {
            currentReport.gpuRenderer = GL11.glGetString(GL11.GL_RENDERER);
            currentReport.glVersion = GL11.glGetString(GL11.GL_VERSION);
        } catch (Throwable ignored) {}
        prevSnapshot = null;
        lastWriterEvent = "FRAME_START";
        lastBlendMutatorEvent = "FRAME_START";
        recordEvent("FRAME_START", "Frame begin", null);
    }

    /** Called at the end of a render frame (RenderTickEvent.END on 1.20.1 / RenderFrameEvent.Post on 1.21.1). */
    public static void onFrameEnd() {
        if (state != CaptureState.CAPTURING) return;
        recordEvent("FRAME_END", "Frame end", null);
        state = CaptureState.IDLE;
        FrameReport report = currentReport;
        currentReport = null;
        prevSnapshot = null;
        if (report != null) {
            exportReport(report);
        }
    }

    /**
     * Records a named checkpoint or draw call with full state inspection and diffing.
     */
    public static void recordEvent(String phaseOrHook, String details, Map<String, Object> extraData) {
        if (state != CaptureState.CAPTURING || currentReport == null) return;
        if (!RenderSystem.isOnRenderThread()) return;

        try {
            EventRecord ev = new EventRecord();
            ev.index = currentReport.events.size();
            ev.name = phaseOrHook;
            ev.details = details;
            ev.gl = captureGlSnapshot();
            ev.stateChanges = computeDiff(prevSnapshot, ev.gl);
            ev.extra = extraData;

            if (prevSnapshot == null || prevSnapshot.glBlendEnabled != ev.gl.glBlendEnabled
                    || !Objects.equals(prevSnapshot.glBlendFunc, ev.gl.glBlendFunc)
                    || !Objects.equals(prevSnapshot.mojangBlendModeLastApplied, ev.gl.mojangBlendModeLastApplied)) {
                lastBlendMutatorEvent = "#" + ev.index + " " + phaseOrHook + (details != null ? " (" + details + ")" : "");
            }

            detectAnomalies(ev);

            currentReport.events.add(ev);
            prevSnapshot = ev.gl;
            lastWriterEvent = "#" + ev.index + " " + phaseOrHook;
        } catch (Throwable t) {
            currentReport.anomalies.add("Capture exception at " + phaseOrHook + ": " + t);
        }
    }

    /**
     * Watchdog hook called around Vignette rendering.
     * If autoWatchdog is ON and the vignette is about to draw opaque, immediately starts a capture!
     */
    public static void inspectVignetteState(boolean isPost) {
        if (!isDebugAllowed()) return;
        if (state != CaptureState.CAPTURING && autoWatchdog) {
            ensureReflection();
            GlSnapshot snap = captureGlSnapshot();
            boolean expectedVanillaBlendMode = "opaque=false (SRC_ALPHA -> ONE_MINUS_SRC_ALPHA)".equals(snap.mojangBlendModeLastApplied);
            boolean blendCacheDesync = snap.gsmBlendEnabled != null && snap.gsmBlendEnabled != snap.glBlendEnabled;
            boolean broken = isPost
                    ? (blendCacheDesync || !expectedVanillaBlendMode)
                    : (!expectedVanillaBlendMode
                       || blendCacheDesync
                       || Boolean.TRUE.equals(snap.irisBlendLocked)
                       || Boolean.TRUE.equals(snap.irisDepthColorLocked));
            if (broken) {
                autoWatchdog = false;
                triggerReason = "AUTO_WATCHDOG: Vignette state corruption detected (" + (isPost ? "POST" : "PRE") + ")";
                // Arm capture for the start of the NEXT frame so the report captures
                // all 3D world stages that caused the corruption, not just the GUI tail.
                state = CaptureState.ARMED;
            }
        }
        if (state == CaptureState.CAPTURING) {
            recordEvent(isPost ? "GUI_VIGNETTE_POST" : "GUI_VIGNETTE_PRE",
                    "Last blend mutator before vignette: " + lastBlendMutatorEvent, null);
        }
    }

    /**
     * Inspects an instance buffer (MDI or GPU Bake) for NaN, Infinity, or uninitialized (0,0,0) flying artifacts.
     */
    public static Map<String, Object> inspectInstanceBuffer(String tag, FloatBuffer buf, int instanceCount, int strideFloats) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("tag", tag);
        info.put("instanceCount", instanceCount);
        if (buf == null || instanceCount <= 0 || buf.remaining() < instanceCount * strideFloats) {
            info.put("status", "BUFFER_UNDERSIZED_OR_NULL");
            return info;
        }
        int pos = buf.position();
        int nanCount = 0;
        int zeroQuatCount = 0;
        int originGhostCount = 0;
        int duplicateCount = 0;
        int visibleCount = 0;
        List<String> samples = new ArrayList<>();

        int maxCheck = Math.min(instanceCount, 256);
        for (int i = 0; i < maxCheck; i++) {
            int baseA = pos + i * strideFloats;
            if (buf.get(baseA + InstancedStaticPartRenderer.FADE_FLOAT_OFFSET) < 0.0f) continue;
            float ax = buf.get(baseA + InstancedStaticPartRenderer.POS_FLOAT_OFFSET);
            float ay = buf.get(baseA + InstancedStaticPartRenderer.POS_FLOAT_OFFSET + 1);
            float az = buf.get(baseA + InstancedStaticPartRenderer.POS_FLOAT_OFFSET + 2);
            for (int j = i + 1; j < maxCheck; j++) {
                int baseB = pos + j * strideFloats;
                if (buf.get(baseB + InstancedStaticPartRenderer.FADE_FLOAT_OFFSET) < 0.0f) continue;
                float bx = buf.get(baseB + InstancedStaticPartRenderer.POS_FLOAT_OFFSET);
                float by = buf.get(baseB + InstancedStaticPartRenderer.POS_FLOAT_OFFSET + 1);
                float bz = buf.get(baseB + InstancedStaticPartRenderer.POS_FLOAT_OFFSET + 2);
                float dx = ax - bx;
                float dy = ay - by;
                float dz = az - bz;
                if (dx * dx + dy * dy + dz * dz < 0.25f) {
                    duplicateCount++;
                }
            }
        }

        for (int i = 0; i < instanceCount; i++) {
            int base = pos + i * strideFloats;
            float px = buf.get(base + InstancedStaticPartRenderer.POS_FLOAT_OFFSET);
            float py = buf.get(base + InstancedStaticPartRenderer.POS_FLOAT_OFFSET + 1);
            float pz = buf.get(base + InstancedStaticPartRenderer.POS_FLOAT_OFFSET + 2);
            float fade = buf.get(base + InstancedStaticPartRenderer.FADE_FLOAT_OFFSET);
            int qxy = Float.floatToRawIntBits(buf.get(base + InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET));
            int qzw = Float.floatToRawIntBits(buf.get(base + InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET + 1));

            if (fade < 0.0f) continue;
            visibleCount++;

            if (!Float.isFinite(px) || !Float.isFinite(py) || !Float.isFinite(pz)) {
                nanCount++;
            }
            if (qxy == 0 && qzw == 0) {
                zeroQuatCount++;
            }
            if (Math.abs(px) < 1e-4f && Math.abs(py) < 1e-4f && Math.abs(pz) < 1e-4f && qxy == 0 && qzw == 0) {
                originGhostCount++;
            }
            if (samples.size() < 2 || i == instanceCount - 1) {
                float qx = InstancedStaticPartRenderer.decodeSnorm16(qxy);
                float qy = InstancedStaticPartRenderer.decodeSnorm16(qxy >>> 16);
                float qz = InstancedStaticPartRenderer.decodeSnorm16(qzw);
                float qw = InstancedStaticPartRenderer.decodeSnorm16(qzw >>> 16);
                samples.add(String.format(Locale.ROOT,
                        "slot[%d]: pos=(%.2f, %.2f, %.2f) quat=(%.3f, %.3f, %.3f, %.3f) fade=%.4f",
                        i, px, py, pz, qx, qy, qz, qw, fade));
            }
        }
        info.put("visibleInstances", visibleCount);
        info.put("nanPositions", nanCount);
        info.put("zeroQuaternions", zeroQuatCount);
        info.put("originGhosts", originGhostCount);
        info.put("duplicateOverlapping", duplicateCount);
        info.put("samples", samples);

        if (state == CaptureState.CAPTURING && currentReport != null) {
            if (duplicateCount > 0) {
                currentReport.anomalies.add(String.format(Locale.ROOT,
                        "[DUPLICATE_OVERLAPPING_INSTANCES] Mesh '%s' has %d duplicate overlapping instance pair(s) in the same buffer!",
                        tag, duplicateCount));
            }
            if (nanCount > 0 || zeroQuatCount > 0 || originGhostCount > 0) {
                currentReport.anomalies.add(String.format(Locale.ROOT,
                        "[GEOMETRY_ARTIFACT] Mesh '%s' has corrupted active records: NaN=%d, zeroQuat=%d, uninitializedOriginGhost=%d (out of %d visible)",
                        tag, nanCount, zeroQuatCount, originGhostCount, visibleCount));
            }
        }
        return info;
    }

    // ─── State Snapshot & Anomaly Analysis ─────────────────────────────────

    private static GlSnapshot captureGlSnapshot() {
        GlSnapshot s = new GlSnapshot();
        Map<Integer, String> texIdToName = buildTextureReverseMap();

        // 1. Physical OpenGL State
        s.glProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        s.glProgramValid = s.glProgram > 0 && GL20.glIsProgram(s.glProgram);
        s.glVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        s.glAttrib1Enabled = (s.glVao > 0) ? (GL20.glGetVertexAttribi(1, GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED) != 0) : null;
        s.glArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        s.glElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        s.glDrawIndirectBuffer = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
        s.glDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        s.glReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

        if (s.glDrawFbo > 0) {
            int cType = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_COLOR_ATTACHMENT0, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
            int cId = (cType == GL11.GL_TEXTURE) ? GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_COLOR_ATTACHMENT0, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) : 0;
            int dType = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
            int dId = (dType == GL11.GL_TEXTURE) ? GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) : 0;
            var mainRt = Minecraft.getInstance().getMainRenderTarget();
            boolean isMainRt = mainRt != null && s.glDrawFbo == mainRt.frameBufferId;
            s.glFboAttachments = String.format("FBO=%d (%s) color0=%d depth=%d",
                    s.glDrawFbo, isMainRt ? "MAIN_RT" : "PACK/CUSTOM", cId, dId);
        }

        s.glBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        s.glBlendFunc = factorName(srcRgb) + ", " + factorName(dstRgb) + " | A: " + factorName(srcA) + ", " + factorName(dstA);

        s.glDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        s.glDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        s.glDepthFunc = funcName(GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
        s.glCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer cm = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, cm);
            s.glColorMask = (cm.get(0) != 0 ? "R" : "-") + (cm.get(1) != 0 ? "G" : "-")
                    + (cm.get(2) != 0 ? "B" : "-") + (cm.get(3) != 0 ? "A" : "-");

            FloatBuffer dr = stack.mallocFloat(2);
            GL11.glGetFloatv(GL11.GL_DEPTH_RANGE, dr);
            s.glDepthRange = String.format(Locale.ROOT, "[%.3f, %.3f]", dr.get(0), dr.get(1));

            IntBuffer vp = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
            s.glViewport = vp.get(0) + "," + vp.get(1) + " " + vp.get(2) + "x" + vp.get(3);
        }

        // Physical Texture Units (0, 1, 2, 4, 10)
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        s.glActiveTextureUnit = prevActive - GL13.GL_TEXTURE0;
        for (int u : new int[]{0, 1, 2, 4, 10}) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            String rl = texIdToName.getOrDefault(bound, "");
            s.glTextureUnits.put("TU" + u, bound + (rl.isEmpty() ? "" : " (" + rl + ")"));
        }
        GL13.glActiveTexture(prevActive);

        // 2. Mojang Cached State (GlStateManager / RenderSystem / BlendMode)
        ShaderInstance rsShader = RenderSystem.getShader();
        s.rsShader = rsShader != null ? (rsShader.getName() + "#" + rsShader.getId() + " [" + rsShader.getClass().getSimpleName() + "]") : "null";

        try {
            if (fShaderLastProgramId != null) {
                s.mojangLastProgramId = fShaderLastProgramId.getInt(null);
            }
            if (fBlendLastApplied != null) {
                Object bm = fBlendLastApplied.get(null);
                if (bm == null) {
                    s.mojangBlendModeLastApplied = "null";
                } else {
                    boolean op = fBlendOpaque != null && fBlendOpaque.getBoolean(bm);
                    int bSrc = fBlendSrcColor != null ? fBlendSrcColor.getInt(bm) : -1;
                    int bDst = fBlendDstColor != null ? fBlendDstColor.getInt(bm) : -1;
                    s.mojangBlendModeLastApplied = "opaque=" + op + " (" + factorName(bSrc) + " -> " + factorName(bDst) + ")";
                }
            }
            if (fGsmBlend != null) {
                Object bState = fGsmBlend.get(null);
                boolean bEn = getSubBoolean(bState, fGsmBlendEnabled);
                int cSrc = fGsmBlendSrcRgb.getInt(bState);
                int cDst = fGsmBlendDstRgb.getInt(bState);
                int cSrcA = fGsmBlendSrcA.getInt(bState);
                int cDstA = fGsmBlendDstA.getInt(bState);
                s.gsmBlendEnabled = bEn;
                s.gsmBlendFunc = factorName(cSrc) + ", " + factorName(cDst) + " | A: " + factorName(cSrcA) + ", " + factorName(cDstA);
            }
            if (fGsmDepth != null) {
                Object dState = fGsmDepth.get(null);
                s.gsmDepthTest = getSubBoolean(dState, fGsmDepthEnabled);
                s.gsmDepthMask = fGsmDepthMask.getBoolean(dState);
                s.gsmDepthFunc = funcName(fGsmDepthFunc.getInt(dState));
            }
            if (fGsmCull != null) {
                Object cState = fGsmCull.get(null);
                s.gsmCullEnabled = getSubBoolean(cState, fGsmCullEnabled);
            }
            if (fGsmColorMask != null) {
                Object cmObj = fGsmColorMask.get(null);
                s.gsmColorMask = (fGsmCmR.getBoolean(cmObj) ? "R" : "-")
                        + (fGsmCmG.getBoolean(cmObj) ? "G" : "-")
                        + (fGsmCmB.getBoolean(cmObj) ? "B" : "-")
                        + (fGsmCmA.getBoolean(cmObj) ? "A" : "-");
            }
            if (fGsmTextures != null && fGsmTexBinding != null) {
                Object[] arr = (Object[]) fGsmTextures.get(null);
                for (int u : new int[]{0, 1, 2, 4, 10}) {
                    if (u < arr.length && arr[u] != null) {
                        s.gsmTextureUnits.put("TU" + u, fGsmTexBinding.getInt(arr[u]));
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. Oculus / Iris State
        s.irisShadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        s.irisDepthColorLocked = ShaderBindResync.irisDepthColorDisabled();
        s.irisBlendLocked = IrisReflectionBridge.blendModeIsLocked();
        try {
            if (fIrisExtLastApplied != null) {
                Object ext = fIrisExtLastApplied.get(null);
                if (ext instanceof ShaderInstance si) {
                    s.irisExtendedLastApplied = si.getName() + "#" + si.getId();
                } else {
                    s.irisExtendedLastApplied = String.valueOf(ext);
                }
            }
            Object pipe = IrisReflectionBridge.getLivePipeline();
            if (pipe != null) {
                Object phase = IrisReflectionBridge.getPhase(pipe);
                s.irisPhase = phase != null ? phase.toString() : "UNKNOWN";
                s.irisAfterTranslucent = IrisReflectionBridge.isAfterTranslucentPhase();
            }
        } catch (Throwable ignored) {}

        // 4. Nucleus / GunGL State
        s.gunGlState = String.format(Locale.ROOT,
                "tex=%s mode=%s(blend=%b,add=%b) cull=%b depthMask=%b light=%b fb=%b gui=%b rgba=(%.2f,%.2f,%.2f,%.2f)",
                GunGL.texture(), !GunGL.isBlend() ? "0:CUTOUT" : (GunGL.isAdditive() ? "2:ADD" : "1:TRANS"),
                GunGL.isBlend(), GunGL.isAdditive(), GunGL.isCull(), GunGL.isDepthMask(),
                GunGL.isLighting(), GunGL.isFullbright(), GunGL.isGuiLighting(),
                GunGL.red(), GunGL.green(), GunGL.blue(), GunGL.alpha());

        Matrix4f mv = RenderSystem.getModelViewMatrix();
        s.rsModelViewSummary = summarizeMatrix("RS_MV", mv);
        s.rsProjSummary = summarizeMatrix("RS_PROJ", RenderSystem.getProjectionMatrix());

        return s;
    }

    private static void detectAnomalies(EventRecord ev) {
        GlSnapshot g = ev.gl;

        // 1. Physical GL vs GlStateManager cache desyncs
        if (g.gsmBlendEnabled != null && g.gsmBlendEnabled != g.glBlendEnabled) {
            flagAnomaly(ev, "[BLEND_ENABLED_DESYNC] GlStateManager thinks blend=" + g.gsmBlendEnabled
                    + ", but physical GL_BLEND=" + g.glBlendEnabled + " (after " + lastWriterEvent + ")");
        }
        if (g.glBlendEnabled && g.gsmBlendFunc != null && !g.gsmBlendFunc.equals(g.glBlendFunc)) {
            flagAnomaly(ev, "[BLEND_FUNC_DESYNC] Active blend has GlStateManager cache=(" + g.gsmBlendFunc
                    + ") != physical GL=(" + g.glBlendFunc + ")");
        }
        if (g.gsmDepthMask != null && g.gsmDepthMask != g.glDepthMask) {
            flagAnomaly(ev, "[DEPTH_MASK_DESYNC] GlStateManager depthMask=" + g.gsmDepthMask
                    + " != physical GL_DEPTH_WRITEMASK=" + g.glDepthMask);
        }
        if (g.gsmColorMask != null && !g.gsmColorMask.equals(g.glColorMask)) {
            flagAnomaly(ev, "[COLOR_MASK_DESYNC] GlStateManager colorMask=" + g.gsmColorMask
                    + " != physical GL_COLOR_WRITEMASK=" + g.glColorMask);
        }

        // 2. Texture Unit 0 Desync
        Integer gsmTu0 = g.gsmTextureUnits.get("TU0");
        String physTu0Str = g.glTextureUnits.get("TU0");
        if (gsmTu0 != null && physTu0Str != null) {
            int physTu0 = Integer.parseInt(physTu0Str.split(" ")[0]);
            if (gsmTu0 != physTu0) {
                flagAnomaly(ev, "[TU0_CACHE_DESYNC] GlStateManager TU0=" + gsmTu0 + " != physical GL TU0=" + physTu0Str);
            }
        }

        // 3. Program 0 / Stale Program Cache Desync on active batch begin / draw
        if (ev.name.contains("BATCH_BEGIN") || ev.name.contains("DRAW")) {
            if (g.glProgram == 0) {
                flagAnomaly(ev, "[PROGRAM_ZERO_DRAW] Draw/batch started with GL_CURRENT_PROGRAM == 0! (rsShader="
                        + g.rsShader + ", lastProgramId=" + g.mojangLastProgramId + ")");
            }
        }

        // 4. Vignette Black Screen Specific Diagnosis
        // Vanilla position_tex.json has blend = opaque=false (SRC_ALPHA -> ONE_MINUS_SRC_ALPHA).
        // If BlendMode.lastApplied is ANYTHING else (e.g. opaque=true (ONE -> ZERO) or SRC_ALPHA -> ONE),
        // position_tex.apply() inside Gui.renderVignette() sees !this.blend.equals(lastApplied) and
        // overwrites Gui.renderVignette()'s (ZERO, ONE_MINUS_SRC_COLOR) multiply blend with
        // (SRC_ALPHA, ONE_MINUS_SRC_ALPHA), drawing vignette.png (alpha=1.0) as an opaque black screen!
        boolean isVanillaDefaultBlendMode = "opaque=false (SRC_ALPHA -> ONE_MINUS_SRC_ALPHA)".equals(g.mojangBlendModeLastApplied);
        if ("GUI_VIGNETTE_PRE".equals(ev.name)) {
            if (!isVanillaDefaultBlendMode) {
                flagAnomaly(ev, "[VIGNETTE_BLACK_SCREEN_TRAP] Entering Gui.renderVignette() with Mojang BlendMode.lastApplied="
                        + g.mojangBlendModeLastApplied + " instead of opaque=false (SRC_ALPHA -> ONE_MINUS_SRC_ALPHA)! "
                        + "When position_tex.apply() runs inside renderVignette(), it will overwrite (ZERO, ONE_MINUS_SRC_COLOR) "
                        + "with (SRC_ALPHA, ONE_MINUS_SRC_ALPHA) and draw a 100% opaque black screen! "
                        + "Last blend mutator: " + lastBlendMutatorEvent);
            }
            if (Boolean.TRUE.equals(g.irisBlendLocked)) {
                flagAnomaly(ev, "[VIGNETTE_IRIS_BLEND_LOCKED] Entering Gui.renderVignette() while Oculus BlendModeStorage.isBlendLocked()==true! "
                        + "Vignette blendFuncSeparate will be swallowed by Oculus!");
            }
        }
        if ("GUI_VIGNETTE_POST".equals(ev.name)) {
            boolean blendDesync = g.gsmBlendEnabled != null && g.gsmBlendEnabled != g.glBlendEnabled;
            if (blendDesync || !isVanillaDefaultBlendMode) {
                flagAnomaly(ev, "[VIGNETTE_DREW_CORRUPTED] Gui.renderVignette() finished with GL_BLEND="
                        + g.glBlendEnabled + " (GSM=" + g.gsmBlendEnabled + ") and BlendMode.lastApplied="
                        + g.mojangBlendModeLastApplied + "! Culprit blend mutator: " + lastBlendMutatorEvent);
            }
        }

        // 5. Lingering Oculus locks or non-default BlendMode after level/hand rendering
        if (ev.name.contains("after_level") || "HAND_RENDER_POST".equals(ev.name)) {
            if (Boolean.TRUE.equals(g.irisDepthColorLocked)) {
                flagAnomaly(ev, "[LEAKED_IRIS_DEPTH_COLOR_LOCK] Oculus DepthColorStorage is STILL LOCKED at " + ev.name);
            }
            if (Boolean.TRUE.equals(g.irisBlendLocked)) {
                flagAnomaly(ev, "[LEAKED_IRIS_BLEND_LOCK] Oculus BlendModeStorage is STILL LOCKED at " + ev.name);
            }
            if (!isVanillaDefaultBlendMode) {
                flagAnomaly(ev, "[LEAKED_NON_DEFAULT_BLENDMODE] Mojang BlendMode.lastApplied left as "
                        + g.mojangBlendModeLastApplied + " at " + ev.name
                        + " instead of opaque=false (SRC_ALPHA -> ONE_MINUS_SRC_ALPHA) (will break vignette/GUI!)");
            }
        }

        // 6. Shaderpack & GPU Bake Anomalies
        if ((ev.name.contains("BATCH") || ev.name.contains("DRAW") || ev.name.contains("BAKE")) && g.glVao > 0) {
            if (Boolean.FALSE.equals(g.glAttrib1Enabled)) {
                flagAnomaly(ev, "[COLOR_ATTRIB_DISABLED] VAO " + g.glVao + " has vertex attribute 1 (Color) DISABLED! "
                        + "Vertex colors will default to vec4(0,0,0,1) pure black!");
            }
        }
        if (ev.name.startsWith("GPU_BAKE_") && !com.hbm_m.client.render.NucleusGpuBaker.cullEnabled()) {
            flagAnomaly(ev, "[GPU_BAKE_CULL_DISABLED] NucleusGpuBaker.cullEnabled() is false during bake! "
                    + "bake_cull.comp failed to compile or was disabled, forcing CPU EBO fallback!");
        }
        if (ev.name.contains("[GPU_BAKE_EXPAND_FAILED]") || (ev.details != null && ev.details.contains("[GPU_BAKE_EXPAND_FAILED]"))) {
            flagAnomaly(ev, "[GPU_BAKE_EXPAND_FAILED] GPU Bake ensureSharedMainExpanded failed! VRAM budget exceeded or pool relayout rejected.");
        }
    }

    private static void flagAnomaly(EventRecord ev, String msg) {
        ev.anomalies.add(msg);
        if (currentReport != null) {
            currentReport.anomalies.add("Event #" + ev.index + " [" + ev.name + "]: " + msg);
        }
    }

    private static List<String> computeDiff(GlSnapshot a, GlSnapshot b) {
        List<String> d = new ArrayList<>();
        if (a == null) return d;
        diffField(d, "glProgram", a.glProgram, b.glProgram);
        diffField(d, "glVao", a.glVao, b.glVao);
        diffField(d, "glDrawFbo", a.glDrawFbo, b.glDrawFbo);
        diffField(d, "glBlendEnabled", a.glBlendEnabled, b.glBlendEnabled);
        diffField(d, "glBlendFunc", a.glBlendFunc, b.glBlendFunc);
        diffField(d, "glDepthTest", a.glDepthTest, b.glDepthTest);
        diffField(d, "glDepthMask", a.glDepthMask, b.glDepthMask);
        diffField(d, "glDepthRange", a.glDepthRange, b.glDepthRange);
        diffField(d, "glCullEnabled", a.glCullEnabled, b.glCullEnabled);
        diffField(d, "glColorMask", a.glColorMask, b.glColorMask);
        diffField(d, "mojangBlendModeLastApplied", a.mojangBlendModeLastApplied, b.mojangBlendModeLastApplied);
        diffField(d, "mojangLastProgramId", a.mojangLastProgramId, b.mojangLastProgramId);
        diffField(d, "rsShader", a.rsShader, b.rsShader);
        diffField(d, "irisPhase", a.irisPhase, b.irisPhase);
        diffField(d, "irisDepthColorLocked", a.irisDepthColorLocked, b.irisDepthColorLocked);
        diffField(d, "irisBlendLocked", a.irisBlendLocked, b.irisBlendLocked);
        diffField(d, "irisExtendedLastApplied", a.irisExtendedLastApplied, b.irisExtendedLastApplied);
        diffField(d, "TU0", a.glTextureUnits.get("TU0"), b.glTextureUnits.get("TU0"));
        diffField(d, "TU2", a.glTextureUnits.get("TU2"), b.glTextureUnits.get("TU2"));
        return d;
    }

    private static void diffField(List<String> out, String name, Object oldV, Object newV) {
        if (!Objects.equals(oldV, newV)) {
            out.add(name + ": " + oldV + " -> " + newV);
        }
    }

    // ─── Export to Markdown & JSON ─────────────────────────────────────────

    private static void exportReport(FrameReport report) {
        try {
            File dir = new File(Minecraft.getInstance().gameDirectory, "logs/nucleus_capture");
            if (!dir.exists()) dir.mkdirs();

            File jsonFile = new File(dir, "frame_" + report.timestamp + ".json");
            File mdFile = new File(dir, "frame_" + report.timestamp + ".md");

            try (FileWriter fw = new FileWriter(jsonFile, StandardCharsets.UTF_8)) {
                GSON.toJson(report, fw);
            }
            try (FileWriter fw = new FileWriter(mdFile, StandardCharsets.UTF_8)) {
                fw.write(buildMarkdownSummary(report));
            }

            MainRegistry.LOGGER.info("[Nucleus RenderDoc] Frame capture saved to {} and {} ({} events, {} anomalies)",
                    mdFile.getAbsolutePath(), jsonFile.getAbsolutePath(), report.events.size(), report.anomalies.size());

            Component link = Component.literal(mdFile.getName()).withStyle(Style.EMPTY
                    .withColor(ChatFormatting.UNDERLINE)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, mdFile.getAbsolutePath())));
            ChatFormatting statusColor = report.anomalies.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.GOLD;
            notifyPlayer(Component.literal("[Nucleus RenderDoc] Frame captured (events: "
                    + report.events.size() + ", anomalies: " + report.anomalies.size() + "): ")
                    .withStyle(statusColor).append(link));
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[Nucleus RenderDoc] Failed to save frame capture", t);
        }
    }

    private static String buildMarkdownSummary(FrameReport r) {
        StringBuilder sb = new StringBuilder(16384);
        sb.append("# Nucleus Mini-RenderDoc Frame Capture\n\n");
        sb.append("- **Timestamp**: `").append(r.timestamp).append("`\n");
        sb.append("- **Trigger**: `").append(r.triggerReason).append("`\n");
        sb.append("- **Render Frame**: `").append(r.renderFrame).append("`\n");
        sb.append("- **GPU**: `").append(r.gpuRenderer).append("` (`").append(r.glVersion).append("`)\n");
        sb.append("- **Oculus/Iris Active**: `").append(r.irisActive).append("`\n");
        sb.append("- **Camera / Anchor**: `cam=").append(r.cameraPos).append("`, `anchor=").append(r.anchorOrigin).append("`\n\n");

        sb.append("## 1. Detected Anomalies (").append(r.anomalies.size()).append(")\n");
        if (r.anomalies.isEmpty()) {
            sb.append("_No state desynchronizations or geometry anomalies detected in this frame._\n\n");
        } else {
            for (String a : r.anomalies) {
                sb.append("- **").append(a).append("**\n");
            }
            sb.append("\n");
        }

        sb.append("## 2. Frame Event Timeline (").append(r.events.size()).append(" events)\n\n");
        for (EventRecord ev : r.events) {
            GlSnapshot g = ev.gl;
            sb.append("### #").append(ev.index).append(" `").append(ev.name).append("`");
            if (ev.details != null && !ev.details.isEmpty()) {
                sb.append(" — ").append(ev.details);
            }
            sb.append("\n");

            if (!ev.anomalies.isEmpty()) {
                for (String a : ev.anomalies) {
                    sb.append("> **ANOMALY**: ").append(a).append("\n");
                }
            }
            if (!ev.stateChanges.isEmpty()) {
                sb.append("- **Diff vs prev**: `").append(String.join("` | `", ev.stateChanges)).append("`\n");
            }
            sb.append("- **GL State**: `prog=").append(g.glProgram)
              .append("` `vao=").append(g.glVao).append(g.glAttrib1Enabled != null ? (g.glAttrib1Enabled ? "(col1:on)" : "(col1:OFF!)") : "")
              .append("` `").append(g.glFboAttachments)
              .append("` `blend=").append(g.glBlendEnabled).append(" (").append(g.glBlendFunc).append(")`")
              .append(" `depth=").append(g.glDepthTest).append("/mask=").append(g.glDepthMask).append("/").append(g.glDepthFunc)
              .append("/range=").append(g.glDepthRange).append("` `cull=").append(g.glCullEnabled)
              .append("` `colorMask=").append(g.glColorMask).append("`\n");

            sb.append("- **Caches**: `RS.shader=").append(g.rsShader)
              .append("` `lastProgId=").append(g.mojangLastProgramId)
              .append("` `BlendMode.lastApplied=").append(g.mojangBlendModeLastApplied)
              .append("` `GSM.blend=").append(g.gsmBlendEnabled)
              .append("` `GSM.depthMask=").append(g.gsmDepthMask).append("`\n");

            if (r.irisActive) {
                sb.append("- **Oculus**: `phase=").append(g.irisPhase)
                  .append("` `shadow=").append(g.irisShadowPass)
                  .append("` `afterTrans=").append(g.irisAfterTranslucent)
                  .append("` `depthColorLocked=").append(g.irisDepthColorLocked)
                  .append("` `blendLocked=").append(g.irisBlendLocked)
                  .append("` `Ext.lastApplied=").append(g.irisExtendedLastApplied).append("`\n");
            }
            sb.append("- **Textures**: `TU0=").append(g.glTextureUnits.get("TU0"))
              .append("` `TU2=").append(g.glTextureUnits.get("TU2"))
              .append("` `TU4=").append(g.glTextureUnits.get("TU4"))
              .append("` `TU10=").append(g.glTextureUnits.get("TU10")).append("`\n");
            sb.append("- **Matrices**: `").append(g.rsModelViewSummary).append("` | `").append(g.rsProjSummary).append("`\n");

            if (ev.extra != null && !ev.extra.isEmpty()) {
                sb.append("- **Payload**: `").append(GSON.toJson(ev.extra)).append("`\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ─── Reflection & Formatting Helpers ───────────────────────────────────

    private static synchronized void ensureReflection() {
        if (reflInitialized) return;
        reflInitialized = true;
        try {
            for (Field f : BlendMode.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == BlendMode.class) {
                    fBlendLastApplied = f;
                } else if (f.getType() == boolean.class && ("opaque".equals(f.getName()) || "f_85519_".equals(f.getName()))) {
                    fBlendOpaque = f;
                } else if (f.getType() == int.class) {
                    String n = f.getName();
                    if ("srcColorFactor".equals(n) || "f_85515_".equals(n)) fBlendSrcColor = f;
                    else if ("dstColorFactor".equals(n) || "f_85516_".equals(n)) fBlendDstColor = f;
                    else if ("srcAlphaFactor".equals(n) || "f_85517_".equals(n)) fBlendSrcAlpha = f;
                    else if ("dstAlphaFactor".equals(n) || "f_85518_".equals(n)) fBlendDstAlpha = f;
                }
            }
            for (Field f : ShaderInstance.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    f.setAccessible(true);
                    fShaderLastProgramId = f;
                    break;
                }
            }
            for (Field f : GlStateManager.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                String typeName = f.getType().getSimpleName();
                if (typeName.contains("BlendState")) {
                    fGsmBlend = f;
                    Field[] bfs = f.getType().getDeclaredFields();
                    List<Field> intFields = new ArrayList<>();
                    for (Field bf : bfs) {
                        bf.setAccessible(true);
                        if (bf.getType().getSimpleName().contains("BooleanState")) fGsmBlendEnabled = bf;
                        else if (bf.getType() == int.class) intFields.add(bf);
                    }
                    if (intFields.size() >= 4) {
                        fGsmBlendSrcRgb = intFields.get(0);
                        fGsmBlendDstRgb = intFields.get(1);
                        fGsmBlendSrcA = intFields.get(2);
                        fGsmBlendDstA = intFields.get(3);
                    }
                } else if (typeName.contains("DepthState")) {
                    fGsmDepth = f;
                    for (Field df : f.getType().getDeclaredFields()) {
                        df.setAccessible(true);
                        if (df.getType().getSimpleName().contains("BooleanState")) fGsmDepthEnabled = df;
                        else if (df.getType() == boolean.class) fGsmDepthMask = df;
                        else if (df.getType() == int.class) fGsmDepthFunc = df;
                    }
                } else if (typeName.contains("CullState")) {
                    fGsmCull = f;
                    for (Field cf : f.getType().getDeclaredFields()) {
                        cf.setAccessible(true);
                        if (cf.getType().getSimpleName().contains("BooleanState")) fGsmCullEnabled = cf;
                    }
                } else if (typeName.contains("ColorMask")) {
                    fGsmColorMask = f;
                    List<Field> bools = new ArrayList<>();
                    for (Field cmf : f.getType().getDeclaredFields()) {
                        cmf.setAccessible(true);
                        if (cmf.getType() == boolean.class) bools.add(cmf);
                    }
                    if (bools.size() >= 4) {
                        fGsmCmR = bools.get(0); fGsmCmG = bools.get(1);
                        fGsmCmB = bools.get(2); fGsmCmA = bools.get(3);
                    }
                } else if (f.getType().isArray() && f.getType().getComponentType().getSimpleName().contains("TextureState")) {
                    fGsmTextures = f;
                    for (Field tf : f.getType().getComponentType().getDeclaredFields()) {
                        tf.setAccessible(true);
                        if (tf.getType() == int.class) fGsmTexBinding = tf;
                    }
                }
            }
            if (IrisReflectionBridge.isIrisPresent()) {
                try {
                    Class<?> ext = IrisReflectionBridge.findIrisClass("pipeline.programs.ExtendedShader");
                    fIrisExtLastApplied = ext.getDeclaredField("lastApplied");
                    fIrisExtLastApplied.setAccessible(true);
                } catch (Throwable ignored) {}
            }
            for (Field f : net.minecraft.client.renderer.texture.TextureManager.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    fTextureManagerByPath = f;
                    break;
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean getSubBoolean(Object holder, Field booleanStateField) throws Exception {
        Object bs = booleanStateField.get(holder);
        for (Field f : bs.getClass().getDeclaredFields()) {
            if (f.getType() == boolean.class) {
                f.setAccessible(true);
                return f.getBoolean(bs);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, String> buildTextureReverseMap() {
        Map<Integer, String> rev = new HashMap<>();
        try {
            if (fTextureManagerByPath != null) {
                Map<ResourceLocation, AbstractTexture> map = (Map<ResourceLocation, AbstractTexture>)
                        fTextureManagerByPath.get(Minecraft.getInstance().getTextureManager());
                if (map != null) {
                    for (Map.Entry<ResourceLocation, AbstractTexture> e : map.entrySet()) {
                        if (e.getValue() != null) {
                            rev.put(e.getValue().getId(), e.getKey().toString());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return rev;
    }

    private static String summarizeMatrix(String label, Matrix4f m) {
        if (m == null) return label + "=null";
        float det = m.determinant();
        boolean finite = Float.isFinite(det) && Float.isFinite(m.m30()) && Float.isFinite(m.m31()) && Float.isFinite(m.m32());
        return String.format(Locale.ROOT, "%s[t=(%.2f,%.2f,%.2f) det=%.3f finite=%b]",
                label, m.m30(), m.m31(), m.m32(), det, finite);
    }

    private static String factorName(int factor) {
        return switch (factor) {
            case 0 -> "ZERO";
            case 1 -> "ONE";
            case GL11.GL_SRC_COLOR -> "SRC_COLOR";
            case GL11.GL_ONE_MINUS_SRC_COLOR -> "ONE_MINUS_SRC_COLOR";
            case GL11.GL_SRC_ALPHA -> "SRC_ALPHA";
            case GL11.GL_ONE_MINUS_SRC_ALPHA -> "ONE_MINUS_SRC_ALPHA";
            case GL11.GL_DST_ALPHA -> "DST_ALPHA";
            case GL11.GL_ONE_MINUS_DST_ALPHA -> "ONE_MINUS_DST_ALPHA";
            case GL11.GL_DST_COLOR -> "DST_COLOR";
            case GL11.GL_ONE_MINUS_DST_COLOR -> "ONE_MINUS_DST_COLOR";
            default -> "0x" + Integer.toHexString(factor);
        };
    }

    private static String funcName(int func) {
        return switch (func) {
            case GL11.GL_NEVER -> "NEVER";
            case GL11.GL_LESS -> "LESS";
            case GL11.GL_EQUAL -> "EQUAL";
            case GL11.GL_LEQUAL -> "LEQUAL";
            case GL11.GL_GREATER -> "GREATER";
            case GL11.GL_NOTEQUAL -> "NOTEQUAL";
            case GL11.GL_GEQUAL -> "GEQUAL";
            case GL11.GL_ALWAYS -> "ALWAYS";
            default -> "0x" + Integer.toHexString(func);
        };
    }

    private static void notifyPlayer(Component msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.displayClientMessage(msg, false);
        }
    }

    // ─── Data Structures for JSON Serialization ────────────────────────────

    public static final class FrameReport {
        public String timestamp;
        public String triggerReason;
        public long renderFrame;
        public String gpuRenderer;
        public String glVersion;
        public boolean irisActive;
        public String cameraPos;
        public String anchorOrigin;
        public final List<String> anomalies = new ArrayList<>();
        public final List<EventRecord> events = new ArrayList<>();
    }

    public static final class EventRecord {
        public int index;
        public String name;
        public String details;
        public final List<String> anomalies = new ArrayList<>();
        public List<String> stateChanges;
        public GlSnapshot gl;
        public Map<String, Object> extra;
    }

    public static final class GlSnapshot {
        public int glProgram;
        public boolean glProgramValid;
        public int glVao;
        public Boolean glAttrib1Enabled;
        public int glArrayBuffer;
        public int glElementArrayBuffer;
        public int glDrawIndirectBuffer;
        public int glDrawFbo;
        public int glReadFbo;
        public String glFboAttachments;
        public boolean glBlendEnabled;
        public String glBlendFunc;
        public boolean glDepthTest;
        public boolean glDepthMask;
        public String glDepthFunc;
        public String glDepthRange;
        public boolean glCullEnabled;
        public String glColorMask;
        public String glViewport;
        public int glActiveTextureUnit;
        public final Map<String, String> glTextureUnits = new LinkedHashMap<>();

        public String rsShader;
        public int mojangLastProgramId;
        public String mojangBlendModeLastApplied;
        public Boolean gsmBlendEnabled;
        public String gsmBlendFunc;
        public Boolean gsmDepthTest;
        public Boolean gsmDepthMask;
        public String gsmDepthFunc;
        public Boolean gsmCullEnabled;
        public String gsmColorMask;
        public final Map<String, Integer> gsmTextureUnits = new LinkedHashMap<>();

        public boolean irisShadowPass;
        public String irisPhase;
        public boolean irisAfterTranslucent;
        public Boolean irisDepthColorLocked;
        public Boolean irisBlendLocked;
        public String irisExtendedLastApplied;

        public String gunGlState;
        public String rsModelViewSummary;
        public String rsProjSummary;
    }
}
