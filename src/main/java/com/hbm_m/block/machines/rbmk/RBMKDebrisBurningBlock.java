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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of {@code RBMKDebrisBurning}: the glowing rubble left where a reactor column burned
 * through.
 *
 * <p>It is not decoration - it keeps working after the meltdown. Every tick it puffs flame, has a
 * chance to push meltdown gas into an adjacent air block, and eventually burns itself out into
 * plain rubble. Foam and boron sand next to it make that burn-out ten times more likely, which is
 * the original's intended way of fighting a meltdown site rather than waiting it out.</p>
 */
public class RBMKDebrisBurningBlock extends Block {

    public RBMKDebrisBurningBlock(Properties props) {
        super(props);
    }

    /** Original: {@code tickRate} of 20-40 ticks. */
    private static int nextDelay(RandomSource random) {
        return 20 + random.nextInt(20);
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

        // Foam smothers the fire, boron stops the fission - both cut the burn-out timer massively.
        boolean suppressed = isSuppressant(level.getBlockState(side));
        int chance = suppressed ? 10 : 100;

        if (random.nextInt(chance) == 0) {
            level.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS.get().defaultBlockState());
        } else {
            level.scheduleTick(pos, this, nextDelay(random));
        }
    }

    /** The original checks foam_layer / block_foam / sand_boron_layer / boron sand mix. */
    static boolean isSuppressant(BlockState state) {
        return state.is(ModBlocks.SAND_BORON.get())
                || state.is(Blocks.WATER)
                || state.getBlock().getDescriptionId().contains("foam");
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
