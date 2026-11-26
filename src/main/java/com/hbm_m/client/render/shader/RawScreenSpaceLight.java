package com.hbm_m.client.render.shader;

import com.hbm_m.client.compat.dh.DhClientState;
import com.hbm_m.client.compat.dh.DhOcclusionGpu;
import com.hbm_m.client.render.GlVaoSafety;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
 * the Minecraft main render target with soft shadows, normal derivation, 3D shockwave,
 * obstacle-wrapping volumetric godrays, and atmospheric glow.
 */
public final class RawScreenSpaceLight {
    private RawScreenSpaceLight() {}

    private static float intensity() { return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeLightIntensity; }
    private static float cinematics() { return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeCinematics; }
    private static float haloStrength() { return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeHaloStrength; }

    private static final float NORMAL_SCALE = 1.0F;
    private static final boolean DEBUG_REPLACE = Boolean.getBoolean("hbm.screenSpaceLight.debugReplace");
    private static final boolean SOLID_ADD = Boolean.getBoolean("hbm.screenSpaceLight.solidAdd");

    private static int program = -1, fbo = -1;
    private static int nearestSampler = -1;
    private static int lastBoundColorTex = -1;
    private static int uDepthTex0Loc = -1, uDhDepthTexLoc = -1, uDhReverseZLoc = -1, uDhPresentLoc = -1;
    private static int uDhNearLoc = -1, uDhFarLoc = -1, uDhFadeDistLoc = -1, uProjLoc = -1;
    private static int uModelViewLoc = -1, uModelViewInvLoc = -1, uCamPosLoc = -1, uSourcesLoc = -1;
    private static int uSourceCountLoc = -1, uSrcRadiusLoc = -1, uVanillaFarLoc = -1, uIntensityLoc = -1;
    private static int uHaloStrengthLoc = -1;
    private static int uHeatScaleLoc = -1, uResolutionLoc = -1, uNormalScaleLoc = -1, uDebugReplaceLoc = -1;
    private static int uSolidAddLoc = -1, uSrcShockLoc = -1, uSceneColorLoc = -1, uHasSceneLoc = -1, uCineLoc = -1;
    private static int uSrcExtraLoc = -1, uTimeLoc = -1;

    private static int sceneTex = -1, sceneTexW = -1, sceneTexH = -1, sceneTexInternalFormat = -1;
    private static boolean sceneCopySupported = true;
    private static final int[] FBO_DRAW_BUFFERS = {GL30.GL_COLOR_ATTACHMENT0};

    private static volatile boolean latched = false;
    private static final int MAX_CONSECUTIVE_FAILURES = 60;
    private static int consecutiveFailures;

    private static final FloatBuffer projBuf = directFloats(16);
    private static final FloatBuffer modelViewBuf = directFloats(16), modelViewInvBuf = directFloats(16);
    private static float camX, camY, camZ; private static volatile boolean headSnapshotValid;
    private static final Matrix4f hbmProj = new Matrix4f(), hbmMv = new Matrix4f(), hbmMvInv = new Matrix4f();

    private static final Matrix4f vanillaCapturedProj = new Matrix4f();
    private static final Matrix4f vanillaCapturedMv = new Matrix4f();
    private static volatile boolean vanillaMatricesValid = false;

