package com.hbm_m.client.render.shader;

import com.hbm_m.client.render.GlVaoSafety;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.particle.nt.NukeTorex;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

/**
 * Renders a screen-space coverage mask (R8) of live {@link NukeTorex} cloudlet
 * particles so {@link RawScreenSpaceLight} can attenuate background/sky/godray
 * light behind opaque mushroom clouds instead of making them look translucent.
 */
public final class NukeCloudMask {
    private NukeCloudMask() {}

    private static final String VERTEX_SHADER = """
            #version 150
            in vec3 Position;
            in vec2 UV0;
            in vec4 Color;
            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;
            out vec2 texCoord0;
            out vec4 vertexColor;
            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                texCoord0 = UV0;
                vertexColor = Color;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 150
            in vec2 texCoord0;
            in vec4 vertexColor;
            uniform sampler2D Sampler0;
            uniform sampler2D uDepthTex;
            uniform int uHasDepth;
            uniform vec2 uDepthScale;
            out vec4 fragColor;
            void main() {
                if (uHasDepth != 0) {
                    ivec2 maxCoord = textureSize(uDepthTex, 0) - ivec2(1);
                    ivec2 depthCoord = clamp(ivec2(gl_FragCoord.xy * uDepthScale), ivec2(0), maxCoord);
                    float sceneDepth = texelFetch(uDepthTex, depthCoord, 0).r;
                    if (gl_FragCoord.z > sceneDepth) discard;
                }
                float a = texture(Sampler0, texCoord0).a * vertexColor.a;
                if (a <= 0.005) discard;
                fragColor = vec4(1.0, 1.0, 1.0, a);
            }
            """;

    private static int program = -1;
    private static int uModelViewMatLoc = -1;
    private static int uProjMatLoc = -1;
    private static int uSampler0Loc = -1;
    private static int uDepthTexLoc = -1;
    private static int uHasDepthLoc = -1;
    private static int uDepthScaleLoc = -1;

    private static int fbo = -1;
    private static int maskTex = -1;
    private static int maskW = -1;
    private static int maskH = -1;
    private static final int[] DRAW_BUFFERS = {GL30.GL_COLOR_ATTACHMENT0};

    private static VertexBuffer vbo;
    //? if < 1.21.1 {
    private static BufferBuilder maskBuilder;
    //?}

    private static final FloatBuffer mvBuf = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final FloatBuffer projBuf = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private static boolean clearedThisFrame = false;
    private static boolean maskValidThisFrame = false;
    private static boolean failed = false;

    public static void beginFrame() {
        clearedThisFrame = false;
        maskValidThisFrame = false;
    }

    public static void resetFrame() {
        clearedThisFrame = false;
        maskValidThisFrame = false;
    }

    public static int getMaskTexture() {
        return maskValidThisFrame ? maskTex : 0;
    }

    public static void capturePass(Camera camera, float partialTick,
                                   double splitSq, boolean filterByDistance, boolean renderFar) {
        if (failed || camera == null || !RawScreenSpaceLight.isMaskNeeded()) {
            return;
        }
        if (!ensureProgram() || !ensureVbo()) {
            return;
        }

        AbstractTexture cloudletTex = Minecraft.getInstance().getTextureManager().getTexture(NukeTorex.getCloudletTexture());
        if (cloudletTex == null) {
            return;
        }
        int texId = cloudletTex.getId();
        if (texId <= 0) {
            return;
        }

        int prevDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

        int vpX, vpY, vpW, vpH;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer vp = stack.mallocInt(16);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
            vpX = vp.get(0);
            vpY = vp.get(1);
            vpW = vp.get(2);
            vpH = vp.get(3);
        }
        if (vpW <= 0 || vpH <= 0) {
            return;
        }

