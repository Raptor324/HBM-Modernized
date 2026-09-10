package com.hbm_m.block.gas;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockGasRadonTomb} (1.7.10): das Gas aus dem Endlager. Es geht durch jeden
 * Schutz hindurch, entfernt heilende Effekte und toetet die Vegetation darunter ab.
 *
 * <p><b>Abweichung:</b> Das Original entfernt neben Radaway auch Rad-X - diesen Effekt gibt es im
 * Port noch nicht. Die Materialpruefung fuer Bewuchs (Gras/Laub/Pflanzen/Ranken) laeuft hier ueber
 * {@link BlockTags#LEAVES} und {@link BushBlock} statt ueber die 1.7.10-Materialien.</p>
 */
public class BlockGasRadonTomb extends BlockGasBase {

    public BlockGasRadonTomb() {
        super(0.1F, 0.3F, 0.1F);
    }

    @Override
    protected void affect(LivingEntity living) {
        living.removeEffect(ModEffects.RADX.get()); // Original: removePotionEffect(HbmPotion.radx.id)

        ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 0.5F);
        HbmLivingProps.incrementAsbestos(living, 10);
    }

    @Override
    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        if (random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState ground = level.getBlockState(below);

            if (ground.is(Blocks.GRASS_BLOCK)) {
                if (random.nextInt(5) == 0) {
                    level.setBlock(below, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                } else {
                    level.setBlock(below, ModBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
                }
            }

            if ((ground.is(BlockTags.LEAVES) || ground.getBlock() instanceof BushBlock)
                    && !ground.isCollisionShapeFullBlock(level, below)) {
                level.removeBlock(below, false);
            }
        }

        if (random.nextInt(600) == 0) {
            level.removeBlock(pos, false);
            return true;
        }

        return false;
    }

    @Override
    public Direction getFirstDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) return Direction.UP;
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}
