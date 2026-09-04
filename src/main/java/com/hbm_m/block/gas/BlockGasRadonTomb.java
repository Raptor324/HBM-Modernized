package com.hbm_m.block.gas;

import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * «Могильный» радон (подземелья): тяжёлое заражение в обход брони.
 * Порт {@link com.hbm.blocks.gas.BlockGasRadonTomb} (1.7.10).
 */
public class BlockGasRadonTomb extends BlockGasBase {

    public BlockGasRadonTomb() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        // 1.7.10: «get fucked» — снимает противоядия (radaway; radx в порте пока не существует)
        PlatformHooks.removeEffect(living, ModEffects.RADAWAY);

        // 1.7.10: ContaminationUtil.contaminate RADIATION RAD_BYPASS 0.5F — в обход брони
        ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 0.5F);
        HbmLivingProps.incrementAsbestos(living, 10);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState b = level.getBlockState(below);

            if (b.is(Blocks.GRASS_BLOCK)) {
                level.setBlockAndUpdate(below, random.nextInt(5) == 0 ? Blocks.DIRT.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
            } else if (isVegetation(b)) {
                level.removeBlock(below, false);
            }
        }

        if (random.nextInt(600) == 0) {
            level.removeBlock(pos, false);
            return;
        }

        super.tick(state, level, pos, random);
    }

    private static boolean isVegetation(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.VINE)
                || PlatformHooks.isGrassBlock(state);
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            return Direction.UP;
        }
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}