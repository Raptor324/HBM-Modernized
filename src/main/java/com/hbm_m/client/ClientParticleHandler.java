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

import dev.architectury.registry.client.particle.ParticleProviderRegistry.DeferredParticleProvider;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

//? if forge {
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
*///?}

@OnlyIn(Dist.CLIENT)
public class ClientParticleHandler {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /**
     * Единая регистрация провайдеров частиц. Вызывается из фазы
     * конструктора мода (MainRegistry.init): на NeoForge 1.21.1 Register-события стреляют
     * раньше FMLClientSetupEvent, поэтому поздняя регистрация из client setup опаздывает.
     *
     * ВАЖНО: НЕ использовать фасад {@code ParticleProviderRegistry} — в Architectury
     * 1.21-ветки (neoforge 13.0.11) у прямых оверлоадов register(ParticleType, ...) в
     * апстриме закомментирован @ExpectPlatform и оставлены пустые тела, фасад — тихий
     * no-op (architectury-api#621). Зовём платформенный impl напрямую: он лежит под одним
     * FQCN в обоих артефактах (architectury-forge 9.2.14 / architectury-neoforge 13.0.11),
     * внутри — тот же deferred-механизм с применением на RegisterParticleProvidersEvent.
     * При обновлении Architectury переверить сигнатуры (javap по jar из gradle-кэша).
     */
    public static void init() {

        // ФУГАСНЫЕ И ЯДЕРНЫЕ ВЗРЫВНЫЕ ЧАСТИЦЫ

        // ВСПЫШКА (яркий белый свет)
        spriteSet(ModExplosionParticles.EXPLOSION_FLASH, ExplosionFlashParticle.Provider::new);

        // ИСКРЫ (оранжевые разлетающиеся)
        spriteSet(ModExplosionParticles.EXPLOSION_SPARK, ExplosionSparkParticle.Provider::new);

        // КОЛЬЦО
        spriteSet(ModExplosionParticles.SHOCKWAVE_RING, ShockwaveRingParticle.Provider::new);

        // ГРИБОВИДНЫЙ ДЫМ (серый дым)
        spriteSet(ModExplosionParticles.MUSHROOM_SMOKE, MushroomSmokeParticle.Provider::new);

        //ТЁМНЫЙ ДЫМ
        spriteSet(ModExplosionParticles.DARK_SMOKE, DarkSmokeParticle.Provider::new);

        // ДЫМОВАЯ ВОЛНА
        spriteSet(ModExplosionParticles.WAVE_SMOKE, WaveSmokeParticle.Provider::new);

        // ОГОНЬ (основание взрыва)
        spriteSet(ModExplosionParticles.EXPLOSION_FIRE, ExplosionFireParticle.Provider::new);

        // AGENT ORANGE
        spriteSet(ModExplosionParticles.AGENT_ORANGE, AgentOrangeParticle.Provider::new);

        spriteSet(ModExplosionParticles.FIRE_SPARK, FireSparkParticle.Provider::new);

        spriteSet(ModExplosionParticles.LARGE_EXPLOSION_SPARK, LargeExplosionSpark.Provider::new);

        spriteSet(ModExplosionParticles.LARGE_DARK_SMOKE, LargeDarkSmoke.Provider::new);

        spriteSet(ModExplosionParticles.DARK_WAVE_SMOKE, DarkWaveSmokeParticle.Provider::new);

        // РАКЕТНЫЕ И СПЕЦИАЛЬНЫЕ ЧАСТИЦЫ
        spriteSet(ModParticleTypes.MISSILE_CONTRAIL, MissileContrailParticle.Provider::new);

        spriteSet(ModParticleTypes.MISSILE_VAPOR_CONTRAIL, MissileVaporContrailParticle.Provider::new);

        spriteSet(ModParticleTypes.MISSILE_NOZZLE_FLARE, MissileNozzleFlareParticle.Provider::new);

        spriteSet(ModParticleTypes.TOM_GLOW, TomGlowParticle.Provider::new);

        spriteSet(ModParticleTypes.SMOKE_COLUMN, SmokeColumnParticle.Provider::new);

        spriteSet(ModParticleTypes.HADRON, com.hbm_m.particle.custom.HadronParticle.Provider::new);

        spriteSet(ModParticleTypes.TOWNAURA, TownauraParticle.Provider::new);

        spriteSet(ModParticleTypes.SCHRABFOG, SchrabfogParticle.Provider::new);

        spriteSet(ModParticleTypes.RAD_FOG_PARTICLE, RadFogParticle.Provider::new);

        spriteSet(ModParticleTypes.MIST, com.hbm_m.particle.custom.MistParticle.Provider::new);

        spriteSet(ModParticleTypes.RBMK_FLAME, com.hbm_m.particle.custom.RBMKFlameParticle.Provider::new);

        spriteSet(ModParticleTypes.RBMK_STEAM, com.hbm_m.particle.custom.RBMKSteamParticle.Provider::new);

        spriteSet(ModParticleTypes.RBMK_MUSH, com.hbm_m.particle.custom.RBMKMushParticle.Provider::new);

        spriteSet(ModParticleTypes.DIGAMMA_SMOKE, com.hbm_m.particle.custom.DigammaSmokeParticle.Provider::new);

        // Самопроверка обхода Architectury (architectury-api#621): к моменту загрузки мира
        // все регистрации обязаны пройти. Если нет — громко, а не немым no-op.
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> {
            if (com.hbm_m.particle.nt.MissileContrailNT.sprites == null) {
                LOGGER.error("Particle providers were NOT registered (MissileContrailNT.sprites == null) — "
                        + "all custom particles will be invisible. ParticleProviderRegistry workaround failed, "
                        + "check Architectury version internals.");
            }
        });
    }

    /**
     * ModExplosionParticles объявлены как {@code RegistrySupplier<ParticleType<?>>} — прямой
     * вызов generic-оверлоада не выводит T. Приводим supplier к raw-типу внутри хелпера.
     *
     * Фаза конструктора мода: реестры ещё не забейкены, {@code type.get()} здесь кидает
     * «Registry Object not present» (лог 2026-09-28). Поэтому — как и задумывал фасад
     * Architectury — откладываем до резолва записи через {@code listen}, но платформенный
     * impl зовём напрямую, минуя битые пустые оверлоады фасада (architectury-api#621).
     * listen срабатывает при регистрации записи (RegisterEvent) — это ДО
     * RegisterParticleProvidersEvent, т.е. Impl-гейт deferred-списка ещё активен.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends ParticleOptions> void spriteSet(RegistrySupplier<? extends ParticleType<?>> type,
                                                              DeferredParticleProvider<T> provider) {
        type.listen(resolved -> {
            try {
                dev.architectury.registry.client.particle.forge.ParticleProviderRegistryImpl.register(
                        (ParticleType) resolved, provider);
            } catch (Throwable t) {
                LOGGER.error("Failed to register particle provider for {}: {}", type.getId(), t.toString());
            }
        });
    }
}
