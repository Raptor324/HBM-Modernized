package com.hbm_m.client.render.shader;

import com.hbm_m.client.compat.dh.DhClientState;
import com.hbm_m.client.compat.dh.DhOcclusionGpu;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Screen-space nuclear explosion post-pass renderer.
 * Injected at TAIL finalizeLevelRendering (Iris/Oculus), rendering directly into
 * the Minecraft main render target with soft shadows, normal derivation and atmospheric glow.
 */
public final class RawScreenSpaceLight {
    private RawScreenSpaceLight() {}

    private static float intensity() { return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeLightIntensity; }
    private static float cinematics() { return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeCinematics; }

    private static final float NORMAL_SCALE = 1.0F;
    private static final boolean DEBUG_REPLACE = Boolean.getBoolean("hbm.screenSpaceLight.debugReplace");
    private static final boolean SOLID_ADD = Boolean.getBoolean("hbm.screenSpaceLight.solidAdd");

    private static int program = -1, fbo = -1;
    private static int uDepthTex0Loc = -1, uDhDepthTexLoc = -1, uDhReverseZLoc = -1, uDhPresentLoc = -1;
    private static int uDhNearLoc = -1, uDhFarLoc = -1, uDhFadeDistLoc = -1, uProjLoc = -1, uProjInvLoc = -1;
    private static int uModelViewLoc = -1, uModelViewInvLoc = -1, uCamPosLoc = -1, uSourcesLoc = -1;
    private static int uSourceCountLoc = -1, uSrcRadiusLoc = -1, uVanillaFarLoc = -1, uIntensityLoc = -1;
    private static int uHeatScaleLoc = -1, uResolutionLoc = -1, uNormalScaleLoc = -1, uDebugReplaceLoc = -1;
    private static int uSolidAddLoc = -1, uSrcShockLoc = -1, uSceneColorLoc = -1, uHasSceneLoc = -1, uCineLoc = -1;

    private static int sceneTex = -1, sceneTexW = -1, sceneTexH = -1;
    private static boolean sceneCopySupported = true;
    private static final int[] FBO_DRAW_BUFFERS = {GL30.GL_COLOR_ATTACHMENT0};

    private static volatile boolean latched = false;
    private static final int MAX_CONSECUTIVE_FAILURES = 60;
    private static int consecutiveFailures;

    private static final FloatBuffer projBuf = directFloats(16), projInvBuf = directFloats(16);
    private static final FloatBuffer modelViewBuf = directFloats(16), modelViewInvBuf = directFloats(16);
    private static float camX, camY, camZ; private static volatile boolean headSnapshotValid;
    private static final Matrix4f hbmProj = new Matrix4f(), hbmProjInv = new Matrix4f(), hbmMv = new Matrix4f(), hbmMvInv = new Matrix4f();

