package com.hbm_m.client.render.shader;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-composite аддитивный проход для Iris/Oculus шейдерпаков.
 *
 * <p>Под активным паком draw кастомного core-шейдера мода ({@code hbm_m:gun_additive})
 * попадает в colortex пака как НЕзарегистрированный контент (Oculus 1.8.0 не подменяет
 * модовые шейдеры ExtendedShader'ами), и дальше его судьбу решает композитная цепочка
 * пака: Photon (gbuffer-as-data) переинтерпретирует сплэт как данные материала — чёрный
 * бокс; Solas (полностью deferred-небо) затирает всё с глубиной неба
 * ({@code if (z0 == 1.0) color = skyColor;}). BSL спасает только то, что пишет финальный
 * цвет прямо в gbuffers и пропускает colortex0 сквозь композиты.</p>
 *
 * <p>Решение: под паком аддитивные квады GunGL (mode 2 — вспышки, трассеры, снаряд-факел)
 * НЕ рисуются в gbuffer, а ставятся в эту очередь; в TAIL
 * {@code finalizeLevelRendering} (финальная картинка пака уже в главном render target,
 * его depth-текстура ещё держит глубину сцены — рецепт {@link RawScreenSpaceLight})
 * квады дорисовываются поверх композита raw-GL с истинным аддитивным блендингом и
 * честным depth-test против depth-текстуры главного RT. Работает одинаково для ЛЮБОГО
 * пака: композиты уже позади. Вершинные координаты хранятся камера-относительными
 * (сырые данные {@code GunGL.Tess} до умножения на матрицу), проекция — gbuffer-матрицы,
 * снапшотнутые в HEAD той же инъекции.</p>
 *
 * <p>Отказоустойчивость по образцу RawScreenSpaceLight: после
 * {@value #MAX_CONSECUTIVE_FAILURES} подряд неуспешных кадров проход навсегда выключается
 * ({@link #engaged()} становится false), и GunGL возвращается к легаси-пути отрисовки в
 * gbuffer пака. Ограничение: DH-рельеф не участвует в depth-test (глубины DH в главном RT
 * нет) — трассеры поверх LOD-террейна далёких чанков не перекрываются им.</p>
 */
public final class PackAdditiveQueue {
    private PackAdditiveQueue() {}

    private static final int MAX_CONSECUTIVE_FAILURES = 60;
    private static final int FLOATS_PER_VERT = 12; // x,y,z,u,v,r,g,b,a,nx,ny,nz (нормаль не используется)

    private static int consecutiveFailures;
    private static boolean latched;

    private static long frameCounter;
    private static boolean loggedFirstDraw;
    private static long sweptStaleErrors;
    private static final long STATE_EVERY_FRAMES = 1200;

    /** Depth-test против depth-текстуры главного RT; {@code -Dhbm.packAdditive.depth=false} выключает. */
    private static boolean depthEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("hbm.packAdditive.depth", "true"));
    }

    // ─── Очередь кадра ─────────────────────────────────────────────────────

    public static final int DEPTH_MODE_WORLD = 0;
    public static final int DEPTH_MODE_HAND = 1;
    public static final int DEPTH_MODE_SKY = 2;

    private static int currentDepthMode = DEPTH_MODE_WORLD;

    public static void setDepthMode(int mode) {
        currentDepthMode = mode;
    }

    public static int getDepthMode() {
        return currentDepthMode;
    }

    private static final class Batch {
        final int texId;
        final int glMode;
        final Matrix4f mvp;
        final int depthMode;
        final float[] data;
        final int floats;

        Batch(int texId, int glMode, Matrix4f mvp, int depthMode, float[] data, int floats) {
            this.texId = texId;
            this.glMode = glMode;
            this.mvp = mvp;
            this.depthMode = depthMode;
            this.data = data;
            this.floats = floats;
        }
    }

    private static final List<Batch> batches = new ArrayList<>();
    private static int curTexId = -1;
    private static int curGlMode;
    private static final Matrix4f curMvp = new Matrix4f();
    private static int curDepthMode = DEPTH_MODE_WORLD;
    private static float[] batchData = new float[1024];
    private static int batchFloats;

    // ─── GL-ресурсы ────────────────────────────────────────────────────────

    private static int program = -1, vao = -1, vbo = -1, fbo = -1;
    private static int ibo = -1, iboQuadCapacity;
    private static int uMvpLoc = -1, uTexLoc = -1;
    private static java.nio.FloatBuffer matBuf = java.nio.ByteBuffer
            .allocateDirect(16 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private static java.nio.ByteBuffer vboBuf = java.nio.ByteBuffer
            .allocateDirect(1024 * 4).order(java.nio.ByteOrder.nativeOrder());

    private static final String VSH = """
            #version 150 core
            in vec3 aPos;
            in vec2 aUv;
            in vec4 aColor;
            uniform mat4 uMvp;
            out vec2 vUv;
            out vec4 vColor;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                vUv = aUv;
                vColor = aColor;
            }
            """;

    private static final String FSH = """
            #version 150 core
            uniform sampler2D uTex;
            in vec2 vUv;
            in vec4 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = texture(uTex, vUv) * vColor;
            }
            """;

    /**
     * Квады аддитивного режима ставятся в очередь только когда пак реально рендерит мир,
     * это не теневой проход и не GUI; иначе GunGL рисует легаси-путём (без пака он корректен).
     */
    public static boolean engaged() {
        return !latched
                && !ShaderCompatibilityDetector.isRenderingShadowPass()
                && !GunGL.isGuiLighting();
    }

    /**
     * Kept for Mixin compatibility at HEAD of {@code finalizeLevelRendering}.
     */
    public static void captureFrameMatrices() {
        if (!engaged()) {
            clearQueue();
        }
    }

    /**
     * Ставит один ad-hoc draw {@code GunGL.Tess} (аддитивный режим) в очередь.
     * Сразу захватывает полный трансформ {@code ProjMat * ModelViewMat * model} и режим глубины.
     */
    public static void push(ResourceLocation texLoc, int glMode, Matrix4fc model, float[] data, int floats) {
        pushWithDepthMode(texLoc, glMode, model, data, floats, currentDepthMode);
    }

    public static void pushWithDepthMode(ResourceLocation texLoc, int glMode, Matrix4fc model, float[] data, int floats, int depthMode) {
        if (floats <= 0 || floats % FLOATS_PER_VERT != 0 || model == null) return;
        int texId = resolveTexture(texLoc);
        if (texId <= 0) return;
        flushBatch();
        curTexId = texId;
        curGlMode = glMode;
        curDepthMode = depthMode;
        curMvp.set(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix())
              .mul(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix())
              .mul(model);
        if (floats > batchData.length) {
            batchData = new float[Math.max(floats, batchData.length * 2)];
        }
        System.arraycopy(data, 0, batchData, 0, floats);
        batchFloats = floats;
    }

    /** TAIL {@code finalizeLevelRendering}: дорисовывает очередь поверх финальной картинки пака. */
    public static void drainAndClear() {
        if (batchFloats == 0 && batches.isEmpty()) {
            return;
        }
        flushBatch();
        if (batches.isEmpty()) {
            return;
        }
        boolean drawn = false;
        try {
            drawn = drawBatches();
        } catch (Throwable t) {
            consecutiveFailures++;
            if (consecutiveFailures == 1 || consecutiveFailures % 10 == 0) {
                MainRegistry.LOGGER.warn("PackAdditiveQueue: drain failed (attempt {}): {}",
                        consecutiveFailures, t.toString());
            }
        }
        if (drawn) {
            consecutiveFailures = 0;
        } else {
            noteOnce("skip-frame", "queue of " + batches.size() + " batch(es) NOT drawn (latched=" + latched + ") - will keep retrying");
        }
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES && !latched) {
            latched = true;
            MainRegistry.LOGGER.warn("PackAdditiveQueue: {} consecutive failures - permanently disabled, "
                    + "additive effects fall back to in-gbuffer rendering", consecutiveFailures);
        }
        clearQueue();
    }

    private static void flushBatch() {
        if (batchFloats > 0 && curTexId > 0) {
            batches.add(new Batch(curTexId, curGlMode, new Matrix4f(curMvp), curDepthMode,
                    java.util.Arrays.copyOf(batchData, batchFloats), batchFloats));
        }
        batchFloats = 0;
        curTexId = -1;
    }

    private static void clearQueue() {
        batches.clear();
        batchFloats = 0;
        curTexId = -1;
    }

    private static int resolveTexture(ResourceLocation loc) {
        try {
            var tm = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            var tex = tm.getTexture(loc);
            if (tex == null) {
                int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
                com.mojang.blaze3d.platform.GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                try {
                    tm.bindForSetup(loc);
                    tex = tm.getTexture(loc);
                } finally {
                    com.mojang.blaze3d.platform.GlStateManager._bindTexture(prevTex0);
                    com.mojang.blaze3d.platform.GlStateManager._activeTexture(prevActive);
                }
            }
            return tex == null ? -1 : tex.getId();
        } catch (Throwable t) {
            return -1;
        }
    }

    // ─── Raw-GL отрисовка ──────────────────────────────────────────────────

    private static boolean drawBatches() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        int colorTex = main.getColorTextureId(), depthTex = main.getDepthTextureId();
        int width = main.width, height = main.height;
        if (colorTex <= 0 || depthTex <= 0 || width <= 0 || height <= 0) return false;
        if (!ensureProgram() || !ensureVao()) return false;

        ShaderBindResync.forceIrisDepthColorEnabledQuiet();
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevDrawFb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        setGlActiveTexture(GL13.GL_TEXTURE0);
        int prevUnit0Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthTestWas = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullWas = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blendWas = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevBlendSrc = GL11.glGetInteger(GL20.GL_BLEND_SRC_RGB);
        int prevBlendDst = GL11.glGetInteger(GL20.GL_BLEND_DST_RGB);
        int prevBlendSrcA = GL11.glGetInteger(GL20.GL_BLEND_SRC_ALPHA);
        int prevBlendDstA = GL11.glGetInteger(GL20.GL_BLEND_DST_ALPHA);
        int prevVx = 0, prevVy = 0, prevVw = width, prevVh = height;
        boolean[] prevColorMask = {true, true, true, true};
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            var viewport = stack.mallocInt(16);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            prevVx = viewport.get(0); prevVy = viewport.get(1); prevVw = viewport.get(2); prevVh = viewport.get(3);
            var mask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            prevColorMask[0] = mask.get(0) != 0;
            prevColorMask[1] = mask.get(1) != 0;
            prevColorMask[2] = mask.get(2) != 0;
            prevColorMask[3] = mask.get(3) != 0;

            if (!ensureFbo(colorTex, depthTex, depthEnabled())) return false;

            boolean depthMode = depthEnabled();
            com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0, width, height);
            com.mojang.blaze3d.systems.RenderSystem.colorMask(true, true, true, true);
            com.mojang.blaze3d.systems.RenderSystem.disableCull();
            if (depthMode) {
                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.depthFunc(GL11.GL_LEQUAL);
            } else {
                com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            }
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.platform.GlStateManager._blendFuncSeparate(0, 0, 0, 0);
            com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
            com.mojang.blaze3d.systems.RenderSystem.blendEquation(GL20.GL_FUNC_ADD);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            com.mojang.blaze3d.platform.GlStateManager._glUseProgram(program);
            if (!loggedFirstDraw) {
                loggedFirstDraw = true;
                int totalVerts = 0;
                for (Batch b : batches) totalVerts += b.floats / FLOATS_PER_VERT;
                MainRegistry.LOGGER.info(
                        "PackAdditiveQueue: FIRST DRAW: batches={} verts={} rt={}x{} colorTex={} depthTex={} depthMode={} sweptStaleErrors={}",
                        batches.size(), totalVerts, width, height, colorTex, depthTex, depthMode, sweptStaleErrors);
            }
            GL20.glUniform1i(uTexLoc, 0);
            com.mojang.blaze3d.platform.GlStateManager._glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            setGlActiveTexture(GL13.GL_TEXTURE0);
            while (GL11.glGetError() != GL11.GL_NO_ERROR) {
                sweptStaleErrors++;
            }
            int lastDepthMode = -1;
            for (Batch b : batches) {
                if (depthMode && b.depthMode != lastDepthMode) {
                    lastDepthMode = b.depthMode;
                    if (b.depthMode == DEPTH_MODE_HAND) {
                        GL11.glDepthRange(0.0, 0.05);
                    } else if (b.depthMode == DEPTH_MODE_SKY) {
                        GL11.glDepthRange(1.0, 1.0);
                    } else {
                        GL11.glDepthRange(0.0, 1.0);
                    }
                }
                int bytes = b.floats * 4;
                if (vboBuf.capacity() < bytes) {
                    vboBuf = java.nio.ByteBuffer.allocateDirect(bytes * 2).order(java.nio.ByteOrder.nativeOrder());
                }
                vboBuf.clear();
                vboBuf.asFloatBuffer().put(b.data, 0, b.floats);
                ((java.nio.Buffer) vboBuf).limit(bytes);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboBuf, GL15.GL_STREAM_DRAW);
                com.mojang.blaze3d.platform.GlStateManager._bindTexture(b.texId);
                matBuf.clear();
                b.mvp.get(matBuf);
                matBuf.limit(16).position(0);
                GL20.glUniformMatrix4fv(uMvpLoc, false, matBuf);
                int verts = b.floats / FLOATS_PER_VERT;
                if (b.glMode == GL11.GL_QUADS) {
                    ensureQuadIndices(verts / 4);
                    GL11.glDrawElements(GL11.GL_TRIANGLES, (verts / 4) * 6, GL20.GL_UNSIGNED_INT, 0L);
                } else {
                    GL11.glDrawArrays(b.glMode, 0, verts);
                }
            }
            if (loggedFirstDraw) {
                frameCounter++;
                if (frameCounter % STATE_EVERY_FRAMES == 0) {
                    int totalVerts = 0;
                    for (Batch b : batches) totalVerts += b.floats / FLOATS_PER_VERT;
                    MainRegistry.LOGGER.info("PackAdditiveQueue: state frame #{}: batches={} verts={} depth={}",
                            frameCounter, batches.size(), totalVerts, depthEnabled() ? "fbo" : "off");
                }
            }
            int err = GL11.glGetError();
            if (err != 0) {
                MainRegistry.LOGGER.warn("PackAdditiveQueue: draw set GL error 0x{} (batch frames kept)",
                        Integer.toHexString(err));
            }
            return true;
        } finally {
            GL11.glDepthRange(0.0, 1.0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            setGlActiveTexture(GL13.GL_TEXTURE0);
            com.mojang.blaze3d.platform.GlStateManager._bindTexture(prevUnit0Tex);
            setGlActiveTexture(prevActiveTex);
            com.mojang.blaze3d.platform.GlStateManager._glUseProgram(prevProgram);
            com.mojang.blaze3d.platform.GlStateManager._glBindVertexArray(prevVao);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFb);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb);
            com.mojang.blaze3d.systems.RenderSystem.viewport(prevVx, prevVy, prevVw, prevVh);
            com.mojang.blaze3d.systems.RenderSystem.colorMask(prevColorMask[0], prevColorMask[1], prevColorMask[2], prevColorMask[3]);
            com.mojang.blaze3d.systems.RenderSystem.depthFunc(prevDepthFunc);
            if (depthTestWas) com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            else com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(prevDepthMask);
            if (cullWas) com.mojang.blaze3d.systems.RenderSystem.enableCull();
            else com.mojang.blaze3d.systems.RenderSystem.disableCull();
            ShaderBindResync.resetBlendModeToOpaque();
            if (blendWas) {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.platform.GlStateManager._blendFuncSeparate(0, 0, 0, 0);
                com.mojang.blaze3d.platform.GlStateManager._blendFuncSeparate(prevBlendSrc, prevBlendDst, prevBlendSrcA, prevBlendDstA);
            } else {
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            }
        }
    }

    private static void setGlActiveTexture(int unit) {
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(unit);
    }

    private static boolean ensureFbo(int colorTex, int depthTex, boolean depthMode) {
        if (fbo != -1 && !GL30.glIsFramebuffer(fbo)) { fbo = -1; }
        if (fbo == -1) fbo = GL30.glGenFramebuffers();
        int prevDrawFb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTex, 0);
        if (depthMode) {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTex, 0);
        } else {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
        }
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFb);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            noteOnce("fbo-incomplete", "post-composite FBO incomplete (0x" + Integer.toHexString(status) + ")");
            return false;
        }
        return true;
    }

    private static boolean ensureVao() {
        if (vao != -1 && GL30.glIsVertexArray(vao)) return true;
        if (vbo != -1 && GL15.glIsBuffer(vbo)) GL15.glDeleteBuffers(vbo);
        vbo = GL15.glGenBuffers();
        vao = GL30.glGenVertexArrays();
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        // stride 12 float: x,y,z (0) | u,v (3) | r,g,b,a (5) | нормаль (9) пропускается
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, FLOATS_PER_VERT * 4, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, FLOATS_PER_VERT * 4, 12L);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, FLOATS_PER_VERT * 4, 20L);
        // Индекс-буфер квадов привязывается к VAO (element array — состояние VAO).
        ibo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        iboQuadCapacity = 0;
        GL30.glBindVertexArray(prevVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
        return true;
    }

    /**
     * Общий индекс-буфер разворота квадов в треугольники (как ванильный
     * {@code RenderSystem.getSequentialBuffer} для Mode.QUADS): паттерн
     * 0,1,2, 2,3 на каждый квад. Растёт с удвоением.
     */
    private static void ensureQuadIndices(int quads) {
        if (quads <= iboQuadCapacity) return;
        int newCap = Math.max(quads, Math.max(64, iboQuadCapacity * 2));
        java.nio.IntBuffer indices = java.nio.ByteBuffer
                .allocateDirect(newCap * 6 * 4).order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();
        for (int q = 0; q < newCap; q++) {
            int v = q * 4;
            indices.put(v).put(v + 1).put(v + 2).put(v + 2).put(v + 3).put(v);
        }
        indices.flip();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
        iboQuadCapacity = newCap;
    }

    private static boolean ensureProgram() {
        if (program != -1 && GL20.glIsProgram(program)) return true;
        try {
            int p = GlShaderUtil.createRasterProgram(VSH, FSH,
                    java.util.Map.of(0, "aPos", 1, "aUv", 2, "aColor"), "PackAdditiveQueue");
            if (p == 0) {
                noteOnce("program-failed", "raster program build failed - falling back to legacy path");
                return false;
            }
            program = p;
            uMvpLoc = GL20.glGetUniformLocation(program, "uMvp");
            uTexLoc = GL20.glGetUniformLocation(program, "uTex");
            return true;
        } catch (Throwable t) {
            noteOnce("program-threw", "raster program init threw: " + t);
            return false;
        }
    }

    private static void noteOnce(String key, String message) {
        MainRegistry.LOGGER.info("PackAdditiveQueue [{}]: {}", key, message);
    }
}
