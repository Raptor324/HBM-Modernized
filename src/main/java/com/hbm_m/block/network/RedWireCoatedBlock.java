package com.hbm_m.block.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт WireCoated (1.7.10): изолированный красный кабель — полноценный проводник
 * энергосети (узел PowerNode со связями по всем граням), блок-куб с CT-текстурой.
 */
public class RedWireCoatedBlock extends BaseEntityBlock {

    public RedWireCoatedBlock(Properties properties) {
        super(properties);
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RedWireCoatedBlock> CODEC =
            simpleCodec(RedWireCoatedBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof WireBlockEntity wire) {
                WireBlockEntity.tick(lvl, pos, st, wire);
            }
        };
    }
}
