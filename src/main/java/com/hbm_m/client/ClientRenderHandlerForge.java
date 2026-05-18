//? if forge {
package com.hbm_m.client;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge-only event wiring for {@link ClientRenderHandler}.
 */
public final class ClientRenderHandlerForge {
    private ClientRenderHandlerForge() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientRenderHandler.onClientTickEnd();
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        var cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // GPU/CPU culling pipeline: update frustum and dispatch compute once
        // after all BlockEntities had a chance to call OcclusionCullingHelper.
        try {
            var cfg = com.hbm_m.config.ModClothConfig.get();
            if (cfg.enableOcclusionCulling
                    && cfg.cullingMode != com.hbm_m.config.ModClothConfig.CullingMode.LEGACY_RAYCAST
                    && !com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()) {

                org.joml.Matrix4f projection = event.getProjectionMatrix();
                org.joml.Matrix4f modelView = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix());
                org.joml.Matrix4f viewProj = new org.joml.Matrix4f(projection).mul(modelView);
                com.hbm_m.client.render.CpuFrustumCuller.updateFrustum(viewProj);

                if (cfg.useGpuCulling) {
                    if (!com.hbm_m.client.render.GpuCullingPipeline.isSupported()) {
                        com.hbm_m.client.render.GpuCullingPipeline.initialize();
                    }
                    if (com.hbm_m.client.render.GpuCullingPipeline.isSupported()) {
                        com.hbm_m.client.render.GpuCullingPipeline.dispatch(viewProj, cameraPos);
                    }
                }
            }
        } catch (Throwable ignored) {}

        ClientRenderHandler.onRenderWorldLate(mc.renderBuffers().bufferSource(), event.getPoseStack(), cameraPos);
    }
}
//?}

