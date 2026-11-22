package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import java.util.ArrayList;
import java.util.List;

public class PowerArmorSpecs {

    // --- Energy Configuration (matches 1.7.10) ---
    public final long capacity;
    public final long maxReceive;
    public final long drain;        // Passive drain per tick
    public final long consumption;  // Energy per damage point (via setDamage)

    // --- Damage Threshold (DT) - Flat damage reduction ---
    public float dtFall = 0f;
    public float dtExplosion = 0f;
    public float dtKinetic = 0f;
    public float dtProjectile = 0f;
    public float dtFire = 0f;
    public float dtCold = 0f;
    public float dtRadiation = 0f;
    public float dtEnergy = 0f;

    // --- Damage Resistance (DR) - Percentage reduction (0.0 - 1.0) ---
    public float drFall = 0f;
    public float drExplosion = 0f;
    public float drKinetic = 0f;
    public float drProjectile = 0f;
    public float drFire = 0f;
    public float drCold = 0f;
    public float drRadiation = 0f;
    public float drEnergy = 0f;

    // --- Legacy resistance values (for compatibility) ---
    public float resFall = 0f;
    public float resExplosion = 0f;
    public float resKinetic = 0f;
    public float resProjectile = 0f;
    public float resFire = 0f;
    public float resCold = 0f;
    public float resRadiation = 0f;

    // --- ArmorFSB Features ---
    public boolean hasVats = false;
    public boolean hasThermal = false;
    public boolean hasNightVision = false;
    public boolean hasGeigerSound = false;
    public boolean hasCustomGeiger = false;
    public boolean hasHardLanding = false;
    public boolean noHelmetRequired = false;

    // --- Movement ---
    public float stepHeight = 0f;
    public int stepSize = 0;
    public int dashCount = 0;

    // --- Sounds ---
    public String stepSound = null;
    public String jumpSound = null;
    public String fallSound = null;

    // --- Visual ---
    public String overlay = null;

    public List<MobEffectInstance> passiveEffects = new ArrayList<>();

