package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.weapon.sedna.impl.IPAMelee;
import com.hbm_m.item.weapon.sedna.impl.IPARanged;
import com.hbm_m.item.weapon.sedna.impl.IPAWeaponsProvider;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** 1:1 {@code com.hbm.items.armor.ArmorRPA}: Remnant Power Armor mit Nahkampfkomponente fuer gun_pa_melee. */
public class ArmorRPA extends ModPowerArmorItem implements IPAWeaponsProvider {

    public static PowerArmorSpecs createRPASpecs(long maxPower, long chargeRate, long drain, long consumption) {
        return new PowerArmorSpecs(maxPower, chargeRate, drain, consumption)
                .setProtection(
                        0F, 1.0F,    // Fall: DT=0, DR=100%
                        15F, 0.25F,  // Explosion: DT=15, DR=25%
                        50F, 0.75F,  // Kinetic (Physical): DT=50, DR=75%
                        50F, 0.75F,  // Projectile: DT=50, DR=75%
                        25F, 0.90F,  // Fire: DT=25, DR=90%
                        15F, 0.45F,  // Cold (Other): DT=15, DR=45%
                        0F, 0.99F,   // Radiation: DT=0, DR=99% (2.0D)
                        30F, 0.80F   // Energy: DT=30, DR=80%
                )
                .setFeatures(true, false, true, true) // VATS=true, Thermal=false, HardLanding=true, Geiger=true
                .addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 3)) // Strength IV
                .setSounds("hbm:step.powered", "hbm:step.powered", "hbm:step.powered");
    }

    public ArmorRPA(ModArmorMaterials material, Type type, Properties properties, String texture,
              long maxPower, long chargeRate, long consumption, long drain) {
        // Texture is resolved from the registry id in ModPowerArmorItem (see ArmorHEV).
        super(material, type, properties, createRPASpecs(maxPower, chargeRate, drain, consumption));
    }

    @Override
    public ModArmorFSB cloneStats(ModArmorFSB original) {
        super.cloneStats(original);
        if (original instanceof ModPowerArmorItem pai && this.getSpecs() != null) {
            PowerArmorSpecs src = pai.getSpecs();
            PowerArmorSpecs dst = this.getSpecs();
            dst.hasVats = src.hasVats;
            dst.hasThermal = src.hasThermal;
            dst.hasHardLanding = src.hasHardLanding;
            dst.hasGeigerSound = src.hasGeigerSound;
            dst.hasCustomGeiger = src.hasCustomGeiger;
            dst.stepSound = src.stepSound;
            dst.jumpSound = src.jumpSound;
            dst.fallSound = src.fallSound;
            dst.dtFall = src.dtFall; dst.drFall = src.drFall;
            dst.dtExplosion = src.dtExplosion; dst.drExplosion = src.drExplosion;
            dst.dtKinetic = src.dtKinetic; dst.drKinetic = src.drKinetic;
            dst.dtProjectile = src.dtProjectile; dst.drProjectile = src.drProjectile;
            dst.dtFire = src.dtFire; dst.drFire = src.drFire;
            dst.dtCold = src.dtCold; dst.drCold = src.drCold;
            dst.dtRadiation = src.dtRadiation; dst.drRadiation = src.drRadiation;
            dst.dtEnergy = src.dtEnergy; dst.drEnergy = src.drEnergy;
        }
        return this;
    }

    public static final ArmorRPAMelee meleeComponent = new ArmorRPAMelee();

    @Override
    public IPAMelee getMeleeComponent(Player entity) {
        if (hasFSBArmorIgnoreCharge(entity)) return meleeComponent;
        return null;
    }

    @Override public IPARanged getRangedComponent(Player entity) { return null; }
}
