package com.hbm_m.entity.mob.botprime;

import com.hbm_m.api.entity.IRadiationImmune;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code EntityBOTPrimeBase} - the shared half of BOT Prime, a 15000 HP worm.
 *
 * <p>Laser wie im Original: {@code EntityBulletBaseNT} mit WORM_LASER / WORM_BOLT.</p>
 */
public abstract class EntityBOTPrimeBase extends EntityWormBase implements IRadiationImmune {

    public int attackCounter = 0;

    protected EntityBOTPrimeBase(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.dragInAir = 0.995F;
        this.dragInGround = 0.98F;
        this.knockbackDivider = 1.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 1000.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override public boolean fireImmune()                { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override protected SoundEvent getHurtSound(@NotNull DamageSource source) { return SoundEvents.BLAZE_HURT; }
    // Original getDeathSound: "hbm:entity.bombDet"
    @Override protected SoundEvent getDeathSound() { return com.hbm_m.sound.HbmSoundsNT.get("hbm:entity.bombDet"); }

    /**
     * {@code canEntityBeSeenThroughNonSolids}: the worm spends its life inside rock, so it aims
     * with a trace that ignores blocks and only stops on fluids.
     */
    public boolean canSeeThroughNonSolids(Entity target) {
        Vec3 from = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        return this.level().clip(new ClipContext(from, to,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, this)).getType() == HitResult.Type.MISS;
    }

    /** 1:1: der Kopf feuert fuenf WORM_LASER (Streuung i * 0,05), Segmente je einen WORM_BOLT (EntityBulletBaseNT). */
    protected void laserAttack(Entity target, boolean head) {
        if (!(target instanceof LivingEntity living)) return;

        if (head) {
            for (int i = 0; i < 5; i++) {
                com.hbm_m.entity.projectile.EntityBulletBaseNT bullet = com.hbm_m.entity.projectile.EntityBulletBaseNT.create(this.level(),
                        com.hbm_m.handler.BulletConfigSyncingUtil.WORM_LASER, this, living, 1.0F, i * 0.05F);
                this.level().addFreshEntity(bullet);
            }
            playLaser(0.75F);
        } else {
            com.hbm_m.entity.projectile.EntityBulletBaseNT bullet = com.hbm_m.entity.projectile.EntityBulletBaseNT.create(this.level(),
                    com.hbm_m.handler.BulletConfigSyncingUtil.WORM_BOLT, this, living, 0.5F, 0.125F);
            this.level().addFreshEntity(bullet);
            playLaser(1.0F);
        }
    }

    private void playLaser(float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.ballsLaser"), SoundSource.HOSTILE, 5.0F, pitch);
    }
}
