//? if forge {
package com.hbm_m.client.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
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

        // ВАЖНО: Forge диспатчит AFTER_WEATHER ВНУТРИ блока RenderSystem.depthMask(false),
        // который занавешивает фазу weather (см. LevelRenderer стр. 1437-1444 в src 1.20.1).
        // Пока depthMask=false, НИ ОДНА из наших частиц (пепел, кости) не пишет глубину
        // — отсюда "пепел поверх костей" и "дырявый череп": depth-test LEQUAL проходит
        // против мира, но не между самими частицами.
        // Временно включаем запись глубины на время рендера, потом восстанавливаем false,
        // чтобы world-border сразу после нас не начал писать глубину (он рассчитывает на false).
        com.mojang.blaze3d.pipeline.RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        mainTarget.bindWrite(false);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        FogRenderer.setupNoFog();

        // ── Фаза 1: все облака/cloudlets + кости + пепел (с писью глубины) ──
        ParticleEngineNT.INSTANCE.render(buffer, event.getCamera(), event.getPartialTick(), event.getPoseStack());
        buffer.endBatch();

        // ── Фаза 2: flash поверх (NO_DEPTH_TEST + ADDITIVE) ──
        ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), event.getPartialTick(), event.getPoseStack());
        buffer.endBatch();

        // Восстанавливаем GL state как было до нас (weather/worldborder рассчитывают на false).
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().isPaused()) {
            ParticleEngineNT.INSTANCE.tick();
        }
    }
}
//?}

//? if fabric {
/*package com.hbm_m.client.particle;

/^*
 * Fabric: Forge event subscriber isn't available here yet.
 * This is a stub to keep compilation working across loaders.
 ^/
public class EngineHandler { }
*///?}
