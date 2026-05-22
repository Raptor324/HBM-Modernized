package com.hbm_m.client.render;

import net.minecraft.client.Minecraft;

/**
 * Per-render-frame lightmap update guard. {@link net.minecraft.client.renderer.LightTexture#updateLightTexture}
 * is expensive when invoked from every {@link SingleMeshVboRenderer#prepareBlockLitSamplers} /
 * instanced draw; call {@link #ensureLightTextureUpdated()} at most once per client render frame.
 */
public final class RenderFrameLight {

    private static int frameSerial;
    private static int lastUpdatedFrame = -1;

    private RenderFrameLight() {}

    /** Start of a new client render frame (before block-entity BER pass). */
    public static void onFrameStart() {
        frameSerial++;
    }

    /** Updates the dynamic lightmap texture once per {@link #onFrameStart} frame. */
    public static void ensureLightTextureUpdated() {
        if (lastUpdatedFrame == frameSerial) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer != null) {
            mc.gameRenderer.lightTexture().updateLightTexture(mc.getFrameTime());
        }
        lastUpdatedFrame = frameSerial;
    }
}
