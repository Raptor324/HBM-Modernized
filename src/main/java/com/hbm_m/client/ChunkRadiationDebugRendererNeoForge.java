//? if neoforge {
/*package com.hbm_m.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/^*
 * NeoForge-only event hook for {@link ChunkRadiationDebugRenderer}.
 *
 * Зеркалирует {@link ChunkRadiationDebugRendererForge} — держит загрузчик-специфичный
 * event hook отдельно, чтобы сам рендерер оставался loader-agnostic.
 ^/
@EventBusSubscriber(value = Dist.CLIENT)
public final class ChunkRadiationDebugRendererNeoForge {
    private ChunkRadiationDebugRendererNeoForge() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        ChunkRadiationDebugRenderer.render(event.getPoseStack(), event.getCamera().getPosition());
    }
}
*///?}
