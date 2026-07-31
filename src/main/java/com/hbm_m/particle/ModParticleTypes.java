package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public class ModParticleTypes {
    // Создаем DeferredRegister для типов частиц
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(RefStrings.MODID, Registries.PARTICLE_TYPE);

    // Вспомогательный кроссплатформенный метод для создания SimpleParticleType
    private static SimpleParticleType createParticle(boolean alwaysShow) {
        //? if fabric {
        /*return net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(alwaysShow);
        *///?} else {
        return new SimpleParticleType(alwaysShow);
         //?}
    }

    /** Town aura (радиоблоки, обеззараживатель). Порт {@code townaura} / {@code EntityAuraFX} (1.7.10). */
    public static final RegistrySupplier<SimpleParticleType> TOWNAURA = PARTICLES.register("townaura",
            () -> createParticle(true));
    /** Schrab aura (шрабидиевые слитковые блоки). Порт {@code schrabfog} / {@code EntityAuraFX} (1.7.10). */
    public static final RegistrySupplier<SimpleParticleType> SCHRABFOG = PARTICLES.register("schrabfog",
            () -> createParticle(true));
    public static final RegistrySupplier<SimpleParticleType> SMOKE_COLUMN = PARTICLES.register("smoke_column",
            () -> createParticle(false));

    public static final RegistrySupplier<SimpleParticleType> EXPLOSION_WAVE = PARTICLES.register("explosion_wave",
            () -> createParticle(false));

    public static final RegistrySupplier<SimpleParticleType> RAD_FOG_PARTICLE = PARTICLES.register("rad_fog",
            () -> createParticle(true));

    // Контрейл баллистической ракеты (огненный выхлоп)
    public static final RegistrySupplier<SimpleParticleType> MISSILE_CONTRAIL = PARTICLES.register("missile_contrail",
            () -> createParticle(true));

    /** Серый конденсационный след (дольше огненного, расплывается). */
    public static final RegistrySupplier<SimpleParticleType> MISSILE_VAPOR_CONTRAIL = PARTICLES.register("missile_vapor_contrail",
            () -> createParticle(true));

    /** Блик работающего двигателя у сопла (flash + flare). */
    public static final RegistrySupplier<SimpleParticleType> MISSILE_NOZZLE_FLARE = PARTICLES.register("missile_nozzle_flare",
            () -> createParticle(true));

    /** Blue glow trailing Gerald's meteor ({@link com.hbm_m.entity.projectile.TomEntity}). */
    public static final RegistrySupplier<SimpleParticleType> TOM_GLOW = PARTICLES.register("tom_glow",
            () -> createParticle(true));

    public static void init() {
        PARTICLES.register();
    }
}