package com.hbm_m.damagesource;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;

public class RadiationDamageSource extends DamageSource {
    public RadiationDamageSource(Level level, Entity entity) {
        super(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.RADIATION_DAMAGE_KEY), entity);
    }
}