package com.hbm_m.block.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code BlockLayering} (1.7.10) для oil_spill: тонкий слой (ковёр),
 * высота как у снега, замещаемый при установке, стекается до 8 слоёв.
 * В отличие от снега не тает и не требует холода.
 */
public class OilSpillBlock extends SnowLayerBlock {

    public OilSpillBlock(Properties props) {
        super(props);
    }

    // В оригинале нефть не тает — отключаем randomTick снега
    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
    }
}
