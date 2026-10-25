package com.hbm_m.handler;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.projectile.EntityBulletBaseNT;
import com.hbm_m.entity.projectile.EntityBulletBaseNT.IBulletImpactBehaviorNT;
import com.hbm_m.entity.projectile.EntityBulletBaseNT.IBulletUpdateBehaviorNT;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1:1 {@code BulletConfiguration} (Altsystem, nur noch fuer {@link EntityBulletBaseNT} der NPC-Geschosse).
 * Port: Munition ({@code ammo}) entfaellt - kein Altwaffen-Gegenstand nutzt diese Konfigurationen mehr;
 * Schadensart ist ein {@link DamageType}-Schluessel statt des Original-Strings.
 */
@Deprecated
public class BulletConfiguration implements Cloneable {

    public float velocity;
    public float spread;
    public int bulletsMin;
    public int bulletsMax;

    public float dmgMin;
    public float dmgMax;
    public float headshotMult = 1.0F;

    public double gravity;
    public int maxAge;

    public boolean doesRicochet;
    public double ricochetAngle;
    public int LBRC;
    public int HBRC;
    public double bounceMod;
    public int selfDamageDelay = 5;

    public boolean doesPenetrate;
    public boolean doesBreakGlass;

    @Nullable public List<MobEffectInstance> effects;
    public int incendiary;
    public boolean blockDamage = true;
    public float explosive;
    public int leadChance;
    public boolean destroysBlocks;
    @Nullable public IBulletImpactBehaviorNT bntImpact;
    @Nullable public IBulletUpdateBehaviorNT bntUpdate;

    public int style;
    public int trail;
    public int plink;
    public String vPFX = "";

    /** Original {@code ModDamageSource.s_bullet} ("revolverBullet"). */
    public ResourceKey<DamageType> damageType = ModDamageTypes.REVOLVER_BULLET;
    public boolean dmgProj = true;
    public boolean dmgFire = false;
    public boolean dmgExplosion = false;
    public boolean dmgBypass = false;

    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_FLECHETTE = 2;
    public static final int STYLE_BOLT = 4;
    public static final int STYLE_ROCKET = 6;
    public static final int STYLE_GRENADE = 10;
    public static final int STYLE_ORB = 12;
    public static final int STYLE_METEOR = 13;
    public static final int STYLE_BLADE = 15;

    public static final int PLINK_NONE = 0;
    public static final int PLINK_BULLET = 1;
    public static final int PLINK_GRENADE = 2;
    public static final int PLINK_ENERGY = 3;
    public static final int PLINK_SING = 4;

    public static final int BOLT_LACUNAE = 0;
    public static final int BOLT_NIGHTMARE = 1;
    public static final int BOLT_LASER = 2;
    public static final int BOLT_WORM = 4;

    public BulletConfiguration setToBolt(int trail) {
        this.style = STYLE_BOLT;
        this.trail = trail;
        return this;
    }

    public BulletConfiguration setToFire(int duration) {
        this.incendiary = duration;
        return this;
    }

    public BulletConfiguration setHeadshot(float mult) {
        this.headshotMult = mult;
        return this;
    }

    public BulletConfiguration accuracyMod(float mod) {
        this.spread *= mod;
        return this;
    }

    /** Original: mit Schuetze indirekt (Geschoss + Schuetze), sonst ohne Verursacher. Projektil/Feuer/Explosion stecken in den Typ-Tags. */
    public DamageSource getDamage(EntityBulletBaseNT bullet, @Nullable LivingEntity shooter) {
        var holder = bullet.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageType);
        return shooter != null ? new DamageSource(holder, bullet, shooter) : new DamageSource(holder, bullet);
    }

    @Override
    public BulletConfiguration clone() {
        try {
            return (BulletConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            return new BulletConfiguration();
        }
    }
}
