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

    // Регистрируем нашу частицу как SimpleParticleType (без доп. данных)
    public static final RegistrySupplier<SimpleParticleType> DARK_PARTICLE = PARTICLES.register("dark_particle",
            () -> createParticle(true)); // true означает, что она всегда будет отрисовываться
    public static final RegistrySupplier<SimpleParticleType> SMOKE_COLUMN = PARTICLES.register("smoke_column",
            () -> createParticle(false));

    public static final RegistrySupplier<SimpleParticleType> EXPLOSION_WAVE = PARTICLES.register("explosion_wave",
            () -> createParticle(false));

    public static final RegistrySupplier<SimpleParticleType> RAD_FOG_PARTICLE = PARTICLES.register("rad_fog",
            () -> createParticle(true));

    // Контрейл баллистической ракеты
    public static final RegistrySupplier<SimpleParticleType> MISSILE_CONTRAIL = PARTICLES.register("missile_contrail",
            () -> createParticle(true));

    public static void init() {
        PARTICLES.register();
    }
}