package com.hbm_m.util;

import com.hbm_m.powerarmor.resist.DamageResistanceHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Port von {@code com.hbm.util.EntityDamageUtil}. Das 1.7.10-Original kennt zwei Wege: den "SEDNA-Patch" (die
 * Vanilla-Schadensrechnung von Hand nachgebaut) und den "Super-Kompatibilitaetsmodus" (Vanilla-Schaden, aber eigene
 * Unverwundbarkeitszeit und eigener Rueckstoss). In 1.20 sind die Interna von {@code LivingEntity.hurt} nicht mehr
 * nachbaubar, ohne Forge-Ereignisse zu umgehen - hier laeuft daher immer der Kompatibilitaetsweg, mit
 * {@link DamageResistanceHandler#setup(float, float)} fuer Schwellwert- und Prozentdurchschlag wie im Original.
 */
public final class EntityDamageUtil {

    private EntityDamageUtil() { }

    /** Shitty hack, if the first attack fails, it retries with damage + previous damage, allowing damage to penetrate */
    @Deprecated
    public static boolean attackEntityFromIgnoreIFrame(Entity victim, DamageSource src, float damage) {
        if (!victim.hurt(src, damage)) {
            if (victim instanceof LivingEntity living) {
                // Original: damage += lastDamage, damit der Treffer die Unverwundbarkeit durchschlaegt - in 1.20 ist
                // lastHurt nicht erreichbar, die Unverwundbarkeit wird stattdessen aufgehoben (gleiche Wirkung).
                if (living.invulnerableTime > living.invulnerableDuration / 2.0F) living.invulnerableTime = 0;
            }
            return victim.hurt(src, damage);
        }
        return true;
    }

    /** New and improved entity damage calc - only use this one */
    public static boolean attackEntityFromNT(LivingEntity living, DamageSource source, float amount, boolean ignoreIFrame, boolean allowSpecialCancel, double knockbackMultiplier, float pierceDT, float pierce) {
        if (living instanceof ServerPlayer playerMP && source.getEntity() instanceof Player attacker) {
            if (!playerMP.canHarmPlayer(attacker)) return false; //handles wack-ass no PVP rule as well as scoreboard friendly fire
        }

        DamageResistanceHandler.setup(pierceDT, pierce);
        boolean ret;
        try {
            ret = attackEntitySuperCompatibility(living, source, amount, ignoreIFrame, knockbackMultiplier);
        } finally {
            DamageResistanceHandler.reset();
        }
        return ret;
    }

    private static boolean attackEntitySuperCompatibility(LivingEntity living, DamageSource source, float amount, boolean ignoreIFrame, double knockbackMultiplier) {
        //disable iframes
        if (ignoreIFrame) living.invulnerableTime = 0;
        //cache last velocity
        Vec3 motion = living.getDeltaMovement();
        //bam!
        boolean ret = living.hurt(source, amount);
        //restore last velocity
        living.setDeltaMovement(motion);
        //apply own knockback
        Entity entity = source.getEntity();
        if (ret && entity != null) {
            double deltaX = entity.getX() - living.getX();
            double deltaZ;
            for (deltaZ = entity.getZ() - living.getZ(); deltaX * deltaX + deltaZ * deltaZ < 1.0E-4D; deltaZ = (Math.random() - Math.random()) * 0.01D) {
                deltaX = (Math.random() - Math.random()) * 0.01D;
            }
            if (knockbackMultiplier > 0) knockBack(living, entity, amount, deltaX, deltaZ, knockbackMultiplier);
        }
        return ret;
    }

    public static void knockBack(LivingEntity living, Entity attacker, float damage, double motionX, double motionZ, double multiplier) {
        if (living.getRandom().nextDouble() >= living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)) {
            living.hasImpulse = true;
            double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
            double magnitude = 0.4D * multiplier;
            Vec3 m = living.getDeltaMovement();
            double mx = m.x / 2.0D, my = m.y / 2.0D, mz = m.z / 2.0D;
            mx -= motionX / horizontal * magnitude;
            my += magnitude;
            mz -= motionZ / horizontal * magnitude;
            if (my > 0.2D) my = 0.2D * multiplier;
            living.setDeltaMovement(mx, my, mz);
            living.hurtMarked = true;
        }
    }

    /** 1:1 {@code EntityDamageUtil.getMouseOver}: Blick-Raytrace mit Bloecken und Wesen (Reichweite, Trefferzuschlag). */
    public static com.hbm_m.util.MovingObjectPosition getMouseOver(net.minecraft.world.entity.player.Player attacker, double reach) {
        return getMouseOver(attacker, reach, 0D);
    }

    public static com.hbm_m.util.MovingObjectPosition getMouseOver(net.minecraft.world.entity.player.Player attacker, double reach, double threshold) {

        net.minecraft.world.level.Level world = attacker.level();
        com.hbm_m.util.MovingObjectPosition objectMouseOver = null;
        net.minecraft.world.entity.Entity pointedEntity = null;

        net.minecraft.world.phys.Vec3 pos = attacker.getEyePosition(1F);
        net.minecraft.world.phys.Vec3 look = attacker.getViewVector(1F);
        net.minecraft.world.phys.Vec3 end = pos.add(look.x * reach, look.y * reach, look.z * reach);

        net.minecraft.world.phys.HitResult blockHit = world.clip(new net.minecraft.world.level.ClipContext(pos, end, net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, attacker));
        if(blockHit != null && blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) objectMouseOver = com.hbm_m.util.MovingObjectPosition.of(blockHit);

        net.minecraft.world.phys.Vec3 hitvec = null;
        float grace = 1.0F;
        java.util.List<net.minecraft.world.entity.Entity> list = world.getEntities(attacker, attacker.getBoundingBox().expandTowards(look.x * reach, look.y * reach, look.z * reach).inflate(grace, grace, grace));

        double closest = reach;

        for(int i = 0; i < list.size(); ++i) {
            net.minecraft.world.entity.Entity entity = list.get(i);

            if(entity.isPickable() && entity.isAlive()) {

                double borderSize = entity.getPickRadius() + threshold;
                net.minecraft.world.phys.AABB axisalignedbb = entity.getBoundingBox().inflate(borderSize, borderSize, borderSize);
                net.minecraft.world.phys.Vec3 intercept = axisalignedbb.clip(pos, end).orElse(null);

                if(axisalignedbb.contains(pos)) {
                    if(0.0D <= closest) {
                        pointedEntity = entity;
                        hitvec = intercept == null ? pos : intercept;
                        closest = 0.0D;
                    }

                } else if(intercept != null) {
                    double dist = pos.distanceTo(intercept);

                    if(dist < closest || closest == 0.0D) {
                        if(entity == attacker.getVehicle()) {
                            if(closest == 0.0D) {
                                pointedEntity = entity;
                                hitvec = intercept;
                            }
                        } else {
                            pointedEntity = entity;
                            hitvec = intercept;
                            closest = dist;
                        }
                    }
                }
            }
        }

        if(pointedEntity != null && (closest < reach || objectMouseOver == null)) {
            objectMouseOver = new com.hbm_m.util.MovingObjectPosition(pointedEntity, new Vec3NT(hitvec));
        }

        return objectMouseOver;
    }
}
