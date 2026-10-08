//? if forge || neoforge {
package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?}

/** Forge-Hook fuer {@link ConveyorWandPreview} (Original: RenderWorldLastEvent -> RenderOverhead.renderActionPreview). */
//? if forge {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(value = Dist.CLIENT)
*///?}
public final class ConveyorWandPreviewForge {
    private ConveyorWandPreviewForge() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        ConveyorWandPreview.render(event.getPoseStack(), event.getCamera().getPosition());
    }
}
//?}
