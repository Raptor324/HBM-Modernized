package com.hbm_m.entity.mob.ai;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityAIBreaking}: steckt der Angreifer fest (kein Pfad, Ziel weiter als 1 Block und am Boden oder
 * nicht sichtbar), bricht er den naechsten Block in Blickrichtung ab. Haltbarkeit = Haerte / 3, 0.05 pro Tick.
 */
public class BreakingGoal extends Goal {

    LivingEntity target;
    BlockPos markedLoc;
    final Mob entityDigger;
    int digTick = 0;
    int scanTick = 0;

    public BreakingGoal(Mob entity) {
        this.entityDigger = entity;
    }

    @Override
    public boolean canUse() {
        target = entityDigger.getTarget();

        if (target != null && entityDigger.getNavigation().isDone() && entityDigger.distanceTo(target) > 1D && (target.onGround() || !entityDigger.hasLineOfSight(target))) {
            BlockHitResult mop = getNextObstical(entityDigger, 2D);

            if (mop == null) {
                return false;
            }

            BlockState block = entityDigger.level().getBlockState(mop.getBlockPos());

            if (block.getDestroySpeed(entityDigger.level(), mop.getBlockPos()) >= 0) {
                markedLoc = mop.getBlockPos();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {

        if (markedLoc != null) {
            Vec3 vector = new Vec3(
                    markedLoc.getX() - entityDigger.getX(),
                    markedLoc.getY() - (entityDigger.getY() + entityDigger.getEyeHeight()),
                    markedLoc.getZ() - entityDigger.getZ());

            return entityDigger.isAlive() && vector.length() <= 4;
        }

        return false;
    }

    @Override
    public void tick() {
        BlockHitResult mop = null;

        if (entityDigger.tickCount % 10 == 0) {
            mop = getNextObstical(entityDigger, 2D);
        }

        if (mop != null) {
            markedLoc = mop.getBlockPos();
        }

        Level world = entityDigger.level();

        if (markedLoc == null || world.getBlockState(markedLoc).isAir()) {
            digTick = 0;
            return;
        }

        BlockState block = world.getBlockState(markedLoc);
        digTick++;

        int health = (int) block.getDestroySpeed(world, markedLoc) / 3;

        if (health < 0) {
            markedLoc = null;
            return;
        }

        float str = (digTick * 0.05F) / (float) health;

        if (str >= 1F) {
            digTick = 0;

            world.destroyBlock(markedLoc, false);
            markedLoc = null;

            if (target != null)
                entityDigger.getNavigation().moveTo(target, 1D);
        } else {
            if (digTick % 5 == 0) {
                var sound = block.getSoundType();
                world.playSound(null, entityDigger.getX(), entityDigger.getY(), entityDigger.getZ(), sound.getStepSound(), SoundSource.HOSTILE, sound.getVolume() + 1F, sound.getPitch());
                entityDigger.swing(InteractionHand.MAIN_HAND);
                world.destroyBlockProgress(entityDigger.getId(), markedLoc, (int) (str * 10F));
            }
        }
    }

    @Override
    public void stop() {
        markedLoc = null;
        digTick = 0;
    }

    /** Rastert die Kastenpunkte und castet in Blickrichtung (bei Bipeds 2 Punkte). */
    @Nullable
    public BlockHitResult getNextObstical(LivingEntity entityLiving, double dist) {
        float f1 = entityLiving.getXRot();
        float f2 = entityLiving.getYRot();

        int digWidth = Mth.ceil(entityLiving.getBbWidth());
        int digHeight = Mth.ceil(entityLiving.getBbHeight());

        int passMax = digWidth * digWidth * digHeight;

        int x = scanTick % digWidth - (digWidth / 2);
        int y = scanTick / (digWidth * digWidth);
        int z = (scanTick % (digWidth * digWidth)) / digWidth - (digWidth / 2);

        double rayX = x + entityLiving.getX();
        double rayY = y + entityLiving.getY();
        double rayZ = z + entityLiving.getZ();

        BlockHitResult mop = rayCastBlocks(entityLiving.level(), rayX, rayY, rayZ, f2, f1, dist);

        if (mop != null && mop.getType() == HitResult.Type.BLOCK) {
            BlockState block = entityLiving.level().getBlockState(mop.getBlockPos());

            if (block.getDestroySpeed(entityLiving.level(), mop.getBlockPos()) >= 0) {
                scanTick = 0;
                return mop;
            } else {
                scanTick = (scanTick + 1) % passMax;
                return null;
            }
        } else {
            scanTick = (scanTick + 1) % passMax;
            return null;
        }
    }

    public static BlockHitResult rayCastBlocks(Level world, double x, double y, double z, float yaw, float pitch, double dist) {
        Vec3 vec3 = new Vec3(x, y, z);
        float f3 = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f4 = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f5 = -Mth.cos(-pitch * 0.017453292F);
        float f6 = Mth.sin(-pitch * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        Vec3 vec31 = vec3.add((double) f7 * dist, (double) f6 * dist, (double) f8 * dist);
        return world.clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
    }
}
