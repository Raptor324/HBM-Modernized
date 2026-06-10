//? if forge {
package com.hbm_m.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-only event hook for {@link ChunkRadiationDebugRenderer}.
 *
 * Kept separate so the renderer itself stays loader-agnostic.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class ChunkRadiationDebugRendererForge {
    private ChunkRadiationDebugRendererForge() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        ChunkRadiationDebugRenderer.render(event.getPoseStack(), event.getCamera().getPosition());
    }
}
//?}

