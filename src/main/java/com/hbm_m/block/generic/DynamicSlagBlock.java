package com.hbm_m.block.generic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.SlagBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 {@code BlockDynamicSlag} - the molten-material puddle dynamically placed by a
 * {@link com.hbm_m.blockentity.machines.MachineFoundrySlagtapBlockEntity}. Not the same block as the
 * static decorative {@code block_slag} (creeper-shell debris) - see {@link SlagBlockEntity} for the
 * scope-simplification note (no auto-spread/decay).
 */
public class DynamicSlagBlock extends BaseEntityBlock {

    public DynamicSlagBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SlagBlockEntity(pos, state);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DynamicSlagBlock> CODEC = simpleCodec(DynamicSlagBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
