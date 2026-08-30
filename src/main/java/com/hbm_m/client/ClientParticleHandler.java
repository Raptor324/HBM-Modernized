package com.hbm_m.client;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.AgentOrangeParticle;
import com.hbm_m.particle.custom.MissileContrailParticle;
import com.hbm_m.particle.custom.MissileNozzleFlareParticle;
import com.hbm_m.particle.custom.MissileVaporContrailParticle;
import com.hbm_m.particle.custom.RadFogParticle;
import com.hbm_m.particle.custom.SchrabfogParticle;
import com.hbm_m.particle.custom.SmokeColumnParticle;
import com.hbm_m.particle.custom.TomGlowParticle;
import com.hbm_m.particle.custom.TownauraParticle;
import com.hbm_m.particle.explosions.basic.ExplosionFireParticle;
import com.hbm_m.particle.explosions.basic.ExplosionFlashParticle;
import com.hbm_m.particle.explosions.basic.ExplosionSparkParticle;
import com.hbm_m.particle.explosions.basic.FireSparkParticle;
import com.hbm_m.particle.explosions.basic.MushroomSmokeParticle;
import com.hbm_m.particle.explosions.basic.ShockwaveRingParticle;
import com.hbm_m.particle.explosions.basic.WaveSmokeParticle;
import com.hbm_m.particle.explosions.nuclear.small.DarkSmokeParticle;
import com.hbm_m.particle.explosions.nuclear.small.DarkWaveSmokeParticle;
import com.hbm_m.particle.explosions.nuclear.small.LargeDarkSmoke;
import com.hbm_m.particle.explosions.nuclear.small.LargeExplosionSpark;

import net.minecraft.core.particles.SimpleParticleType;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
@EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
*///?}
public class ClientParticleHandler {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {


        // ФУГАСНЫЕ И ЯДЕРНЫЕ ВЗРЫВНЫЕ ЧАСТИЦЫ

        // ВСПЫШКА (яркий белый свет)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_FLASH.get(),
                ExplosionFlashParticle.Provider::new);

        // ИСКРЫ (оранжевые разлетающиеся)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get(),
                ExplosionSparkParticle.Provider::new);

        // КОЛЬЦО
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.SHOCKWAVE_RING.get(),
                ShockwaveRingParticle.Provider::new);

        // ГРИБОВИДНЫЙ ДЫМ (серый дым)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.MUSHROOM_SMOKE.get(),
                MushroomSmokeParticle.Provider::new);

        //ТЁМНЫЙ ДЫМ
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get(),
                DarkSmokeParticle.Provider::new);

        // ДЫМОВАЯ ВОЛНА
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.WAVE_SMOKE.get(),
                WaveSmokeParticle.Provider::new);

        // ОГОНЬ (основание взрыва)
        event.registerSpriteSet(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_FIRE.get(),
                ExplosionFireParticle.Provider::new);

        // AGENT ORANGE
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


        // РАКЕТНЫЕ И СПЕЦИАЛЬНЫЕ ЧАСТИЦЫ
        event.registerSpriteSet(
                ModParticleTypes.MISSILE_CONTRAIL.get(),
                MissileContrailParticle.Provider::new);

        event.registerSpriteSet(
                ModParticleTypes.MISSILE_VAPOR_CONTRAIL.get(),
                MissileVaporContrailParticle.Provider::new);

        event.registerSpriteSet(
                ModParticleTypes.MISSILE_NOZZLE_FLARE.get(),
                MissileNozzleFlareParticle.Provider::new);

        event.registerSpriteSet(
                ModParticleTypes.TOM_GLOW.get(),
                TomGlowParticle.Provider::new);

        event.registerSpriteSet(
                ModParticleTypes.SMOKE_COLUMN.get(),
                SmokeColumnParticle.Provider::new);

        event.registerSpriteSet(
                ModParticleTypes.TOWNAURA.get(),
                TownauraParticle.Provider::new);

        event.registerSpriteSet(
                ModParticleTypes.SCHRABFOG.get(),
                SchrabfogParticle.Provider::new);

        event.registerSpriteSet(
                ModParticleTypes.RAD_FOG_PARTICLE.get(),
                RadFogParticle.Provider::new);
    }
}