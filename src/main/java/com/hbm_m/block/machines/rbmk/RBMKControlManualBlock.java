package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.rbmk.RBMKControlManualBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RBMKControlManualBlock extends RBMKColumnBlock {

    public final boolean moderated;

    public RBMKControlManualBlock(boolean moderated, Properties props) {
        super(props);
        this.moderated = moderated;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        RBMKControlManualBlockEntity be = new RBMKControlManualBlockEntity(pos, state);
        be.moderated = moderated;
        return be;
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_CONTROL_BE.get(), RBMKControlManualBlockEntity::tick);
    }
}
