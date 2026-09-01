package com.hbm_m.entity.mob.botprime;

import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.entity.projectile.TurretBulletEntity;
import com.hbm_m.item.ModItems;

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
 * <p><b>Substitution:</b> the original's laser fires {@code EntityBulletBaseNT} with the
 * {@code WORM_LASER} / {@code WORM_BOLT} configs. That bullet system is not ported, so the shots
 * are carried by {@link TurretBulletEntity} with the original's counts, spreads and damage kept.</p>
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
    //? if < 1.21.1 {
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GENERIC_EXPLODE; }
    //?} else {
    /*// 1.21 liefert GENERIC_EXPLODE als Holder.Reference<SoundEvent>.
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GENERIC_EXPLODE.value(); }
    *///?}

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

    /** The head fires a five-round fan; body segments fire single bolts. */
    protected void laserAttack(Entity target, boolean head) {
        if (!(target instanceof LivingEntity living)) return;

        Vec3 dir = new Vec3(
                living.getX() - this.getX(),
                living.getEyeY() - this.getEyeY(),
                living.getZ() - this.getZ()).normalize();

        if (head) {
            for (int i = 0; i < 5; i++) {
                fire(dir, 1.0F, i * 0.05F, 6F);
            }
            playLaser(0.75F);
        } else {
            fire(dir, 0.5F, 0.125F, 3F);
            playLaser(1.0F);
        }
    }

    private void fire(Vec3 dir, double speed, double spread, float damage) {
        TurretBulletEntity bullet = TurretBulletEntity.create(this.level(),
                this.getX(), this.getEyeY(), this.getZ(),
                dir.x * speed + this.random.nextGaussian() * spread,
                dir.y * speed + this.random.nextGaussian() * spread,
                dir.z * speed + this.random.nextGaussian() * spread,
                damage, ModItems.PARTICLE_DIGAMMA.get());
        bullet.setOwner(this);
        this.level().addFreshEntity(bullet);
    }

    private void playLaser(float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 5.0F, pitch);
    }
}
