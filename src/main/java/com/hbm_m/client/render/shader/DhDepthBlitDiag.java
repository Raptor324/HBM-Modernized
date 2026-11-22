package com.hbm_m.client.render.shader;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ОДНОКАДРОВАЯ ДИАГНОСТИКА DH-блита глубины (dh_depth_blit.fsh).
 *
 * Блит - точка безмолвного отказа: когда его декод ошибочен, КАЖДЫЙ фрагмент
 * уходит в discard (небо или fade-маска), главный z-buffer в пикселях LOD
 * остаётся небом (1.0) и окклюзия/подсветка/шокволна против LOD молча
 * ломаются (регресс DH 3.3.x REVERSE_Z: все лоды декодировались в [near, 2*near]
 * блоков - ниже fade-маски - и копия была гарантированным no-op).
 *
 * Класс снимает грубую выборку DEPTH32F текстуры DH (64x64 из центра экрана),
 * прогоняет её через ТОЧНО ту же логику discard/декода, что и шейдер, и
 * считает: небо / fade-маска / будет записано. Запускается ОДИН раз за сессию
 * после первого фактического блита; повтор - через
 * -Dhbm.dhDepthBlitDiag=every (отключить: =off).
 *
 * Срабатывание тревоги: write == 0 при ненулевом числе террейн-текселей -
 * сигнатура «весь террейн ушёл под discard» (старый баг декода); также
 * сообщается, если ВСЕ террейн-тексели декодировались в нереалистично малые
 * дистанции (свернулись к near).
 */
public final class DhDepthBlitDiag {

    private DhDepthBlitDiag() {}

    private static final int SAMPLE = 64;
    private static boolean done;
    private static boolean failed;

    /**
     * Called right AFTER a successful dh_depth_blit draw (both the vanilla
     * DhDepthCopy path and the Iris RawDhDepthCopy path). Analyzes the blit's
     * INPUT texture - the same texels the shader just consumed. Never throws,
     * never breaks the render.
     */
    public static void onBlitRan(int dhDepthTex, float dhNear, float dhFar, boolean reverseZ, float fadeMaskDist) {
        if (failed || dhDepthTex <= 0 || dhNear <= 0.0F || dhFar <= dhNear) {
            return;
        }
        String mode = System.getProperty("hbm.dhDepthBlitDiag", "once");
        if ("off".equalsIgnoreCase(mode) || "false".equalsIgnoreCase(mode)) {
            return;
        }
        if (!"every".equalsIgnoreCase(mode)) {
            if (done) {
                return;
            }
            done = true;
        }
        try {
            sampleAndReport(dhDepthTex, dhNear, dhFar, reverseZ, fadeMaskDist);
        } catch (Throwable t) {
            failed = true; // diagnostics latch: never interfere with rendering
            MainRegistry.LOGGER.warn("DhDepthBlitDiag: failed - diagnostics disabled:", t);
        }
    }

    private static void sampleAndReport(int dhDepthTex, float dhNear, float dhFar, boolean reverseZ, float fadeMaskDist) {
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int scratch = -1;
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, dhDepthTex);
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (w <= 0 || h <= 0) {
                return;
            }
            int rw = Math.min(SAMPLE, w);
            int rh = Math.min(SAMPLE, h);
            int sx = Math.max(0, (w - rw) / 2);
            int sy = Math.max(0, (h - rh) / 2);

            while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* clear stale */ }
            scratch = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scratch);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, rw, rh, 0,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
            GL43.glCopyImageSubData(dhDepthTex, GL11.GL_TEXTURE_2D, 0, sx, sy, 0,
                    scratch, GL11.GL_TEXTURE_2D, 0, 0, 0, 0, rw, rh, 1);
            if (GL11.glGetError() != GL11.GL_NO_ERROR) {
                MainRegistry.LOGGER.info("DhDepthBlitDiag: glCopyImageSubData failed - sample skipped");
                return;
            }

            FloatBuffer buf = ByteBuffer.allocateDirect(rw * rh * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, buf);
            if (GL11.glGetError() != GL11.GL_NO_ERROR) {
                MainRegistry.LOGGER.info("DhDepthBlitDiag: glGetTexImage failed - sample skipped");
                return;
            }

            int total = rw * rh, sky = 0, fade = 0, write = 0, nearCollapsed = 0;
            float maxDecoded = 0.0F;
            for (int i = 0; i < total; i++) {
                float d = buf.get(i);
                float dist;
                if (reverseZ) {
                    // Same logic as dh_depth_blit.fsh (fixed): window depth,
                    // terrain (0.5, 1.0], sky <= 0.5.
                    if (d <= 0.5F) { sky++; continue; }
                    float ndc = d * 2.0F - 1.0F;
                    float denom = dhNear + ndc * (dhFar - dhNear);
                    dist = denom > 1.0e-6F ? dhNear * dhFar / denom : dhFar;
                } else {
                    if (d >= 0.999999F) { sky++; continue; }
                    float ndc = d * 2.0F - 1.0F;
                    float denom = (dhFar + dhNear) - ndc * (dhFar - dhNear);
                    dist = denom > 1.0e-6F ? 2.0F * dhFar * dhNear / denom : dhFar;
                }
                if (fadeMaskDist > 0.0F && dist < fadeMaskDist) {
                    fade++;
                } else {
                    write++;
                    if (dist < dhNear * 3.0F) {
                        nearCollapsed++;
                    }
                    maxDecoded = Math.max(maxDecoded, dist);
                }
            }

            int terrain = total - sky;
            MainRegistry.LOGGER.info("DhDepthBlitDiag: {}x{} sample of DH DEPTH32F @({},{}) (near={}, far={}, reverseZ={}, fadeMask={}): total={} sky/unwritten={} fade-mask-discard={} would-write={} (maxWrittenDist={})",
                    rw, rh, sx, sy, dhNear, dhFar, reverseZ, fadeMaskDist, total, sky, fade, write, (int) maxDecoded);
            if (terrain > 0 && write == 0) {
                MainRegistry.LOGGER.warn("DhDepthBlitDiag: ZERO fragments would be written ({} terrain texels, all discarded by the fade mask) - the DH->main depth copy is a silent NO-OP; check dh_depth_blit.fsh decode/uniforms", terrain);
            } else if (write > 0 && nearCollapsed == write) {
                MainRegistry.LOGGER.warn("DhDepthBlitDiag: all {} written texels decoded below {} blocks (3*near) - decode collapse signature (wrong convention/planes?)", write, (int) (dhNear * 3.0F));
            }
        } finally {
            if (scratch != -1) {
                GL11.glDeleteTextures(scratch);
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
            GlStateManager._activeTexture(prevActive);
            while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* clear stale */ }
        }
    }
}
