package com.hbm_m.client.render;

import net.minecraft.client.Minecraft;

/**
 * Per-render-frame lightmap update guard. {@link net.minecraft.client.renderer.LightTexture#updateLightTexture}
 * is expensive when invoked from every {@link SingleMeshVboRenderer#prepareBlockLitSamplers} /
 * instanced draw; call {@link #ensureLightTextureUpdated()} at most once per client render frame.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
            //? if < 1.21.1 {
            mc.gameRenderer.lightTexture().updateLightTexture(mc.getFrameTime());
            //?} else {
            /*// 1.21.1: getPartialTick() удалён — частичное время тика через DeltaTracker.Timer.
            mc.gameRenderer.lightTexture().updateLightTexture(mc.getTimer().getGameTimeDeltaPartialTick(true));
            *///?}
        }
        lastUpdatedFrame = frameSerial;
    }
}
