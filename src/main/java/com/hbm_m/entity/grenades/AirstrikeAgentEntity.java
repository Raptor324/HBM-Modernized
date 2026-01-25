package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.UUID;

/**
 * ☠️ САМОЛЁТ-РАСПЫЛИТЕЛЬ AGENT ORANGE
 *
 * Летит над целью и НЕПРЕРЫВНО распыляет отравленные частицы
 */
public class AirstrikeAgentEntity extends Entity {

    // ✅ Параметры полёта
    private static final double AIRSTRIKE_HEIGHT = 50.0;
    private static final double SPAWN_DISTANCE = 150.0;
    private static final double ATTACK_RADIUS = 30.0;
    private static final double PLANE_SPEED = 1.25;

    // ☠️ ПАРАМЕТРЫ РАСПЫЛЕНИЯ
    private static final int SPRAY_INTERVAL = 3;              // Распылять каждые 3 тика (0.15 сек)
    private static final int PARTICLES_PER_SPRAY = 10;        // 30 частиц за раз (густое облако)

    private static final int DESPAWN_DELAY = 80;
    private static final int CHUNK_RETRY_DELAY = 60;

    private int sprayTimer = 0;                               // Таймер распыления
    private int despawnTimer = -1;
    private int chunkRetryTimer = 0;
    private boolean hasFinishedAttack = false;
    private boolean isWaitingForChunk = false;
    private Vec3 direction = Vec3.ZERO;

    private static final Random RANDOM = new Random();

    // ✅ 8 НАПРАВЛЕНИЙ
    private static final double[] DIRECTION_ANGLES = {
            0.0, Math.PI/4, Math.PI/2, 3*Math.PI/4,
            Math.PI, 5*Math.PI/4, 3*Math.PI/2, 7*Math.PI/4
    };