    private static int cleanDepthTex = -1, cleanDepthW = -1, cleanDepthH = -1, cleanDepthInternalFormat = -1;
    private static boolean cleanDepthSnapshotValid = false;
    private static boolean depthCopySupported = true;
    private static boolean dhBlittedThisFrame = false;
    private static int uDhBlittedInDepth0Loc = -1;

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
        long currentRenderFrame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (currentRenderFrame > 0 && lastDrawnRenderFrame == currentRenderFrame) {
            return;
        }
        frameCounter++;
        try {
            Inputs inputs = resolveTailInputs();
            if (inputs == null) return;
            if (!ensureProgram() || !RawFullscreenQuad.ensureQuad()) { latch("program/quad init failed"); return; }
            int sourceCount = NukeLightTracker.collectAndTrackSources();
            if (sourceCount == 0) { noteOnce("no-sources", "no live NukeTorex sources - pass idle"); return; }
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
            lastDrawnRenderFrame = currentRenderFrame;
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
        } finally {
            cleanDepthSnapshotValid = false;
            dhBlittedThisFrame = false;
            headSnapshotValid = false;
            vanillaMatricesValid = false;
        }
    }

    private static void latch(String reason) { latched = true; MainRegistry.LOGGER.warn("RawScreenSpaceLight: permanently disabled after failure: {}", reason); }

    private static final Map<String, Boolean> notedOnce = new HashMap<>();
    private static long frameCounter, framesDrawn, framesSkipped;
    // Render-frame stamp of the last successful draw; guards against a double draw
    // when drawIfReady() is invoked from more than one hook in the same frame.
    private static long lastDrawnRenderFrame = -1L;
    private static String lastSkipReason = "(none)";
    private static boolean loggedFirstDraw;
    private static final long ALWAYS_LOG_FRAMES = 10, SKIP_RESURFACE_FRAMES = 3600, STATE_EVERY_FRAMES = 1200;

    private static void noteOnce(String key, String message) { if (notedOnce.putIfAbsent(key, Boolean.TRUE) == null) MainRegistry.LOGGER.info("RawScreenSpaceLight [{}]: {}", key, message); }

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

    /**
     * Captures the clean vanilla projection and modelView matrices for the current frame
     * when no external Iris/Oculus shaderpack is active.
     */
    public static void captureVanillaFrameMatrices(Matrix4fc proj, Matrix4fc mv) {
        if (!isEnabled() || proj == null || mv == null) {
            return;
        }
        vanillaCapturedProj.set(proj);
        vanillaCapturedMv.set(mv);
        vanillaMatricesValid = true;
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            storeFrameMatrices(vanillaCapturedProj, vanillaCapturedMv);
            headSnapshotValid = true;
        }
    }

    /** Fills the hbm* matrices, their FloatBuffers and the camera position from the pack gbuffer matrices. */
    private static void storeFrameMatrices(Matrix4fc proj, Matrix4fc mv) {
        hbmProj.set(proj); setMat(projBuf, hbmProj);
        hbmMv.set(mv); setMat(modelViewBuf, hbmMv);
        hbmMvInv.set(mv).invert(); setMat(modelViewInvBuf, hbmMvInv);
        net.minecraft.world.phys.Vec3 cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        camX = (float) cam.x; camY = (float) cam.y; camZ = (float) cam.z;
    }

    /**
     * Called from DhDepthCopy.copyToMain() right BEFORE it writes extended-projection
     * DH depth into mainRenderTarget's depth buffer.
     * If nuclear sources are active, copies the clean vanilla depth texture so
     * RawScreenSpaceLight can read uncorrupted vanilla depth in uDepthTex0.
     */
    public static void onBeforeDhDepthBlit() {
        cleanDepthSnapshotValid = false;
        dhBlittedThisFrame = true;
        if (!isEnabled() || !depthCopySupported) return;
        if (NukeLightTracker.collectAndTrackSources() <= 0) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        if (main == null) return;
        int srcDepth = main.getDepthTextureId();
        int w = main.width, h = main.height;
        if (srcDepth <= 0 || w <= 0 || h <= 0) return;

        GLCapabilities caps = GL.getCapabilities();
        if (caps == null || caps.glCopyImageSubData == 0L) {
            depthCopySupported = false;
            return;
        }

        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, srcDepth);
            int internalFmt = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            if (internalFmt <= 0) internalFmt = GL30.GL_DEPTH_COMPONENT32F;

            if (cleanDepthTex != -1 && (cleanDepthW != w || cleanDepthH != h || cleanDepthInternalFormat != internalFmt)) {
                GL11.glDeleteTextures(cleanDepthTex);
                cleanDepthTex = -1;
            }
            if (cleanDepthTex == -1) {
                cleanDepthTex = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, cleanDepthTex);
                int format = (internalFmt == GL30.GL_DEPTH24_STENCIL8 || internalFmt == GL30.GL_DEPTH32F_STENCIL8)
                        ? GL30.GL_DEPTH_STENCIL : GL11.GL_DEPTH_COMPONENT;
                int type = (internalFmt == GL30.GL_DEPTH24_STENCIL8) ? GL30.GL_UNSIGNED_INT_24_8
                        : (internalFmt == GL30.GL_DEPTH32F_STENCIL8) ? GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV
                        : GL11.GL_FLOAT;
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFmt, w, h, 0, format, type, (ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                cleanDepthW = w;
                cleanDepthH = h;
                cleanDepthInternalFormat = internalFmt;
            }
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
            GlStateManager._activeTexture(prevActive);
        }

        while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* clear stale */ }
        GL43.glCopyImageSubData(srcDepth, GL11.GL_TEXTURE_2D, 0, 0, 0, 0,
                                cleanDepthTex, GL11.GL_TEXTURE_2D, 0, 0, 0, 0, w, h, 1);
        if (GL11.glGetError() == GL11.GL_NO_ERROR) {
            cleanDepthSnapshotValid = true;
        } else {
            depthCopySupported = false;
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: depth glCopyImageSubData failed - using shader-side DH blit filter");
        }
    }

    /**
     * Called from RawDhDepthCopy after a successful Iris-path blit: the bound
     * pipeline FB depth (which is what the main RT depth texture holds at TAIL)
     * now carries dh_depth_blit-encoded pixels (EXTENDED projection near=0.05 /
     * far=8e6), so the TAIL pass must run the isBlittedDhDepth guard on
     * uDepthTex0 and re-route those pixels to the DH layer instead of decoding
     * them with the pack's frame projection.
     */
    public static void markDhBlittedInDepth0() {
        dhBlittedThisFrame = true;
    }

    private static Inputs resolveTailInputs() throws Throwable {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        int color = main.getColorTextureId(), width = main.width, height = main.height;
        int depth = (cleanDepthSnapshotValid && cleanDepthTex > 0) ? cleanDepthTex : main.getDepthTextureId();
        if (color <= 0 || depth <= 0 || width <= 0 || height <= 0) return skipFrame("main RT invalid: color=" + color + " depth=" + depth + " " + width + "x" + height);
        int dhTex = -1; float dhNear = 0.05F, dhFar = 4096.0F; boolean dhReverseZ = false;
        if (DhClientState.isActive()) {
            int candidate = DhOcclusionGpu.getDhActiveDepthTextureId();
            if (candidate > 0) {
                dhTex = candidate; dhNear = DhClientState.dhNear(); dhFar = DhClientState.dhFar(); dhReverseZ = DhClientState.dhReverseZ();
                if (!(dhNear > 0.0F) || !(dhFar > dhNear)) { dhTex = -1; noteOnce("dh-params-invalid", "DH clip params invalid: near=" + dhNear + " far=" + dhFar + " - LOD layer OFF"); }
            } else { noteOnce("dh-tex-missing", "DhClientState active but DhOcclusionGpu returned texture id " + candidate); }
        } else { noteOnce("dh-inactive", "DhClientState.isActive()==false - LOD layer OFF"); }
        return new Inputs(color, width, height, depth, dhTex, dhNear, dhFar, dhReverseZ);
    }

    private static boolean fillFrameMatrices() throws Throwable {
        if (headSnapshotValid) return true;
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            Matrix4fc proj = IrisReflectionBridge.getGbufferProjection();
            Matrix4fc mv = IrisReflectionBridge.getGbufferModelView();
            if (proj != null && mv != null) {
                storeFrameMatrices(proj, mv);
                return true;
            }
        }
        if (vanillaMatricesValid) {
            storeFrameMatrices(vanillaCapturedProj, vanillaCapturedMv);
            return true;
        }
        Matrix4f rsProj = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
        // RenderSystem.getModelViewMatrix() is IDENTITY during world rendering on 1.20.1
        // (no camera pitch/yaw), which would pin the effect to a fixed screen corner.
        // FrameViewState keeps the real camera rotation instead.
        Matrix4f basisMv = com.hbm_m.client.render.FrameViewState.viewBasisRotation();
        if (rsProj != null && basisMv != null) {
            storeFrameMatrices(rsProj, basisMv);
            return true;
        }
        skipFrame("no valid frame matrices (neither Iris nor Vanilla)");
        return false;
    }

    private static void draw(Inputs in) {
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE), prevDrawFb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE), blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevBlendSrcRgb = GL11.glGetInteger(GL20.GL_BLEND_SRC_RGB), prevBlendDstRgb = GL11.glGetInteger(GL20.GL_BLEND_DST_RGB), prevBlendSrcAlpha = GL11.glGetInteger(GL20.GL_BLEND_SRC_ALPHA), prevBlendDstAlpha = GL11.glGetInteger(GL20.GL_BLEND_DST_ALPHA), prevBlendEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);

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
            sceneActive = copySceneTexture(in.colorTexture(), in.width(), in.height(), prevUnit0Tex);
        }
        if (!ensureFbo(in.colorTexture())) { GlStateManager._activeTexture(prevActiveTex); return; }

        boolean depthBindingsActive = false;
        try {
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, true);
            if (DEBUG_REPLACE || sceneActive) {
                RenderSystem.disableBlend();
            } else {
                RenderSystem.enableBlend();
                GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
                RenderSystem.blendEquation(GL20.GL_FUNC_ADD);
            }

            RenderSystem.viewport(0, 0, in.width(), in.height());
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            if (lastBoundColorTex != in.colorTexture()) {
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, in.colorTexture(), 0);
                GL20.glDrawBuffers(FBO_DRAW_BUFFERS);
                lastBoundColorTex = in.colorTexture();
            }

            GlStateManager._glUseProgram(program);
            GL20.glUniform1i(uDepthTex0Loc, 0); GL20.glUniform1i(uDhDepthTexLoc, 1); GL20.glUniform1i(uSceneColorLoc, 2);
            GL20.glUniformMatrix4fv(uProjLoc, false, projBuf);
            GL20.glUniformMatrix4fv(uModelViewLoc, false, modelViewBuf); GL20.glUniformMatrix4fv(uModelViewInvLoc, false, modelViewInvBuf);
            GL20.glUniform3f(uCamPosLoc, camX, camY, camZ);
            GL20.glUniform4fv(uSourcesLoc, NukeLightTracker.getSources());
            GL20.glUniform1iv(uSourceCountLoc, new int[]{NukeLightTracker.getSourceCount()});
            GL20.glUniform1fv(uSrcRadiusLoc, NukeLightTracker.getSrcRadius());
            GL20.glUniform2fv(uSrcShockLoc, NukeLightTracker.getSrcShock());
            GL20.glUniform4fv(uSrcExtraLoc, NukeLightTracker.getSrcExtra());
            GL20.glUniform1f(uTimeLoc, NukeLightTracker.getTimeSeconds());

            // Boundary of vanilla geometry in blocks, (RD+1)*16 (HEAD used
            // DhOcclusionGpu.vanillaFarPlane()) - one chunk LOOSER than
            // gameRenderer.getRenderDistance() (= RD*16), so boundary-chunk
            // pixels decoding marginally beyond the render distance are not
            // dropped from the vanilla geometry class.
            float vanillaFar = com.hbm_m.client.compat.dh.DhOcclusionGpu.vanillaFarPlane();
            GL20.glUniform1f(uVanillaFarLoc, vanillaFar);
            GL20.glUniform1f(uIntensityLoc, intensity());
            GL20.glUniform1f(uHaloStrengthLoc, haloStrength());
            GL20.glUniform1f(uHeatScaleLoc, NukeLightTracker.HEAT_SCALE);
            GL20.glUniform2f(uResolutionLoc, (float) in.width(), (float) in.height());
            GL20.glUniform1f(uNormalScaleLoc, NORMAL_SCALE);
            GL20.glUniform1i(uDebugReplaceLoc, DEBUG_REPLACE ? 1 : 0);
            GL20.glUniform1i(uSolidAddLoc, SOLID_ADD ? 1 : 0);
            GL20.glUniform1i(uHasSceneLoc, sceneActive ? 1 : 0);
            GL20.glUniform1f(uCineLoc, cine);
            GL20.glUniform1i(uDhBlittedInDepth0Loc, (dhBlittedThisFrame && !cleanDepthSnapshotValid) ? 1 : 0);

            if (in.dhTexture() > 0) {
                GL20.glUniform1i(uDhPresentLoc, 1); GL20.glUniform1i(uDhReverseZLoc, in.dhReverseZ() ? 1 : 0);
                GL20.glUniform1f(uDhNearLoc, in.dhNear()); GL20.glUniform1f(uDhFarLoc, in.dhFar());
                // Dither-fade ring of DH ("Fade Nearby DH LODs"): inside it the
                // DEPTH32F is stochastic noise - excluded from the DH layer
                // (normals/shadows/shell must not read it). 0 = not determined.
                GL20.glUniform1f(uDhFadeDistLoc, DhOcclusionGpu.ditherFadeMaskDistance());
            } else {
                GL20.glUniform1i(uDhPresentLoc, 0); GL20.glUniform1i(uDhReverseZLoc, 0);
                GL20.glUniform1f(uDhNearLoc, 0.05F); GL20.glUniform1f(uDhFarLoc, 4096.0F); GL20.glUniform1f(uDhFadeDistLoc, 0.0F);
            }

            int sampler = ensureNearestSampler();
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(in.depthTexture());
            GL33.glBindSampler(0, sampler);

            if (in.dhTexture() > 0) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE1);
                GlStateManager._bindTexture(in.dhTexture());
                GL33.glBindSampler(1, sampler);
            }

            if (sceneActive) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE2);
                GlStateManager._bindTexture(sceneTex);
            }
            depthBindingsActive = true;

            RawFullscreenQuad.render();
        } finally {
            if (depthBindingsActive) {
                GL33.glBindSampler(0, 0);
                if (in.dhTexture() > 0) {
                    GL33.glBindSampler(1, 0);
                }
            }
            GlStateManager._activeTexture(GL13.GL_TEXTURE0); GlStateManager._bindTexture(prevUnit0Tex);
            GlStateManager._activeTexture(GL13.GL_TEXTURE1); GlStateManager._bindTexture(prevUnit1Tex);
            GlStateManager._activeTexture(GL13.GL_TEXTURE2); GlStateManager._bindTexture(prevUnit2Tex);
            GlStateManager._activeTexture(prevActiveTex);

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFb);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb);
            RenderSystem.viewport(prevViewportX, prevViewportY, prevViewportW, prevViewportH);
            RenderSystem.colorMask(prevColorMask[0], prevColorMask[1], prevColorMask[2], prevColorMask[3]);

            if (cullWasEnabled) RenderSystem.enableCull();
            if (depthTestWasEnabled) RenderSystem.enableDepthTest();
            RenderSystem.depthMask(prevDepthMask); RenderSystem.depthFunc(prevDepthFunc);
            if (blendWasEnabled) {
                RenderSystem.enableBlend();
                GlStateManager._blendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);
                RenderSystem.blendEquation(prevBlendEquation);
            } else {
                RenderSystem.disableBlend();
            }

            GlVaoSafety.bindVertexArray(prevVao);
            GlStateManager._glUseProgram(prevProgram);
        }
    }

    private static boolean copySceneTexture(int srcColorTex, int w, int h, int prevTex0) {
        if (!sceneCopySupported || srcColorTex <= 0 || w <= 0 || h <= 0) return false;
        GLCapabilities caps = GL.getCapabilities();
        if (caps == null || caps.glCopyImageSubData == 0L) {
            sceneCopySupported = false;
            noteOnce("copy-unsupported", "GL43/ARB_copy_image unavailable - cinematic pass falling back to additive");
            return false;
        }

        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, srcColorTex);
            int internalFmt = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            if (internalFmt <= 0) internalFmt = GL11.GL_RGBA8;

            if (sceneTex != -1 && (sceneTexW != w || sceneTexH != h || sceneTexInternalFormat != internalFmt)) {
                GL11.glDeleteTextures(sceneTex);
                sceneTex = -1;
            }
            if (sceneTex == -1) {
                sceneTex = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTex);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFmt, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                sceneTexW = w;
                sceneTexH = h;
                sceneTexInternalFormat = internalFmt;
            }
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
            GlStateManager._activeTexture(prevActive);
        }

        while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* clear stale */ }
        GL43.glCopyImageSubData(srcColorTex, GL11.GL_TEXTURE_2D, 0, 0, 0, 0,
                                sceneTex, GL11.GL_TEXTURE_2D, 0, 0, 0, 0, w, h, 1);
        int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            sceneCopySupported = false;
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: scene glCopyImageSubData failed (err 0x{}) - disabling cinematics pass", Integer.toHexString(err));
            return false;
        }
        return true;
    }

    private static boolean ensureFbo(int colorTexture) {
        if (fbo == -1) {
            fbo = GL30.glGenFramebuffers();
            lastBoundColorTex = -1;
        }
        return fbo > 0;
    }

    private static int ensureNearestSampler() {
        if (nearestSampler == -1) {
            nearestSampler = GL33.glGenSamplers();
            GL33.glSamplerParameteri(nearestSampler, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL33.glSamplerParameteri(nearestSampler, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL33.glSamplerParameteri(nearestSampler, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL33.glSamplerParameteri(nearestSampler, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }
        return nearestSampler;
    }

    private static boolean ensureProgram() {
        if (program != -1) return true;
        try {
            String fshSource = GlShaderUtil.loadResource(RawScreenSpaceLight.class, "/assets/hbm_m/shaders/core/screen_space_light.fsh");
            int prog = GlShaderUtil.createRasterProgram(
                    RawFullscreenQuad.VERTEX_SHADER_SOURCE,
                    fshSource,
                    java.util.Map.of(0, "Position"),
                    "RawScreenSpaceLight");
            if (prog == 0) {
                return false;
            }
            program = prog;
            resolveUniformLocations();
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("RawScreenSpaceLight: shader compilation error:", t);
            return false;
        }
    }

    private static void resolveUniformLocations() {
        uDepthTex0Loc = GL20.glGetUniformLocation(program, "uDepthTex0");
        uDhDepthTexLoc = GL20.glGetUniformLocation(program, "uDhDepthTex");
        uDhReverseZLoc = GL20.glGetUniformLocation(program, "uDhReverseZ");
        uDhPresentLoc = GL20.glGetUniformLocation(program, "uDhPresent");
        uDhNearLoc = GL20.glGetUniformLocation(program, "uDhNear");
        uDhFarLoc = GL20.glGetUniformLocation(program, "uDhFar");
        uDhFadeDistLoc = GL20.glGetUniformLocation(program, "uDhFadeDist");
        uProjLoc = GL20.glGetUniformLocation(program, "uProj");
        uModelViewLoc = GL20.glGetUniformLocation(program, "uModelView");
        uModelViewInvLoc = GL20.glGetUniformLocation(program, "uModelViewInv");
        uCamPosLoc = GL20.glGetUniformLocation(program, "uCamPos");
        uSourcesLoc = GL20.glGetUniformLocation(program, "uSources[0]");
        uSourceCountLoc = GL20.glGetUniformLocation(program, "uSourceCount");
        uSrcRadiusLoc = GL20.glGetUniformLocation(program, "uSrcRadius[0]");
        uSrcShockLoc = GL20.glGetUniformLocation(program, "uSrcShock[0]");
        uSrcExtraLoc = GL20.glGetUniformLocation(program, "uSrcExtra[0]");
        uTimeLoc = GL20.glGetUniformLocation(program, "uTime");
        uVanillaFarLoc = GL20.glGetUniformLocation(program, "uVanillaFar");
        uIntensityLoc = GL20.glGetUniformLocation(program, "uIntensity");
        uHaloStrengthLoc = GL20.glGetUniformLocation(program, "uHaloStrength");
        uHeatScaleLoc = GL20.glGetUniformLocation(program, "uHeatScale");
        uResolutionLoc = GL20.glGetUniformLocation(program, "uResolution");
        uNormalScaleLoc = GL20.glGetUniformLocation(program, "uNormalScale");
        uDebugReplaceLoc = GL20.glGetUniformLocation(program, "uDebugReplace");
        uSolidAddLoc = GL20.glGetUniformLocation(program, "uSolidAdd");
        uSceneColorLoc = GL20.glGetUniformLocation(program, "uSceneColor");
        uHasSceneLoc = GL20.glGetUniformLocation(program, "uHasScene");
        uCineLoc = GL20.glGetUniformLocation(program, "uCine");
        uDhBlittedInDepth0Loc = GL20.glGetUniformLocation(program, "uDhBlittedInDepth0");
    }

    public static void destroy() {
        if (nearestSampler != -1) {
            GL33.glDeleteSamplers(nearestSampler);
            nearestSampler = -1;
        }
        if (program != -1) {
            GL20.glDeleteProgram(program);
            program = -1;
        }
        if (fbo != -1) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = -1;
            lastBoundColorTex = -1;
        }
        if (sceneTex != -1) {
            GL11.glDeleteTextures(sceneTex);
            sceneTex = -1;
            sceneTexW = -1;
            sceneTexH = -1;
            sceneTexInternalFormat = -1;
        }
        if (cleanDepthTex != -1) {
            GL11.glDeleteTextures(cleanDepthTex);
            cleanDepthTex = -1;
            cleanDepthW = -1;
            cleanDepthH = -1;
            cleanDepthInternalFormat = -1;
        }
        cleanDepthSnapshotValid = false;
        dhBlittedThisFrame = false;
        vanillaMatricesValid = false;
        latched = false;
        consecutiveFailures = 0;
        loggedFirstDraw = false;
        lastDrawnRenderFrame = -1L;
        notedOnce.clear();
    }
}

