package com.hbm_m.client.compat.dh;

import org.joml.Matrix4f;

import com.hbm_m.compat.dh.DhCompat;

/**
 * Frame-local DH rendering state.
 * Set by the DhRenderBridge event while DH's FBO is bound and depth contains LOD depth.
 * Used to:
 *  - skip far objects in vanilla passes (they go via DH path)
 *  - hold DH projection for the extended vanilla far pass.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
 *///?}
public final class DhClientState {

    private static volatile boolean dhFboActive = false;
    private static volatile Matrix4f dhProjection = null;
    /** Клип-плоскости проекции DH (блоки) — для линеаризации их DEPTH32F
     *  при копировании глубины в главный буфер (DhDepthCopy). */
    private static volatile float dhNear = 0.05F;
    private static volatile float dhFar = 4096.0F;
    /** Время последнего реального DH-кадра: защита от «залипшего» флага,
     *  если DH перестал рендерить (настройка/выгрузка), пока мод установлен. */
    private static volatile long lastBridgeMs = 0;
    private static long nearClampMismatchCounter = 0;

    private DhClientState() {}

    /** Called from DhRenderBridge BEFORE applyToMcTexture (DH FBO still bound). */
    public static void beginDhPass(Matrix4f proj, float near, float far, boolean irisLodOverrideActive) {
        dhProjection = proj != null ? new Matrix4f(proj) : null;
        // КЛИП-ПЛОСКОСТИ зависят от того, КТО растеризует LOD'ы:
        //
        // БЕЗ Iris-override LOD'ы рисует нативный DH-шейдер с матрицей, где
        // near ЗАКЛАМПЛЕН до 7.5 (RenderUtil.setDhProjectionMatrix), а в
        // событии лежит НЕклампленное значение (rd*16*overdraw/norm: 9.1@rd4,
        // 22.9@rd5). Декодируем инверсией ИЗ МАТРИЦЫ: n = B/(A-1), f = B/(A+1).
        //
        // ПОД Iris-override (пак с dhTerrain-программами) Iris САМ строит
        // проекцию LOD из СЫРЫХ rp-значений (LodRendererEvents: setPerspective(
        // fov, aspect, event.value.nearClipPlane, farClipPlane)) — матрица с
        // клампом вообще не используется. Декод по матрице давал
        // dist ≈ (7.5/22.9)·true ≈ 0.33·true @rd5: вся скопированная глубина
        // была втрое «ближе» → гриб резался LOD'ами за своей спиной. Декодируем
        // ровно те rp-значения, что уходят в setPerspective.
        float n = near, f = far;
        if (!irisLodOverrideActive && dhProjection != null) {
            float[] ext = extractClipPlanes(dhProjection);
            if (ext != null) {
                n = ext[0];
                f = ext[1];
                // DEFENSE-IN-DEPTH (near-clamp mismatch): нативный DH клампит
                // near матрицы до min(near, 7.5), а Iris рендерит глубину LOD
                // БЕЗ клампа (setPerspective с сырыми rp-значениями). Если пак
                // активен, а rp.near СИЛЬНО больше матричного — глубину писал
                // Iris, и декодировать надо rp-значениями: иначе вся дальняя
                // глубина декодируется как (7.5/n_real)·true «ближе», и
                // LOD-гора позади гриба перетирает его, ошибка ∝ дистанции
                // (при rd≤4 под паком near=7.34<7.5 кламп не срабатывал, чем
                // баг маскировался). Кейс «пак без dhTerrain-программ +
                // нативный рендер» деградирует мягко: такой пак Iris всё
                // равно лишает композита DH, честной DH-глубины не существует.
                if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                        && near > ext[0] * 1.001F) {
                    if (nearClampMismatchCounter++ % 600 == 0) {
                        com.hbm_m.main.MainRegistry.LOGGER.info(
                                "HBM DH near-clamp mismatch (native matrix near={} vs rp near={}): decoding with rp values",
                                String.format("%.2f", ext[0]), String.format("%.2f", near));
                    }
                    n = near;
                    f = far;
                }
            }
        }
        if (n > 0.0F && f > n) {
            dhNear = n;
            dhFar = f;
        }
        dhFboActive = true;
        lastBridgeMs = System.currentTimeMillis();
    }

    /**
     * Клип-плоскости стандартной перспективы из её матрицы:
     * A = m22 = (f+n)/(n-f), B = m32 = 2fn/(n-f)  ⇒  n = B/(A-1), f = B/(A+1).
     * null = матрица не похожа на перспективу (используем rp-значения как фолбэк).
     */
    private static float[] extractClipPlanes(Matrix4f p) {
        float a = p.m22(), b = p.m32();
        if (Math.abs(a - 1.0F) < 1.0E-4F || Math.abs(a + 1.0F) < 1.0E-6F || b >= 0.0F) return null;
        float n = b / (a - 1.0F);
        float f = b / (a + 1.0F);
        if (n <= 0.0F || f <= n || Float.isInfinite(f)) return null;
        return new float[] {n, f};
    }

    public static void endDhPass() {
        dhFboActive = false;
    }

    /** Called at AFTER_LEVEL to age the flag. */
    public static void onAfterLevel() {
        // If DH was not present this frame, we want isActive() to become false next frame.
        // Keep flag until next frame's start, then it will be overwritten if DH renders again.
        // For now clear after level so that if DH stops rendering for one frame, next frame's
        // vanilla pass won't incorrectly think DH is active before the bridge check.
        // However missiles use isActive() at AFTER_ENTITIES which is BEFORE AFTER_LEVEL of same frame,
        // so they need the flag set by the bridge earlier in same frame. Hence we do NOT clear here
        // synchronously for that use — we clear on next AFTER_SKY.
    }

    public static void onAfterSky() {
        // РАНЬШЕ здесь сбрасывался dhRenderedThisFrame (если !dhFboActive), но
        // endDhPass() вызывается сразу после beginDhPass() — флаг fboActive к
        // этому моменту всегда false, поэтому сброс происходил КАЖДЫЙ кадр.
        // В итоге isActive() на AFTER_WEATHER означало «мост успел выстрелить
        // раньше этой фазы», что зависело от того, рендерит ли DH до или после
        // погоды в данном кадре → dhActive мигал окнами по десятки секунд,
        // дёргая fallback-виртуализацию дальнего контента («гриб улетает»).
        // Теперь факт DH-кадра определяется ТОЛЬКО свежестью моста в isActive().
    }

    public static boolean isActive() {
        if (!DhCompat.isModPresent()) return false;
        // Свежесть: флаг ставится каждый DH-кадр; если DH перестал рендерить,
        // через 500 мс считаем его неактивным. Этого достаточно: мост стреляет
        // каждый кадр, пока DH реально рисует LOD'ы, независимо от того,
        // какая фаза ванильного кадра идёт раньше.
        return System.currentTimeMillis() - lastBridgeMs < 500;
    }

    public static Matrix4f dhProjection() { return dhProjection; }

    public static float dhNear() { return dhNear; }

    public static float dhFar() { return dhFar; }
}
