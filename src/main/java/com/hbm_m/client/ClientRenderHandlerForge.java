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
        ClientRenderHandler.onRenderWorldLate(mc.renderBuffers().bufferSource(), event.getPoseStack(), cameraPos);
    }
}
//?}

