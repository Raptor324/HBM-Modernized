package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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

public class AirstrikeHeavyEntity extends Entity {

    // ✅ Параметры (увеличено расстояние спавна самолёта)
    private static final double AIRSTRIKE_HEIGHT = 80.0;  // Выше для драматичности
    private static final double SPAWN_DISTANCE = 150.0;    // 🆕 ДАЛЕКО ОТ ЦЕЛИ (было 75)
    private static final double ATTACK_RADIUS = 100;
    private static final double PLANE_SPEED = 1.25;         // Немного быстрее
    private static final int BOMB_INTERVAL = 12;           // Больше пауза между бомбами
    private static final int TOTAL_BOMBS = 4;              // 🆕 РОВНО 3 БОМБЫ
    private static final int DESPAWN_DELAY = 80;
    private static final int CHUNK_RETRY_DELAY = 60;

    private int bombTimer = 0;
    private int bombsDropped = 0;                          // 🆕 Счётчик сброшенных бомб
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
            SynchedEntityData.defineId(AirstrikeHeavyEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<String> OWNER_UUID_ACCESSOR =
            SynchedEntityData.defineId(AirstrikeHeavyEntity.class, EntityDataSerializers.STRING);

    public AirstrikeHeavyEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AirstrikeHeavyEntity(Level level, LivingEntity owner, BlockPos targetPos) {
        super(ModEntities.AIRSTRIKE_ENTITY.get(), level);
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

        // Поиск ближайшего загруженного чанка
        for (int chunkDist = 1; chunkDist <= 24; chunkDist++) {  // 🆕 Больше попыток
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
        // 🆕 Спавним Гораздо ДАЛЕЕ от цели (150 блоков вместо 75)
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
        // 🆕 Используем УВЕЛИЧЕННОЕ расстояние спавна
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

            // 🆕 Логика: сбрасываем РОВНО 3 бомбы в радиусе атаки
            if (!hasFinishedAttack && bombsDropped < TOTAL_BOMBS &&
                    distanceToCenterSq <= ATTACK_RADIUS * ATTACK_RADIUS) {
                bombTimer++;
                if (bombTimer >= BOMB_INTERVAL) {
                    dropAirBomb(target);  // 🆕 НОВЫЙ метод для авиабомб
                    bombTimer = 0;
                }
            } else if (bombsDropped >= TOTAL_BOMBS) {
                // 🆕 Завершаем атаку после 3 бомб
                hasFinishedAttack = true;
                despawnTimer = 0;
            } else if (!hasFinishedAttack && distanceToCenterSq > ATTACK_RADIUS * ATTACK_RADIUS) {
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

    private void dropAirBomb(BlockPos targetPos) {
        ServerLevel serverLevel = (ServerLevel) this.level();
        LivingEntity owner = getOwner();
        if (owner == null) return;

        // ✅ Направление самолёта (нормализованное)
        Vec3 planeDirection = this.getDeltaMovement().normalize();

        // ✅ ПЕРПЕНДИКУЛЯРНОЕ НАПРАВЛЕНИЕ ВПРАВО (90° по часовой)
        Vec3 rightDirection = new Vec3(-planeDirection.z, 0, planeDirection.x).normalize();

        // ✅ ТОЧКА СБРОСА: прямо под самолётом + 2 блока вправо
        Vec3 dropPos = new Vec3(
                this.getX() + rightDirection.x * 2.0,  // +2 блока по X вправо
                this.getY() - 2.0,                     // под самолётом
                this.getZ() + rightDirection.z * 2.0   // +2 блока по Z вправо
        );

        // ✅ ПЕРЕДАЁМ YAW САМОЛЁТА БОМБЕ!
        AirBombProjectileEntity airBomb = new AirBombProjectileEntity(
                serverLevel, owner, this.getYRot()
        );

        airBomb.setPos(dropPos);  // ← НОВАЯ ТОЧКА СБРОСА

        // Направление бомбы: параллельно самолёту + падение вниз
        Vec3 bombDirection = new Vec3(planeDirection.x, -0.8, planeDirection.z).normalize();
        airBomb.shoot(bombDirection.x, bombDirection.y, bombDirection.z, 1.8F, 0.2F);

        serverLevel.addFreshEntity(airBomb);
        bombsDropped++;
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
        this.bombsDropped = tag.getInt("BombsDropped");  // 🆕 Сохранение счётчика
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
        tag.putInt("BombsDropped", bombsDropped);  // 🆕 Сохранение счётчика
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
}
