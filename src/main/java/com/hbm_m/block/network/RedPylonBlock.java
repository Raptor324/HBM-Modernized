package com.hbm_m.block.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RedPylonBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт PylonRedWire (1.7.10): обычный (деревянный) и стальной пилон, SINGLE, радиус 25 м, башня 5 блоков.
 */
public class RedPylonBlock extends RedPylonCoreBlock {

    public RedPylonBlock(Properties properties) {
        super(properties);
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RedPylonBlock> CODEC =
            simpleCodec(RedPylonBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected int[] getDimensions() {
        return new int[] {4, 0, 0, 0, 0, 0};
    }

    @Override
    protected BlockEntityType<? extends RedPylonBlockEntity> getBlockEntityType() {
        return ModBlockEntities.RED_PYLON_BE.get();
    }

    @Override
    protected String getTypeKey() {
        return "tooltip.hbm_m.connection_single";
    }

    @Override
    protected int getRange() {
        return 25;
    }
}
