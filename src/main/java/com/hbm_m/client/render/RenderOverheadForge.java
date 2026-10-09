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

/** Forge-Haken fuer {@link RenderOverhead} (Original: {@code RenderWorldLastEvent}). */
//? if forge {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(value = Dist.CLIENT)
*///?}
public final class RenderOverheadForge {
    private RenderOverheadForge() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        RenderOverhead.renderMarkers(event.getPoseStack(), event.getCamera());
    }
}
//?}
