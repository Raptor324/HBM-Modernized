package com.hbm_m.client;

import com.hbm_m.particle.*;
import com.hbm_m.particle.explosions.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientParticleHandler {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Регистрируем провайдеры для каждого типа частиц
        event.registerSpriteSet(ModExplosionParticles.EXPLOSION_FLASH.get(),
                ExplosionFlashParticle.Provider::new);

        event.registerSpriteSet(ModExplosionParticles.SHOCKWAVE_RING.get(),
                ShockwaveRingParticle.Provider::new);

        event.registerSpriteSet(ModExplosionParticles.MUSHROOM_STEM_SMOKE.get(),
                MushroomStemSmokeParticle.Provider::new);

        event.registerSpriteSet(ModExplosionParticles.MUSHROOM_CAP_SMOKE.get(),
                MushroomCapSmokeParticle.Provider::new);

        event.registerSpriteSet(ModExplosionParticles.EXPLOSION_FIRE.get(),
                ExplosionFireParticle.Provider::new);
    }
}