package com.hbm_m.block.machines.fusion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code MachineFusionPlasmaForge} (1.7.10): neun ineinandergeschachtelte Quader
 * bilden die abgetreppte Halle, dazu zehn Anschlusszellen an den beiden Stirnseiten.
 */
public class MachineFusionPlasmaForgeBlock extends FusionMultiblockBlock {

    public MachineFusionPlasmaForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder builder = DummyableStructureBuilder.create()
                .box(2, 0, 2, 2, 5, 5)
                .box(4, -3, 0, 0, 4, 4)
                .box(2, 0, 3, -2, 4, 4)
                .box(2, 0, -2, 3, 4, 4)
                .box(2, 0, 4, -3, 3, 3)
                .box(2, 0, -3, 4, 3, 3)
                .box(2, 0, 5, -4, 2, 2)
                .box(2, 0, -4, 5, 2, 2)
                .box(3, -2, 1, 1, 5, 5);

        for (int i = -2; i <= 2; i++) {
            builder.extra(5, 0, i);
            builder.extra(-5, 0, i);
        }

        return builder.placementOffset(5)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionPlasmaForgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FUSION_PLASMA_FORGE_BE.get(), FusionPlasmaForgeBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFusionPlasmaForgeBlock> CODEC = simpleCodec(MachineFusionPlasmaForgeBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
