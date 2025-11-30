package com.hbm_m.entity.grenades;

import com.hbm_m.block.IDetonatable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.ShockwaveGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GrenadeNucProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> TIMER_ACTIVATED = SynchedEntityData.defineId(GrenadeNucProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DETONATION_TIME = SynchedEntityData.defineId(GrenadeNucProjectileEntity.class, EntityDataSerializers.INT);

    // Параметры ядерной гранаты
    private static final int FUSE_SECONDS = 6; // Чуть дольше, чтобы успеть убежать
    private static final float EXPLOSION_POWER = 10.0f; // Мощный взрыв (ванильный TNT = 4.0f)
    private static final float RADIATION_RADIUS = 25.0f; // Радиус поражения радиацией
    private static final float MIN_BOUNCE_SPEED = 0.1f; // Тяжелая граната, меньше скачет
    private static final float BOUNCE_MULTIPLIER = 0.4f; // Гасит скорость сильнее при отскоке

    // Новые параметры для урона
    private static final float DAMAGE_RADIUS = 25.0f; // Радиус поражения урона (в блоках)
    private static final float DAMAGE_AMOUNT = 200.0f; // 150 урона = 75 полных сердец
    private static final float MAX_DAMAGE_DISTANCE = 25.0f; // Максимальное расстояние для полного урона

    private static final Random RANDOM = new Random();

    public GrenadeNucProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GrenadeNucProjectileEntity(Level level, LivingEntity thrower) {
        // Убедись, что зарегистрировал GRENADE_NUC_PROJECTILE в ModEntities
        super(ModEntities.GRENADE_NUC_PROJECTILE.get(), thrower, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIMER_ACTIVATED, false);
        this.entityData.define(DETONATION_TIME, 0);
    }

    @Override
    protected Item getDefaultItem() {
        // Ссылка на предмет ядерной гранаты
        return ModItems.GRENADE_NUC.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.entityData.get(TIMER_ACTIVATED)) {
                int detonationTime = this.entityData.get(DETONATION_TIME);
                if (this.tickCount >= detonationTime) {
                    explode(this.blockPosition());
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // Активация таймера при первом касании
            if (!this.entityData.get(TIMER_ACTIVATED)) {
                this.entityData.set(TIMER_ACTIVATED, true);
                this.entityData.set(DETONATION_TIME, this.tickCount + (FUSE_SECONDS * 20));
            }

            if (result.getType() == HitResult.Type.BLOCK) {
                handleBounce((BlockHitResult) result);
            }
        }
    }

    private void handleBounce(BlockHitResult result) {
        Vec3 velocity = this.getDeltaMovement();
        float speed = (float) velocity.length();
        if (speed < MIN_BOUNCE_SPEED) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true); // Останавливаемся, если скорость слишком мала
            return;
        }

        // Тяжелый звук удара
        BlockPos blockPos = result.getBlockPos();
        this.level().playSound(null, blockPos, ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 2.5F, 0.6F);
        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 hitNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));

        // Отскок слабее, так как граната тяжелая
        this.setDeltaMovement(reflectedVelocity.scale(BOUNCE_MULTIPLIER));
    }

    private void explode(BlockPos pos) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            // 1. Удаляем гранату
            this.discard();

            // 2. Основной взрыв (без разрушения блоков - за это кратер)
            serverLevel.explode(this, x, y, z, 9.0F, true, Level.ExplosionInteraction.NONE);

            // 3. Цепная детонация соседних IDetonatable блоков
            triggerNearbyDetonations(serverLevel, pos, null);

            // 4. Урон в зоне действия (НОВОЕ)
            dealExplosionDamage(serverLevel, x, y, z);

            // 5. Поэтапные эффекты взрыва
            scheduleExplosionEffects(serverLevel, x, y, z);

            // 6. Звук взрыва с повышенной громкостью (ИЗМЕНЕННОЕ)
            playRandomDetonationSound(level(), pos);

            // 7. Отложенный кратер (через 1.5 секунды = 30 тиков)
            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(30, () -> {
                    // Дополнительный взрыв для физики
                    serverLevel.explode(null, x, y, z, 9.0F, Level.ExplosionInteraction.NONE);

                    // Генерация кратера с ядерными блоками (замени на свои)
                    ShockwaveGenerator.generateCrater(
                            serverLevel,
                            pos,
                            25, // CRATER_RADIUS = 20 блоков (под гранату поменьше)
                            10, // CRATER_DEPTH = 6 блоков
                            ModBlocks.WASTE_LOG.get(), // заменить на твои радиоактивные блоки
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get()
                    );
                }));
            }
        }
    }

    // НОВЫЙ МЕТОД: Нанесение урона в зоне действия гранаты
    private void dealExplosionDamage(ServerLevel serverLevel, double x, double y, double z) {
        List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(x - DAMAGE_RADIUS, y - DAMAGE_RADIUS, z - DAMAGE_RADIUS,
                        x + DAMAGE_RADIUS, y + DAMAGE_RADIUS, z + DAMAGE_RADIUS)
        );

        for (LivingEntity entity : entitiesNearby) {
            double distanceToEntity = Math.sqrt(
                    Math.pow(entity.getX() - x, 2) +
                            Math.pow(entity.getY() - y, 2) +
                            Math.pow(entity.getZ() - z, 2)
            );

            // Проверяем, находится ли сущность в радиусе поражения
            if (distanceToEntity <= DAMAGE_RADIUS) {
                // Вычисляем урон в зависимости от расстояния
                // На близком расстоянии (0-5 блоков): полный урон
                // На среднем расстоянии (5-20 блоков): уменьшающийся урон
                // На дальнем расстоянии (20+ блоков): минимальный урон
                float damage = DAMAGE_AMOUNT;

                if (distanceToEntity > MAX_DAMAGE_DISTANCE) {
                    // Линейное снижение урона на расстоянии
                    float remainingDistance = DAMAGE_RADIUS - MAX_DAMAGE_DISTANCE;
                    float damageDistance = (float) distanceToEntity - MAX_DAMAGE_DISTANCE;
                    damage = DAMAGE_AMOUNT * (1.0f - (damageDistance / remainingDistance)) * 0.5f; // Минимум 50% урона
                }

                // Наносим урон с источником (взрыв) - метатель тоже получает урон!
                entity.hurt(entity.damageSources().explosion(null), damage);

            }
        }
    }

    private void playRandomDetonationSound(Level level, BlockPos pos) {
        List<SoundEvent> sounds = Arrays.asList(
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null)
        );
        sounds.removeIf(Objects::isNull);
        if (!sounds.isEmpty()) {
            SoundEvent sound = sounds.get(RANDOM.nextInt(sounds.size()));
            // Повышенная громкость: с 1.0F на 4.0F для дальней слышимости
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS, 4.0F, 1.0F);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("TimerActivated", this.entityData.get(TIMER_ACTIVATED));
        tag.putInt("DetonationTime", this.entityData.get(DETONATION_TIME));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TIMER_ACTIVATED, tag.getBoolean("TimerActivated"));
        this.entityData.set(DETONATION_TIME, tag.getInt("DetonationTime"));
    }

    // === ПЕРЕНЕСЕННЫЕ МЕТОДЫ ИЗ SmokeBombBlock ===

    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // Фаза 1: Яркая вспышка (мгновенно)
        spawnFlash(level, x, y, z);

        // Фаза 2: Искры (0-10 тиков)
        spawnSparks(level, x, y, z);

        // Фаза 3: Взрывная волна (5 тиков задержки)
        level.getServer().tell(new net.minecraft.server.TickTask(5, () -> {
            spawnShockwave(level, x, y, z);
        }));

        // Фаза 4: Грибовидное облако (10 тиков задержки)
        level.getServer().tell(new net.minecraft.server.TickTask(10, () -> {
            spawnMushroomCloud(level, x, y, z);
        }));
    }

    private void spawnFlash(ServerLevel level, double x, double y, double z) {
        level.sendParticles(
                ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );
    }

    private void spawnSparks(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 300; i++) { // Немного больше искр для ядерки
            double xSpeed = (level.random.nextDouble() - 0.5) * 5.0;
            double ySpeed = level.random.nextDouble() * 4.0;
            double zSpeed = (level.random.nextDouble() - 0.5) * 5.0;
            level.sendParticles(
                    ModExplosionParticles.EXPLOSION_SPARK.get(),
                    x, y, z, 1, xSpeed, ySpeed, zSpeed, 1.2
            );
        }
    }

    private void spawnShockwave(ServerLevel level, double x, double y, double z) {
        // 4 кольца для более мощного эффекта
        for (int ring = 0; ring < 4; ring++) {
            double ringY = y + (ring * 0.4);
            level.sendParticles(
                    ModExplosionParticles.SHOCKWAVE.get(),
                    x, ringY, z, 1, 0, 0, 0, 0
            );
        }
    }

    private void spawnMushroomCloud(ServerLevel level, double x, double y, double z) {
        // Стебель гриба (больше дыма)
        for (int i = 0; i < 120; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 5.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 5.0;
            double ySpeed = 0.6 + level.random.nextDouble() * 0.4;
            level.sendParticles(
                    ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, y, z + offsetZ,
                    1, offsetX * 0.06, ySpeed, offsetZ * 0.06, 1.2
            );
        }

        // Шапка гриба (больше и выше)
        for (int i = 0; i < 200; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 6.0 + level.random.nextDouble() * 10.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double capY = y + 18 + level.random.nextDouble() * 8;
            double xSpeed = Math.cos(angle) * 0.4;
            double ySpeed = -0.05 + level.random.nextDouble() * 0.15;
            double zSpeed = Math.sin(angle) * 0.4;
            level.sendParticles(
                    ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, capY, z + offsetZ,
                    1, xSpeed, ySpeed, zSpeed, 1.2
            );
        }
    }

    private void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos, Player player) {
        int DETONATION_RADIUS = 8; // Немного больше для ядерки
        for (int x = -DETONATION_RADIUS; x <= DETONATION_RADIUS; x++) {
            for (int y = -DETONATION_RADIUS; y <= DETONATION_RADIUS; y++) {
                for (int z = -DETONATION_RADIUS; z <= DETONATION_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();
                        if (block instanceof IDetonatable detonatable) {
                            int delay = (int)(dist * 1.5); // Быстрее цепная реакция
                            serverLevel.getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                                detonatable.onDetonate(serverLevel, checkPos, checkState, player);
                            }));
                        }
                    }
                }
            }
        }
    }
}