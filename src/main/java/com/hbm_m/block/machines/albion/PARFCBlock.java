package com.hbm_m.block.machines.albion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.albion.PARFCBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockPARFC} (1.7.10): die Hochfrequenzkammer.
 *
 * <p>Neun Felder lang - das laengste Bauteil der Strecke. Sie ist es, die dem Teilchen Impuls
 * gibt; alles andere lenkt nur. Das Teilchen tritt vier Felder vor dem Kern ein und verlaesst sie
 * fuenf dahinter.</p>
 */
public class PARFCBlock extends PAMultiblockBlock {

    public PARFCBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: BlockPARFC - getDimensions {1,1,1,1,4,4}, getOffset 0, sechs Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(1, 1, 1, 1, 4, 4)
                .extra(3, 1, 0)
                .extra(-3, 1, 0)
                .extra(0, 1, 0)
                .extra(3, -1, 0)
                .extra(-3, -1, 0)
                .extra(0, -1, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PARFCBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PA_RFC_BE.get(),
                (lvl, pos, st, be) -> PARFCBlockEntity.tick(lvl, pos, st, (PARFCBlockEntity) be));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PARFCBlock> CODEC = simpleCodec(PARFCBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
