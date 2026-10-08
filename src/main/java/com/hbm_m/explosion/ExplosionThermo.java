package com.hbm_m.explosion;

import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.explosion.ExplosionThermo}: Frost- und Glutwirkung der Thermobomben und ihrer Raketen.
 * Vanilla-Bloecke von 1.7 sind auf die Tags/Bloecke von 1.20 abgebildet (Stamm, Bretter, Laub, Steinziegel, Lava,
 * Wasser; gefaerbter Ton = zufaellige Terrakotta).
 */
public final class ExplosionThermo {

    private ExplosionThermo() {}

    private static final Block[] TERRACOTTA = {
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
            Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA };

    private interface Dest { void apply(Level world, BlockPos pos); }

    private static void sphere(Level world, int x, int y, int z, int bombStartStrength, Dest dest) {
        int r = bombStartStrength * 2;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22 + world.random.nextInt(r22 / 2)) dest.apply(world, new BlockPos(xx + x, yy + y, zz + z));
                }
            }
        }
    }

    public static void freeze(Level world, int x, int y, int z, int bombStartStrength) { sphere(world, x, y, z, bombStartStrength, ExplosionThermo::freezeDest); }
    public static void scorch(Level world, int x, int y, int z, int bombStartStrength) { sphere(world, x, y, z, bombStartStrength, ExplosionThermo::scorchDest); }
    public static void scorchLight(Level world, int x, int y, int z, int bombStartStrength) { sphere(world, x, y, z, bombStartStrength, ExplosionThermo::scorchDestLight); }

    public static void snow(Level world, int x, int y, int z, int bound) {
        int r = bound;
        int r22 = r * r / 2;
        for (int xx = -r; xx < r; xx++) {
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    if (YY + zz * zz < r22) {
                        BlockPos up = new BlockPos(xx + x, yy + y + 1, zz + z);
                        BlockState above = world.getBlockState(up);
                        if (Blocks.SNOW.defaultBlockState().canSurvive(world, up) && (above.isAir() || above.is(Blocks.FIRE))) {
                            world.setBlockAndUpdate(up, Blocks.SNOW.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void set(Level world, BlockPos pos, Block block) {
        world.setBlockAndUpdate(pos, block.defaultBlockState());
    }

    private static boolean isVanillaLog(BlockState s) { return s.is(BlockTags.LOGS) && s.getBlock().builtInRegistryHolder().key().location().getNamespace().equals("minecraft"); }
    private static boolean isVanillaPlanks(BlockState s) { return s.is(BlockTags.PLANKS) && s.getBlock().builtInRegistryHolder().key().location().getNamespace().equals("minecraft"); }
    private static boolean isVanillaLeaves(BlockState s) { return s.is(BlockTags.LEAVES) && s.getBlock().builtInRegistryHolder().key().location().getNamespace().equals("minecraft"); }

    public static void freezeDest(Level world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.is(ModBlocks.VOLCANIC_LAVA_BLOCK.get())) set(world, pos, Blocks.COBBLESTONE);
        else if (s.is(Blocks.GRASS_BLOCK)) set(world, pos, ModBlocks.FROZEN_GRASS.get());
        else if (s.is(Blocks.DIRT)) set(world, pos, ModBlocks.FROZEN_DIRT.get());
        else if (isVanillaLog(s) || s.is(ModBlocks.WASTE_LOG.get())) set(world, pos, ModBlocks.FROZEN_LOG.get());
        else if (isVanillaPlanks(s) || s.is(ModBlocks.WASTE_PLANKS.get())) set(world, pos, ModBlocks.FROZEN_PLANKS.get());
        else if (s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(BlockTags.STONE_BRICKS)) set(world, pos, Blocks.PACKED_ICE);
        else if (isVanillaLeaves(s)) set(world, pos, Blocks.SNOW_BLOCK);
        else if (s.is(Blocks.LAVA)) set(world, pos, Blocks.OBSIDIAN);
        else if (s.is(Blocks.WATER)) set(world, pos, Blocks.ICE);
    }

    public static void scorchDest(Level world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.is(Blocks.GRASS_BLOCK) || s.is(ModBlocks.FROZEN_GRASS.get())) set(world, pos, Blocks.DIRT);
        else if (s.is(Blocks.DIRT)) set(world, pos, Blocks.NETHERRACK);
        else if (s.is(ModBlocks.FROZEN_DIRT.get())) set(world, pos, Blocks.DIRT);
        else if (s.is(Blocks.NETHERRACK)) set(world, pos, Blocks.LAVA);
        else if (isVanillaLog(s) || s.is(ModBlocks.FROZEN_LOG.get())) set(world, pos, ModBlocks.WASTE_LOG.get());
        else if (s.is(ModBlocks.FROZEN_PLANKS.get()) || isVanillaPlanks(s)) set(world, pos, ModBlocks.WASTE_PLANKS.get());
        else if (s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(BlockTags.STONE_BRICKS) || s.is(Blocks.OBSIDIAN)) set(world, pos, Blocks.LAVA);
        else if (isVanillaLeaves(s) || s.is(Blocks.WATER) || s.is(Blocks.ICE)) set(world, pos, Blocks.AIR);
        else if (s.is(Blocks.PACKED_ICE)) set(world, pos, Blocks.WATER);
    }

    public static void scorchDestLight(Level world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.is(Blocks.GRASS_BLOCK) || s.is(ModBlocks.FROZEN_GRASS.get())) set(world, pos, Blocks.DIRT);
        else if (s.is(Blocks.DIRT)) set(world, pos, Blocks.NETHERRACK);
        else if (s.is(ModBlocks.FROZEN_DIRT.get())) set(world, pos, Blocks.DIRT);
        else if (s.is(ModBlocks.WASTE_EARTH.get())) set(world, pos, Blocks.NETHERRACK);
        else if (isVanillaLog(s) || s.is(ModBlocks.FROZEN_LOG.get())) set(world, pos, ModBlocks.WASTE_LOG.get());
        else if (s.is(ModBlocks.FROZEN_PLANKS.get()) || isVanillaPlanks(s)) set(world, pos, ModBlocks.WASTE_PLANKS.get());
        else if (s.is(Blocks.OBSIDIAN)) set(world, pos, ModBlocks.GRAVEL_OBSIDIAN.get());
        else if (isVanillaLeaves(s) || s.is(Blocks.WATER) || s.is(Blocks.ICE)) set(world, pos, Blocks.AIR);
        else if (s.is(Blocks.PACKED_ICE)) set(world, pos, Blocks.WATER);
        else if (s.is(Blocks.SAND)) set(world, pos, Blocks.GLASS);
        else if (s.is(Blocks.CLAY)) set(world, pos, TERRACOTTA[world.random.nextInt(16)]);
    }

    /** Lebewesen im Radius werden in Eis eingeschlossen und geschwaecht (Ozelots nicht). */
    public static void freezer(Level world, int x, int y, int z, int bombStartStrength) {
        double wat = bombStartStrength;
        double strength = bombStartStrength * 2.0;
        AABB box = new AABB(Mth.floor(x - wat - 1.0D), Mth.floor(y - wat - 1.0D), Mth.floor(z - wat - 1.0D),
                Mth.floor(x + wat + 1.0D), Mth.floor(y + wat + 1.0D), Mth.floor(z + wat + 1.0D));
        List<Entity> list = world.getEntities((Entity) null, box);

        for (Entity entity : list) {
            double d4 = Math.sqrt(entity.distanceToSqr(x, y, z)) / strength;
            if (d4 <= 1.0D) {
                double d5 = entity.getX() - x;
                double d6 = entity.getY() + entity.getEyeHeight() - y;
                double d7 = entity.getZ() - z;
                double d9 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
                if (d9 < wat && !(entity instanceof Ocelot) && entity instanceof LivingEntity living) {
                    for (int a = (int) entity.getX() - 2; a < (int) entity.getX() + 1; a++)
                        for (int b = (int) entity.getY(); b < (int) entity.getY() + 3; b++)
                            for (int c = (int) entity.getZ() - 1; c < (int) entity.getZ() + 2; c++)
                                world.setBlockAndUpdate(new BlockPos(a, b, c), Blocks.ICE.defaultBlockState());

                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 4));
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90 * 20, 2));
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3 * 60 * 20, 2));
                }
            }
        }
    }

    /** Alles im Radius brennt 10 s und wird geschwaecht, ausser Spieler mit Asbestschutz. */
    public static void setEntitiesOnFire(Level world, double x, double y, double z, int radius) {
        List<Entity> list = world.getEntities((Entity) null, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));
        for (Entity e : list) {
            if (Math.sqrt(e.distanceToSqr(x, y, z)) <= radius) {
                if (!(e instanceof Player p && ArmorUtil.checkForAsbestos(p))) {
                    if (e instanceof LivingEntity l) l.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 4));
                    PlatformHooks.setSecondsOnFire(e, 10);
                }
            }
        }
    }
}
