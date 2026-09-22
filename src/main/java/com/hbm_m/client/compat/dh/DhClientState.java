package com.hbm_m.client.compat.dh;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import org.joml.Matrix4f;

import com.hbm_m.compat.dh.DhCompat;

/**
 * Frame-local DH rendering state.
 * Set by the DhRenderBridge event while DH's FBO is bound and depth contains LOD depth.
 * Used to:
 *  - skip far objects in vanilla passes (they go via DH path)
 *  - hold DH projection for the extended vanilla far pass.
 */
@OnlyIn(Dist.CLIENT)
public final class DhClientState {

    private static volatile boolean dhFboActive = false;
    private static volatile Matrix4f dhProjection = null;
    /** Клип-плоскости проекции DH (блоки) — для линеаризации их DEPTH32F
     *  при копировании глубины в главный буфер (DhDepthCopy). */
    private static volatile float dhNear = 0.05F;
    private static volatile float dhFar = 4096.0F;
    /**
     * Конвенция глубины DH-кадра. DH 3.3.1 перевёл GL-движок на REVERSE_Z
     * (GlDhRenderApiDefinition.getRenderDepth): без активного Iris-пака
     * DEPTH32F чистится в 0 (небо=0, близко=1), террейн растеризуется
     * реверс-матрицей (setClipPlanes(mat, far, near, true)). DH <= 3.2.x и
     * форвард-Z паки — FORWARD_Z. Приходит параметром в {@link #beginDhPass}
     * из DhRenderBridge.isReverseZDepthActive() (рефлексия RENDER_DEF — сам
     * DH матрицу события реверсной НЕ отдаёт, детект по знаку m32 ложно
     * срабатывал). Читают копирующие проходы (DhDepthCopy/RawDhDepthCopy)
     * для ветки декодирования.
     */
    private static volatile boolean dhReverseZ = false;
    /** Время последнего реального DH-кадра: защита от «залипшего» флага,
     *  если DH перестал рендерить (настройка/выгрузка), пока мод установлен. */
    private static volatile long lastBridgeMs = 0;

    private DhClientState() {}

    /** Called from DhRenderBridge BEFORE applyToMcTexture (DH FBO still bound). */
    public static void beginDhPass(Matrix4f proj, float near, float far, boolean irisLodOverrideActive, boolean reverseDepth) {
        dhProjection = proj != null ? new Matrix4f(proj) : null;
        // Конвенция глубины приходит из самого DH (RenderUtil.RENDER_DEF
        // .getRenderDepth(), см. DhRenderBridge.isReverseZDepthActive).
        // МАТРИЦА ИЗ СОБЫТИЯ для этого непригодна: в 3.3.1 это форвард-копия
        // с сырыми значениями, реальную реверс-матрицу DH строит отдельно.
        dhReverseZ = reverseDepth;
        // КЛИП-ПЛОСКОСТИ декода приходят ГОТОВЫМИ из моста:
        //  - нативный рендер — репликация RenderUtil.setDhProjectionMatrix
        //    (R-формула + кламп min(near, 7.5) без height-override);
        //  - под Iris-override — сырые rp-значения (Iris строит перспективу
        //    из них без клампа).
        // Матрица события НЕ используется: в 3.3.1 это форвард-копия с сырыми
        // значениями — кламп в ней не виден, и декод по rp.near при rd≥3
        // кодировал LOD-глубину в разы дальше реальной («гриб перед горами,
        // чем больше RD тем ближе»). В 3.2.x спасало то, что событие отдавало
        // мутнутый (клампнутый) матрикс — регресс именно 3.3.1.
        if (near > 0.0F && far > near) {
            dhNear = near;
            dhFar = far;
        }
        dhFboActive = true;
        lastBridgeMs = System.currentTimeMillis();
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

    /** true — глубина DH-кадра реверсивная (близко=1, небо=0), DH 3.3.1+. */
    public static boolean dhReverseZ() { return dhReverseZ; }
}
