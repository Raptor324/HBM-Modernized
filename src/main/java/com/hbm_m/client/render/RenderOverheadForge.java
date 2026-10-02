//? if forge {
package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-Haken fuer {@link RenderOverhead} (Original: {@code RenderWorldLastEvent}). */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class RenderOverheadForge {
    private RenderOverheadForge() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        RenderOverhead.renderMarkers(event.getPoseStack(), event.getCamera());
    }
}
//?}
