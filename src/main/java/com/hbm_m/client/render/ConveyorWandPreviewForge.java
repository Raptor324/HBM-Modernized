//? if forge {
package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-Hook fuer {@link ConveyorWandPreview} (Original: RenderWorldLastEvent -> RenderOverhead.renderActionPreview). */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class ConveyorWandPreviewForge {
    private ConveyorWandPreviewForge() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        ConveyorWandPreview.render(event.getPoseStack(), event.getCamera().getPosition());
    }
}
//?}
