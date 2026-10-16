package com.hbm_m.block.machines.albion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.albion.PADipoleBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockPADipole} (1.7.10): der Ablenkmagnet.
 *
 * <p>Er nimmt das Teilchen aus jeder der vier waagerechten Richtungen an und schickt es je nach
 * Impuls und Redstonesignal in eine andere weiter - daraus entsteht der Ring. Die acht
 * Anschlusszellen sitzen ueber und unter den vier Seiten und sind darum in jeder Ausrichtung
 * dieselben.</p>
 */
public class PADipoleBlock extends PAMultiblockBlock {

    public PADipoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: BlockPADipole - getDimensions {1,1,1,1,1,1}, getOffset 0, acht Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(1, 1, 1, 1, 1, 1)
                .extra(1, -1, 0)
                .extra(-1, -1, 0)
                .extra(0, -1, 1)
                .extra(0, -1, -1)
                .extra(1, 1, 0)
                .extra(-1, 1, 0)
                .extra(0, 1, 1)
                .extra(0, 1, -1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PADipoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PA_DIPOLE_BE.get(),
                (lvl, pos, st, be) -> PADipoleBlockEntity.tick(lvl, pos, st, (PADipoleBlockEntity) be));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PADipoleBlock> CODEC = simpleCodec(PADipoleBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
