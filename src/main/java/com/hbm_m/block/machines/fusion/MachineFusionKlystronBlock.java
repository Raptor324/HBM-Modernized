package com.hbm_m.block.machines.fusion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.fusion.FusionKlystronBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code MachineFusionKlystron} (1.7.10) - Freie-Elektronen-Laser, der den Torus zuendet.
 */
public class MachineFusionKlystronBlock extends FusionMultiblockBlock {

    public MachineFusionKlystronBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: MachineFusionKlystron - getDimensions {3,0,4,3,2,2}, Zusatzkasten {4,-3,4,3,1,1}, drei Anschlusszellen, Offset 3
        return DummyableStructureBuilder.create()
                .box(3, 0, 4, 3, 2, 2)
                .box(4, -3, 4, 3, 1, 1)
                .extra(3, 2, 0)
                .extra(0, 0, 2)
                .extra(0, 0, -2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionKlystronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FUSION_KLYSTRON_BE.get(), FusionKlystronBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFusionKlystronBlock> CODEC = simpleCodec(MachineFusionKlystronBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
