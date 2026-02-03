package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Dineutronium (DNT) Power Armor.
 *
 * This is the 1.20.1 counterpart of the 1.7.10 {@code ArmorDNT} class:
 * - Uses power-armor energy pipeline (ModPowerArmorItem + PowerArmorSpecs)
 * - Protection values are based on the original DNS/DNT role: extreme late‑game set,
 *   with very strong general and explosion resistance.
 * - Active movement/jetpack behaviour is handled by the generic FSB/power‑armor systems;
 *   if/when a dedicated jetpack controller is ported, it should hook into this set via specs.
 */
public class DNTArmor extends ModPowerArmorItem {

    /**
     * DNT power armor specifications.
     *
     * Energy numbers are taken directly from 1.7.10 ArmorDNT (DNS set):
     *  - capacity   = 1_000_000_000
     *  - maxReceive = 1_000_000
     *  - drain      = 115
     *  - consumption= 100_000
     *
     * Protection is intentionally stronger than T51/AJR, matching the original
     * "endgame tank" design: very high explosion and "other" resistance.
     */
    public static final PowerArmorSpecs DNT_SPECS = new PowerArmorSpecs(
            1_000_000_000L, // Capacity
            1_000_000L,     // Max receive (charge rate)
            115L,           // Passive drain per tick
            100_000L        // Consumption per damage point
    )
            // Rough port of the original DNS/DNT role:
            //  - excellent explosion + generic protection
            //  - full fall damage immunity (with energy)
            .setProtection(
                    0F, 1.0F,     // Fall: DT=0,  DR=100%
                    20F, 0.90F,   // Explosion: high DT/DR
                    5F, 0.5F,     // Kinetic: strong physical
                    5F, 0.5F,     // Projectile: same as kinetic
                    5F, 0.75F,    // Fire: near‑immunity
                    2F, 0.5F,     // Cold
                    2F, 0.5F,     // Radiation
                    10F, 0.75F    // Energy / "other"
            )
            // Full‑set mobility and utility:
            //  - high dash count to emulate the original mobility focus
            //  - step height slightly increased
            .setMovement(0.5F, 4)
            // Core FSB features: VATS, thermal, hard landing, Geiger HUD
            .setFeatures(true, true, true, true, true)
            // Passive Strength and Speed buffs (mirrors high‑tier feel)
            .addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2, true, false))
            .addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, true, false))
            .setStepSound("hbm_m:step.metal")
            .setJumpSound("hbm_m:step.iron_jump")
            .setFallSound("hbm_m:step.iron_land");

    public DNTArmor(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties, DNT_SPECS);
    }
}

