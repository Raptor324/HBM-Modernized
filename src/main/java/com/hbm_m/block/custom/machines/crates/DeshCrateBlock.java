package com.hbm_m.block.custom.machines.crates;

import com.hbm_m.block.entity.custom.crates.DeshCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DeshCrateBlock extends BaseCrateBlock {

    public DeshCrateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeshCrateBlockEntity(pos, state);
    }
}
