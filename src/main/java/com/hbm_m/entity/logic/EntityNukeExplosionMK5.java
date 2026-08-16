package com.hbm_m.entity.logic;

import java.util.List;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;
import com.hbm_m.explosion.IExplosionRay;
import com.hbm_m.explosion.NoOpExplosionRay;
import com.hbm_m.explosion.NukeMk5ChunkEater;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Сущность длительного ядерного взрыва (MK5) для 1.20.1 Forge.
 * Управляет лучевым разрушением, уроном и (опционально) fallout-осадками.
 */
public class EntityNukeExplosionMK5 extends EntityExplosionChunkloading {

    /** Сила взрыва (масштаб радиуса и длины лучей). */
    public int strength;
    /** Количество лучей, вычисляемых за тик. */
    public int speed;
    /** Максимальная длина лучей. */
    public int length;

    private long explosionStart;
    public boolean fallout = true;
    private int falloutAdd = 0;

    private IExplosionRay explosion;

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
    protected int getChunkLoadRadius() {
        if (this.length <= 0) {
            return super.getChunkLoadRadius();
        }
        // Тикет должен покрывать кратер, НО уровень тикета = 33 - radius, и отрицательные
        // уровни системой тикетов не поддерживаются (ванильный FORCED использует максимум 31):
        // при радиусе >33 внешнее кольцо области вообще не догружается (замерено: 128 чанков
        // висели незагруженными вечно). Поэтому кап 31, а недостающие внешние чанки
        // NukeMk5ChunkEater догружает сам точечными тикетами радиуса 2 (уровень 31 = FULL).
        return Math.max(super.getChunkLoadRadius(), Math.min(31, ((this.length + 15) >> 4) + 3));
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        // нет синхронизируемых полей
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

        // нет синхронизируемых полей
    
    }
    *///?}

    @Override
    public void tick() {
        super.tick();

        if (strength == 0) {
            this.discard();
            return;
        }

        if (!level().isClientSide) {
            updateChunkTicket();
        }

        // радиация в первые тики после начала взрыва
        if (!level().isClientSide && applyInstantPlayerRads && explosion != null && this.tickCount < 10 && strength >= 75) {
            float baseRads = 2_500_000F / (this.tickCount * 5 + 1);
            radiate(baseRads, this.length * 2);
        }

        // урон и поджог живых сущностей
        if (applyEntityDamage) {
            ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.length * 2);
        }

        // лениво инициализируем лучевой движок
        if (explosion == null) {
            explosionStart = System.currentTimeMillis();
            MainRegistry.LOGGER.info("[NUKE MK5] Explosion entity started: algorithm={}, strength={}, speed={}, length={} at ({},{},{})",
                    ModClothConfig.get().explosionAlgorithm, strength, speed, length,
                    (int) getX(), (int) getY(), (int) getZ());
            if (destroyTerrain) {
                // 6.06_explosionAlgorithm: 0 = Legacy (Batched, однопоточный),
                // 1 = Threaded DDA, 2 = Threaded DDA с накоплением урона.
                // В оригинале 1.7.10 ветка 1/2 (ExplosionNukeRayParallelized) была закомментирована
                // и конфиг фактически не работал — здесь подключён по назначению.
                int algorithm = ModClothConfig.get().explosionAlgorithm;
                if ((algorithm == 1 || algorithm == 2) && level() instanceof ServerLevel server) {
                    explosion = new com.hbm_m.explosion.ExplosionNukeRayParallelized(
                            server,
                            getX(), getY(), getZ(),
                            strength, speed, length
                    );
                } else {
                    explosion = new NukeMk5ChunkEater(
                            level(),
                            (int) getX(),
                            (int) getY(),
                            (int) getZ(),
                            strength,
                            speed,
                            length
                    );
                }
            } else {
                explosion = NoOpExplosionRay.INSTANCE;
            }
        }

        int timeBudgetMs = ModClothConfig.get().mk5TickTimeMs;

        if (!explosion.isComplete()) {
            explosion.cacheChunksTick(timeBudgetMs);
            explosion.destructionTick(timeBudgetMs);
        } else {
            if (explosionStart != 0) {
                MainRegistry.LOGGER.info("[NUKE MK5] Explosion complete. Time elapsed: {}ms",
                        (System.currentTimeMillis() - explosionStart));
            }

            // Fallout будет реализован отдельной задачей (FalloutRain + RenderFallout).
            //
            // ВНИМАНИЕ: здесь НЕ должно быть прямого ChunkRadiationManager.incrementRad.
            // В 1.7.10 путь NukeBoy/NukeMan/NukeMike → EntityNukeExplosionMK5 НЕ накачивает
            // chunk-радиацию напрямую. Источников chunk radiation после взрыва block-fatman
            // в оригинале НЕТ (BlockSellafieldSlaked extends Block — инертный, MK5 только
            // применяет дозу живым сущностям через ContaminationUtil.contaminate).
            //
            // Радиация в кратере в 1.7.10 появляется ИСКЛЮЧИТЕЛЬНО через crater biomes:
            // EntityFalloutRain → getBiomeChange меняет биом на craterBiome/craterInnerBiome/
            // craterOuterBiome, а EntityEffectHandler.onUpdate каждые 20 тиков добавляет
            // игроку 25/5/0.5 RAD/сек соответственно (см. WorldConfig.craterBiome*Rad).

            if (fallout) {
                spawnFallout();
            }

            this.discard();
        }
    }

    /**
     * Применяет дозу радиации к живым существам по линии видимости.
     * В Modernized можно интегрировать с системой PlayerHandler/ChunkRadiationManager.
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
                        (int) Math.floor(getZ() + vec.z * i)
                );
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

    /**
     * Точка расширения для спавна FalloutRain и других осадков.
     * Реальная реализация будет добавлена при портировании FalloutRain.
     */
    private void spawnFallout() {
        int scale = (int) (this.length * 2.5 + getFalloutAdd());
        scale = scale * ModClothConfig.get().falloutRangePercent / 100;
        if (scale < 1) scale = 1;
        com.hbm_m.entity.effect.EntityFalloutRain fallout = new com.hbm_m.entity.effect.EntityFalloutRain(ModEntities.NUKE_FALLOUT_RAIN.get(), level());
        fallout.setPos(getX(), getY(), getZ());
        fallout.setScale(scale);
        WorldUtil.loadAndSpawnEntityInWorld(fallout);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (explosion != null) explosion.cancel();
        clearChunkTicket();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", this.tickCount);
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

