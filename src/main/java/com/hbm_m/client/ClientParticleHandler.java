package com.hbm_m.client;

import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientParticleHandler {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {

        // ════════════════════════════════════════════════════════════════
        // 💣 ФУГАСНЫЕ ВЗРЫВНЫЕ ЧАСТИЦЫ
        // ════════════════════════════════════════════════════════════════

        // 🔥 ВСПЫШКА (яркий белый свет)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_FLASH.get(),
                ExplosionFlashParticle.Provider::new);

        // ⚡ ИСКРЫ (оранжевые разлетающиеся)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get(),
                ExplosionSparkParticle.Provider::new);

        // 🌊 ШОКВОЛНА (кольца расширения)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.SHOCKWAVE_RING.get(),
                ShockwaveRingParticle.Provider::new);

        // 💨 ГРИБОВИДНЫЙ ДЫМ (серый дым)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.MUSHROOM_SMOKE.get(),
                MushroomSmokeParticle.Provider::new);

        // 💨 ТЁМНЫЙ ДЫМ
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get(),
                DarkSmokeParticle.Provider::new);

        // 💨 ВОЛНОВОЙ ДЫМ
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.WAVE_SMOKE.get(),
                WaveSmokeParticle.Provider::new);

        // 🔥 ОГОНЬ (основание взрыва)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_FIRE.get(),
                ExplosionFireParticle.Provider::new);

        // ☠️ AGENT ORANGE
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.AGENT_ORANGE.get(),
                AgentOrangeParticle.Provider::new);

        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.FIRE_SPARK.get(),
                FireSparkParticle.Provider::new);

    }
}
