package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings; // Убедитесь, что RefStrings.MODID правильный
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

    // Этот метод должен вызываться в главном классе вашего мода
    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}