    /**
     * Basic constructor - matches 1.7.10 ArmorFSBPowered constructor
     * @param capacity Maximum energy capacity
     * @param maxReceive Maximum energy received per tick (charging)
     * @param drain Passive energy drain per tick (when wearing full set)
     * @param consumption Energy cost per damage point (when armor takes damage)
     */
    public PowerArmorSpecs(long capacity, long maxReceive, long drain, long consumption) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.drain = drain;
        this.consumption = consumption;
    }

    /**
     * Simplified constructor with default consumption
     */
    public PowerArmorSpecs(long capacity, long maxReceive, long drain) {
        this(capacity, maxReceive, drain, 10);
    }

    // --- Configuration Methods ---

    /**
     * Set damage resistances (legacy method - sets DR values)
     */
    public PowerArmorSpecs setResistances(float fall, float explosion, float kinetic, 
                                         float projectile, float fire, float cold, float radiation) {
        // Set DR values
        this.drFall = fall;
        this.drExplosion = explosion;
        this.drKinetic = kinetic;
        this.drProjectile = projectile;
        this.drFire = fire;
        this.drCold = cold;
        this.drRadiation = radiation;

        // Also set legacy values for compatibility
        this.resFall = fall;
        this.resExplosion = explosion;
        this.resKinetic = kinetic;
        this.resProjectile = projectile;
        this.resFire = fire;
        this.resCold = cold;
        this.resRadiation = radiation;

        return this;
    }

    /**
     * Set Damage Threshold (DT) values
     */
    public PowerArmorSpecs setDT(float fall, float explosion, float kinetic, float projectile,
                                 float fire, float cold, float radiation, float energy) {
        this.dtFall = fall;
        this.dtExplosion = explosion;
        this.dtKinetic = kinetic;
        this.dtProjectile = projectile;
        this.dtFire = fire;
        this.dtCold = cold;
        this.dtRadiation = radiation;
        this.dtEnergy = energy;
        return this;
    }

    /**
     * Set Damage Resistance (DR) values
     */
    public PowerArmorSpecs setDR(float fall, float explosion, float kinetic, float projectile,
                                 float fire, float cold, float radiation, float energy) {
        this.drFall = fall;
        this.drExplosion = explosion;
        this.drKinetic = kinetic;
        this.drProjectile = projectile;
        this.drFire = fire;
        this.drCold = cold;
        this.drRadiation = radiation;
        this.drEnergy = energy;

        // Update legacy values
        this.resFall = fall;
        this.resExplosion = explosion;
        this.resKinetic = kinetic;
        this.resProjectile = projectile;
        this.resFire = fire;
        this.resCold = cold;
        this.resRadiation = radiation;

        return this;
    }

    /**
     * Set both DT and DR for complete protection configuration
     */
    public PowerArmorSpecs setProtection(float dtFall, float drFall, 
                                        float dtExplosion, float drExplosion,
                                        float dtKinetic, float drKinetic,
                                        float dtProjectile, float drProjectile,
                                        float dtFire, float drFire,
                                        float dtCold, float drCold,
                                        float dtRadiation, float drRadiation,
                                        float dtEnergy, float drEnergy) {
        this.dtFall = dtFall; this.drFall = drFall;
        this.dtExplosion = dtExplosion; this.drExplosion = drExplosion;
        this.dtKinetic = dtKinetic; this.drKinetic = drKinetic;
        this.dtProjectile = dtProjectile; this.drProjectile = drProjectile;
        this.dtFire = dtFire; this.drFire = drFire;
        this.dtCold = dtCold; this.drCold = drCold;
        this.dtRadiation = dtRadiation; this.drRadiation = drRadiation;
        this.dtEnergy = dtEnergy; this.drEnergy = drEnergy;

        // Update legacy
        this.resFall = drFall;
        this.resExplosion = drExplosion;
        this.resKinetic = drKinetic;
        this.resProjectile = drProjectile;
        this.resFire = drFire;
        this.resCold = drCold;
        this.resRadiation = drRadiation;

        return this;
    }

    public PowerArmorSpecs addEffect(MobEffectInstance effect) {
        this.passiveEffects.add(effect);
        return this;
    }

    public PowerArmorSpecs setFeatures(boolean vats, boolean thermal, boolean hardLanding, boolean geiger) {
        this.hasVats = vats;
        this.hasThermal = thermal;
        this.hasHardLanding = hardLanding;
        this.hasGeigerSound = geiger;
        return this;
    }

    public PowerArmorSpecs setFeatures(boolean vats, boolean thermal, boolean hardLanding, 
                                      boolean geiger, boolean customGeiger) {
        this.hasVats = vats;
        this.hasThermal = thermal;
        this.hasHardLanding = hardLanding;
        this.hasGeigerSound = geiger;
        this.hasCustomGeiger = customGeiger;
        return this;
    } //TODO че за херня, почему 2 метода

    public PowerArmorSpecs setMovement(float stepHeight, int dashCount) {
        this.stepHeight = stepHeight;
        this.dashCount = dashCount;
        return this;
    }

    public PowerArmorSpecs setNoHelmet(boolean noHelmet) {
        this.noHelmetRequired = noHelmet;
        return this;
    }

    public PowerArmorSpecs setGeigerHUD(boolean customGeiger) {
        this.hasCustomGeiger = customGeiger;
        return this;
    }

    public PowerArmorSpecs setStepSize(int stepSize) {
        this.stepSize = stepSize;
        this.stepHeight = stepSize;
        return this;
    }

    public PowerArmorSpecs setSounds(String step, String jump, String fall) {
        this.stepSound = step;
        this.jumpSound = jump;
        this.fallSound = fall;
        return this;
    }

    public PowerArmorSpecs setStepSound(String step) {
        this.stepSound = step;
        return this;
    }

    public PowerArmorSpecs setJumpSound(String jump) {
        this.jumpSound = jump;
        return this;
    }

    public PowerArmorSpecs setFallSound(String fall) {
        this.fallSound = fall;
        return this;
    }

    public PowerArmorSpecs setOverlay(String path) {
        this.overlay = path;
        return this;
    }
}