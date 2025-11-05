package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Регистрация кастомных типов частиц для эффектов взрыва
 */
public class ModExplosionParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, RefStrings.MODID);

    // Яркая вспышка при детонации
    public static final RegistryObject<SimpleParticleType> EXPLOSION_FLASH =
            PARTICLE_TYPES.register("explosion_flash",
                    () -> new SimpleParticleType(true));

    // Частицы для взрывной волны
    public static final RegistryObject<SimpleParticleType> SHOCKWAVE_RING =
            PARTICLE_TYPES.register("shockwave_ring",
                    () -> new SimpleParticleType(true));

    // Яркая вспышка
    public static final RegistryObject<SimpleParticleType> FLASH =
            PARTICLE_TYPES.register("flash", () -> new SimpleParticleType(true));

    // Взрывная волна
    public static final RegistryObject<SimpleParticleType> SHOCKWAVE =
            PARTICLE_TYPES.register("shockwave", () -> new SimpleParticleType(true));

    // Дым для гриба
    public static final RegistryObject<SimpleParticleType> MUSHROOM_SMOKE =
            PARTICLE_TYPES.register("mushroom_smoke", () -> new SimpleParticleType(true));

    // Искры
    public static final RegistryObject<SimpleParticleType> EXPLOSION_SPARK =
            PARTICLE_TYPES.register("explosion_spark", () -> new SimpleParticleType(true));

    // Дым для ножки грибовидного облака
    public static final RegistryObject<SimpleParticleType> MUSHROOM_STEM_SMOKE =
            PARTICLE_TYPES.register("mushroom_stem_smoke",
                    () -> new SimpleParticleType(true));

    // Дым для шляпки грибовидного облака
    public static final RegistryObject<SimpleParticleType> MUSHROOM_CAP_SMOKE =
            PARTICLE_TYPES.register("mushroom_cap_smoke",
                    () -> new SimpleParticleType(true));

    // Огненные частицы для основания взрыва
    public static final RegistryObject<SimpleParticleType> EXPLOSION_FIRE =
            PARTICLE_TYPES.register("explosion_fire",
                    () -> new SimpleParticleType(true));
}