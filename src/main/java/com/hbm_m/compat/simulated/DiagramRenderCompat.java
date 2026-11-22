package com.hbm_m.compat.simulated;

/**
 * Client detection of the Simulated mod's "contraption diagram" render (the graphite-sketch
 * screen of {@code contraption_diagram}). During capture block entities are drawn through the
 * vanilla dispatcher into a shared buffer source while an offscreen FBO with a fake orthographic
 * projection is bound - our deferred machine paths (instancing / single-VBO) flush only in the
 * main world pass with the main camera, so machines would be invisible in the sketch. Callers
 * must switch to the immediate putBulkData path while this is true.
 *
 * <p>The detection must cover ONLY the capture itself, not the whole lifetime of the open
 * screen: {@code SimpleSubLevelGroupRenderer.RENDERING_SIMPLE} covers just the terrain phase
 * (it is reset before BEs render), but every capture call site renders BEs/entities with
 * {@code RenderSystem}'s projection swapped to an orthographic matrix (restored in a
 * {@code finally}). The main world pass - machines behind the open GUI and on physical ships -
 * uses the perspective matrix, so testing the projection instead of the open screen keeps the
 * world on the instanced path while the sketch is drawn.
 *
 * <p>Reflection is used because Simulated is an optional runtime dependency; absence simply
 * makes this return {@code false} without consulting the projection at all.
 */
public final class DiagramRenderCompat {

    private static boolean checked;
    private static java.lang.reflect.Field flagField;

    // Memo for enqueue(), which consults this per machine PART per frame (tens of
    // thousands of calls on a big farm): a per-call reflective Field.getBoolean
    // plus projection probe cost 1.9% of the frame on the 5000-machine fleet
    // (spark 2026-10-07). The memo key is the pass signature (frame stamp,
    // projection ortho-ness, shadow pass), NOT the frame stamp alone: the capture
    // runs late in the SAME render frame as the main pass, when renderFrame has
    // already been stamped by the main pass - a stamp-only memo kept returning the
    // main pass's "false" all through the capture, the capture went down the
    // instanced path and its records (capture basis) flushed into the world on the
    // next frame (phantom machine beside the real one, alternating = flicker).
    // The signature reads are three cheap calls; the reflective read runs once per
    // signature flip (a handful per frame) - NOT keyed on the frame stamp alone:
    // renderFrame is bumped inside InstancedRenderFrame.onBeforeBlockEntities, i.e.
    // downstream of the AFTER_ENTITIES gate, so the first main-pass consult of frame
    // N+1 still sees stamp N and a stamp-keyed simpleMemo would keep the capture's
    // stale true forever (the gate then blocks the very bump that would refresh it).
    // Simulated-absent short-circuits before any of it.
    private static long memoStamp = -1L;
    private static boolean memoOrtho;
    private static boolean memoShadowPass;
    private static boolean memoValue;

    private DiagramRenderCompat() {}

    // RENDERING_SIMPLE covers only the terrain phase of the capture; the BE/entity phase runs
    // after it is reset (SimpleSubLevelGroupRenderer line ~160). Every capture call site
    // (DiagramScreen, DiagramStickyNote, EndSeaShadowRenderer) builds its projection with
    // Matrix4f#ortho and installs it via GameRenderer#resetProjectionMatrix →
    // RenderSystem.setProjectionMatrix, restoring the previous matrix in a finally. So while
    // BEs render inside the capture, RenderSystem's projection is orthographic (m33 == 1);
    // the main world pass renders machines with the perspective matrix (m33 == 0) and stays
    // on the deferred paths.
    public static boolean isRenderingDiagram() {
        if (flagField == null) {
            // Simulated not installed - no capture can ever run (checked lazily once).
            ensureResolved();
            if (flagField == null) {
                return false;
            }
        }
        org.joml.Matrix4f projection = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
        boolean projOrtho = projection != null && projection.m33() != 0.0f;
        boolean shadowPass =
                com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass();
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (frame != memoStamp || projOrtho != memoOrtho || shadowPass != memoShadowPass) {
            memoStamp = frame;
            memoOrtho = projOrtho;
            memoShadowPass = shadowPass;
            // Same verdict as the old probe: RENDERING_SIMPLE is authoritative, the
            // ortho projection is the capture's own BE-phase signal (suppressed in
            // the Iris shadow pass - its sun camera is orthographic too, which the
            // matrix shape alone cannot distinguish; unsuppressed it degrades the
            // ENTIRE shadow BE pass to putBulkData, 73% of the frame, Oculus
            // profile 2026-09-27). The capture never runs inside Iris's shadow
            // phase (screen-driven, own buffer source), so the suppression is safe.
            memoValue = readRenderingSimple() || (!shadowPass && projOrtho);
        }
        return memoValue;
    }

    /** Reflective RENDERING_SIMPLE read; false on any failure. */
    private static boolean readRenderingSimple() {
        try {
            return flagField.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Resolves the reflective field once (first consult). */
    private static void ensureResolved() {
        if (!checked) {
            checked = true;
            try {
                flagField = Class.forName("dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer")
                        .getField("RENDERING_SIMPLE");
            } catch (Throwable t) {
                flagField = null;
            }
        }
    }
}
