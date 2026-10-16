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
        if (!checked) {
            checked = true;
            try {
                flagField = Class.forName("dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer")
                        .getField("RENDERING_SIMPLE");
            } catch (Throwable t) {
                flagField = null;
            }
        }
        // Simulated not installed — no capture can ever run. Must NOT fall through to the
        // projection heuristic below: an orthographic projection occurs in level passes
        // without Simulated too, and each one of those would switch the whole machine
        // pipeline to putBulkData (see the shadow-pass note).
        if (flagField == null) {
            return false;
        }
        try {
            if (flagField.getBoolean(null)) {
                return true;
            }
        } catch (Throwable t) {
            // fall through to the projection check
        }
        // The Iris shadow-map projection is orthographic as well (sun camera, m33 != 0),
        // indistinguishable from the capture's ortho by matrix shape alone. Without this
        // gate the ENTIRE shadow BE pass degrades to putBulkData (73% of the frame,
        // Oculus profile 2026-09-27). The capture never runs inside Iris's shadow phase
        // (it is screen-driven with its own buffer source), so suppressing the heuristic
        // there is safe. Cheap: MethodHandle against Iris's cached state, already called
        // several times per machine part per frame.
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false;
        }
        org.joml.Matrix4f projection = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
        return projection != null && projection.m33() != 0.0f;
    }
}
