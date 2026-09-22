package com.hbm_m.block.machines.fusion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.fusion.StructTorusCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockFusionTorusStruct} (1.7.10): ein gewoehnlicher Block, der zum fertigen
 * Fusionstorus wird, sobald die Torusform um ihn herum vollstaendig ist. Er ist im Original
 * ausdruecklich kein voller Block ({@code isOpaqueCube() == false}).
 */
public class StructTorusCoreBlock extends BaseEntityBlock {

    public StructTorusCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructTorusCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STRUCT_TORUS_CORE_BE.get(), StructTorusCoreBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<StructTorusCoreBlock> CODEC = simpleCodec(StructTorusCoreBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
