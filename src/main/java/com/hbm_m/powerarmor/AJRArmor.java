package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorMaterial;

public class AJRArmor extends ModPowerArmorItem {

    // AJR Specifications
    public static final PowerArmorSpecs AJR_SPECS = new PowerArmorSpecs(
            1500000L,
            15000,
            7,
            1500
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
    .addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 2)) // Strength III
    .addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 1)) // Resistance II
    .setStepSound("hbm_m:step.metal")
    .setJumpSound("hbm_m:step.iron_jump")
    .setFallSound("hbm_m:step.iron_land");

    public AJRArmor(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties, AJR_SPECS);
    }
}