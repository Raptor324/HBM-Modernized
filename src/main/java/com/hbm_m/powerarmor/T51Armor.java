package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorMaterial;

// T51 Power Armor with full set bonus - powered armor with various enhancements
public class T51Armor extends ModPowerArmorItem {

    // T51 Specifications - exact values from original HBM 1.7.10
    public static final PowerArmorSpecs T51_SPECS = new PowerArmorSpecs(
            PowerArmorSpecs.EnergyMode.CONSTANT_DRAIN,
            1000000L, // Capacity
            10000,    // Max receive (charge rate from original)
            5,        // Usage per tick (drain in idle, from original)
            1000      // Usage per damage point (consumption when hit, from original)
    )
            .setProtection(
                    0F, 1.0F,   // Fall: DT=0, DR=100%
                    5F, 0.25F,  // Explosion: DT=5, DR=25%
                    2F, 0.15F,  // Kinetic: DT=2, DR=15%
                    2F, 0.15F,  // Projectile: DT=2, DR=15% (same as kinetic)
                    0.5F, 0.35F,// Fire: DT=0.5, DR=35%
                    0F, 0.1F,   // Cold: DT=0, DR=10%
                    0F, 0.1F,   // Radiation: DT=0, DR=10%
                    0F, 0.1F    // Energy: DT=0, DR=10%
            )
            .setFeatures(true, false, true, true) // VATS=true, Thermal=false, HardLanding=true, Geiger=true
            .addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1)) // Strength II
            .setStepSound("hbm_m:step.metal")
            .setJumpSound("hbm_m:step.iron_jump")
            .setFallSound("hbm_m:step.iron_land");

    public T51Armor(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties, T51_SPECS);
    }
}
