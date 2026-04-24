package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModParticleTypes {
    // Создаем DeferredRegister для типов частиц
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(RefStrings.MODID, Registries.PARTICLE_TYPE);

    // Регистрируем нашу частицу как SimpleParticleType (без доп. данных)
    public static final RegistrySupplier<SimpleParticleType> DARK_PARTICLE = PARTICLES.register("dark_particle",
            () -> new SimpleParticleType(true)); // true означает, что она всегда будет отрисовываться
    public static final RegistrySupplier<SimpleParticleType> SMOKE_COLUMN = PARTICLES.register("smoke_column",
            () -> new SimpleParticleType(false));

    public static final RegistrySupplier<SimpleParticleType> EXPLOSION_WAVE = PARTICLES.register("explosion_wave",
            () -> new SimpleParticleType(false));

    public static final RegistrySupplier<SimpleParticleType> RAD_FOG_PARTICLE = PARTICLES.register("rad_fog",
            () -> new SimpleParticleType(true));

    // Контрейл баллистической ракеты
    public static final RegistrySupplier<SimpleParticleType> MISSILE_CONTRAIL = PARTICLES.register("missile_contrail",
            () -> new SimpleParticleType(true));

    public static void init() {
        PARTICLES.register();
    }
}