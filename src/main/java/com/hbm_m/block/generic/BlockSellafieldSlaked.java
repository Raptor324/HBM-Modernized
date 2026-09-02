package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class BlockSellafieldSlaked extends Block {

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
    public static final IntegerProperty COLOR_LEVEL = IntegerProperty.create("color_level", 0, 10);

    public BlockSellafieldSlaked(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(VARIANT, 0).setValue(COLOR_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, COLOR_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(VARIANT, variantFromPos(context.getClickedPos()));
    }

    public static BlockState getStateForPos(Block block, BlockPos pos) {
        return block.defaultBlockState().setValue(VARIANT, variantFromPos(pos));
    }

    public static int variantFromPos(BlockPos pos) {
        long l = (pos.getX() * 3129871L) ^ (long) pos.getY() * 116129781L ^ (long) pos.getZ();
        l = l * l * 42317861L + l * 11L;
        return Math.abs((int) (l >> 16 & 3L)) % 4;
    }
}