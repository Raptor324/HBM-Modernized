package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ✅ ВСЕ ЧАСТИЦЫ теперь как ИСКРЫ - видны на 256+ блоков!
 * Все используют alwaysShow=true + AbstractExplosionParticle + LongRangeParticleRenderType
 */
public class ModExplosionParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, RefStrings.MODID);



    // ⚡ ИСКРЫ (оранжевые разлетающиеся)
    public static final RegistryObject<ParticleType<?>> LARGE_DARK_SMOKE =
            PARTICLE_TYPES.register("large_dark_smoke", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<?>> LARGE_EXPLOSION_SPARK =
            PARTICLE_TYPES.register("large_explosion_spark", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<?>> DARK_WAVE_SMOKE =
            PARTICLE_TYPES.register("dark_wave_smoke", () -> new SimpleParticleType(true));

    // ✅ ГЛАВНЫЕ ЭФФЕКТЫ ВЗРЫВА (все как искры!)

    // 🔥 ВСПЫШКА (яркий белый свет)
    public static final RegistryObject<ParticleType<?>> EXPLOSION_FLASH =
            PARTICLE_TYPES.register("explosion_flash", () -> new SimpleParticleType(true));

    // ⚡ ИСКРЫ (оранжевые разлетающиеся)
    public static final RegistryObject<ParticleType<?>> EXPLOSION_SPARK =
            PARTICLE_TYPES.register("explosion_spark", () -> new SimpleParticleType(true));

    // 🌊 ШОКВОЛНА (кольца расширения)
    public static final RegistryObject<ParticleType<?>> SHOCKWAVE_RING =
            PARTICLE_TYPES.register("shockwave_ring", () -> new SimpleParticleType(true));

    // 💨 ГРИБОВИДНЫЙ ДЫМ (серый дым стебля + шапки)
    public static final RegistryObject<ParticleType<?>> MUSHROOM_SMOKE =
            PARTICLE_TYPES.register("mushroom_smoke", () -> new SimpleParticleType(true));

    // 💨 ГРИБОВИДНЫЙ ДЫМ (серый дым стебля + шапки)
    public static final RegistryObject<ParticleType<?>> DARK_SMOKE =
            PARTICLE_TYPES.register("dark_smoke", () -> new SimpleParticleType(true));

    // 💨 ГРИБОВИДНЫЙ ДЫМ (серый дым стебля + шапки)
    public static final RegistryObject<ParticleType<?>> WAVE_SMOKE =
            PARTICLE_TYPES.register("wave_smoke", () -> new SimpleParticleType(true));

    // 🔥 ОГОНЬ (основание взрыва)
    public static final RegistryObject<ParticleType<?>> EXPLOSION_FIRE =
            PARTICLE_TYPES.register("explosion_fire", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<?>> AGENT_ORANGE =
            PARTICLE_TYPES.register("agent_orange", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<?>> FIRE_SPARK =
            PARTICLE_TYPES.register("fire_spark", () -> new SimpleParticleType(true));

    // ✅ СТАРЫЕ/ЗАПАСНЫЕ (можно удалить если не используются)
    public static final RegistryObject<ParticleType<?>> FLASH =
            PARTICLE_TYPES.register("flash", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<?>> SHOCKWAVE =
            PARTICLE_TYPES.register("shockwave", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<?>> MUSHROOM_STEM_SMOKE =
            PARTICLE_TYPES.register("mushroom_stem_smoke", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<?>> MUSHROOM_CAP_SMOKE =
            PARTICLE_TYPES.register("mushroom_cap_smoke", () -> new SimpleParticleType(true));
}
