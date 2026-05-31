package com.hbm_m.explosion;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.BlackHoleEntity;
import com.hbm_m.entity.logic.EmpPulseEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.explosions.ServerExplosionParticles;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Общие эффекты боеголовок баллистических ракет.
 * Визуалы — {@link ExplosionParticleUtils}; разрушение блоков — vanilla {@code level.explode}.
 * Полный паритет заблокирован до порта ExplosionVNT.
 * Контрейл на Fabric: {@code ClientSetup.registerParticlesCommon()} (iteration 6).
 */
public final class MissileWarheadEffects {

    private MissileWarheadEffects() {
    }

    public static void empPulse(Level level, double x, double y, double z) {
        EmpPulseEntity emp = new EmpPulseEntity(ModEntities.EMP_PULSE.get(), level);
        emp.setPos(x, y, z);
        level.addFreshEntity(emp);
    }

    public static void blackHole(Level level, double x, double y, double z) {
        level.explode(null, x, y, z, 5.0F, Level.ExplosionInteraction.BLOCK);
        BlackHoleEntity hole = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), level);
        hole.setPos(x, y, z);
        level.addFreshEntity(hole);
    }

    public static void scatterTaint(Level level, BlockPos center) {
        level.explode(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                5.0F, Level.ExplosionInteraction.BLOCK);

        for (int i = 0; i < 100; i++) {
            int a = level.random.nextInt(11) + center.getX() - 5;
            int b = level.random.nextInt(11) + center.getY() - 5;
            int c = level.random.nextInt(11) + center.getZ() - 5;
            BlockPos randPos = new BlockPos(a, b, c);
            BlockState state = level.getBlockState(randPos);
            if (BlockTaint.canBeReplacedByTaint(level, randPos, state)) {
                level.setBlock(randPos, ModBlocks.TAINT.get().defaultBlockState(), 3);
            }
        }
    }

    public static void standardExplode(Entity source, Level level, BlockPos pos, float power, boolean causesFire) {
        level.explode(source, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                power, causesFire ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.TNT);
    }

    public static void warheadTier1(Entity source, ServerLevel level, BlockPos pos, boolean fire) {
        composeEffectSmall(level, pos);
        standardExplode(source, level, pos, 15.0F, fire);
    }

    public static void warheadTier2(Entity source, ServerLevel level, BlockPos pos, boolean fire) {
        composeEffectStandard(level, pos);
        standardExplode(source, level, pos, 30.0F, fire);
    }

    public static void warheadTier3Large(Entity source, ServerLevel level, BlockPos pos, boolean fire) {
        composeEffectLarge(level, pos);
        standardExplode(source, level, pos, 50.0F, fire);
    }

    public static void warheadStealth(Entity source, ServerLevel level, BlockPos pos) {
        composeEffectStandard(level, pos);
        standardExplode(source, level, pos, 20.0F, false);
    }

    public static void warheadShuttle(Entity source, ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        composeEffectLarge(level, pos);
        standardExplode(source, level, pos, 20.0F, false);
        ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z);
        SimpleParticleType smoke = (SimpleParticleType) ModExplosionParticles.LARGE_DARK_SMOKE.get();
        for (int i = 0; i < 80; i++) {
            double vx = (level.random.nextDouble() - 0.5) * 0.4;
            double vy = 0.2 + level.random.nextDouble() * 0.5;
            double vz = (level.random.nextDouble() - 0.5) * 0.4;
            ServerExplosionParticles.sendAlwaysVisible(level, smoke, x, y, z, vx, vy, vz);
        }
    }

    public static void warheadBusterTier1(Entity source, ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (int i = 0; i < 15; i++) {
            level.explode(source, x, pos.getY() - i, z, 5.0F, Level.ExplosionInteraction.BLOCK);
        }
        composeEffectSmall(level, pos);
        spawnShrapnelBurst(level, x, pos.getY(), z, 10);
        spawnLightRubble(level, x, pos.getY(), z, 5);
    }

    public static void warheadBusterTier2(Entity source, ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (int i = 0; i < 20; i++) {
            level.explode(source, x, pos.getY() - i, z, 7.5F, Level.ExplosionInteraction.BLOCK);
        }
        composeEffectStandard(level, pos);
        spawnShrapnelBurst(level, x, pos.getY(), z, 15);
        spawnLightRubble(level, x, pos.getY(), z, 8);
    }

    public static void warheadDrill(Entity source, ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (int i = 0; i < 30; i++) {
            level.explode(source, x, pos.getY() - i, z, 10.0F, Level.ExplosionInteraction.BLOCK);
        }
        composeEffectLarge(level, pos);
        ExplosionParticleUtils.spawnLargeDarkSmokes(level, x, pos.getY(), z, 40);
        spawnShrapnelBurst(level, x, pos.getY(), z, 5);
        spawnShrapnelBurst(level, x, pos.getY(), z, 20);
        ExplosionParticleUtils.spawnImpactJoltShockwave(level, x, pos.getY(), z);
    }

    /** Small compose (tier 1 generic / incendiary). */
    public static void composeEffectSmall(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        SimpleParticleType flash = (SimpleParticleType) ModExplosionParticles.EXPLOSION_FLASH.get();
        level.sendParticles(flash, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        ExplosionParticleUtils.spawnCustomExplosion(level, x, y, z, 0.5F,
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get());
    }

    /** Standard compose (tier 2, stealth). */
    public static void composeEffectStandard(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        composeEffectSmall(level, pos);
        level.getServer().tell(new TickTask(2, () ->
                ExplosionParticleUtils.spawnCustomExplosion(level, x, y, z, 1.0F,
                        (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get())));
    }

    /** Large compose (tier 3 burst, shuttle aux). */
    public static void composeEffectLarge(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        SimpleParticleType flash = (SimpleParticleType) ModExplosionParticles.EXPLOSION_FLASH.get();
        level.sendParticles(flash, x, y, z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);
        level.getServer().tell(new TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));
    }

    public static void missileDestroyed(ServerLevel level, Entity source, double x, double y, double z,
                                      Vec3 motion, @Nullable List<ItemStack> debris, @Nullable ItemStack rareDrop) {
        level.explode(source, x, y, z, 5.0F, Level.ExplosionInteraction.TNT);
        spawnShrapnelShower(level, x, y, z, motion.x, motion.y, motion.z, 15, 0.075D);
        spawnMissileDebris(level, x, y, z, motion.x, motion.y, motion.z, 0.25D, debris, rareDrop);
    }

    private static void spawnLightRubble(ServerLevel level, double x, double y, double z, int count) {
        BlockPos ground = BlockPos.containing(x, y - 0.5D, z);
        BlockState state = level.getBlockState(ground);
        if (state.isAir()) {
            state = Blocks.STONE.defaultBlockState();
        }
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < count; i++) {
            double vx = level.random.nextGaussian() * 0.75D;
            double vy = 0.5D + level.random.nextDouble() * 0.5D;
            double vz = level.random.nextGaussian() * 0.75D;
            level.sendParticles(particle, x, y, z, 1, vx, vy, vz, 0.15D);
        }
    }

    public static void spawnShrapnelBurst(ServerLevel level, double x, double y, double z, int count) {
        SimpleParticleType spark = (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get();
        for (int i = 0; i < count; i++) {
            double vx = level.random.nextGaussian() * 0.8;
            double vy = 0.3 + level.random.nextDouble() * 0.6;
            double vz = level.random.nextGaussian() * 0.8;
            ServerExplosionParticles.sendAlwaysVisible(level, spark, x, y, z, vx, vy, vz);
        }
    }

    public static void spawnShrapnelShower(ServerLevel level, double x, double y, double z,
                                           double motionX, double motionY, double motionZ,
                                           int count, double deviation) {
        SimpleParticleType spark = (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get();
        for (int i = 0; i < count; i++) {
            double vx = motionX + level.random.nextGaussian() * deviation;
            double vy = motionY + level.random.nextGaussian() * deviation;
            double vz = motionZ + level.random.nextGaussian() * deviation;
            ServerExplosionParticles.sendAlwaysVisible(level, spark, x, y, z, vx, vy, vz);
        }
    }

    public static void spawnMissileDebris(ServerLevel level, double x, double y, double z,
                                          double motionX, double motionY, double motionZ, double deviation,
                                          @Nullable List<ItemStack> debris, @Nullable ItemStack rareDrop) {
        if (debris != null) {
            for (ItemStack stack : debris) {
                if (stack.isEmpty()) {
                    continue;
                }
                int drops = level.random.nextInt(stack.getCount() + 1);
                for (int j = 0; j < drops; j++) {
                    ItemEntity item = new ItemEntity(level, x, y, z, stack.copyWithCount(1));
                    item.setDeltaMovement(
                            (motionX + level.random.nextGaussian() * deviation) * 0.85D,
                            (motionY + level.random.nextGaussian() * deviation) * 0.85D,
                            (motionZ + level.random.nextGaussian() * deviation) * 0.85D);
                    level.addFreshEntity(item);
                }
            }
        }
        if (rareDrop != null && !rareDrop.isEmpty() && level.random.nextInt(10) == 0) {
            ItemEntity item = new ItemEntity(level, x, y, z, rareDrop.copy());
            item.setDeltaMovement(
                    (motionX + level.random.nextGaussian() * deviation) * 0.85D,
                    (motionY + level.random.nextGaussian() * deviation) * 0.85D,
                    (motionZ + level.random.nextGaussian() * deviation) * 0.85D);
            level.addFreshEntity(item);
        }
    }

    /** Дым при пуске с площадки (клиент видит через sendParticles). */
    public static void spawnLaunchSmoke(ServerLevel level, double x, double y, double z) {
        SimpleParticleType smoke = (SimpleParticleType) ModParticleTypes.SMOKE_COLUMN.get();
        for (int i = 0; i < 40; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * 1.5D;
            double oz = (level.random.nextDouble() - 0.5D) * 1.5D;
            double vy = 0.15D + level.random.nextDouble() * 0.25D;
            level.sendParticles(smoke, x + ox, y, z + oz, 1, 0.0D, vy, 0.0D, 0.02D);
        }
        SimpleParticleType dark = (SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get();
        for (int i = 0; i < 25; i++) {
            double vx = (level.random.nextDouble() - 0.5D) * 0.15D;
            double vy = 0.4D + level.random.nextDouble() * 0.3D;
            double vz = (level.random.nextDouble() - 0.5D) * 0.15D;
            ServerExplosionParticles.sendAlwaysVisible(level, dark, x, y, z, vx, vy, vz);
        }
    }

    public static List<ItemStack> defaultDebrisForTier(int tier) {
        ItemStack scrap = new ItemStack(ModItems.SCRAP.get());
        return switch (tier) {
            case 0 -> List.of(
                    scrap.copyWithCount(2),
                    scrap.copyWithCount(1));
            case 1 -> List.of(
                    scrap.copyWithCount(4),
                    scrap.copyWithCount(1));
            case 2 -> List.of(
                    scrap.copyWithCount(10),
                    scrap.copyWithCount(6),
                    scrap.copyWithCount(1));
            case 3 -> List.of(
                    scrap.copyWithCount(16),
                    scrap.copyWithCount(10),
                    scrap.copyWithCount(1));
            case 4 -> List.of(
                    scrap.copyWithCount(16),
                    scrap.copyWithCount(20),
                    scrap.copyWithCount(12),
                    scrap.copyWithCount(1));
            default -> List.of(scrap.copyWithCount(2));
        };
    }
}
