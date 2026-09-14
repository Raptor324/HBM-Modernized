package com.hbm_m.block.machines.albion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.albion.PASourceBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockPASource} (1.7.10): die Teilchenquelle.
 *
 * <p>Hier faengt alles an - sie schiesst das Teilchen los und rechnet danach jeden Schritt durch
 * die ganze Strecke ab. Ihre zehn Anschlusszellen liegen seitlich und unter der Kammer.</p>
 */
public class PASourceBlock extends PAMultiblockBlock {

    public PASourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: BlockPASource - getDimensions {1,1,1,1,4,4}, getOffset 0, zehn Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(1, 1, 1, 1, 4, 4)
                .extra(0, 0, 4)
                .extra(1, 0, 0)
                .extra(1, 0, 2)
                .extra(1, 0, -2)
                .extra(-1, 0, 0)
                .extra(-1, 0, 2)
                .extra(-1, 0, -2)
                .extra(0, -1, 0)
                .extra(0, -1, 2)
                .extra(0, -1, -2)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PASourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PA_SOURCE_BE.get(),
                (lvl, pos, st, be) -> PASourceBlockEntity.tick(lvl, pos, st, (PASourceBlockEntity) be));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PASourceBlock> CODEC = simpleCodec(PASourceBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
