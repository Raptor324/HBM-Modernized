package com.hbm_m.client.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = RefStrings.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
*///?}
public class EngineHandler {

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ParticleEngineNT.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        com.mojang.blaze3d.pipeline.RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        mainTarget.bindWrite(false);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        FogRenderer.setupNoFog();

        //? if < 1.21.1 {
        float partialTick = event.getPartialTick();
        //?} else {
        /*float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        *///?}

        // ── Фаза 1: все облака/cloudlets + кости + пепел (с записью глубины) ──
        ParticleEngineNT.INSTANCE.render(buffer, event.getCamera(), partialTick, event.getPoseStack());
        buffer.endBatch();

        // ── Фаза 2: flash поверх (NO_DEPTH_TEST + ADDITIVE) ──
        ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), partialTick, event.getPoseStack());
        buffer.endBatch();

        // Восстанавливаем GL state как было до нас (weather/worldborder рассчитывают на false).
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
    }

    //? if forge {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().isPaused()) {
            ParticleEngineNT.INSTANCE.tick();
        }
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (!Minecraft.getInstance().isPaused()) {
            ParticleEngineNT.INSTANCE.tick();
        }
    }
    *///?}
}