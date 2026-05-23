package com.hbm_m.entity.missile;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import api.hbm.entity.IRadarDetectable;

import com.hbm_m.explosion.MissileWarheadEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Базовая сущность баллистической ракеты.
 *
 * Управляет собственной баллистикой без гравитации и сопротивления среды,
 * как в оригинале EntityMissileBaseNT: вертикальная подача с torsion-замедлением
 * и горизонтальный разгон к точке цели.
 *
 * Дополнительно держит чанк‑тикет на текущем чанке, чтобы ракета не выгружалась
 * на больших дистанциях полёта.
 */
public abstract class MissileBaseEntity extends Projectile implements IRadarDetectable {

    /** Тикет на загрузку чанка под ракетой. */
    private static final TicketType<UUID> CHUNK_TICKET =
            TicketType.create("hbm_m_missile", Comparator.comparing(UUID::toString));

    /** Радиус region ticket'а (в чанках). */
    private static final int CHUNK_TICKET_RADIUS = 3;

    protected int startX;
    protected int startZ;
    protected int targetX;
    protected int targetZ;
    protected double velocity;
    protected double decelY;
    protected double accelXZ;
    protected boolean isCluster = false;
    protected int health = 50;
    protected boolean exploded = false;

    private static final EntityDataAccessor<Direction> DATA_LAUNCH_FACING =
            SynchedEntityData.defineId(MissileBaseEntity.class, EntityDataSerializers.DIRECTION);

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    private ChunkPos loadedChunk;

    protected MissileBaseEntity(EntityType<? extends MissileBaseEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.startX = (int) this.getX();
        this.startZ = (int) this.getZ();
        this.targetX = this.startX;
        this.targetZ = this.startZ;
    }

