package com.hbm_m.explosion;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.effect.EntityCloudFleija;
import com.hbm_m.util.confetti.ConfettiUtil;
import com.hbm_m.entity.logic.EntityExplosionChunkloading;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class ExplosionNukeGeneric {

    public static void empBlast(Level level, int x, int y, int z, int bombStartStrength) {
        if (level.isClientSide) return;
        int r = bombStartStrength;
        int r22 = r * r / 2;
        // The original walks every position of the sphere and looks a tile up per position — with
        // r=50 that is ~1M iterations resolving ~49 chunk columns 185k times over. Only block
        // entities can be affected, so iterate the chunks' block entities instead, like
        // EmpPulseEntity.allocate does. Same machines hit, each chunk resolved once, no chunk loads.
        for (int cx = (x - r) >> 4; cx <= (x + r) >> 4; cx++) {
            for (int cz = (z - r) >> 4; cz <= (z + r) >> 4; cz++) {
                LevelChunk chunk = com.hbm_m.util.Compat.getLoadedChunkAt(level, cx, cz);
                if (chunk == null) continue;

                for (BlockEntity be : new java.util.ArrayList<>(chunk.getBlockEntities().values())) {
                    if (!(be instanceof IEnergyReceiver receiver)) continue;
                    BlockPos pos = be.getBlockPos();
                    int dx = pos.getX() - x;
                    int dy = pos.getY() - y;
                    int dz = pos.getZ() - z;
                    if (dx * dx + dy * dy + dz * dz < r22) {
                        receiver.setEnergyStored(0);
                    }
                }
            }
        }
    }

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

    public static void dealDamage(Level level, double x, double y, double z, double radius, float maxDamage) {
        dealDamageInternal(level, x, y, z, radius, maxDamage);
    }

    private static void dealDamageInternal(Level level, double x, double y, double z, double radius, float maxDamage) {
        List<Entity> entities = level.getEntities(null, new AABB(x, y, z, x, y, z).inflate(radius));

        for (Entity entity : entities) {
            if (entity.isRemoved()) continue;

            if (entity instanceof LivingEntity l) {
                if (!l.isAlive() || l.invulnerableTime > 10) {
                    continue;
                }
            }

            double distSq = entity.distanceToSqr(x, y, z);
            if (distSq <= radius * radius) {
                double entX = entity.getX();
                double entY = entity.getY() + entity.getEyeHeight();
                double entZ = entity.getZ();

                if (!isExplosionExempt(entity) && !isObstructedSafe(level, x, y, z, entX, entY, entZ, distSq)) {
                    double dist = Math.sqrt(distSq);
                    double damage = maxDamage * (radius - dist) / radius;

                    if (damage > 0.5D) {
                        entity.hurt(ModDamageSources.nuclearBlast(level), (float) damage);
                        if (entity instanceof LivingEntity living && !living.isAlive()) {
                            ConfettiUtil.decideConfetti(living, ModDamageSources.nuclearBlast(level));
                        }
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
    }

    private static boolean isExplosionExempt(Entity entity) {
        if (entity instanceof Ocelot) return true;
        if (entity instanceof EntityCloudFleija) return true;
        if (entity instanceof EntityExplosionChunkloading) return true;
        if (entity instanceof Player player && player.isCreative()) return true;
        return false;
    }

    public static void solinium(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            BlockState state = level.getBlockState(pos);
            Block b = state.getBlock();

            if (b == Blocks.GRASS_BLOCK || b == Blocks.MYCELIUM || b == ModBlocks.WASTE_GRASS.get()) {
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), NukeMk5ChunkEater.FAST_BLOCK_FLAGS);
                return;
            }

            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), NukeMk5ChunkEater.FAST_BLOCK_FLAGS);
            }
        }
    }

    /**
     * Безопасная проверка видимости без зависаний и блокировок потока.
     */
    private static boolean isObstructedSafe(Level level, double x, double y, double z, double a, double b, double c, double distSq) {
        // 1. В радиусе 45 блоков ударная волна испаряет все преграды - проверять препятствия бессмысленно
        if (distSq <= 45.0 * 45.0) {
            return false;
        }

        // 2. Never clip into an unloaded chunk - it would load it from the tick thread. Compat and
        // not level.hasChunk: on the client that one always returns true.
        if (!com.hbm_m.util.Compat.isPositionLoaded(level, (int) a, (int) c)) {
            return true;
        }

        // 3. Вызываем быстрый clip. ВНИМАНИЕ: на 1.21.1+ в ClipContext передаётся
        // CollisionContext и null там НЕДОПУСТИМ (CollisionContext.of -> NPE в
        // EntityCollisionContext). На 1.20.1 параметр - Entity, null допустим.
        //? if < 1.21.1 {
        /*HitResult hit = level.clip(new ClipContext(
                new Vec3(x, y, z),
                new Vec3(a, b, c),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                (net.minecraft.world.entity.Entity) null
        ));
        *///?} else {
        HitResult hit = level.clip(new ClipContext(
                new Vec3(x, y, z),
                new Vec3(a, b, c),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));
        //?}
        return hit.getType() != HitResult.Type.MISS;
    }
}