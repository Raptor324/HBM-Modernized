package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.LivingEntity;

/**
 * Миксин для отслеживания шагов игрока в силовой брони.
 * Реальная замена звуков происходит через Forge событие PlaySoundEvent в PowerArmorStepSoundHandler.
 */
@Mixin(LivingEntity.class)
public abstract class PlayerStepSoundMixin {
    
    @Unique
    private static final String HBM_M_STEP_TRACKER = "hbm_m_step_tracker";
}
