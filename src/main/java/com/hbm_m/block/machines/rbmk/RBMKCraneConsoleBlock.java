package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.rbmk.RBMKCraneConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RBMKCraneConsoleBlock extends RBMKColumnBlock {

    public RBMKCraneConsoleBlock(Properties props) { super(props); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKCraneConsoleBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), RBMKCraneConsoleBlockEntity::tick);
    }
}
