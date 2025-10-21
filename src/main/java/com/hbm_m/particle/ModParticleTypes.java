package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticleTypes {
    // Создаем DeferredRegister для типов частиц
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, RefStrings.MODID);

    // Регистрируем нашу частицу как SimpleParticleType (без доп. данных)
    public static final RegistryObject<SimpleParticleType> DARK_PARTICLE = PARTICLES.register("dark_particle",
            () -> new SimpleParticleType(true)); // true означает, что она всегда будет отрисовываться
    public static final RegistryObject<SimpleParticleType> SMOKE_COLUMN = PARTICLES.register("smoke_column",
            () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> EXPLOSION_WAVE = PARTICLES.register("explosion_wave",
            () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> RAD_FOG_PARTICLE = PARTICLES.register("rad_fog",
            () -> new SimpleParticleType(true));

    // Этот метод должен вызываться в главном классе вашего мода
    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}