package com.hbm_m.entity.mob.ai;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import com.hbm_m.entity.mob.EntityPigeon;
import com.hbm_m.entity.mob.IFlyingCreature;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityAIStartFlying}, {@code EntityAIStopFlying}, {@code EntityAISwimmingConditional},
 * {@code EntityAIWanderConditional} und {@code EntityAIEatBread}.
 */
public final class FlyingCreatureGoals {

    private FlyingCreatureGoals() { }

    /** Abheben bei Angriff, Feuer oder zufaellig (im Schnitt 30 s). */
    public static class StartFlying extends Goal {
        private final LivingEntity living;
        private final IFlyingCreature flying;

        public StartFlying(LivingEntity living, IFlyingCreature flying) {
            this.living = living;
            this.flying = flying;
        }

        @Override
        public boolean canUse() {
            return this.flying.getFlyingState() == IFlyingCreature.STATE_WALKING && (this.living.getLastHurtByMob() != null || this.living.isOnFire() || this.living.getRandom().nextInt(600) == 0);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.flying.setFlyingState(IFlyingCreature.STATE_FLYING);
        }
    }

    public static class StopFlying extends Goal {
        private final LivingEntity living;
        private final IFlyingCreature flying;

        public StopFlying(LivingEntity living, IFlyingCreature flying) {
            this.living = living;
            this.flying = flying;
        }

        @Override
        public boolean canUse() {
            return this.flying.getFlyingState() == IFlyingCreature.STATE_FLYING && this.living.getRandom().nextInt(200) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.flying.setFlyingState(IFlyingCreature.STATE_WALKING);
        }
    }

    /** Wie die Schwimm-KI, aber nur wenn die Bedingung gilt. */
    public static class SwimmingConditional extends Goal {
        private final Mob living;
        private final Predicate<Mob> condition;

        public SwimmingConditional(Mob living, Predicate<Mob> condition) {
            this.living = living;
            this.condition = condition;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP));
            living.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return (this.living.isInWater() || this.living.isInLava()) && condition.test(living);
        }

        @Override
        public void tick() {
            if (this.living.getRandom().nextFloat() < 0.8F) {
                this.living.getJumpControl().jump();
            }
        }
    }

    /** Umherwandern (1/120, Ziel 10/7) nur, wenn die Bedingung gilt. */
    public static class WanderConditional extends Goal {
        private final PathfinderMob creature;
        private final double speed;
        private final Predicate<Mob> condition;
        private double xPosition, yPosition, zPosition;

        public WanderConditional(PathfinderMob creature, double speed, Predicate<Mob> condition) {
            this.creature = creature;
            this.speed = speed;
            this.condition = condition;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {

            if (!condition.test(creature)) return false;

            if (this.creature.getNoActionTime() >= 100) {
                return false;
            } else if (this.creature.getRandom().nextInt(120) != 0) {
                return false;
            } else {
                Vec3 vec3 = DefaultRandomPos.getPos(this.creature, 10, 7);

                if (vec3 == null) {
                    return false;
                } else {
                    this.xPosition = vec3.x;
                    this.yPosition = vec3.y;
                    this.zPosition = vec3.z;
                    return true;
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !this.creature.getNavigation().isDone() && condition.test(creature);
        }

        @Override
        public void start() {
            this.creature.getNavigation().moveTo(this.xPosition, this.yPosition, this.zPosition, this.speed);
        }
    }

    /** Laeuft zu Brot in 10 Bloecken, frisst (1/3 verbraucht eines) und wird dick. */
    public static class EatBread extends Goal {
        private final EntityPigeon pigeon;
        private final double speed;
        private ItemEntity item;

        public EatBread(EntityPigeon pigeon, double speed) {
            this.pigeon = pigeon;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (pigeon.isFat() || pigeon.getFlyingState() != IFlyingCreature.STATE_WALKING) return false;

            List<ItemEntity> items = pigeon.level().getEntitiesOfClass(ItemEntity.class, this.pigeon.getBoundingBox().inflate(10, 10, 10));

            for (ItemEntity item : items) {
                if (item.getItem().is(Items.BREAD)) {
                    this.item = item;
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.item != null && this.item.isAlive() && this.canUse();
        }

        @Override
        public void tick() {
            this.pigeon.getLookControl().setLookAt(this.item, 30.0F, (float) this.pigeon.getMaxHeadXRot());

            if (this.pigeon.distanceTo(this.item) > 1) {
                this.pigeon.getNavigation().moveTo(this.item, this.speed);
            } else {

                if (this.pigeon.getRandom().nextInt(3) == 0) {
                    ItemStack stack = this.item.getItem();

                    if (stack.getCount() > 1) {
                        stack.shrink(1);
                        ItemEntity newItem = new ItemEntity(this.pigeon.level(), this.item.getX(), this.item.getY(), this.item.getZ(), stack.copy());
                        this.pigeon.level().addFreshEntity(newItem);
                    }

                    this.item.discard();
                }
                this.pigeon.setFat(true);
                this.pigeon.playSound(SoundEvents.GENERIC_EAT, 0.5F + 0.5F * this.pigeon.getRandom().nextInt(2), (this.pigeon.getRandom().nextFloat() - this.pigeon.getRandom().nextFloat()) * 0.2F + 1.0F);
            }
        }
    }
}
