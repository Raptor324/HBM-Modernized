package com.hbm_m.block.machines;

import com.hbm_m.blockentity.machines.JAS39TrophyBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Port of the Su-47 trophy pattern ({@link SU47TrophyBlock}), applied to the Saab JAS 39 Gripen E. */
public class JAS39TrophyBlock extends BaseEntityBlock {

    public JAS39TrophyBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JAS39TrophyBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
