package com.hbm_m.block;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

import static com.hbm_m.block.MachineBatteryBlock.FACING;

public class AnvilBlock extends Block {
    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext pContext) {
        // Устанавливаем направление блока в зависимости от того, куда смотрел игрок при установке
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }


    public AnvilBlock(Properties p_49795_) {
        super(p_49795_);
    }
}
