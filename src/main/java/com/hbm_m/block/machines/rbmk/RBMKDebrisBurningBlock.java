package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of {@code RBMKDebrisBurning}: the glowing rubble left where a reactor column burned
 * through.
 *
 * <p>It is not decoration - it keeps working after the meltdown. Every tick cycle it puffs flame
 * (1-in-5), has a 1-in-10 chance to push meltdown gas into an adjacent air block, and eventually
 * burns itself out into plain rubble. Foam and boron sand next to it make that burn-out ten times
 * more likely (1-in-10 instead of 1-in-100), which is the original's intended way of fighting a
 * meltdown site rather than waiting it out. The burning rubble itself damages nobody.</p>
 */
public class RBMKDebrisBurningBlock extends Block {

    public RBMKDebrisBurningBlock(Properties props) {
        super(props);
    }

    /** 1.7.10 {@code RBMKDebrisBurning.tickRate}: 100-120 ticks between updates. */
    private static int nextDelay(RandomSource random) {
        return 100 + random.nextInt(20);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) level.scheduleTick(pos, this, nextDelay(level.random));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            spawnFlame(level, pos, random);
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
        }

        // Meltdown gas creeps outward into open air.
        Direction dir = Direction.values()[random.nextInt(6)];
        BlockPos side = pos.relative(dir);
        BlockState neighbour = level.getBlockState(side);

        if (random.nextInt(10) == 0 && neighbour.isAir()) {
            level.setBlockAndUpdate(side, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
        }

        // Foam helps stop the fire; Boron smothers it. 1.66% chance every 100-120 seconds for one
        // side (1.7.10 RBMKDebrisBurning.updateTick: chance = 10 with foam/boron, else 100).
        boolean suppressed = isSuppressant(level.getBlockState(side));
        int chance = suppressed ? 10 : 100;

        if (random.nextInt(chance) == 0) {
            level.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS.get().defaultBlockState());
        } else {
            level.scheduleTick(pos, this, nextDelay(random));
        }
    }

    /**
     * 1.7.10 {@code RBMKDebrisBurning.updateTick} burn-out roll counts foam_layer, block_foam,
     * sand_boron_layer and the boron sand mix (the port's one-block-per-variant analog of the
     * {@code sand_mix} BORON metadata is the full {@code sand_boron} block) - all four cut the
     * burn-out timer tenfold.
     */
    static boolean isSuppressant(BlockState state) {
        return state.is(ModBlocks.FOAM_LAYER.get()) || state.is(ModBlocks.BLOCK_FOAM.get())
                || isBoronSuppressant(state);
    }

    /**
     * 1.7.10 {@code RBMKDebrisRadiating.updateTick} decay roll counts only the boron sands
     * ({@code sand_boron_layer} and the {@code sand_mix} BORON metadata) - foam does nothing
     * there.
     */
    static boolean isBoronSuppressant(BlockState state) {
        return state.is(ModBlocks.SAND_BORON_LAYER.get()) || state.is(ModBlocks.SAND_BORON.get());
    }

    /**
     * 1.7.10 {@code RBMKDebrisRadiating.radiate}: flat 100 {@code inFire} damage to every living
     * being within 5 blocks of the rubble's centre - no ignite, no distance falloff. Distance is
     * measured to the entity's eye, matching the original's {@code len} computation.
     */
    static void scorch(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;

        for (net.minecraft.world.entity.LivingEntity e :
                level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(5))) {
            double dx = e.getX() - cx;
            double dy = (e.getY() + e.getEyeHeight()) - cy;
            double dz = e.getZ() - cz;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) < 5)
                e.hurt(level.damageSources().inFire(), 100F);
        }
    }

    /**
     * The original fires a "rbmkflame" particle packet from the server on a 1-in-5 tick roll,
     * positioned a little above the rubble; here the server spawns it directly so it reaches
     * every nearby client the same way.
     */
    static void spawnFlame(net.minecraft.server.level.ServerLevel level, BlockPos pos,
                            net.minecraft.util.RandomSource random) {
        level.sendParticles(com.hbm_m.particle.ModParticleTypes.RBMK_FLAME.get(),
                pos.getX() + 0.25 + random.nextDouble() * 0.5,
                pos.getY() + 1.75,
                pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                1, 0, 0, 0, 0);
    }
}
