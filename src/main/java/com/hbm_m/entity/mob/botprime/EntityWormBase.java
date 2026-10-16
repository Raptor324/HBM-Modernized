package com.hbm_m.entity.mob.botprime;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 1:1 port of {@code EntityWormBaseNT} + {@code EntityBurrowingNT}, merged - the latter is only
 * four overrides and is marked {@code @Deprecated} in the original anyway.
 *
 * <p>A worm is one head plus a long chain of body segments. Each segment chases the one in front
 * of it at a fixed distance rather than pathing on its own, which is what makes the whole thing
 * move like a rope. Damage dealt to a segment is forwarded up the chain, so only the head has a
 * real health pool.</p>
 */
public abstract class EntityWormBase extends PathfinderMob {

    public int aggroCooldown = 0;
    public int courseChangeCooldown = 0;
    public double waypointX;
    public double waypointY;
    public double waypointZ;

    @Nullable protected Entity targetedEntity = null;
    @Nullable protected LivingEntity followed;

    protected boolean canFly = false;
    protected int dmgCooldown = 0;
    protected boolean wasNearGround;
    protected BlockPos spawnPoint = BlockPos.ZERO;
    protected double attackRange;
    protected double maxSpeed;
    protected double fallSpeed;
    protected double rangeForParts;
    protected int surfaceY = 60;
    protected boolean didCheck;
    protected double maxBodySpeed;
    protected double segmentDistance;
    protected double knockbackDivider;

    /** In-air and in-ground drag, from {@code EntityBurrowingNT}. */
    protected float dragInAir = 0.995F;
    protected float dragInGround = 0.98F;

    private int headID;
    private int partNum;

