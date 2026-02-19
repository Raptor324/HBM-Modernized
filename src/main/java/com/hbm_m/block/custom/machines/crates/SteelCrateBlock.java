package com.hbm_m.block.custom.machines.crates;

import com.hbm_m.block.entity.custom.crates.SteelCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SteelCrateBlock extends BaseCrateBlock {

    public SteelCrateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteelCrateBlockEntity(pos, state);
    }
}
