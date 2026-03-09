package com.hbm_m.client;

import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.AgentOrangeParticle;
import com.hbm_m.particle.custom.MissileContrailParticle;
import com.hbm_m.particle.explosions.basic.*;
import com.hbm_m.particle.explosions.nuclear.small.DarkSmokeParticle;
import com.hbm_m.particle.explosions.nuclear.small.DarkWaveSmokeParticle;
import com.hbm_m.particle.explosions.nuclear.small.LargeDarkSmoke;
import com.hbm_m.particle.explosions.nuclear.small.LargeExplosionSpark;
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

        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.LARGE_EXPLOSION_SPARK.get(),
                LargeExplosionSpark.Provider::new);

        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.LARGE_DARK_SMOKE.get(),
                LargeDarkSmoke.Provider::new);

        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.DARK_WAVE_SMOKE.get(),
                DarkWaveSmokeParticle.Provider::new);

        // 🚀 Контрейл баллистической ракеты
        event.registerSpriteSet(
                ModParticleTypes.MISSILE_CONTRAIL.get(),
                MissileContrailParticle.Provider::new);

    }
}
