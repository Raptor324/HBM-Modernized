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

    /**
     * Не регистрируется на EVENT_BUS в текущем билде: MDI → {@code OcclusionCullingHelper.runGpuCullingAfterBlockEntities}
     * и подсветка мира выполняются в {@link com.hbm_m.event.ClientModEvents#onRenderLevelStage}.
     * Оставлено как эталон «позднего» хука без дублирующего {@code GpuCullingPipeline.dispatch} (иначе двойной dispatch).
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        ClientRenderHandler.onRenderWorldLate(mc.renderBuffers().bufferSource(), event.getPoseStack(), cameraPos);
    }
}
//?}

