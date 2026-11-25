package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

/** 1:1 {@code com.hbm.items.armor.ArmorT51}: T-51b Powered Armor. */
public class T51Armor extends ModPowerArmorItem {

    public static final PowerArmorSpecs T51_SPECS = new PowerArmorSpecs(
            1_000_000L,
            10_000L,
            5L,
            1_000L
    )
    .setProtection(
            0F, 1.0F,    // Fall: DT=0, DR=100%
            5F, 0.25F,   // Explosion: DT=5, DR=25%
            2F, 0.15F,   // Kinetic: DT=2, DR=15%
            2F, 0.15F,   // Projectile: DT=2, DR=15%
            0.5F, 0.35F, // Fire: DT=0.5, DR=35%
            0F, 0.1F,    // Cold: DT=0, DR=10%
            0F, 0.1F,    // Radiation: DT=0, DR=10%
            0F, 0.1F     // Energy: DT=0, DR=10%
    )
    .setFeatures(true, false, true, true) // VATS=true, Thermal=false, HardLanding=true, Geiger=true
    .addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 0)) // Strength I
    .setStepSound("hbm:step.metal")
    .setJumpSound("hbm:step.iron_jump")
    .setFallSound("hbm:step.iron_land");

    public T51Armor(ModArmorMaterials material, Type type, Properties properties) {
        super(material, type, properties, T51_SPECS);
    }

    public T51Armor(ModArmorMaterials material, Type type, Properties properties, String texture,
              long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, T51_SPECS);
    }
}
