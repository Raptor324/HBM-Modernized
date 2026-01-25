package com.hbm_m.powerarmor.resist;

import net.minecraft.world.damagesource.DamageSource;

/**
 * Interface for entities that provide custom damage resistance
 * Ported from original HBM 1.7.10
 */
public interface IResistanceProvider {

    /**
     * Get current Damage Threshold and Damage Resistance values
     * @param damage The damage source
     * @param amount The damage amount
     * @param pierceDT Armor piercing for DT
     * @param pierceDR Armor piercing for DR
     * @return float array [dt, dr] where dt is damage threshold and dr is damage resistance
     */
    float[] getCurrentDTDR(DamageSource damage, float amount, float pierceDT, float pierceDR);
}