    private static FloatBuffer directFloats(int floats) {
        return ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
    private static void setMat(FloatBuffer buf, Matrix4f mat) {
        buf.clear(); mat.get(buf); buf.limit(16).position(0);
    }

    private record Inputs(int colorTexture, int width, int height, int depthTexture, int dhTexture, float dhNear, float dhFar, boolean dhReverseZ) {}

    /** Config + system-property gate, shared by the HEAD capture and the TAIL draw. */
    private static boolean isEnabled() {
        return !latched
                && com.hbm_m.config.ModClothConfig.get().graphicEffects.screenSpaceLightFill
                && !"false".equalsIgnoreCase(System.getProperty("hbm.screenSpaceLight", "true"));
    }

    public static void drawIfReady() {
        if (!isEnabled()) return;
        frameCounter++;
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) { noteOnce("pack-inactive", "external shader inactive - pass idle"); return; }
        try {
            Inputs inputs = resolveTailInputs();
            if (inputs == null) return;
            if (!ensureProgram() || !RawFullscreenQuad.ensureQuad()) { latch("program/quad init failed"); return; }
            int sourceCount = NukeLightTracker.collectAndTrackSources();
            if (sourceCount == 0) { noteEvery("no-sources", STATE_EVERY_FRAMES, "no live NukeTorex sources - pass idle"); return; }
            if (!fillFrameMatrices()) return;

            if (!loggedFirstDraw) {
                loggedFirstDraw = true;
                MainRegistry.LOGGER.info("RawScreenSpaceLight: FIRST LIGHT frame #{}: {}x{} colorTex={} depthTex={} | sources={} src0={} cam=({}, {}, {}) camDist={} radius0={} intensity={} | lift={} normalScale={} | DH: tex={} near={} far={} reverseZ={} present={} | headSnap={} debugReplace={}",
                        frameCounter, inputs.width(), inputs.height(), inputs.colorTexture(), inputs.depthTexture(), sourceCount, NukeLightTracker.src0String(), camX, camY, camZ, NukeLightTracker.camDist0(camX, camY, camZ), NukeLightTracker.getSrcRadius()[0], intensity(), NukeLightTracker.LIGHT_SOURCE_LIFT, NORMAL_SCALE, inputs.dhTexture(), inputs.dhNear(), inputs.dhFar(), inputs.dhReverseZ(), inputs.dhTexture() > 0, headSnapshotValid, DEBUG_REPLACE);
            } else if (frameCounter % STATE_EVERY_FRAMES == 0) {
                MainRegistry.LOGGER.info("RawScreenSpaceLight: state frame #{}: drawn={} skipped={} lastSkip={} sources={} src0={} cam=({}, {}, {}) camDist={} radius0={} snap={} dh=(tex={}, near={}, far={}, revZ={}, present={})",
                        frameCounter, framesDrawn, framesSkipped, lastSkipReason, sourceCount, NukeLightTracker.src0String(), camX, camY, camZ, NukeLightTracker.camDist0(camX, camY, camZ), NukeLightTracker.getSrcRadius()[0], headSnapshotValid, inputs.dhTexture(), inputs.dhNear(), inputs.dhFar(), inputs.dhReverseZ(), inputs.dhTexture() > 0);
            }
            draw(inputs);
            framesDrawn++; consecutiveFailures = 0;
        } catch (Throwable t) {
            consecutiveFailures++;
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                latched = true; MainRegistry.LOGGER.warn("RawScreenSpaceLight: " + consecutiveFailures + " consecutive frame failures - permanently disabled:", t);
            } else if (consecutiveFailures == 1) {
                MainRegistry.LOGGER.warn("RawScreenSpaceLight: frame failed - retrying (up to " + MAX_CONSECUTIVE_FAILURES + " in a row):", t);
            } else if (consecutiveFailures % 10 == 0) {
                MainRegistry.LOGGER.warn("RawScreenSpaceLight: {} frames failed in a row (last: {})", consecutiveFailures, t.toString());
            }
        }
    }

    private static void latch(String reason) { latched = true; MainRegistry.LOGGER.warn("RawScreenSpaceLight: permanently disabled after failure: {}", reason); }

    private static final Map<String, Boolean> notedOnce = new HashMap<>();
    private static long frameCounter, framesDrawn, framesSkipped;
    private static String lastSkipReason = "(none)";
    private static boolean loggedFirstDraw;
    private static final long ALWAYS_LOG_FRAMES = 10, SKIP_RESURFACE_FRAMES = 3600, STATE_EVERY_FRAMES = 1200;

    private static void noteOnce(String key, String message) { if (notedOnce.putIfAbsent(key, Boolean.TRUE) == null) MainRegistry.LOGGER.info("RawScreenSpaceLight [{}]: {}", key, message); }
    private static void noteEvery(String key, long everyN, String message) { if (frameCounter % everyN == 0) MainRegistry.LOGGER.info("RawScreenSpaceLight [{}]: {}", key, message); }

    private static Inputs skipFrame(String reason) {
        lastSkipReason = reason; framesSkipped++;
        if (frameCounter <= ALWAYS_LOG_FRAMES || frameCounter % SKIP_RESURFACE_FRAMES == 0 || notedOnce.putIfAbsent("skip:" + reason, Boolean.TRUE) == null) {
            MainRegistry.LOGGER.info("RawScreenSpaceLight: frame #{} skipped ({}), retry next frame", frameCounter, reason);
        }
        return null;
    }

    public static void captureFrameMatrices() {
        headSnapshotValid = false;
        if (!isEnabled()) return;
        try {
            Matrix4fc proj = IrisReflectionBridge.getGbufferProjection();
            Matrix4fc mv = IrisReflectionBridge.getGbufferModelView();
            if (proj == null || mv == null) return;
            storeFrameMatrices(proj, mv);
            headSnapshotValid = true;
            noteOnce("head-snap-ok", "HEAD matrix snapshot captured - TAIL draws with pre-composite matrices");
        } catch (Throwable t) {
            noteOnce("head-capture-failed", "HEAD matrix capture failed (" + t + ") - TAIL will resolve its own");
        }
    }

    /** Fills the hbm* matrices, their FloatBuffers and the camera position from the pack gbuffer matrices. */
    private static void storeFrameMatrices(Matrix4fc proj, Matrix4fc mv) {
        hbmProj.set(proj); setMat(projBuf, hbmProj);
        hbmProjInv.set(proj).invert(); setMat(projInvBuf, hbmProjInv);
        hbmMv.set(mv); setMat(modelViewBuf, hbmMv);
        hbmMvInv.set(mv).invert(); setMat(modelViewInvBuf, hbmMvInv);
        net.minecraft.world.phys.Vec3 cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        camX = (float) cam.x; camY = (float) cam.y; camZ = (float) cam.z;
    }

    private static Inputs resolveTailInputs() throws Throwable {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        int color = main.getColorTextureId(), depth = main.getDepthTextureId(), width = main.width, height = main.height;
        if (color <= 0 || depth <= 0 || width <= 0 || height <= 0) return skipFrame("main RT invalid: color=" + color + " depth=" + depth + " " + width + "x" + height);
        int dhTex = -1; float dhNear = 0.05F, dhFar = 4096.0F; boolean dhReverseZ = false;
        if (DhClientState.isActive()) {
            int candidate = DhOcclusionGpu.getDhActiveDepthTextureId();
            if (candidate > 0) {
                dhTex = candidate; dhNear = DhClientState.dhNear(); dhFar = DhClientState.dhFar(); dhReverseZ = DhClientState.dhReverseZ();
                if (!(dhNear > 0.0F) || !(dhFar > dhNear)) { dhTex = -1; noteOnce("dh-params-invalid", "DH clip params invalid: near=" + dhNear + " far=" + dhFar + " - LOD layer OFF"); }
            } else { noteOnce("dh-tex-missing", "DhClientState active but DhOcclusionGpu returned texture id " + candidate); }
        } else { noteEvery("dh-inactive", STATE_EVERY_FRAMES, "DhClientState.isActive()==false - LOD layer OFF"); }
        return new Inputs(color, width, height, depth, dhTex, dhNear, dhFar, dhReverseZ);
    }

    private static boolean fillFrameMatrices() throws Throwable {
        // The HEAD snapshot (if captured) already left the FloatBuffers filled.
        if (headSnapshotValid) return true;
        Matrix4fc proj = IrisReflectionBridge.getGbufferProjection();
        if (proj == null) { skipFrame("gbuffer projection null"); return false; }
        Matrix4fc mv = IrisReflectionBridge.getGbufferModelView();
        if (mv == null) { skipFrame("gbuffer modelView null"); return false; }
        storeFrameMatrices(proj, mv);
        return true;
    }

    private static void draw(Inputs in) {
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE), prevDrawFb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE), blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevBlendSrcRgb = GL11.glGetInteger(GL20.GL_BLEND_SRC_RGB), prevBlendDstRgb = GL11.glGetInteger(GL20.GL_BLEND_DST_RGB), prevBlendSrcAlpha = GL11.glGetInteger(GL20.GL_BLEND_SRC_ALPHA), prevBlendDstAlpha = GL11.glGetInteger(GL20.GL_BLEND_DST_ALPHA), prevBlendEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        boolean stencilWasEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST), scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), rasterizerDiscardWasEnabled = GL11.glIsEnabled(0x8C89), logicOpWasEnabled = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP), sampleMaskWasEnabled = GL11.glIsEnabled(0x8E51);
        int logicOpMode = logicOpWasEnabled ? GL11.glGetInteger(GL11.GL_LOGIC_OP_MODE) : 0;
        int[] prevPolyMode = new int[2]; GL11.glGetIntegerv(0x0B40, prevPolyMode);
        boolean[] clipWasEnabled = new boolean[8]; for (int i = 0; i < clipWasEnabled.length; i++) clipWasEnabled[i] = GL11.glIsEnabled(0x3000 + i);

        int prevViewportX, prevViewportY, prevViewportW, prevViewportH;
        boolean[] prevColorMask = new boolean[4];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(16); GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            prevViewportX = viewport.get(0); prevViewportY = viewport.get(1); prevViewportW = viewport.get(2); prevViewportH = viewport.get(3);
            ByteBuffer mask = stack.malloc(4); GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            for (int i = 0; i < 4; i++) prevColorMask[i] = mask.get(i) != 0;
        }

        GlStateManager._activeTexture(GL13.GL_TEXTURE0); int prevUnit0Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13.GL_TEXTURE1); int prevUnit1Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13.GL_TEXTURE2); int prevUnit2Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        float cine = cinematics(); boolean sceneActive = false;
        if (sceneCopySupported && cine > 0.001F && !DEBUG_REPLACE && !SOLID_ADD) {
            sceneActive = copySceneTexture(in.colorTexture(), in.width(), in.height());
        }
        if (!ensureFbo(in.colorTexture())) { GlStateManager._activeTexture(prevActiveTex); return; }

        boolean depthBindingsActive = false;
        int[] savedFilters0 = new int[2], savedFilters1 = new int[2];
        try {
            GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (stencilWasEnabled) GL11.glDisable(GL11.GL_STENCIL_TEST);
            if (scissorWasEnabled) GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            if (DEBUG_REPLACE || sceneActive) {
                GL11.glDisable(GL11.GL_BLEND);
            } else {
                GL11.glEnable(GL11.GL_BLEND);
                GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
                GL20.glBlendEquation(GL20.GL_FUNC_ADD);
            }

            if (rasterizerDiscardWasEnabled) GL11.glDisable(0x8C89);
            if (logicOpWasEnabled) GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
            if (sampleMaskWasEnabled) GL11.glDisable(0x8E51);
            GL11.glPolygonMode(0x408, 0x1B02);
            for (int i = 0; i < clipWasEnabled.length; i++) if (clipWasEnabled[i]) GL11.glDisable(0x3000 + i);

            GL11.glViewport(0, 0, in.width(), in.height());
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo); GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, in.colorTexture(), 0); GL20.glDrawBuffers(FBO_DRAW_BUFFERS);

            GlStateManager._glUseProgram(program);
            GL20.glUniform1i(uDepthTex0Loc, 0); GL20.glUniform1i(uDhDepthTexLoc, 1); GL20.glUniform1i(uSceneColorLoc, 2);
            GL20.glUniformMatrix4fv(uProjLoc, false, projBuf); GL20.glUniformMatrix4fv(uProjInvLoc, false, projInvBuf);
            GL20.glUniformMatrix4fv(uModelViewLoc, false, modelViewBuf); GL20.glUniformMatrix4fv(uModelViewInvLoc, false, modelViewInvBuf);
            GL20.glUniform3f(uCamPosLoc, camX, camY, camZ); GL20.glUniform2f(uResolutionLoc, in.width(), in.height());
            GL20.glUniform4fv(uSourcesLoc, NukeLightTracker.getSources()); GL20.glUniform1i(uSourceCountLoc, NukeLightTracker.getSourceCount());
            GL20.glUniform1fv(uSrcRadiusLoc, NukeLightTracker.getSrcRadius()); GL20.glUniform2fv(uSrcShockLoc, NukeLightTracker.getSrcShock());
            GL20.glUniform1f(uIntensityLoc, intensity()); GL20.glUniform1f(uHeatScaleLoc, NukeLightTracker.HEAT_SCALE);
            GL20.glUniform1f(uNormalScaleLoc, NORMAL_SCALE); GL20.glUniform1f(uCineLoc, cine);
            GL20.glUniform1f(uDebugReplaceLoc, DEBUG_REPLACE ? 1.0F : 0.0F); GL20.glUniform1f(uSolidAddLoc, SOLID_ADD ? 1.0F : 0.0F);
            GL20.glUniform1f(uHasSceneLoc, sceneActive ? 1.0F : 0.0F); GL20.glUniform1f(uDhReverseZLoc, in.dhReverseZ() ? 1.0F : 0.0F);
            GL20.glUniform1f(uDhPresentLoc, in.dhTexture() > 0 ? 1.0F : 0.0F); GL20.glUniform1f(uDhNearLoc, in.dhTexture() > 0 ? in.dhNear() : 1.0F);
            GL20.glUniform1f(uDhFarLoc, in.dhTexture() > 0 ? in.dhFar() : 2.0F); GL20.glUniform1f(uDhFadeDistLoc, in.dhTexture() > 0 ? DhOcclusionGpu.ditherFadeMaskDistance() : 0.0F);
            GL20.glUniform1f(uVanillaFarLoc, DhOcclusionGpu.vanillaFarPlane());

            bindDepthTexture(GL13.GL_TEXTURE0, in.depthTexture(), savedFilters0);
            depthBindingsActive = true;
            if (in.dhTexture() > 0) bindDepthTexture(GL13.GL_TEXTURE1, in.dhTexture(), savedFilters1);
            if (sceneActive) { GlStateManager._activeTexture(GL13.GL_TEXTURE2); GlStateManager._bindTexture(sceneTex); }

            RawFullscreenQuad.draw();
            int drawErr = GL11.glGetError();
            if (drawErr != 0) MainRegistry.LOGGER.warn("RawScreenSpaceLight: glDrawArrays set GL error 0x{}", Integer.toHexString(drawErr));

            GL11.glViewport(prevViewportX, prevViewportY, prevViewportW, prevViewportH);
            GL11.glColorMask(prevColorMask[0], prevColorMask[1], prevColorMask[2], prevColorMask[3]);
            if (blendWasEnabled) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha); GL20.glBlendEquation(prevBlendEquation);
            if (scissorWasEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST); if (stencilWasEnabled) GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glDepthFunc(prevDepthFunc); if (depthTestWasEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(prevDepthMask); if (cullWasEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
        } finally {
            if (rasterizerDiscardWasEnabled) GL11.glEnable(0x8C89);
            if (logicOpWasEnabled) { GL11.glEnable(GL11.GL_COLOR_LOGIC_OP); GL11.glLogicOp(logicOpMode); }
            if (sampleMaskWasEnabled) GL11.glEnable(0x8E51);
            if (prevPolyMode[0] != 0x1B02) GL11.glPolygonMode(0x408, prevPolyMode[0]);
            for (int i = 0; i < clipWasEnabled.length; i++) if (clipWasEnabled[i]) GL11.glEnable(0x3000 + i);
            if (depthBindingsActive) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE1);
                if (in.dhTexture() > 0) { restoreDepthFilters(in.dhTexture(), savedFilters1); GlStateManager._bindTexture(prevUnit1Tex); }
                GlStateManager._activeTexture(GL13.GL_TEXTURE0); restoreDepthFilters(in.depthTexture(), savedFilters0); GlStateManager._bindTexture(prevUnit0Tex);
                GlStateManager._activeTexture(GL13.GL_TEXTURE2); GlStateManager._bindTexture(prevUnit2Tex);
            }
            GlStateManager._glUseProgram(prevProgram); GlStateManager._glBindVertexArray(prevVao); GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFb); GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb); GlStateManager._activeTexture(prevActiveTex);
            ShaderBindResync.resetBlendModeToOpaque();
            if (blendWasEnabled) {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                GlStateManager._blendFuncSeparate(0, 0, 0, 0);
                GlStateManager._blendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);
            } else {
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            }
        }
    }

    private static boolean ensureFbo(int colorTexture) {
        if (fbo != -1 && GL30.glIsFramebuffer(fbo)) return true;
        if (fbo != -1) { GL30.glDeleteFramebuffers(fbo); fbo = -1; }
        fbo = GL30.glGenFramebuffers();
        int prevDrawFb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTexture, 0);
        GL20.glDrawBuffers(FBO_DRAW_BUFFERS);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFb); GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) { GL30.glDeleteFramebuffers(fbo); fbo = -1; return false; }
        return true;
    }

    private static void bindDepthTexture(int unit, int textureId, int[] savedOut) {
        GlStateManager._activeTexture(unit); GlStateManager._bindTexture(textureId);
        savedOut[0] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        savedOut[1] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST); GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    private static void restoreDepthFilters(int textureId, int[] savedFilters) {
        if (textureId <= 0) return;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, savedFilters[0]); GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, savedFilters[1]);
    }

    private static boolean copySceneTexture(int srcTexture, int w, int h) {
        if (sceneTex != -1 && (sceneTexW != w || sceneTexH != h)) { GL11.glDeleteTextures(sceneTex); sceneTex = -1; }
        if (sceneTex == -1) {
            sceneTex = GL11.glGenTextures(); GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR); GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 0x812D); GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 0x812D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            sceneTexW = w; sceneTexH = h;
        }
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* drain stale errors */ }
        GL43.glCopyImageSubData(srcTexture, GL11.GL_TEXTURE_2D, 0, 0, 0, 0, sceneTex, GL11.GL_TEXTURE_2D, 0, 0, 0, 0, w, h, 1);
        boolean ok = GL11.glGetError() == GL11.GL_NO_ERROR;
        if (!ok) {
            sceneCopySupported = false;
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: scene color copy failed (GL error) - cinematics disabled");
        }
        return ok;
    }

    private static boolean ensureProgram() {
        if (latched) return false;
        if (program != -1 && GL20.glIsProgram(program)) return true;
        try {
            String fshSource = GlShaderUtil.loadResource(RawScreenSpaceLight.class, "/assets/hbm_m/shaders/core/screen_space_light.fsh");
            int newProgram = GlShaderUtil.createRasterProgram(RawFullscreenQuad.FULLSCREEN_VSH, fshSource, java.util.Map.of(0, "Position"), "RawScreenSpaceLight");
            if (newProgram == 0) {
                latched = true;
                return false;
            }
            uDepthTex0Loc = GL20.glGetUniformLocation(newProgram, "uDepthTex0"); uDhDepthTexLoc = GL20.glGetUniformLocation(newProgram, "uDhDepthTex");
            uDhReverseZLoc = GL20.glGetUniformLocation(newProgram, "uDhReverseZ"); uDhPresentLoc = GL20.glGetUniformLocation(newProgram, "uDhPresent");
            uDhNearLoc = GL20.glGetUniformLocation(newProgram, "uDhNear"); uDhFarLoc = GL20.glGetUniformLocation(newProgram, "uDhFar");
            uDhFadeDistLoc = GL20.glGetUniformLocation(newProgram, "uDhFadeDist"); uProjLoc = GL20.glGetUniformLocation(newProgram, "uProj");
            uProjInvLoc = GL20.glGetUniformLocation(newProgram, "uProjInv"); uModelViewLoc = GL20.glGetUniformLocation(newProgram, "uModelView");
            uModelViewInvLoc = GL20.glGetUniformLocation(newProgram, "uModelViewInv"); uCamPosLoc = GL20.glGetUniformLocation(newProgram, "uCamPos");
            uSourcesLoc = GL20.glGetUniformLocation(newProgram, "uSources[0]"); uSourceCountLoc = GL20.glGetUniformLocation(newProgram, "uSourceCount");
            uSrcRadiusLoc = GL20.glGetUniformLocation(newProgram, "uSrcRadius[0]"); uVanillaFarLoc = GL20.glGetUniformLocation(newProgram, "uVanillaFar");
            uIntensityLoc = GL20.glGetUniformLocation(newProgram, "uIntensity"); uHeatScaleLoc = GL20.glGetUniformLocation(newProgram, "uHeatScale");
            uResolutionLoc = GL20.glGetUniformLocation(newProgram, "uResolution"); uNormalScaleLoc = GL20.glGetUniformLocation(newProgram, "uNormalScale");
            uDebugReplaceLoc = GL20.glGetUniformLocation(newProgram, "uDebugReplace"); uSolidAddLoc = GL20.glGetUniformLocation(newProgram, "uSolidAdd");
            uSrcShockLoc = GL20.glGetUniformLocation(newProgram, "uSrcShock[0]"); uSceneColorLoc = GL20.glGetUniformLocation(newProgram, "uSceneColor");
            uHasSceneLoc = GL20.glGetUniformLocation(newProgram, "uHasScene"); uCineLoc = GL20.glGetUniformLocation(newProgram, "uCine");
            program = newProgram;
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: program init failed: {}", t.toString());
            latched = true;
            return false;
        }
    }
}
