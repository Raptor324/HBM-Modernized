package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

/** 1:1 {@code com.hbm.items.armor.ArmorAJRO}: T-45 AJR (Offizier). */
public class AJROArmor extends ModPowerArmorItem {

    /**
     * AJRO specifications.
     *
     * From 1.7.10 `ArmorAJRO` / `ModItemsArmor`:
     *  - maxPower    = 2_500_000
     *  - chargeRate  = 10_000
     *  - consumption = 2_000
     *  - drain       = 25
     *
     * Protection & features match 1.7.10 ArmorAJRO:
     *  - Jump Boost I (amp 0)
     *  - Strength I (amp 0)
     *  - VATS: true, Geiger: true, Hard Landing: true
     *  - Sounds: step.metal, step.iron_jump, step.iron_land
     */
    public static final PowerArmorSpecs AJRO_SPECS = new PowerArmorSpecs(
            2_500_000L, // Capacity
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
    .setFeatures(true, false, true, true) // VATS=true, Thermal=false, HardLanding=true, Geiger=true
    .addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 0)) // Jump Boost I
    .addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 0)) // Strength I
    .setStepSound("hbm:step.metal")
    .setJumpSound("hbm:step.iron_jump")
    .setFallSound("hbm:step.iron_land");

    public AJROArmor(ModArmorMaterials material, Type type, Properties properties) {
        super(material, type, properties, AJRO_SPECS);
    }
}
