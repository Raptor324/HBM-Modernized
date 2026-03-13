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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        // ── Фаза 1: все облака/cloudlets ──
        ParticleEngineNT.INSTANCE.render(buffer, event.getCamera(), event.getPartialTick(), event.getPoseStack());
        buffer.endBatch();   // ← GPU draw: облака уже на экране

        // ── Фаза 2: flash поверх (NO_DEPTH_TEST + ADDITIVE) ──
        ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), event.getPartialTick(), event.getPoseStack());
        buffer.endBatch();   // ← GPU draw: flash гарантированно поверх
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().isPaused()) {
            ParticleEngineNT.INSTANCE.tick();
        }
    }
}
