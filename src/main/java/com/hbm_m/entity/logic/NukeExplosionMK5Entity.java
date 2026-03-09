package com.hbm_m.entity.logic;

import java.util.List;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.IExplosionRay;
import com.hbm_m.explosion.NukeMk5ChunkEater;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.explosions.nuclear.CraterBiomeHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Сущность длительного ядерного взрыва (MK5) для 1.20.1 Forge.
 * Управляет лучевым разрушением, уроном и (опционально) fallout-осадками.
 */
public class NukeExplosionMK5Entity extends ChunkloadingEntity {

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

    /** Для API: задать дополнительный радиус fallout. */
    public void setFalloutAdd(int add) {
        this.falloutAdd = add;
    }

    public int getFalloutAdd() {
        return falloutAdd;
    }

    public NukeExplosionMK5Entity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        // нет синхронизируемых полей
    }

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
        if (!level().isClientSide && fallout && explosion != null && this.tickCount < 10 && strength >= 75) {
            float baseRads = 2_500_000F / (this.tickCount * 5 + 1);
            radiate(baseRads, this.length * 2);
        }

        // урон и поджог живых сущностей
        ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.length * 2);

        // лениво инициализируем лучевой движок
        if (explosion == null) {
            explosionStart = System.currentTimeMillis();
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

        int timeBudgetMs = ModClothConfig.get().mk5TickTimeMs;

        if (!explosion.isComplete()) {
            explosion.cacheChunksTick(timeBudgetMs);
            explosion.destructionTick(timeBudgetMs);
        } else {
            if (ModClothConfig.get().enableDebugLogging && explosionStart != 0) {
                MainRegistry.LOGGER.info("[NUKE MK5] Explosion complete. Time elapsed: {}ms",
                        (System.currentTimeMillis() - explosionStart));
            }

            // Fallout будет реализован отдельной задачей (FalloutRain + RenderFallout)
            // Здесь оставляем точку расширения.
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
                res += state.getExplosionResistance(level(), pos, null);
            }

            if (res < 1) res = 1;

            float eRads = rads;
            eRads /= res;
            eRads /= (float) (len * len);

            if (e instanceof Player player) {
                PlayerHandler.incrementPlayerRads(player, eRads);
            }
            // (ContaminationUtil на любом LivingEntity) при появлении системы контаминации сущностей можно применить eRads и к не-игрокам.
        }
    }

    /**
     * Точка расширения для спавна FalloutRain и других осадков.
     * Реальная реализация будет добавлена при портировании FalloutRain.
     */
    private void spawnFallout() {
        int scale = (int) (this.length * 3.5 + getFalloutAdd());
        scale = scale * ModClothConfig.get().falloutRangePercent / 100;
        if (scale < 1) scale = 1;
        com.hbm_m.entity.effect.FalloutRain fallout = new com.hbm_m.entity.effect.FalloutRain(ModEntities.NUKE_FALLOUT_RAIN.get(), level());
        fallout.setPos(getX(), getY(), getZ());
        fallout.setScale(scale);
        level().addFreshEntity(fallout);

        if (ModClothConfig.get().enableCraterBiomes && level() instanceof ServerLevel serverLevel) {
            BlockPos center = BlockPos.containing(getX(), getY(), getZ());
            double innerRadius = Math.min(15, scale * 0.1);
            CraterBiomeHelper.applyBiomesAsync(serverLevel, center, innerRadius, (double) scale);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (explosion != null) explosion.cancel();
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
    public static NukeExplosionMK5Entity start(Level level, int strength, double x, double y, double z) {
        if (strength == 0) strength = 25;
        strength *= 2;

        NukeExplosionMK5Entity explosionMK5 = new NukeExplosionMK5Entity(ModEntities.NUKE_MK5.get(), level);
        explosionMK5.strength = strength;
        explosionMK5.speed = (int) Math.ceil(100000D / explosionMK5.strength);
        explosionMK5.setPos(x, y, z);
        explosionMK5.length = explosionMK5.strength / 2;
        level.addFreshEntity(explosionMK5);
        return explosionMK5;
    }
}

