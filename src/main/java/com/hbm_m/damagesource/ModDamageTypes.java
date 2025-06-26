package com.hbm_m.damagesource;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {
    // Для получения ключа типа урона
    public static final ResourceKey<DamageType> RADIATION_DAMAGE_KEY = ResourceKey.create(
        net.minecraft.core.registries.Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath("hbm_m", "radiation_damage")
    );
    // Используйте этот ключ для получения типа урона из реестра
}