        // Build cloudlet geometry before touching GL state
        //? if < 1.21.1 {
        if (maskBuilder == null) {
            maskBuilder = new BufferBuilder(4096);
        }
        maskBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean drewAny = ParticleEngineNT.INSTANCE.renderNukeTorexForMask(
                maskBuilder, camera, partialTick, splitSq, filterByDistance, renderFar);
        if (!drewAny || maskBuilder.isCurrentBatchEmpty()) {
            maskBuilder.endOrDiscardIfEmpty();
            return;
        }
        BufferBuilder.RenderedBuffer rendered = maskBuilder.end();
        //?} else {
        /*com.mojang.blaze3d.vertex.ByteBufferBuilder byteBuf = new com.mojang.blaze3d.vertex.ByteBufferBuilder(4096);
        BufferBuilder builder = new BufferBuilder(byteBuf, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean drewAny = ParticleEngineNT.INSTANCE.renderNukeTorexForMask(
                builder, camera, partialTick, splitSq, filterByDistance, renderFar);
        if (!drewAny) {
            byteBuf.close();
            return;
        }
        com.mojang.blaze3d.vertex.MeshData rendered = builder.build();
        if (rendered == null) {
            byteBuf.close();
            return;
        }
        *///?}

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        int prevUnit0Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        int prevUnit1Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevBlendSrcRgb = GL11.glGetInteger(GL20.GL_BLEND_SRC_RGB);
        int prevBlendDstRgb = GL11.glGetInteger(GL20.GL_BLEND_DST_RGB);
        int prevBlendSrcAlpha = GL11.glGetInteger(GL20.GL_BLEND_SRC_ALPHA);
        int prevBlendDstAlpha = GL11.glGetInteger(GL20.GL_BLEND_DST_ALPHA);
        int prevBlendEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);

        boolean[] prevColorMask = new boolean[4];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer mask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            for (int i = 0; i < 4; i++) {
                prevColorMask[i] = mask.get(i) != 0;
            }
        }

        int depthType = GL11.GL_NONE;
        int depthObj = 0;
        if (prevDrawFbo > 0) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevDrawFbo);
            depthType = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
            if (depthType == GL11.GL_TEXTURE) {
                depthObj = GL30.glGetFramebufferAttachmentParameteri(
                        GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            }
        }

        int targetW = Math.max(1, vpW >> 2);
        int targetH = Math.max(1, vpH >> 2);
        boolean uploaded = false;
        try {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            if (!ensureMaskTarget(targetW, targetH)) {
                return;
            }

            vbo.bind();
            vbo.upload(rendered);
            uploaded = true;

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
            RenderSystem.viewport(0, 0, targetW, targetH);
            RenderSystem.colorMask(true, true, true, true);
            if (!clearedThisFrame) {
                GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                clearedThisFrame = true;
            }

            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();

            RenderSystem.enableBlend();
            GlStateManager._blendFuncSeparate(
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.blendEquation(GL20.GL_FUNC_ADD);

            GlStateManager._glUseProgram(program);
            setMat(mvBuf, RenderSystem.getModelViewMatrix());
            setMat(projBuf, RenderSystem.getProjectionMatrix());
            GL20.glUniformMatrix4fv(uModelViewMatLoc, false, mvBuf);
            GL20.glUniformMatrix4fv(uProjMatLoc, false, projBuf);
            GL20.glUniform1i(uSampler0Loc, 0);
            GL20.glUniform1i(uDepthTexLoc, 1);

            boolean hasDepthTex = depthType == GL11.GL_TEXTURE && depthObj > 0;
            GL20.glUniform1i(uHasDepthLoc, hasDepthTex ? 1 : 0);
            if (hasDepthTex) {
                GL20.glUniform2f(uDepthScaleLoc, (float) vpW / (float) targetW, (float) vpH / (float) targetH);
                GlStateManager._activeTexture(GL13.GL_TEXTURE1);
                GlStateManager._bindTexture(depthObj);
            }

            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(texId);

            vbo.draw();
            VertexBuffer.unbind();
            maskValidThisFrame = true;
        } catch (Throwable t) {
            failed = true;
            MainRegistry.LOGGER.warn("NukeCloudMask: failed, disabling cloud mask:", t);
        } finally {
            //? if < 1.21.1 {
            if (!uploaded && rendered != null) {
                rendered.release();
            }
            //?} else {
            /*rendered.close();
            byteBuf.close();
            *///?}
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
            RenderSystem.viewport(vpX, vpY, vpW, vpH);
            RenderSystem.colorMask(prevColorMask[0], prevColorMask[1], prevColorMask[2], prevColorMask[3]);

            if (cullWasEnabled) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (depthTestWasEnabled) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(prevDepthMask);
            RenderSystem.depthFunc(prevDepthFunc);

            GlStateManager._blendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);
            RenderSystem.blendEquation(prevBlendEquation);
            ShaderBindResync.resetBlendModeToOpaque();
            if (blendWasEnabled) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }

            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(prevUnit0Tex);
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(prevUnit1Tex);
            GlStateManager._activeTexture(prevActiveTex);

            GlVaoSafety.bindVertexArray(prevVao);
            GlStateManager._glUseProgram(prevProgram);
        }
    }

    private static void setMat(FloatBuffer buf, Matrix4f mat) {
        buf.clear();
        mat.get(buf);
        buf.limit(16).position(0);
    }

    private static boolean ensureMaskTarget(int w, int h) {
        if (fbo != -1 && !GL30.glIsFramebuffer(fbo)) {
            fbo = -1;
        }
        if (fbo == -1) {
            fbo = GL30.glGenFramebuffers();
            if (fbo <= 0) {
                fbo = -1;
                failed = true;
                MainRegistry.LOGGER.warn("NukeCloudMask: failed to allocate FBO, disabling cloud mask");
                return false;
            }
        }
        if (maskTex != -1 && (maskW != w || maskH != h)) {
            GL11.glDeleteTextures(maskTex);
            maskTex = -1;
        }
        if (maskTex == -1) {
            maskTex = GL11.glGenTextures();
            GlStateManager._bindTexture(maskTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, w, h, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            maskW = w;
            maskH = h;

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, maskTex, 0);
            GL20.glDrawBuffers(DRAW_BUFFERS);
            if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                GL11.glDeleteTextures(maskTex);
                maskTex = -1;
                maskW = -1;
                maskH = -1;
                failed = true;
                MainRegistry.LOGGER.warn("NukeCloudMask: FBO incomplete, disabling cloud mask");
                return false;
            }
        }
        return true;
    }

    private static boolean ensureProgram() {
        if (program != -1) return true;
        int prog = GlShaderUtil.createRasterProgram(
                VERTEX_SHADER,
                FRAGMENT_SHADER,
                Map.of(0, "Position", 1, "UV0", 2, "Color"),
                "NukeCloudMask");
        if (prog == 0) {
            failed = true;
            return false;
        }
        program = prog;
        uModelViewMatLoc = GL20.glGetUniformLocation(program, "ModelViewMat");
        uProjMatLoc = GL20.glGetUniformLocation(program, "ProjMat");
        uSampler0Loc = GL20.glGetUniformLocation(program, "Sampler0");
        uDepthTexLoc = GL20.glGetUniformLocation(program, "uDepthTex");
        uHasDepthLoc = GL20.glGetUniformLocation(program, "uHasDepth");
        uDepthScaleLoc = GL20.glGetUniformLocation(program, "uDepthScale");
        return true;
    }

    private static boolean ensureVbo() {
        if (vbo == null) {
            vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        return true;
    }

    public static void destroy() {
        if (program != -1) {
            GL20.glDeleteProgram(program);
            program = -1;
        }
        if (fbo != -1) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = -1;
        }
        if (maskTex != -1) {
            GL11.glDeleteTextures(maskTex);
            maskTex = -1;
            maskW = -1;
            maskH = -1;
        }
        if (vbo != null) {
            vbo.close();
            vbo = null;
        }
        //? if < 1.21.1 {
        maskBuilder = null;
        //?}
        clearedThisFrame = false;
        maskValidThisFrame = false;
        failed = false;
    }
}