    protected EntityWormBase(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public int getPartNumber()          { return this.partNum; }
    public void setPartNumber(int num)  { this.partNum = num; }
    public int getHeadID()              { return this.headID; }
    public void setHeadID(int id)       { this.headID = id; }

    @Nullable
    public Entity getHead() {
        return this.level().getEntity(this.headID);
    }

    /** Overridden by the head to return true. */
    public boolean getIsHead() {
        return false;
    }

    public abstract float getAttackStrength(Entity target);

    // ─── Damage routing ──────────────────────────────────────────────────────

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_WALL)) {
            return false;
        }
        // A worm cannot hurt itself, and segments of the same worm cannot hurt each other.
        if (source.getEntity() instanceof EntityWormBase other && other.getHeadID() == this.getHeadID()) {
            return false;
        }

        this.markHurt();

        if (this.getIsHead()) {
            return super.hurt(source, amount);
        }

        // Body segments pass damage forward to whatever they are following.
        Entity ahead = this.targetedEntity;
        return ahead != null ? ahead.hurt(source, amount) : super.hurt(source, amount);
    }

    // ─── Movement ────────────────────────────────────────────────────────────

    /**
     * {@code moveEntityWithHeading}: a worm does not walk, it swims through whatever it is inside.
     * Drag is higher in solid ground than in air, and body segments are draggier than the head.
     */
    @Override
    public void travel(@NotNull Vec3 input) {
        float drag = this.dragInGround;
        if (!this.isInWall() && !this.isInWater() && !this.isInLava()) {
            drag = this.dragInAir;
        }
        if (!this.getIsHead()) {
            drag *= 0.9F;
        }

        this.moveRelative(0.02F, input);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(drag));
    }

    /** {@code updateEntityActionState}: keeps the worm out of the void and mauls anything touching it. */
    protected void updateActionState() {
        if (!this.level().isClientSide
                && this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
            return;
        }

        if (this.targetedEntity != null && !this.targetedEntity.isAlive()) {
            this.targetedEntity = null;
        }

        if (this.getY() < -10.0D) {
            this.setDeltaMovement(this.getDeltaMovement().x, 1.0D, this.getDeltaMovement().z);
        } else if (this.getY() < 3.0D) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.3D, this.getDeltaMovement().z);
        }

        if (this.tickCount % 5 == 0) {
            attackEntitiesInList(this.level().getEntities(this, this.getBoundingBox().inflate(0.5D)));
        }
    }

    protected void attackEntitiesInList(List<Entity> targets) {
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity)) continue;
            if (target instanceof EntityWormBase worm && worm.getHeadID() == this.getHeadID()) continue;
            doHurtTarget(target);
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        boolean hit = target.hurt(this.damageSources().mobAttack(this), getAttackStrength(target));

        if (hit) {
            // The original flings the victim away along the vector from the segment's centre, at a
            // strength that falls off with distance - a body-check, not a knockback.
            Vec3 centre = this.getBoundingBox().getCenter();
            double dx = target.getX() - centre.x;
            double dy = target.getY() - centre.y;
            double dz = target.getZ() - centre.z;
            double knockback = this.knockbackDivider * (dx * dx + dy * dy + dz * dz + 0.1D);
            target.push(dx / knockback, dy / knockback, dz / knockback);
        }

        return hit;
    }

    /** Worms cannot be pushed around and never turn to face anything. */
    @Override public void push(double x, double y, double z) { }
    @Override public boolean isPushable()                    { return false; }
    @Override public boolean causeFallDamage(float d, float m, @NotNull DamageSource s) { return false; }
    @Override public boolean onClimbable()                   { return false; }
    @Override protected float getSoundVolume()               { return 5.0F; }

    @Override
    protected float getStandingEyeHeight(@NotNull net.minecraft.world.entity.Pose pose,
                                         @NotNull net.minecraft.world.entity.EntityDimensions size) {
        return size.height * 0.5F;
    }

    protected boolean isCourseTraversable() {
        return this.canFly || this.isInWall();
    }

    /**
     * {@code updateMovement}: the segment logic. Each part homes on the one in front, slowing hard
     * once it is inside {@code segmentDistance} so the chain concertinas rather than piling up.
     */
    protected void updateMovement() {
        double targetingRange = 128.0D;

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < targetingRange * targetingRange) {
            this.waypointX = this.targetedEntity.getX();
            this.waypointY = this.targetedEntity.getY();
            this.waypointZ = this.targetedEntity.getZ();
        }

        if ((this.tickCount % 60 == 0 || this.tickCount == 1)
                && (this.targetedEntity == null || this.followed == null)) {
            AABB search = this.getBoundingBox().inflate(this.rangeForParts);
            findEntityToFollow(this.level().getEntitiesOfClass(EntityWormBase.class, search));
        }

        double dx = this.waypointX - this.getX();
        double dy = this.waypointY - this.getY();
        double dz = this.waypointZ - this.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1.0E-4D) return;

        double speed = Math.max(0.0D, Math.min(distance - this.segmentDistance, this.maxBodySpeed));

        if (distance < this.segmentDistance * 0.895D) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
        } else {
            this.setDeltaMovement(dx / distance * speed, dy / distance * speed, dz / distance * speed);
        }
    }

    /**
     * Finds this segment's place in the chain: part 0 follows the head, part N follows part N-1.
     * Only segments sharing this worm's head id are considered.
     */
    protected void findEntityToFollow(List<EntityWormBase> segments) {
        for (EntityWormBase segment : segments) {
            if (segment.getHeadID() != this.getHeadID()) continue;

            if (segment.getIsHead()) {
                if (this.getPartNumber() == 0) this.targetedEntity = segment;
                this.followed = segment;
            } else if (segment.getPartNumber() == this.getPartNumber() - 1) {
                this.targetedEntity = segment;
            }
        }
        this.didCheck = true;
    }

    /**
     * Points the segment along the vector to whatever it is chasing. The original assigns prev and
     * current rotation in the same statement so the part snaps rather than interpolating - a body
     * segment that lerped its rotation would visibly lag behind the one ahead of it.
     */
    protected void faceFollowed() {
        if (this.targetedEntity == null) return;
        double dx = this.targetedEntity.getX() - this.getX();
        double dy = this.targetedEntity.getY() - this.getY();
        double dz = this.targetedEntity.getZ() - this.getZ();
        float flat = (float) Math.sqrt(dx * dx + dz * dz);

        float yaw   = (float) (Math.atan2(dx, dz) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(dy, flat) * 180.0D / Math.PI);

        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("wormID", this.getHeadID());
        tag.putInt("partID", this.getPartNumber());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setHeadID(tag.getInt("wormID"));
        this.setPartNumber(tag.getInt("partID"));
    }
}
