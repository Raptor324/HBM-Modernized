package com.hbm_m.entity.mob.botprime;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code EntityBOTPrimeBody} - one of the seventy-four segments trailing the head.
 *
 * <p>Two details are worth keeping in mind: a segment hits for three quarters of its victim's
 * <em>current</em> health, so it can never quite finish anyone off on its own; and a segment that
 * has lost the part in front of it deletes itself by taking 1999 damage a tick, which is how the
 * original cleans up a broken chain.</p>
 */
public class EntityBOTPrimeBody extends EntityBOTPrimeBase {

    public EntityBOTPrimeBody(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.rangeForParts = 70.0D;
        this.segmentDistance = 3.5D;
        this.maxBodySpeed = 1.4D;
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public float getAttackStrength(@NotNull Entity target) {
        if (target instanceof LivingEntity living) {
            return living.getHealth() * 0.75F;
        }
        return 100F;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance effect) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        updateActionState();
        super.customServerAiStep();
        updateMovement();

        if (this.didCheck) {
            // Orphaned segment: bleed out fast rather than hang around detached.
            if (this.targetedEntity == null || !this.targetedEntity.isAlive()) {
                this.setHealth(this.getHealth() - 1999.0F);
            }
            // And occasionally cook off while doing it.
            if ((this.followed == null || !this.followed.isAlive()) && this.random.nextInt(60) == 0) {
                this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                        2.0F, Level.ExplosionInteraction.NONE);
            }
        }

        if (this.followed != null && this.followed.isAlive() && this.getTarget() != null) {
            if (canSeeThroughNonSolids(this.getTarget())) {
                this.attackCounter++;
                if (this.attackCounter == 10) {
                    laserAttack(this.getTarget(), false);
                    // Negative so there is a long pause before the next shot winds up.
                    this.attackCounter = -20;
                }
            } else if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        } else if (this.attackCounter > 0) {
            this.attackCounter--;
        }

        faceFollowed();
    }

    @Override
    public void tick() {
        super.tick();
        faceFollowed();
    }
}
