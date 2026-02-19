package com.hbm_m.block.custom.machines.crates;

import com.hbm_m.block.entity.custom.crates.TemplateCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TemplateCrateBlock extends BaseCrateBlock {

    public TemplateCrateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TemplateCrateBlockEntity(pos, state);
    }
}
