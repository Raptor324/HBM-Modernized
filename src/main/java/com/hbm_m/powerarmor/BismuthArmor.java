package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * Bismuth Power Armor (ported from 1.7.10 ArmorBismuth/ArmorFSB).
 *
 * Original gameplay highlights:
 * - Full set potion buffs: Jump VII, Speed VII, Regeneration II, Night Vision
 * - 3 dashes
 * - Full set resist profile:
 *   - Physical: DT=2 / DR=15%
 *   - Fire:     DT=5 / DR=50%
 *   - Explosion:DT=5 / DR=25%
 *   - Fall:     DT=0 / DR=100%
 *   - Other:    DT=2 / DR=25%
 *
 * In 1.20.1 this is integrated into the existing power armor pipeline (energy, OBJ rendering, DT+DR handler).
 */
public class BismuthArmor extends ModPowerArmorItem {

    public static final PowerArmorSpecs BISMUTH_SPECS = new PowerArmorSpecs(
            2_000_000L, // Capacity (chosen tier: above AJR)
            20_000,     // Max receive
            10,         // Passive drain per tick
            2_000       // Consumption per damage point
    )
            // Keep protection values roughly aligned with the original DT/DR. Exact set logic is registered
            // in powerarmor.resist.DamageResistanceHandler (full set only).
            .setProtection(
                    0F, 1.0F,    // Fall
                    5F, 0.25F,   // Explosion
                    2F, 0.15F,   // Kinetic/physical
                    2F, 0.15F,   // Projectile
                    5F, 0.5F,    // Fire
                    2F, 0.25F,   // Cold (mapped from "other")
                    2F, 0.25F,   // Radiation (mapped from "other")
                    2F, 0.25F    // Energy (mapped from "other")
            )
            .setMovement(0F, 3) // 3 dashes (full set)
            .setFeatures(false, false, false, false) // no VATS/thermal/hardLanding/geiger by default
            .addEffect(new MobEffectInstance(MobEffects.JUMP, 50, 6, true, false))
            .addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 6, true, false))
            .addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 1, true, false))
            // Use a longer duration to avoid vanilla night vision flicker; ModPowerArmorItem will respect it.
            .addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 50, 0, true, false))
            .setStepSound("hbm_m:step.metal")
            .setJumpSound("hbm_m:step.iron_jump")
            .setFallSound("hbm_m:step.iron_land");

    public BismuthArmor(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties, BISMUTH_SPECS);
    }

    /**
     * У висмутовой брони одна общая текстура (`bismuth.png`), без разбиения на `*_helmet/chest/leg`.
     * Иначе ванильный armor renderer пытается грузить несуществующие файлы и спамит WARN в лог.
     */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "hbm_m:textures/block/armor/bismuth.png";
    }
}

