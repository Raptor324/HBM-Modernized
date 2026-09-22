package com.hbm_m.compat.simulated;

/**
 * Client detection of the Simulated mod's "contraption diagram" render (the graphite-sketch
 * screen of {@code contraption_diagram}). During capture ({@code SimpleSubLevelGroupRenderer.RENDERING_SIMPLE})
 * block entities are drawn through the vanilla dispatcher into a shared buffer source while an
 * offscreen FBO with a fake orthographic projection is bound - our deferred machine paths
 * (instancing / single-VBO) flush only in the main world pass with the main camera, so machines
 * would be invisible in the sketch. Callers must switch to the immediate putBulkData path while
 * this is true.
 *
 * <p>Reflection is used because Simulated is an optional runtime dependency; absence simply
 * makes this return {@code false}.
 */
public final class DiagramRenderCompat {

    private static boolean checked;
    private static java.lang.reflect.Field flagField;

    private DiagramRenderCompat() {}

    // Covers the terrain-phase capture flag AND the block-entity phase. RENDERING_SIMPLE is
    // reset before BEs render (SimpleSubLevelGroupRenderer line ~160), and the BE/entity phase
    // runs from DiagramScreen's own render pass - so an open Simulated DiagramScreen is the only
    // signal that spans it. While the screen is open the main world pass also renders machines
    // behind it; forcing immediate there for one screen's lifetime is harmless.
    private static final String DIAGRAM_SCREEN_CLASS =
            "dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen";

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
        try {
            if (flagField != null && flagField.getBoolean(null)) {
                return true;
            }
        } catch (Throwable t) {
            // fall through to the screen check
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        return mc.screen != null && DIAGRAM_SCREEN_CLASS.equals(mc.screen.getClass().getName());
    }
}
