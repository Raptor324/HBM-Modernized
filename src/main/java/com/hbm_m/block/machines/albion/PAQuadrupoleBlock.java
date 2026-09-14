package com.hbm_m.block.machines.albion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.albion.PAQuadrupoleBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockPAQuadrupole} (1.7.10): die Fokussierspule.
 *
 * <p>Ein Wuerfel von drei mal drei mal drei, dazu vier Anschlusszellen - vorn, hinten, oben und
 * unten. Sie buendelt den Strahl wieder, den die Ablenkmagnete auffaechern.</p>
 */
public class PAQuadrupoleBlock extends PAMultiblockBlock {

    public PAQuadrupoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: BlockPAQuadrupole - getDimensions {1,1,1,1,1,1}, getOffset 0, vier Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(1, 1, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 1, 0)
                .extra(0, -1, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PAQuadrupoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PA_QUADRUPOLE_BE.get(),
                (lvl, pos, st, be) -> PAQuadrupoleBlockEntity.tick(lvl, pos, st, (PAQuadrupoleBlockEntity) be));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PAQuadrupoleBlock> CODEC = simpleCodec(PAQuadrupoleBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
