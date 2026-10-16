package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKModeratorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RBMKModeratorBlock extends RBMKColumnBlock {

    public RBMKModeratorBlock(Properties props) { super(props); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKModeratorBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_MODERATOR_BE.get(), RBMKModeratorBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RBMKModeratorBlock> CODEC = simpleCodec(RBMKModeratorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
