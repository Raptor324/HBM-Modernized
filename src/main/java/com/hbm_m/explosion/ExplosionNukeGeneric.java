package com.hbm_m.explosion;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Общая логика урона/огня/трансформации мира для мощных ядерных взрывов.
 * Порт оригинального ExplosionNukeGeneric на Forge 1.20.1 (HBM Modernized).
 */
public class ExplosionNukeGeneric {

    /**
     * Увеличивает радиацию в окрестных чанках вокруг эпицентра.
     * Использует систему ChunkRadiationManager из Modernized.
     */
    public static void incrementRad(Level level, double posX, double posY, double posZ, float mult) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) < 4) {
                    int cx = (int) Math.floor(posX + i * 16);
                    int cy = (int) Math.floor(posY);
                    int cz = (int) Math.floor(posZ + j * 16);
                    float amount = 50F / (Math.abs(i) + Math.abs(j) + 1) * mult;
                    ChunkRadiationManager.incrementRad(level, cx, cy, cz, amount);
                }
            }
        }
    }

    public static void dealDamage(Level level, double x, double y, double z, double radius) {
        dealDamage(level, x, y, z, radius, 250F);
    }

    private static void dealDamage(Level level, double x, double y, double z, double radius, float maxDamage) {
        List<Entity> entities = level.getEntities(null, new AABB(x, y, z, x, y, z).inflate(radius));

        for (Entity entity : entities) {
            double distSq = entity.distanceToSqr(x, y, z);
            if (distSq <= radius * radius) {

                double entX = entity.getX();
                double entY = entity.getY() + entity.getEyeHeight();
                double entZ = entity.getZ();

                if (!isExplosionExempt(entity) && !isObstructed(level, x, y, z, entX, entY, entZ)) {
                    double dist = Math.sqrt(distSq);
                    double damage = maxDamage * (radius - dist) / radius;
                    entity.hurt(ModDamageSources.nuclearBlast(level), (float) damage);
                    entity.setRemainingFireTicks(100);

                    double knockX = entX - x;
                    double knockY = (entity.getY() + entity.getEyeHeight()) - y;
                    double knockZ = entZ - z;

                    Vec3 knock = new Vec3(knockX, knockY, knockZ).normalize().scale(0.2D);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knock));
                }
            }
        }
    }

    private static boolean isExplosionExempt(Entity entity) {
        if (entity instanceof Ocelot) return true;

        if (entity instanceof Player player && player.isCreative()) {
            return true;
        }

        return false;
    }

    /**
     * Преобразование растительности и слабых блоков вокруг эпицентра взрыва.
     */
    public static void solinium(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            BlockState state = level.getBlockState(pos);
            Block b = state.getBlock();

            if (b == Blocks.GRASS_BLOCK
                    || b == Blocks.MYCELIUM
                    || b == ModBlocks.WASTE_GRASS.get()) {
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                return;
            }

            // Удаляем листву/брёвна/доски и другие легко выгорающие блоки
            if (state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.PLANKS)
                    || state.is(BlockTags.LOGS)) {
                level.removeBlock(pos, false);
            }
        }
    }

    /**
     * Проверяет, есть ли между двумя точками блоки, перекрывающие прямую видимость.
     */
    private static boolean isObstructed(Level level, double x, double y, double z, double a, double b, double c) {
        HitResult hit = level.clip(new ClipContext(
                new Vec3(x, y, z),
                new Vec3(a, b, c),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                null
        ));
        return hit.getType() != HitResult.Type.MISS;
    }
}

