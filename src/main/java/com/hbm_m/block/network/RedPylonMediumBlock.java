package com.hbm_m.block.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RedPylonMediumBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт PylonMedium (1.7.10): средний пилон, TRIPLE, радиус 45 м, башня 7 блоков.
 * Вариант с трансформером связывает сеть с блоком позади (пара «пилон + трансформер»).
 */
public class RedPylonMediumBlock extends RedPylonCoreBlock {

    protected final boolean transformer;

    public RedPylonMediumBlock(Properties properties, boolean transformer) {
        super(properties);
        this.transformer = transformer;
        //? if > 1.20.1 {
        /*this.codec = makeCodec(p -> new RedPylonMediumBlock(p, transformer));
        *///?}
    }

    //? if > 1.20.1 {
    /*private final com.mojang.serialization.MapCodec<RedPylonMediumBlock> codec;
    *///?}
    //? if > 1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return codec; }
    *///?}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Крепления уходят в сторону взгляда игрока; трансформер ставится сзади.
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

    public boolean isTransformer() {
        return transformer;
    }

    @Override
    protected int[] getDimensions() {
        return new int[] {6, 0, 0, 0, 0, 0};
    }

    @Override
    protected BlockEntityType<? extends RedPylonMediumBlockEntity> getBlockEntityType() {
        return ModBlockEntities.RED_PYLON_MEDIUM_BE.get();
    }

    @Override
    protected String getTypeKey() {
        return "tooltip.hbm_m.connection_triple";
    }

    @Override
    protected int getRange() {
        return 45;
    }
}