    private static final EntityDataAccessor<BlockPos> TARGET_POS =
            SynchedEntityData.defineId(AirstrikeAgentEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<String> OWNER_UUID_ACCESSOR =
            SynchedEntityData.defineId(AirstrikeAgentEntity.class, EntityDataSerializers.STRING);

    public AirstrikeAgentEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AirstrikeAgentEntity(Level level, LivingEntity owner, BlockPos targetPos) {
        super(ModEntities.AIRSTRIKE_ENTITY.get(), level); // ← Используй СУЩЕСТВУЮЩИЙ тип!
        this.noPhysics = true;

        this.entityData.set(TARGET_POS, targetPos);
        this.entityData.set(OWNER_UUID_ACCESSOR, owner.getUUID().toString());

        if (trySpawnInLoadedChunk(targetPos)) {
            initializePath();
        } else {
            isWaitingForChunk = true;
        }
    }


    private boolean trySpawnInLoadedChunk(BlockPos targetPos) {
        ServerLevel serverLevel = (ServerLevel) this.level();
        Vec3 startPos = calculateStartPos(targetPos);
        if (isChunkLoaded(serverLevel, startPos)) {
            this.setPos(startPos);
            return true;
        }

        for (int chunkDist = 1; chunkDist <= 24; chunkDist++) {
            for (int chunkX = -chunkDist; chunkX <= chunkDist; chunkX++) {
                for (int chunkZ = -chunkDist; chunkZ <= chunkDist; chunkZ++) {
                    if (Math.abs(chunkX) == chunkDist || Math.abs(chunkZ) == chunkDist) {
                        BlockPos candidatePos = targetPos.offset(chunkX * 16, 0, chunkZ * 16);
                        Vec3 candidateStart = calculateStartPos(candidatePos);
                        if (isChunkLoaded(serverLevel, candidateStart)) {
                            this.setPos(candidateStart);
                            this.entityData.set(TARGET_POS, candidatePos);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Vec3 calculateStartPos(BlockPos target) {
        int directionIndex = RANDOM.nextInt(8);
        double angle = DIRECTION_ANGLES[directionIndex];
        Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();
        return Vec3.atCenterOf(target).subtract(dir.scale(SPAWN_DISTANCE)).add(0, AIRSTRIKE_HEIGHT, 0);
    }

    private boolean isChunkLoaded(ServerLevel level, Vec3 pos) {
        return level.hasChunkAt(BlockPos.containing(pos));
    }

    private void initializePath() {
        BlockPos target = getTargetPos();
        int directionIndex = RANDOM.nextInt(8);
        double angle = DIRECTION_ANGLES[directionIndex];
        this.direction = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();
        Vec3 startPos = Vec3.atCenterOf(target)
                .subtract(this.direction.scale(SPAWN_DISTANCE))
                .add(0, AIRSTRIKE_HEIGHT, 0);
        this.setPos(startPos);
        this.setDeltaMovement(this.direction.scale(PLANE_SPEED));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_POS, BlockPos.ZERO);
        this.entityData.define(OWNER_UUID_ACCESSOR, "");
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (isWaitingForChunk) {
                chunkRetryTimer++;
                if (chunkRetryTimer >= CHUNK_RETRY_DELAY) {
                    BlockPos targetPos = getTargetPos();
                    if (trySpawnInLoadedChunk(targetPos)) {
                        isWaitingForChunk = false;
                        initializePath();
                    } else {
                        chunkRetryTimer = 0;
                    }
                }
                return;
            }

            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.position().add(motion));

            if (motion.lengthSqr() > 1.0E-7) {
                float targetYaw = (float) (Math.toDegrees(Math.atan2(motion.z, motion.x)) - 90.0F);
                float currentYaw = this.getYRot();
                float newYaw = yawRotationSpeed(currentYaw, targetYaw, 5.0F);
                this.setYRot(newYaw);
                this.yRotO = newYaw;
            }

            playAmbientSound();

            BlockPos target = getTargetPos();
            Vec3 targetCenter = Vec3.atCenterOf(target);
            Vec3 relativePos = this.position().subtract(targetCenter);
            double distanceToCenterSq = relativePos.horizontalDistanceSqr();

            // ════════════════════════════════════════════════════════════════
            // ☠️ РАСПЫЛЕНИЕ AGENT ORANGE НАД ЗОНОЙ АТАКИ
            // ════════════════════════════════════════════════════════════════

            if (!hasFinishedAttack && distanceToCenterSq <= ATTACK_RADIUS * ATTACK_RADIUS) {
                sprayTimer++;
                if (sprayTimer >= SPRAY_INTERVAL) {
                    sprayAgentOrange();
                    sprayTimer = 0;
                }
            } else if (!hasFinishedAttack && distanceToCenterSq > ATTACK_RADIUS * ATTACK_RADIUS) {
                // Проверяем, пролетели ли мы зону атаки
                Vec3 toTarget = targetCenter.subtract(this.position());
                if (toTarget.dot(this.direction) < 0) {
                    hasFinishedAttack = true;
                    despawnTimer = 0;
                }
            }

            if (hasFinishedAttack) {
                despawnTimer++;
                if (despawnTimer > DESPAWN_DELAY) {
                    this.discard();
                }
            }
        }
    }

    private void sprayAgentOrange() {
        ServerLevel serverLevel = (ServerLevel) this.level();

        // вектор "вправо" от направления полёта
        Vec3 right = new Vec3(-this.direction.z, 0, this.direction.x).normalize();
        // 2 блока влево = -2 * right
        Vec3 lateralOffset = right.scale(1.25);

        // базовая точка под самолётом
        Vec3 base = this.position().add(0, -2.5, 0);

        // итоговая позиция распыления: 2 блока влево от направления
        Vec3 sprayPos = base.add(lateralOffset);

        // ✅ СПАВНИМ ГУСТОЕ ОБЛАКО AGENT ORANGE
        ExplosionParticleUtils.spawnAgentOrange(
                serverLevel,
                sprayPos.x,
                sprayPos.y,
                sprayPos.z,
                PARTICLES_PER_SPRAY
        );

        // 🔊 Звук сброса/распыления
        serverLevel.playSound(
                null,
                sprayPos.x, sprayPos.y, sprayPos.z,
                net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                4.0F,
                0.7F
        );
    }


    private void playAmbientSound() {
        if (RANDOM.nextBoolean()) {
            if (ModSounds.BOMBER1.isPresent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.BOMBER1.get(), SoundSource.HOSTILE, 6.0F, 1.0F);
            }
        } else {
            if (ModSounds.BOMBER2.isPresent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.BOMBER2.get(), SoundSource.HOSTILE, 6.0F, 1.0F);
            }
        }
    }

    private float yawRotationSpeed(float currentYaw, float targetYaw, float maxChange) {
        float f = wrapDegrees(targetYaw - currentYaw);
        if (f > maxChange) f = maxChange;
        if (f < -maxChange) f = -maxChange;
        return currentYaw + f;
    }

    private float wrapDegrees(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    public BlockPos getTargetPos() {
        return this.entityData.get(TARGET_POS);
    }

    public LivingEntity getOwner() {
        try {
            UUID uuid = UUID.fromString(this.entityData.get(OWNER_UUID_ACCESSOR));
            return this.level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetX")) {
            this.entityData.set(TARGET_POS, new BlockPos(
                    tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ")
            ));
        }
        if (tag.contains("OwnerUUID")) {
            this.entityData.set(OWNER_UUID_ACCESSOR, tag.getString("OwnerUUID"));
        }
        this.isWaitingForChunk = tag.getBoolean("WaitingForChunk");
        this.chunkRetryTimer = tag.getInt("ChunkRetryTimer");
        this.sprayTimer = tag.getInt("SprayTimer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        BlockPos target = getTargetPos();
        tag.putInt("TargetX", target.getX());
        tag.putInt("TargetY", target.getY());
        tag.putInt("TargetZ", target.getZ());
        tag.putString("OwnerUUID", this.entityData.get(OWNER_UUID_ACCESSOR));
        tag.putBoolean("WaitingForChunk", isWaitingForChunk);
        tag.putInt("ChunkRetryTimer", chunkRetryTimer);
        tag.putInt("SprayTimer", sprayTimer);
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
}
