package com.hbm_m.entity.mob;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBurrowingSwingingBase}: "schwingende" Wegfindung - das Wesen taucht wie ein Delfin aus dem Boden
 * auf und wieder ein, steuert auf Wegpunkte um den Spawnpunkt bzw. das Ziel zu und dreht sich mit der Bewegung.
 * Alle Ableitungen sind feindlich.
 */
public abstract class EntityBurrowingSwingingBase extends EntityBurrowingBase {

    @Nullable protected Entity targetedEntity = null;
    protected boolean wasNearGround = false;
    public int courseChangeCooldown = 0;
    public int aggroCooldown = 0;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    protected BlockPos spawnPoint = BlockPos.ZERO;
    public boolean lastInGround = false;

    protected EntityBurrowingSwingingBase(EntityType<? extends EntityBurrowingSwingingBase> type, Level world) {
        super(type, world);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Original EntityAINearestAttackableTargetNT mit fester Reichweite 128
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null) {
            @Override
            protected double getFollowDistance() {
                return 128.0D;
            }
        });
    }

    /** {@code EntityLiving.canAttackClass}: alles ausser Creeper und Ghast. */
    protected static boolean canAttackClass(Entity e) {
        return e.getClass() != Creeper.class && e.getClass() != Ghast.class;
    }

    @Override
    public void tick() {
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        super.tick();

        Vec3 m = this.getDeltaMovement();
        double dx = m.x;
        double dy = m.y;
        double dz = m.z;
        float f3 = Mth.sqrt((float) (dx * dx + dz * dz));
        this.setYRot((float) (Math.atan2(dx, dz) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(dy, f3) * 180.0D / Math.PI));

        boolean inGround = isInsideOpaqueBlock();

        if (this.lastInGround != inGround) {
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.DEBRIS.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            } else {
                double mod = 0.25D;

                if (inGround)
                    mod *= -1;

                for (int i = 0; i < 10; i++) {
                    double dev = 0.05D;
                    level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), dx * mod + random.nextGaussian() * dev, dy * mod + random.nextGaussian() * dev, dz * mod + random.nextGaussian() * dev);
                }
            }
        }

        this.lastInGround = inGround;
    }

    /** Original {@code updateAITasks}: eigene Steuerung, dann die Vanilla-KI, dann die Schwingbewegung. */
    @Override
    protected void customServerAiStep() {
        this.updateEntityActionState();
        if (this.isRemoved()) return;
        super.customServerAiStep();

        updateSwingingMovement();
    }

    protected void updateEntityActionState() {

        if (!this.level().isClientSide && this.level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if ((this.targetedEntity != null) && (!this.targetedEntity.isAlive())) {
            this.targetedEntity = null;
        }
        // Original -10 / 3 bezogen auf den Weltboden 0
        int bottom = this.level().getMinBuildHeight();
        if (this.getY() < bottom - 10.0D) {
            this.setDeltaMovement(getDeltaMovement().x, 1D, getDeltaMovement().z);
        } else if (this.getY() < bottom + 3.0D) {
            this.setDeltaMovement(getDeltaMovement().x, 0.3D, getDeltaMovement().z);
        }

        if (this.tickCount % 2 == 0) {
            attackEntitiesInList(this.level().getEntities(this, this.getBoundingBox().inflate(0.5D, 0.5D, 0.5D)));
        }
    }

    protected void attackEntitiesInList(List<Entity> targets) {

        for (Entity target : targets) {
            if (target instanceof LivingEntity && canAttackClass(target)) {
                doHurtTarget(target);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {

        boolean hit = target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));

        if (hit) {
            this.setNoActionTime(0);
            double tx = (this.getBoundingBox().minX + this.getBoundingBox().maxX) / 2.0D;
            double tz = (this.getBoundingBox().minZ + this.getBoundingBox().maxZ) / 2.0D;
            double ty = (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D;
            double deltaX = target.getX() - tx;
            double deltaZ = target.getZ() - tz;
            double deltaY = target.getY() - ty;
            double knockback = 0.5 * (deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY + 0.1D);
            target.setDeltaMovement(target.getDeltaMovement().add(deltaX / knockback, deltaY / knockback, deltaZ / knockback));
            target.hurtMarked = true;
        }

        return hit;
    }

    protected void updateSwingingMovement() {

        double deltaX = this.waypointX - this.getX();
        double deltaY = this.waypointY - this.getY();
        double deltaZ = this.waypointZ - this.getZ();
        double delta = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        if (this.courseChangeCooldown-- <= 0) {

            this.courseChangeCooldown += this.getRandom().nextInt(5) + 5;

            double speed = this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();

            if (!this.canSupportMovement()) {
                speed /= 4D;
            }

            if (delta > 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(deltaX / delta * speed, deltaY / delta * speed, deltaZ / delta * speed));
            }
        }

        if (!this.canSupportMovement() && !this.wasNearGround) {
            //? if < 1.21.1 {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -this.getGravity(), 0));
            //?} else {
            /*this.setDeltaMovement(this.getDeltaMovement().add(0, -this.hbmGravity(), 0));
            *///?}
        }

        this.aggroCooldown--;

        if (this.getTarget() != null) {

            if (this.aggroCooldown <= 0) {
                this.targetedEntity = this.getTarget();
                this.aggroCooldown = getAggroCooldown();
            }

        } else if (this.targetedEntity == null) {
            this.waypointX = this.spawnPoint.getX() - 50 + this.getRandom().nextInt(100);
            this.waypointY = this.spawnPoint.getY() - 30 + this.getRandom().nextInt(60);
            this.waypointZ = this.spawnPoint.getZ() - 50 + this.getRandom().nextInt(100);
        }

        Vec3 m = this.getDeltaMovement();
        this.setYRot(-(float) -(Math.atan2(m.x, m.z) * 180.0F / Math.PI));
        this.setXRot((float) -(Math.atan2(m.y, Math.sqrt(m.x * m.x + m.z * m.z)) * 180.0D / Math.PI));

        double range = 100;
        if (this.targetedEntity != null && this.targetedEntity.distanceToSqr(this) < range * range) {

            if (this.canSupportMovement() || this.wasNearGround) {

                this.waypointX = this.targetedEntity.getX();
                this.waypointY = this.targetedEntity.getY() + this.targetedEntity.getBbHeight() * 0.5;
                this.waypointZ = this.targetedEntity.getZ();

                int surface = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(this.waypointX), Mth.floor(this.waypointZ));

                if (this.getRandom().nextInt(80) == 0 && this.getY() > surface && !this.canSupportMovement()) {
                    this.wasNearGround = false;
                }

            } else {

                this.waypointX = this.targetedEntity.getX();
                this.waypointY = 10.0D;
                this.waypointZ = this.targetedEntity.getZ();

                if (this.getY() < 15.0D) {
                    this.wasNearGround = true;
                }
            }
        } else {
            this.waypointX = this.spawnPoint.getX() - 20 + this.getRandom().nextInt(40);
            this.waypointY = this.spawnPoint.getY() - 5 + this.getRandom().nextInt(100);
            this.waypointZ = this.spawnPoint.getZ() - 20 + this.getRandom().nextInt(40);
        }
    }

    //? if < 1.21.1 {
    protected double getGravity() {
        return 0.01;
    }
    //?} else {
    /*// 1.21.1: Entity.getGravity() ist final - eigener Name, gleiche Logik
    protected double hbmGravity() {
        return 0.01;
    }
    *///?}

    protected int getAggroCooldown() {
        return 20;
    }

    /** Original {@code addVelocity}: Rueckstoss wird ignoriert. */
    @Override
    public void push(double x, double y, double z) { }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("spawnX", this.spawnPoint.getX());
        nbt.putInt("spawnY", this.spawnPoint.getY());
        nbt.putInt("spawnZ", this.spawnPoint.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.spawnPoint = new BlockPos(nbt.getInt("spawnX"), nbt.getInt("spawnY"), nbt.getInt("spawnZ"));
    }

    @Nullable
    @Override
    //? if < 1.21.1 {
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
    //?} else {
    /*public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        net.minecraft.nbt.CompoundTag tag = null;
    *///?}
        this.spawnPoint = new BlockPos(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
        //? if < 1.21.1 {
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
        //?} else {
        /*return super.finalizeSpawn(level, difficulty, reason, data);
        *///?}
    }
}
