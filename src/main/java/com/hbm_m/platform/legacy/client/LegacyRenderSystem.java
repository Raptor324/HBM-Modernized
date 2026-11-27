package com.hbm_m.platform.legacy.client;

import java.util.function.Supplier;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.opengl.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * [26.x] Ersatz für die Immediate-State-Methoden von {@code RenderSystem} (1.21.1), die es mit den
 * RenderPipelines von 26.x nicht mehr gibt. Stonecutter leitet {@code RenderSystem.xyz(...)} hierher um
 * (Regeln {@code mc26_rendersystem*}).
 *
 * <p>Verhalten:
 * <ul>
 *   <li>Mit OpenGL-Backend werden Zustandsänderungen (Blend, Depth, Cull, Texturen, ...) an
 *       {@link GlStateManager} durchgereicht, damit roher GL-Code (Nucleus) weiter funktioniert und der
 *       Zustands-Cache von renderpearl konsistent bleibt.</li>
 *   <li>Mit Vulkan-Backend sind alle Zustandsmethoden No-ops (es gibt keinen GL-Kontext).</li>
 *   <li>Shader-Farbe/-Textur werden nur gemerkt (z. B. für {@link GuiCompat}).</li>
 *   <li>Projektion/Modelview: {@link #setLegacyMatrices} muss pro Frame aus dem Level-Render-Hook gesetzt
 *       werden. TODO(26.x): Hook in RenderLevelStageEvent anschließen.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public final class LegacyRenderSystem {

    private LegacyRenderSystem() {}

    // ---------------------------------------------------------------- Backend

    private static Boolean openGl;

    /** true, wenn renderpearl das OpenGL-Backend nutzt (nur dann ist roher GL-Code erlaubt). */
    public static boolean isOpenGl() {
        Boolean cached = openGl;
        if (cached != null) return cached;
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null) return false;
        String backend = device.getDeviceInfo().backendName();
        boolean gl = backend != null && backend.toLowerCase(java.util.Locale.ROOT).contains("gl");
        openGl = gl;
        return gl;
    }

    // ---------------------------------------------------------------- Shader-Farbe / -Textur / -Shader

    private static final float[] SHADER_COLOR = {1f, 1f, 1f, 1f};
    private static final Identifier[] SHADER_TEXTURES = new Identifier[12];
    private static final int[] SHADER_TEXTURE_IDS = new int[12];
    private static String vanillaShader;
    private static Supplier<?> shader;

    public static void setShaderColor(float r, float g, float b, float a) {
        SHADER_COLOR[0] = r;
        SHADER_COLOR[1] = g;
        SHADER_COLOR[2] = b;
        SHADER_COLOR[3] = a;
    }

    public static float[] getShaderColor() {
        return SHADER_COLOR;
    }

    /** Gemerkte Shader-Farbe als ARGB (für GUI-Aufträge mit Farbparameter). */
    public static int shaderColorArgb() {
        return ((int) (SHADER_COLOR[3] * 255f) & 0xFF) << 24 | ((int) (SHADER_COLOR[0] * 255f) & 0xFF) << 16
                | ((int) (SHADER_COLOR[1] * 255f) & 0xFF) << 8 | ((int) (SHADER_COLOR[2] * 255f) & 0xFF);
    }

    /** Alt: {@code RenderSystem.setShader(GameRenderer::getXxxShader)}. Es gibt keine globalen Shader mehr. */
    public static void setVanillaShader(String gameRendererGetter) {
        vanillaShader = gameRendererGetter;
        shader = null;
    }

    public static void setShader(Supplier<?> supplier) {
        shader = supplier;
        vanillaShader = null;
    }

    public static Object getShader() {
        return shader != null ? shader.get() : null;
    }

    public static void setShaderTexture(int unit, Identifier texture) {
        if (unit >= 0 && unit < SHADER_TEXTURES.length) {
            SHADER_TEXTURES[unit] = texture;
            SHADER_TEXTURE_IDS[unit] = 0;
        }
    }

    public static void setShaderTexture(int unit, int glTextureId) {
        if (unit >= 0 && unit < SHADER_TEXTURE_IDS.length) {
            SHADER_TEXTURE_IDS[unit] = glTextureId;
            SHADER_TEXTURES[unit] = null;
        }
    }

    public static int getShaderTexture(int unit) {
        return unit >= 0 && unit < SHADER_TEXTURE_IDS.length ? SHADER_TEXTURE_IDS[unit] : 0;
    }

    public static Identifier getShaderTextureLocation(int unit) {
        return unit >= 0 && unit < SHADER_TEXTURES.length ? SHADER_TEXTURES[unit] : null;
    }

    public static void setShaderGameTime(long gameTime, float partialTick) {
    }

    // ---------------------------------------------------------------- Blend

    public enum SourceFactor {
        CONSTANT_ALPHA(0x8003), CONSTANT_COLOR(0x8001), DST_ALPHA(0x0304), DST_COLOR(0x0306), ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(0x8004), ONE_MINUS_CONSTANT_COLOR(0x8002), ONE_MINUS_DST_ALPHA(0x0305),
        ONE_MINUS_DST_COLOR(0x0307), ONE_MINUS_SRC_ALPHA(0x0303), ONE_MINUS_SRC_COLOR(0x0301), SRC_ALPHA(0x0302),
        SRC_ALPHA_SATURATE(0x0308), SRC_COLOR(0x0300), ZERO(0);

        public final int value;

        SourceFactor(int value) {
            this.value = value;
        }
    }

    public enum DestFactor {
        CONSTANT_ALPHA(0x8003), CONSTANT_COLOR(0x8001), DST_ALPHA(0x0304), DST_COLOR(0x0306), ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(0x8004), ONE_MINUS_CONSTANT_COLOR(0x8002), ONE_MINUS_DST_ALPHA(0x0305),
        ONE_MINUS_DST_COLOR(0x0307), ONE_MINUS_SRC_ALPHA(0x0303), ONE_MINUS_SRC_COLOR(0x0301), SRC_ALPHA(0x0302),
        SRC_COLOR(0x0300), ZERO(0);

        public final int value;

        DestFactor(int value) {
            this.value = value;
        }
    }

    public static void enableBlend() {
        if (isOpenGl()) GlStateManager._enableBlend();
    }

    public static void disableBlend() {
        if (isOpenGl()) GlStateManager._disableBlend();
    }

    public static void defaultBlendFunc() {
        blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
    }

    public static void blendFunc(SourceFactor src, DestFactor dst) {
        blendFuncSeparate(src.value, dst.value, src.value, dst.value);
    }

    public static void blendFunc(int src, int dst) {
        blendFuncSeparate(src, dst, src, dst);
    }

    public static void blendFuncSeparate(SourceFactor src, DestFactor dst, SourceFactor srcA, DestFactor dstA) {
        blendFuncSeparate(src.value, dst.value, srcA.value, dstA.value);
    }

    public static void blendFuncSeparate(int src, int dst, int srcA, int dstA) {
        if (isOpenGl()) GlStateManager._blendFuncSeparate(src, dst, srcA, dstA);
    }

    public static void blendEquation(int mode) {
        if (isOpenGl()) GlStateManager._blendEquationSeparate(mode, mode);
    }

    // ---------------------------------------------------------------- Depth / Cull / Mask

    public static void enableDepthTest() {
        if (isOpenGl()) GlStateManager._enableDepthTest();
    }

    public static void disableDepthTest() {
        if (isOpenGl()) GlStateManager._disableDepthTest();
    }

    public static void depthMask(boolean flag) {
        if (isOpenGl()) GlStateManager._depthMask(flag);
    }

    public static void depthFunc(int func) {
        if (isOpenGl()) GlStateManager._depthFunc(func);
    }

    public static void enableCull() {
        if (isOpenGl()) GlStateManager._enableCull();
    }

    public static void disableCull() {
        if (isOpenGl()) GlStateManager._disableCull();
    }

    public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
        if (isOpenGl()) GlStateManager._colorMask((r ? 1 : 0) | (g ? 2 : 0) | (b ? 4 : 0) | (a ? 8 : 0));
    }

    public static void enablePolygonOffset() {
        if (isOpenGl()) GlStateManager._enablePolygonOffset();
    }

    public static void disablePolygonOffset() {
        if (isOpenGl()) GlStateManager._disablePolygonOffset();
    }

    public static void polygonOffset(float factor, float units) {
        if (isOpenGl()) GlStateManager._polygonOffset(factor, units);
    }

    public static void enableScissor(int x, int y, int w, int h) {
        if (isOpenGl()) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(x, y, w, h);
        }
    }

    public static void disableScissor() {
        if (isOpenGl()) GlStateManager._disableScissorTest();
    }

    // ---------------------------------------------------------------- Texturen / Viewport / Clear

    public static void activeTexture(int unit) {
        if (isOpenGl()) GlStateManager._activeTexture(unit);
    }

    public static void bindTexture(int id) {
        if (isOpenGl()) GlStateManager._bindTexture(id);
    }

    public static void viewport(int x, int y, int w, int h) {
        if (isOpenGl()) GlStateManager._viewport(x, y, w, h);
    }

    public static void clear(int mask) {
        if (isOpenGl()) GlStateManager._clear(mask);
    }

    public static void clear(int mask, boolean checkError) {
        clear(mask);
    }

    // ---------------------------------------------------------------- Matrizen

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f PROJECTION_BACKUP = new Matrix4f();
    private static final Matrix3f INVERSE_VIEW_ROTATION = new Matrix3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static Object vertexSorting;

    /** Pro Frame aus dem Level-Render-Hook setzen (Ersatz für den globalen Matrix-Zustand von 1.21.1). */
    public static void setLegacyMatrices(Matrix4f projection, Matrix3f inverseViewRotation) {
        PROJECTION.set(projection);
        INVERSE_VIEW_ROTATION.set(inverseViewRotation);
    }

    public static Matrix4f getProjectionMatrix() {
        return PROJECTION;
    }

    public static void setProjectionMatrix(Matrix4f projection, Object sorting) {
        PROJECTION.set(projection);
        vertexSorting = sorting;
    }

    public static Object getVertexSorting() {
        return vertexSorting;
    }

    public static void backupProjectionMatrix() {
        PROJECTION_BACKUP.set(PROJECTION);
    }

    public static void restoreProjectionMatrix() {
        PROJECTION.set(PROJECTION_BACKUP);
    }

    public static Matrix4f getModelViewMatrix() {
        return RenderSystem.getModelViewMatrixCopy();
    }

    public static Matrix4fStack getModelViewStack() {
        return RenderSystem.getModelViewStack();
    }

    /** In 26.x liest jeder Pass die Modelview-Matrix selbst; nichts zu tun. */
    public static void applyModelViewMatrix() {
    }

    public static Matrix3f getInverseViewRotationMatrix() {
        return INVERSE_VIEW_ROTATION;
    }

    public static void setTextureMatrix(Matrix4f matrix) {
        TEXTURE_MATRIX.set(matrix);
    }

    public static void resetTextureMatrix() {
        TEXTURE_MATRIX.identity();
    }

    // ---------------------------------------------------------------- Nebel

    private static float fogStart = 0f;
    private static float fogEnd = 1024f;
    private static final float[] FOG_COLOR = {0f, 0f, 0f, 0f};

    public static float getShaderFogStart() {
        return fogStart;
    }

    public static float getShaderFogEnd() {
        return fogEnd;
    }

    public static void setShaderFogStart(float start) {
        fogStart = start;
    }

    public static void setShaderFogEnd(float end) {
        fogEnd = end;
    }

    public static float[] getShaderFogColor() {
        return FOG_COLOR;
    }

    // ---------------------------------------------------------------- Threads

    public static boolean isOnRenderThread() {
        return RenderSystem.isOnRenderThread();
    }

    public static void recordRenderCall(Runnable call) {
        if (RenderSystem.isOnRenderThread()) {
            call.run();
        } else {
            Minecraft.getInstance().execute(call);
        }
    }

    static {
        // Sicherstellen, dass die GL-Konstanten-Klassen gebunden sind (nur für IDE-Navigation).
        @SuppressWarnings("unused") int unusedBlend = GL14.GL_FUNC_ADD + GL11.GL_ONE;
    }
}
