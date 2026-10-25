package com.hbm_m.block.gas;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code com.hbm.blocks.generic.BlockOutgas} (1.7.10): ein Block, der von sich aus
 * Gas in die umliegende Luft abgibt.
 */
public class OutgasBlock extends Block {

    private final Supplier<Block> gas;
    private final boolean onBreak;
    private final boolean onNeighbour;
    private final boolean burstOnBreak;
    private final boolean dustOnStep;

    public OutgasBlock(Properties props, Supplier<Block> gas, boolean randomTick, boolean onBreak) {
        this(props, gas, randomTick, onBreak, false, false, false);
    }

    public OutgasBlock(Properties props, Supplier<Block> gas, boolean randomTick, boolean onBreak,
                       boolean onNeighbour, boolean burstOnBreak, boolean dustOnStep) {
        super(randomTick ? props.randomTicks() : props);
        this.gas = gas;
        this.onBreak = onBreak;
        this.onNeighbour = onNeighbour;
        this.burstOnBreak = burstOnBreak;
        this.dustOnStep = dustOnStep;
    }

    private BlockState gasState() {
        return gas.get().defaultBlockState();
    }

    private void gasIfAir(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, gasState(), 3);
        }
    }

    @Override
    @Deprecated
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        gasIfAir(level, pos.relative(Direction.from3DDataValue(random.nextInt(6))));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);

        if (!dustOnStep) return;

        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir()) return;

        if (level instanceof ServerLevel serverLevel) {
            if (level.random.nextInt(10) == 0) serverLevel.setBlock(above, gasState(), 3);
        } else {
            for (int i = 0; i < 5; i++)
                level.addParticle(com.hbm_m.particle.ModParticleTypes.TOWNAURA.get(), pos.getX() + level.random.nextFloat(), pos.getY() + 1.1, pos.getZ() + level.random.nextFloat(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp) {
        super.spawnAfterBreak(state, level, pos, tool, dropXp);

        if (onBreak) {
            gasIfAir(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, world, pos, newState, isMoving);

        if (burstOnBreak && !state.is(newState.getBlock()) && world instanceof ServerLevel level) {
            for (int ix = -2; ix <= 2; ix++) {
                for (int iy = -2; iy <= 2; iy++) {
                    for (int iz = -2; iz <= 2; iz++) {
                        int sum = Math.abs(ix + iy + iz);
                        if (sum > 0 && sum < 5) {
                            gasIfAir(level, pos.offset(ix, iy, iz));
                        }
                    }
                }
            }
        }
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (!onNeighbour || !(level instanceof ServerLevel serverLevel)) return;

        if (level.random.nextInt(3) == 0) {
            for (Direction dir : Direction.values()) {
                gasIfAir(serverLevel, pos.relative(dir));
            }
        }
    }
}
