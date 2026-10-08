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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class ExplosionNukeGeneric {

    public static void empBlast(Level level, int x, int y, int z, int bombStartStrength) {
        if (level.isClientSide) return;
        int r = bombStartStrength;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int blockX = xx + x;
            int xx2 = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int blockY = yy + y;
                int xy2 = xx2 + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    if (xy2 + zz * zz < r22) {
                        emp(level, blockX, blockY, zz + z);
                    }
                }
            }
        }
    }

    private static void emp(Level level, int x, int y, int z) {
        BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
        if (be instanceof IEnergyReceiver receiver) {
            receiver.setEnergyStored(0);
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
        // Original: EntityB92Beam / EntityBulletBaseNT / EntityBulletBaseMK4 sind ausgenommen
        if (entity instanceof com.hbm_m.entity.projectile.EntityB92Beam) return true;
        if (entity instanceof com.hbm_m.entity.projectile.EntityBulletBaseNT) return true;
        if (entity instanceof com.hbm_m.entity.projectile.EntityBulletBaseMK4) return true;
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

        // 2. Если цель находится в непрогруженном чанке - не вызываем clip (он повесит сервер)
        int targetChunkX = (int) a >> 4;
        int targetChunkZ = (int) c >> 4;
        if (!level.hasChunk(targetChunkX, targetChunkZ)) {
            return true;
        }

        // 3. Вызываем быстрый clip. ВНИМАНИЕ: на 1.21.1+ в ClipContext передаётся
        // CollisionContext и null там НЕДОПУСТИМ (CollisionContext.of -> NPE в
        // EntityCollisionContext). На 1.20.1 параметр - Entity, null допустим.
        //? if < 1.21.1 {
        HitResult hit = level.clip(new ClipContext(
                new Vec3(x, y, z),
                new Vec3(a, b, c),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                (net.minecraft.world.entity.Entity) null
        ));
        //?} else {
        /*HitResult hit = level.clip(new ClipContext(
                new Vec3(x, y, z),
                new Vec3(a, b, c),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()
        ));
        *///?}
        return hit.getType() != HitResult.Type.MISS;
    }

    // ─── 1:1 waste / wasteDest / wasteNoSchrab / wasteDestNoSchrab ─────────────

    /** Original {@code waste}: verstrahlt Bloecke in einer ausgefransten Kugel ({@code r^2/2 + rand(r^2/10)}). */
    public static void waste(Level world, int x, int y, int z, int radius) {
        wasteSphere(world, x, y, z, radius, false);
    }

    public static void wasteNoSchrab(Level world, int x, int y, int z, int radius) {
        wasteSphere(world, x, y, z, radius, true);
    }

    private static void wasteSphere(Level world, int x, int y, int z, int radius, boolean noSchrab) {
        if (world.isClientSide) return;
        int r = radius;
        int r2 = r * r;
        int r22 = r2 / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22 + world.random.nextInt(Math.max(1, r22 / 5))) {
                        pos.set(X, Y, Z);
                        if (!world.getBlockState(pos).isAir()) {
                            if (noSchrab) wasteDestNoSchrab(world, pos.immutable());
                            else wasteDest(world, pos.immutable());
                        }
                    }
                }
            }
        }
    }

    private static boolean isMushroomBlock(BlockState s) {
        return s.is(Blocks.BROWN_MUSHROOM_BLOCK) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.MUSHROOM_STEM);
    }

    /** Pilzbloecke: Stiel (Meta 10) wird verstrahltes Holz, der Rest verschwindet. */
    private static void wasteMushroom(Level world, BlockPos pos, BlockState s) {
        if (s.is(Blocks.MUSHROOM_STEM)) world.setBlock(pos, ModBlocks.WASTE_LOG.get().defaultBlockState(), 3);
        else world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    public static void wasteDest(Level world, BlockPos pos) {
        if (world.isClientSide) return;
        int rand;
        BlockState s = world.getBlockState(pos);
        Block b = s.getBlock();

        if (s.is(BlockTags.WOODEN_DOORS) || b == Blocks.IRON_DOOR) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        } else if (b == Blocks.GRASS_BLOCK) {
            world.setBlock(pos, ModBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
        } else if (b == Blocks.MYCELIUM) {
            world.setBlock(pos, ModBlocks.WASTE_MYCELIUM.get().defaultBlockState(), 3);
        } else if (b == Blocks.SAND || b == Blocks.RED_SAND) {
            rand = RANDOM.nextInt(20);
            if (rand == 1 && b == Blocks.SAND) world.setBlock(pos, ModBlocks.WASTE_TRINITITE.get().defaultBlockState(), 3);
            if (rand == 1 && b == Blocks.RED_SAND) world.setBlock(pos, ModBlocks.WASTE_TRINITITE_RED.get().defaultBlockState(), 3);
        } else if (b == Blocks.CLAY) {
            world.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 3);
        } else if (b == Blocks.MOSSY_COBBLESTONE) {
            world.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), 3);
        } else if (b == Blocks.COAL_ORE) {
            rand = RANDOM.nextInt(10);
            if (rand == 1 || rand == 2 || rand == 3) world.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            if (rand == 9) world.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), 3);
        } else if (s.is(BlockTags.LOGS_THAT_BURN)) {
            world.setBlock(pos, ModBlocks.WASTE_LOG.get().defaultBlockState(), 3);
        } else if (isMushroomBlock(s)) {
            wasteMushroom(world, pos, s);
        } else if (s.getSoundType() == net.minecraft.world.level.block.SoundType.WOOD && s.isSolidRender(world, pos) && b != ModBlocks.WASTE_LOG.get()) {
            // Original: Material.wood && isOpaqueCube
            world.setBlock(pos, ModBlocks.WASTE_PLANKS.get().defaultBlockState(), 3);
        } else if (b == ModBlocks.URANIUM_ORE.get() || b == ModBlocks.URANIUM_ORE_DEEPSLATE.get()) {
            rand = RANDOM.nextInt(com.hbm_m.config.VersatileConfig.getSchrabOreChance());
            if (rand == 1) world.setBlock(pos, ModBlocks.SCHRABIDIUM_ORE.get().defaultBlockState(), 3);
            else world.setBlock(pos, ModBlocks.ORE_URANIUM_SCORCHED.get().defaultBlockState(), 3);
        } else if (b == ModBlocks.NETHER_URANIUM_ORE.get()) {
            rand = RANDOM.nextInt(com.hbm_m.config.VersatileConfig.getSchrabOreChance());
            if (rand == 1) world.setBlock(pos, ModBlocks.SCHRABIDIUM_ORE_NETHER.get().defaultBlockState(), 3);
            else world.setBlock(pos, ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get().defaultBlockState(), 3);
        } else if (b == ModBlocks.GNEISS_URANIUM_ORE.get()) {
            rand = RANDOM.nextInt(com.hbm_m.config.VersatileConfig.getSchrabOreChance());
            if (rand == 1) world.setBlock(pos, ModBlocks.SCHRABIDIUM_ORE_GNEISS.get().defaultBlockState(), 3);
            else world.setBlock(pos, ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get().defaultBlockState(), 3);
        }
    }

    public static void wasteDestNoSchrab(Level world, BlockPos pos) {
        if (world.isClientSide) return;
        int rand;
        BlockState s = world.getBlockState(pos);
        Block b = s.getBlock();

        //? if < 1.21.1 {
        if (s.is(net.minecraftforge.common.Tags.Blocks.GLASS) || s.is(BlockTags.WOODEN_DOORS) || b == Blocks.IRON_DOOR || s.is(BlockTags.LEAVES)) {
        //?} else {
        /*if (s.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || s.is(BlockTags.WOODEN_DOORS) || b == Blocks.IRON_DOOR || s.is(BlockTags.LEAVES)) {
        *///?}
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (b == Blocks.GRASS_BLOCK) {
            world.setBlock(pos, ModBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
        } else if (b == Blocks.MYCELIUM) {
            world.setBlock(pos, ModBlocks.WASTE_MYCELIUM.get().defaultBlockState(), 3);
        } else if (b == Blocks.SAND || b == Blocks.RED_SAND) {
            rand = RANDOM.nextInt(20);
            if (rand == 1 && b == Blocks.SAND) world.setBlock(pos, ModBlocks.WASTE_TRINITITE.get().defaultBlockState(), 3);
            if (rand == 1 && b == Blocks.RED_SAND) world.setBlock(pos, ModBlocks.WASTE_TRINITITE_RED.get().defaultBlockState(), 3);
        } else if (b == Blocks.CLAY) {
            world.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 3);
        } else if (b == Blocks.MOSSY_COBBLESTONE) {
            world.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), 3);
        } else if (b == Blocks.COAL_ORE) {
            rand = RANDOM.nextInt(30);
            if (rand == 1 || rand == 2 || rand == 3) world.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            if (rand == 29) world.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), 3);
        } else if (s.is(BlockTags.LOGS_THAT_BURN)) {
            world.setBlock(pos, ModBlocks.WASTE_LOG.get().defaultBlockState(), 3);
        } else if (s.is(BlockTags.PLANKS)) {
            world.setBlock(pos, ModBlocks.WASTE_PLANKS.get().defaultBlockState(), 3);
        } else if (isMushroomBlock(s)) {
            wasteMushroom(world, pos, s);
        }
    }

    private static final java.util.Random RANDOM = new java.util.Random();
}