package com.hbm_m.block.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RedPylonLargeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт PylonLarge (1.7.10): большой пилон, QUAD, радиус 100 м.
 * Габарит 3×3×15 (ядро внизу по центру); требует подстанцию для пользы в сети.
 */
public class RedPylonLargeBlock extends RedPylonCoreBlock {

    public RedPylonLargeBlock(Properties properties) {
        super(properties);
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RedPylonLargeBlock> CODEC =
            simpleCodec(RedPylonLargeBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected int[] getDimensions() {
        return new int[] {13, 0, 1, 1, 1, 1};
    }

    @Override
    protected BlockEntityType<? extends RedPylonLargeBlockEntity> getBlockEntityType() {
        return ModBlockEntities.RED_PYLON_LARGE_BE.get();
    }

    @Override
    protected String getTypeKey() {
        return "tooltip.hbm_m.connection_quad";
    }

    @Override
    protected int getRange() {
        return 100;
    }
}
