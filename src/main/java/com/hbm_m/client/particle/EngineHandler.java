package com.hbm_m.client.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.nt.ParticleEngineNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EngineHandler {

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ParticleEngineNT.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            ParticleEngineNT.INSTANCE.render(buffer, event.getCamera(), event.getPartialTick(), event.getPoseStack());
            buffer.endBatch();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), event.getPartialTick(), event.getPoseStack());
            buffer.endBatch();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().isPaused()) {
            ParticleEngineNT.INSTANCE.tick();
        }
    }
}
