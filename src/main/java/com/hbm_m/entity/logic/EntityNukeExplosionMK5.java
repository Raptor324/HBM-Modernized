// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.entity.logic;

import java.util.List;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.ExplosionNukeRayBatched;
import com.hbm_m.explosion.ExplosionNukeRayParallelized;
import com.hbm_m.explosion.IExplosionRay;
import com.hbm_m.explosion.NoOpExplosionRay;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;
import com.hbm_m.util.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Сущность длительного ядерного взрыва (MK5).
 *
 * <p>Логика переделана на бюджетную модель: движок взрыва выполняет работу в
 * {@link IExplosionRay#update(long)} с ms-бюджетом на тик (конфиг
 * {@code mk5TickTimeMs}), чанки грузятся по требованию с капом in-flight и не
 * удерживаются тикетами после обработки. Состояние взрыва в NBT НЕ сохраняется —
 * после перезахода взрыв пересчитывается (уже разрушенные блоки — воздух).
 */
public class EntityNukeExplosionMK5 extends EntityExplosionChunkloading
        implements com.hbm_m.explosion.BombForkJoinPool.IJobCancellable {

    /** Сила взрыва (масштаб радиуса и длины лучей). */
    public int strength;
    /** Количество лучей за тик — легаси-поле, движки больше не используют его. */
    public int speed;
    /** Максимальная длина лучей (радиус кратера). */
    public int length;

    private long explosionStart;
    public boolean fallout = true;
    private int falloutAdd = 0;

    private IExplosionRay explosion;
    private boolean initialized = false;

    /** Разрушение блоков лучами MK5 (кратер). */
    public boolean destroyTerrain = true;
    /** Урон сущностям через {@link ExplosionNukeGeneric}. */
    public boolean applyEntityDamage = true;
    /** Импульсная доза игрокам в первые тики (радиация). */
    public boolean applyInstantPlayerRads = true;
    /** Смена биомов после fallout (если включено в конфиге мода). */
    public boolean applyCraterBiomes = true;

    /** Для API: задать дополнительный радиус fallout. */
    public void setFalloutAdd(int add) {
        this.falloutAdd = add;
    }

    public int getFalloutAdd() {
        return falloutAdd;
    }

    public EntityNukeExplosionMK5(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (strength == 0) {
            this.discard();
            return;
        }

        if (!level().isClientSide) {
            // Аварийный выключатель (ориг. ключ 6.00): лимит жизни взрыва в секундах.
            // tickCount персистится в NBT, поэтому лимит срабатывает и после перезахода —
            // можно остановить зависший/нежелательный взрыв правкой конфига.
            int lifespan = ModClothConfig.get().limitExplosionLifespan;
            if (lifespan > 0 && this.tickCount > lifespan * 20) {
                MainRegistry.LOGGER.warn("[NUKE MK5] Explosion exceeded lifespan limit ({}s), discarding", lifespan);
                this.discard();
                return;
            }

            if (this.tickCount <= 1) {
                com.hbm_m.advancement.ModAdvancements.grantAll(level(),
                        com.hbm_m.advancement.ModAdvancements.MANHATTAN);
            }

            // радиация в первые тики после начала взрыва
            if (applyInstantPlayerRads && initialized && this.tickCount < 10 && strength >= 75) {
                float baseRads = 2_500_000F / (this.tickCount * 5 + 1);
                radiate(baseRads, this.length * 2);
            }

            // урон и поджог живых сущностей
            if (applyEntityDamage) {
                ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.length * 2);
            }
        }

        if (!level().isClientSide && !initialized) {
            explosionStart = System.currentTimeMillis();
            createExplosionEngine();
            initialized = true;
        }

        if (explosion == null) return;

        if (explosion.hasFailed()) {
            this.discard();
        } else if (!explosion.isComplete()) {
            explosion.update(ModClothConfig.get().mk5TickTimeMs);
        } else {
            if (explosionStart != 0) {
                MainRegistry.LOGGER.info("[NUKE MK5] Explosion complete. Time elapsed: {}ms",
                        (System.currentTimeMillis() - explosionStart));
            }

            // Радиация в кратере появляется ИСКЛЮЧИТЕЛЬНО через crater biomes:
            // EntityFalloutRain меняет биом на craterBiome/craterInnerBiome/craterOuterBiome,
            // а EntityEffectHandler добавляет дозу по WorldConfig.craterBiome*Rad.
            if (fallout) {
                spawnFallout();
            }

            this.discard();
        }
    }

    /**
     * 6.06_explosionAlgorithm: 0 = Legacy (Batched, однопоточный),
     * 1 = Threaded DDA, 2 = Threaded DDA с накоплением урона.
     */
    private void createExplosionEngine() {
        // Original: eine ausgewachsene Zuendung ist eine volle Minute lang aus dem Orbit zu sehen.
        com.hbm_m.satellite.DetectorEvents.reportEvent(level(), com.hbm_m.satellite.DetectorEvents.DURATION_HIGH,
                com.hbm_m.satellite.DetectorEvents.BurstIntensity.HIGH, getX(), getZ());

        if (!destroyTerrain) {
            explosion = NoOpExplosionRay.INSTANCE;
        } else {
            // Массовые операции: включить параллельный сейв чанков (ChunkMapSaveMixin)
            if (!level().isClientSide) {
                hbm$massOpAcquired = true;
                com.hbm_m.util.ChunkSaveParallelizer.acquireMassOp();
            }
            int algorithm = ModClothConfig.get().explosionAlgorithm;
            MainRegistry.LOGGER.info("[NUKE MK5] Explosion started: algorithm={}, strength={}, length={} at ({},{},{})",
                    algorithm, strength, length, (int) getX(), (int) getY(), (int) getZ());
            if ((algorithm == 1 || algorithm == 2) && level() instanceof ServerLevel server) {
                explosion = new ExplosionNukeRayParallelized(
                        server, getX(), getY(), getZ(), strength, length, algorithm);
            } else {
                explosion = new ExplosionNukeRayBatched(
                        level(),
                        (int) getX(),
                        (int) getY(),
                        (int) getZ(),
                        strength,
                        length);
            }
        }
    }

    /** Активен ли наш счётчик массовых операций (защита от двойного release). */
    private boolean hbm$massOpAcquired;

    /**
     * Применяет дозу радиации к живым существам по линии видимости.
     */
    private void radiate(float rads, double range) {
        AABB box = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, box);

        for (LivingEntity e : entities) {
            Vec3 vec = new Vec3(e.getX() - getX(), (e.getEyeY()) - getY(), e.getZ() - getZ());
            double len = vec.length();
            if (len <= 0) continue;

            vec = vec.normalize();

            float res = 0;

            for (int i = 1; i < len; i++) {
                BlockPos pos = new BlockPos(
                        (int) Math.floor(getX() + vec.x * i),
                        (int) Math.floor(getY() + vec.y * i),
                        (int) Math.floor(getZ() + vec.z * i));
                BlockState state = level().getBlockState(pos);
                res += state.getBlock().getExplosionResistance();
            }

            if (res < 1) res = 1;

            float eRads = rads;
            eRads /= res;
            eRads /= (float) (len * len);

            ContaminationUtil.contaminate(e, HazardType.RADIATION, ContaminationType.RAD_BYPASS, eRads);
        }
    }

    private void spawnFallout() {
        int scale = (int) (this.length * 2.5 + getFalloutAdd());
        scale = scale * ModClothConfig.get().falloutRangePercent / 100;
        if (scale < 1) scale = 1;
        com.hbm_m.entity.effect.EntityFalloutRain fallout = new com.hbm_m.entity.effect.EntityFalloutRain(ModEntities.NUKE_FALLOUT_RAIN.get(), level());
        fallout.setPos(getX(), getY(), getZ());
        fallout.setScale(scale);
        fallout.applyCraterBiomes = this.applyCraterBiomes;
        WorldUtil.loadAndSpawnEntityInWorld(fallout);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (explosion != null) explosion.cancel();
        clearChunkTicket();
        if (hbm$massOpAcquired) {
            hbm$massOpAcquired = false;
            com.hbm_m.util.ChunkSaveParallelizer.releaseMassOp();
        }
        super.remove(reason);
    }

    @Override
    public void cancelJob() {
        if (level() instanceof ServerLevel server) {
            if (server.getServer().isSameThread()) this.discard();
            else server.getServer().execute(this::discard);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("age");
        this.strength = tag.getInt("strength");
        this.speed = tag.getInt("speed");
        this.length = tag.getInt("length");
        this.destroyTerrain = tag.getBoolean("destroyTerrain");
        this.applyEntityDamage = tag.getBoolean("applyEntityDamage");
        this.applyInstantPlayerRads = tag.getBoolean("applyInstantPlayerRads");
        this.applyCraterBiomes = !tag.contains("applyCraterBiomes") || tag.getBoolean("applyCraterBiomes");
        this.fallout = tag.getBoolean("fallout");
        this.falloutAdd = tag.getInt("falloutAdd");
        // Старые сейвы несут "explosionState" (снапшот ChunkEater) — намеренно НЕ читаем:
        // движки состояния не персистируют, взрыв пересчитывается с нуля.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", this.tickCount);
        tag.putInt("strength", this.strength);
        tag.putInt("speed", this.speed);
        tag.putInt("length", this.length);
        tag.putBoolean("destroyTerrain", this.destroyTerrain);
        tag.putBoolean("applyEntityDamage", this.applyEntityDamage);
        tag.putBoolean("applyInstantPlayerRads", this.applyInstantPlayerRads);
        tag.putBoolean("applyCraterBiomes", this.applyCraterBiomes);
        tag.putBoolean("fallout", this.fallout);
        tag.putInt("falloutAdd", this.falloutAdd);
    }

    /**
     * Статическая фабрика для запуска взрыва MK5.
     */
    public static EntityNukeExplosionMK5 start(Level level, int strength, double x, double y, double z) {
        if (strength == 0) strength = 25;
        strength *= 2;

        EntityNukeExplosionMK5 explosionMK5 = new EntityNukeExplosionMK5(ModEntities.NUKE_MK5.get(), level);
        explosionMK5.strength = strength;
        explosionMK5.speed = (int) Math.ceil(100000D / explosionMK5.strength);
        explosionMK5.setPos(x, y, z);
        explosionMK5.length = explosionMK5.strength / 2;
        WorldUtil.loadAndSpawnEntityInWorld(explosionMK5);
        return explosionMK5;
    }
}
