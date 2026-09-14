package com.hbm_m.block.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.pneumatic.PneumoStorageImporterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port von {@code PneumoStorageImporter} (1.7.10). Die Eingabe: schiebt alles, was hineingelegt wird, ins Lagernetz.
 */
public class PneumoStorageImporterBlock extends PneumaticStorageBlockBase {

    public PneumoStorageImporterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumoStorageImporterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PNEUMO_STORAGE_IMPORTER_BE.get(),
                (lvl, p, st, be) -> PneumoStorageImporterBlockEntity.tick(lvl, p, st, (PneumoStorageImporterBlockEntity) be));
    }

    //? if >1.20.1 {
    public static final com.mojang.serialization.MapCodec<PneumoStorageImporterBlock> CODEC = simpleCodec(PneumoStorageImporterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    //?}
}