    /**
     * Инициализация полёта при старте с пусковой площадки.
     */
    public void initLaunch(double x, double y, double z, int targetX, int targetZ) {
        this.setPos(x, y, z);
        this.startX = (int) this.getX();
        this.startZ = (int) this.getZ();
        this.targetX = targetX;
        this.targetZ = targetZ;

        Vec3 vec = new Vec3(this.targetX - this.startX, 0, this.targetZ - this.startZ);
        double len = vec.length();
        if (len == 0) {
            len = 1.0D;
        }
        this.accelXZ = this.decelY = 1.0D / len;
        this.decelY *= 2.0D;
        this.velocity = 0.0D;

        this.setDeltaMovement(this.getDeltaMovement().x(), 2.0D, this.getDeltaMovement().z());

        float yaw = (float) (Math.atan2(this.targetX - this.getX(), this.targetZ - this.getZ()) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(0.0F);
        this.xRotO = 0.0F;
    }

    public void setLaunchFacing(Direction facing) {
        Direction dir = facing == null ? Direction.NORTH : facing;
        this.entityData.set(DATA_LAUNCH_FACING, dir);
    }

    public Direction getLaunchFacing() {
        return this.entityData.get(DATA_LAUNCH_FACING);
    }

    protected List<ItemStack> getDebris() {
        return MissileWarheadEffects.defaultDebrisForTier(debrisTierIndex());
    }

    @javax.annotation.Nullable
    protected ItemStack getDebrisRareDrop() {
        return ItemStack.EMPTY;
    }

    protected int debrisTierIndex() {
        return switch (getTargetType()) {
            case MISSILE_TIER4 -> 4;
            case MISSILE_TIER3 -> 3;
            case MISSILE_TIER2 -> 2;
            case MISSILE_TIER1 -> 1;
            default -> 0;
        };
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER0;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps + 1;
    }

    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    private void clientLerpStep() {
        if (this.lerpSteps > 0) {
            float d = 1.0F / this.lerpSteps;
            this.setPos(
                    Mth.lerp(d, this.getX(), this.lerpX),
                    Mth.lerp(d, this.getY(), this.lerpY),
                    Mth.lerp(d, this.getZ(), this.lerpZ));
            this.setYRot((float) Mth.rotLerp(d, this.getYRot(), (float) this.lerpYRot));
            this.setXRot((float) Mth.rotLerp(d, this.getXRot(), (float) this.lerpXRot));
            --this.lerpSteps;
        } else {
            this.reapplyPosition();
        }
    }

    /**
     * Полностью кастомный тик: мы не наследуем поведение от ThrowableProjectile,
     * поэтому здесь нет ни гравитации, ни сопротивления воздуха.
     */
    @Override
    public void tick() {
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();

        if (!this.level().isClientSide) {
            this.baseTick();
            updateChunkTicket();

            if (this.isRemoved()) {
                return;
            }

            double mult = this.velocity;
            Vec3 motion = this.getDeltaMovement();
            Vec3 step = motion.scale(mult);

            if (step.lengthSqr() > 0.0D) {
                Vec3 from = this.position();
                Vec3 to = from.add(step);
                BlockHitResult blockHit = this.level().clip(
                        new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (blockHit.getType() != HitResult.Type.MISS && !this.exploded) {
                    this.onHit(blockHit);
                }
            }

            if (this.isRemoved()) {
                return;
            }

            this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
            updateRotationFromMotion();
            normalizeRotationDeltas();

            serverTickLogic();
        } else {
            clientLerpStep();
            spawnContrail();
        }
    }

    private void normalizeRotationDeltas() {
        for (this.xRotO = this.getXRot(); this.getXRot() - this.xRotO < -180.0F; this.xRotO -= 360.0F) {
        }
        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
    }

    private void serverTickLogic() {
        if (this.velocity < 4.0D) {
            this.velocity += Mth.clamp((double) this.tickCount / 60.0D * 0.05D, 0.0D, 0.05D);
        }

        Vec3 motion = this.getDeltaMovement();

        if (hasPropulsion()) {
            double motionY = motion.y - this.decelY * this.velocity;

            Vec3 vector = new Vec3(this.targetX - this.startX, 0, this.targetZ - this.startZ).normalize();
            vector = new Vec3(vector.x * this.accelXZ, 0, vector.z * this.accelXZ);

            double motionX = motion.x;
            double motionZ = motion.z;

            if (motionY > 0) {
                motionX += vector.x * this.velocity;
                motionZ += vector.z * this.velocity;
            }

            if (motionY < 0) {
                motionX -= vector.x * this.velocity;
                motionZ -= vector.z * this.velocity;
            }

            motion = new Vec3(motionX, motionY, motionZ);
        } else {
            motion = motion.multiply(0.99D, 1.0D, 0.99D);
            if (motion.y > -1.5D) {
                motion = motion.add(0.0D, -0.05D, 0.0D);
            }
        }

        if (motion.y < -this.velocity && this.isCluster) {
            cluster();
            releaseChunkTicket();
            this.discard();
            return;
        }

        this.setDeltaMovement(motion);
    }

    protected boolean hasPropulsion() {
        return true;
    }

    protected float getContrailScale() {
        return 1.0F;
    }

    protected void spawnContrail() {
        spawnContrailWithOffset(0.0D, 0.0D, 0.0D);
    }

    protected void spawnContrailWithOffset(double offsetX, double offsetY, double offsetZ) {
        if (!(this.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return;
        }

        Vec3 motion = new Vec3(this.xOld - this.getX(), this.yOld - this.getY(), this.zOld - this.getZ());
        double len = motion.length();
        if (len <= 0.0D) {
            return;
        }
        motion = motion.normalize();

        Vec3 thrust = new Vec3(0.0D, 1.0D, 0.0D);
        thrust = thrust.xRot(-this.getXRot() * ((float) Math.PI / 180.0F));
        thrust = thrust.yRot(-(this.getYRot() + 90.0F) * ((float) Math.PI / 180.0F));

        float contrailScale = getContrailScale();
        com.hbm_m.particle.custom.MissileContrailParticle.currentSpawnScale = contrailScale;
        try {
            for (int i = 0; i < Math.max(Math.min(len * 4.0D, 10.0D), 1.0D); i++) {
                double t = i / 4.0D;
                double px = this.getX() + motion.x * t + offsetX;
                double py = this.getY() + motion.y * t + offsetY;
                double pz = this.getZ() + motion.z * t + offsetZ;

                clientLevel.addParticle(
                        com.hbm_m.particle.ModParticleTypes.MISSILE_CONTRAIL.get(),
                        px, py, pz,
                        -thrust.x * 0.1D,
                        -thrust.y * 0.1D,
                        -thrust.z * 0.1D);
            }
        } finally {
            com.hbm_m.particle.custom.MissileContrailParticle.currentSpawnScale = 1.0F;
        }
    }

    private void updateRotationFromMotion() {
        Vec3 motion = this.getDeltaMovement();
        double f2 = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float pitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90.0F;
        float yaw = (float) (Math.atan2(this.targetX - this.getX(), this.targetZ - this.getZ()) * 180.0D / Math.PI);

        this.setXRot(pitch);
        this.setYRot(yaw);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (this.level().isClientSide || this.exploded) {
            return;
        }

        this.exploded = true;
        BlockPos pos;
        if (result.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult) result).getBlockPos();
        } else {
            pos = this.blockPosition();
        }

        onMissileImpact(pos);
        releaseChunkTicket();
        this.discard();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    protected abstract void onMissileImpact(BlockPos pos);

    public boolean canBeDetectedByRadar() {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LAUNCH_FACING, Direction.NORTH);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StartX", this.startX);
        tag.putInt("StartZ", this.startZ);
        tag.putInt("TargetX", this.targetX);
        tag.putInt("TargetZ", this.targetZ);
        tag.putDouble("Velocity", this.velocity);
        tag.putDouble("DecelY", this.decelY);
        tag.putDouble("AccelXZ", this.accelXZ);
        tag.putInt("Health", this.health);
        tag.putString("LaunchFacing", this.getLaunchFacing().getName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.startX = tag.getInt("StartX");
        this.startZ = tag.getInt("StartZ");
        this.targetX = tag.getInt("TargetX");
        this.targetZ = tag.getInt("TargetZ");
        this.velocity = tag.getDouble("Velocity");
        this.decelY = tag.getDouble("DecelY");
        this.accelXZ = tag.getDouble("AccelXZ");
        this.health = tag.getInt("Health");
        if (tag.contains("LaunchFacing")) {
            Direction facing = Direction.byName(tag.getString("LaunchFacing"));
            this.setLaunchFacing(facing == null ? Direction.NORTH : facing);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isInvulnerableTo(source) && !this.level().isClientSide) {
            if (this.health > 0) {
                this.health -= (int) amount;
                if (this.health <= 0) {
                    killMissile();
                }
            }
            return true;
        }
        return false;
    }

    protected void killMissile() {
        if (this.isRemoved()) {
            return;
        }
        releaseChunkTicket();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            Vec3 motion = this.getDeltaMovement();
            MissileWarheadEffects.missileDestroyed(server, this,
                    this.getX(), this.getY(), this.getZ(),
                    motion, getDebris(), getDebrisRareDrop());
        }
        this.discard();
    }

    protected void cluster() {
        if (this.level().isClientSide || this.exploded) {
            return;
        }
        this.exploded = true;
        onMissileImpact(BlockPos.containing(getX(), getY(), getZ()));
    }

    private void updateChunkTicket() {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        ChunkPos newPos = new ChunkPos(this.blockPosition());
        if (newPos.equals(this.loadedChunk)) {
            return;
        }
        if (this.loadedChunk != null) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
        }
        this.loadedChunk = newPos;
        server.getChunkSource().addRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
    }

    protected void releaseChunkTicket() {
        if (this.loadedChunk == null) {
            return;
        }
        if (this.level() instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
        }
        this.loadedChunk = null;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            this.loadedChunk = new ChunkPos(this.blockPosition());
            server.getChunkSource().addRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        releaseChunkTicket();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, MobCategory.MISC.ordinal());
    }
}
