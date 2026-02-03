package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorMaterial;

/**
 * AJRO Power Armor – "orange" AJR variant.
 *
 * In 1.7.10 this was implemented as {@code ArmorAJRO}:
 * - Uses the same OBJ model as AJR (armor_ajr/dnt.obj groups)
 * - Has slightly higher energy capacity and drain than AJR
 * - Shares the same resistance profile and general role as AJR
 *
 * Here we mirror AJR's protection/features/effects, but with the original
 * AJRO energy numbers (capacity/receive/drain/consumption).
 */
public class AJROArmor extends ModPowerArmorItem {

    /**
     * AJRO specifications.
     *
     * From 1.7.10 `ModItems`:
     *  - maxPower   = 2_500_000
     *  - chargeRate = 10_000
     *  - consumption= 2_000
     *  - drain      = 25
     *
     * Protection/features/effects are kept identical to AJRArmor.
     */
    public static final PowerArmorSpecs AJRO_SPECS = new PowerArmorSpecs(
            2_500_000L, // Capacity (higher than AJR)
            10_000L,    // Max receive
            25L,        // Passive drain per tick
            2_000L      // Consumption per damage point
    )
    .setProtection(
            0F, 1.0F,   // Fall: DT=0, DR=100%
            7.5F, 0.25F,// Explosion: DT=7.5, DR=25%
            4F, 0.15F,  // Kinetic: DT=4, DR=15%
            4F, 0.15F,  // Projectile: DT=4, DR=15%
            0.5F, 0.35F,// Fire: DT=0.5, DR=35%
            0F, 0.15F,  // Cold: DT=0, DR=15%
            0F, 0.15F,  // Radiation: DT=0, DR=15%
            0F, 0.15F   // Energy: DT=0, DR=15%
    )
    .setFeatures(true, false, true, true) // Same feature set as AJR
    .addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 2)) // Strength III
    .addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 1)) // Resistance II
    .setStepSound("hbm_m:step.metal")
    .setJumpSound("hbm_m:step.iron_jump")
    .setFallSound("hbm_m:step.iron_land");

    public AJROArmor(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties, AJRO_SPECS);
    }
}

