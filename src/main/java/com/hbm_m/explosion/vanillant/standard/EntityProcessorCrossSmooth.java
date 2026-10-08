package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.item.weapon.sedna.SednaDamage;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;
import com.hbm_m.util.EntityDamageUtil;
import com.hbm_m.util.confetti.ConfettiUtil;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1:1 {@code EntityProcessorCrossSmooth}: fester Schaden, linear zur Entfernung abfallend; SEDNA-Schadensklasse
 * (Standard EXPLOSIVE) und Panzerungsdurchschlag (DT/DR) wie bei Geschossen.
 */
public class EntityProcessorCrossSmooth extends EntityProcessorCross {

    protected float fixedDamage;
    protected float pierceDT = 0;
    protected float pierceDR = 0;
    protected DamageClass clazz = DamageClass.EXPLOSIVE;

    public EntityProcessorCrossSmooth(double nodeDist, float fixedDamage) {
        super(nodeDist);
        this.fixedDamage = fixedDamage;
        this.setAllowSelfDamage();
    }

    public EntityProcessorCrossSmooth setupPiercing(float pierceDT, float pierceDR) {
        this.pierceDT = pierceDT;
        this.pierceDR = pierceDR;
        return this;
    }

    public EntityProcessorCrossSmooth setDamageClass(DamageClass clazz) {
        this.clazz = clazz;
        return this;
    }

    /** Kovariant, damit Verkettungen wie im Original beim Typ CrossSmooth bleiben. */
    @Override
    public EntityProcessorCrossSmooth withRangeMod(float mod) {
        super.withRangeMod(mod);
        return this;
    }

    @Override
    public EntityProcessorCrossSmooth setKnockback(double mult) {
        super.setKnockback(mult);
        return this;
    }

    @Override
    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (!entity.isAlive()) return;
        if (source.exploder == entity) amount *= 0.5F;
        DamageSource dmg = SednaDamage.create(entity.level(), null, source.exploder instanceof LivingEntity le ? le : null, clazz);
        if (!(entity instanceof LivingEntity living)) {
            entity.hurt(dmg, amount);
        } else {
            EntityDamageUtil.attackEntityFromNT(living, dmg, amount, true, false, 0F, pierceDT, pierceDR);
            if (!living.isAlive()) ConfettiUtil.decideConfetti(living, dmg);
        }
    }

    @Override
    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        if (density < 0.125) return 0; //shitty hack
        return (float) (fixedDamage * (1 - distanceScaled));
    }
}
