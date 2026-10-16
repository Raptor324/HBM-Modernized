package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 port of {@code RBMKDebrisDigamma} - what a meltdown leaves behind when the reactor was
 * running Digamma fuel.
 *
 * <p>It does two things ordinary rubble does not: it bathes everything within 32 blocks in digamma
 * radiation, and it <b>spreads</b>, converting any adjacent RBMK rubble or corium into more of
 * itself once per tick cycle. Left alone it will eat the whole crater.</p>
 *
 * <p>The port had this registered as a plain decorative {@link Block} with no behaviour at all, so
 * a Digamma meltdown produced blocks that looked ominous and did nothing.</p>
 */
public class RBMKDebrisDigammaBlock extends Block {

    private static final double RANGE = 32D;
    private static final float DIGAMMA = 200F;

    public RBMKDebrisDigammaBlock(Properties props) {
        super(props);
    }

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
        radiate(level, pos);

        for (Direction dir : Direction.values()) {
            BlockPos side = pos.relative(dir);
            BlockState neighbour = level.getBlockState(side);
            if (isConvertible(neighbour)) {
                level.setBlockAndUpdate(side, defaultBlockState());
            }
        }

        level.scheduleTick(pos, this, nextDelay(random));
    }

    /** Any other kind of RBMK rubble, plus corium, turns into digamma rubble on contact. */
    private static boolean isConvertible(BlockState state) {
        return state.is(ModBlocks.RBMK_DEBRIS.get())
                || state.is(ModBlocks.RBMK_DEBRIS_BURNING.get())
                || state.is(ModBlocks.RBMK_DEBRIS_RADIATING.get())
                || state.is(ModBlocks.RBMK_CORIUM.get());
    }

    private static void radiate(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(RANGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            ContaminationUtil.applyDigammaData(e, DIGAMMA);
        }
    }
}
