package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/**
 *  ВСЕ ЧАСТИЦЫ теперь как ИСКРЫ - видны на 256+ блоков!
 * Все используют alwaysShow=true + AbstractExplosionParticle + LongRangeParticleRenderType
 */
public class ModExplosionParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(RefStrings.MODID, Registries.PARTICLE_TYPE);

    // Вспомогательный кроссплатформенный метод для создания SimpleParticleType
    private static SimpleParticleType createParticle(boolean alwaysShow) {
        //? if fabric {
        /*return net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(alwaysShow);
        *///?} else {
        return new SimpleParticleType(alwaysShow);
         //?}
    }



    // ⚡ ИСКРЫ (оранжевые разлетающиеся)
    public static final RegistrySupplier<ParticleType<?>> LARGE_DARK_SMOKE =
            PARTICLE_TYPES.register("large_dark_smoke", () -> createParticle(true));

    public static final RegistrySupplier<ParticleType<?>> LARGE_EXPLOSION_SPARK =
            PARTICLE_TYPES.register("large_explosion_spark", () -> createParticle(true));

    public static final RegistrySupplier<ParticleType<?>> DARK_WAVE_SMOKE =
            PARTICLE_TYPES.register("dark_wave_smoke", () -> createParticle(true));

    //  ГЛАВНЫЕ ЭФФЕКТЫ ВЗРЫВА (все как искры!)

    // 🔥 ВСПЫШКА (яркий белый свет)
    public static final RegistrySupplier<ParticleType<?>> EXPLOSION_FLASH =
            PARTICLE_TYPES.register("explosion_flash", () -> createParticle(true));

    // ⚡ ИСКРЫ (оранжевые разлетающиеся)
    public static final RegistrySupplier<ParticleType<?>> EXPLOSION_SPARK =
            PARTICLE_TYPES.register("explosion_spark", () -> createParticle(true));

    // 🌊 ШОКВОЛНА (кольца расширения)
    public static final RegistrySupplier<ParticleType<?>> SHOCKWAVE_RING =
            PARTICLE_TYPES.register("shockwave_ring", () -> createParticle(true));

    // 💨 ГРИБОВИДНЫЙ ДЫМ (серый дым стебля + шапки)
    public static final RegistrySupplier<ParticleType<?>> MUSHROOM_SMOKE =
            PARTICLE_TYPES.register("mushroom_smoke", () -> createParticle(true));

    // 💨 ГРИБОВИДНЫЙ ДЫМ (серый дым стебля + шапки)
    public static final RegistrySupplier<ParticleType<?>> DARK_SMOKE =
            PARTICLE_TYPES.register("dark_smoke", () -> createParticle(true));

    // 💨 ГРИБОВИДНЫЙ ДЫМ (серый дым стебля + шапки)
    public static final RegistrySupplier<ParticleType<?>> WAVE_SMOKE =
            PARTICLE_TYPES.register("wave_smoke", () -> createParticle(true));

    // 🔥 ОГОНЬ (основание взрыва)
    public static final RegistrySupplier<ParticleType<?>> EXPLOSION_FIRE =
            PARTICLE_TYPES.register("explosion_fire", () -> createParticle(true));

    public static final RegistrySupplier<ParticleType<?>> AGENT_ORANGE =
            PARTICLE_TYPES.register("agent_orange", () -> createParticle(true));

    public static final RegistrySupplier<ParticleType<?>> FIRE_SPARK =
            PARTICLE_TYPES.register("fire_spark", () -> createParticle(true));

    //  СТАРЫЕ/ЗАПАСНЫЕ (можно удалить если не используются)
    public static final RegistrySupplier<ParticleType<?>> FLASH =
            PARTICLE_TYPES.register("flash", () -> createParticle(true));

    public static final RegistrySupplier<ParticleType<?>> SHOCKWAVE =
            PARTICLE_TYPES.register("shockwave", () -> createParticle(true));

    public static final RegistrySupplier<ParticleType<?>> MUSHROOM_STEM_SMOKE =
            PARTICLE_TYPES.register("mushroom_stem_smoke", () -> createParticle(true));

    public static final RegistrySupplier<ParticleType<?>> MUSHROOM_CAP_SMOKE =
            PARTICLE_TYPES.register("mushroom_cap_smoke", () -> createParticle(true));

    public static void init() {
        PARTICLE_TYPES.register();
    }
}
