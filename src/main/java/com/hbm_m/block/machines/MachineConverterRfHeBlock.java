package com.hbm_m.block.machines;

import com.hbm_m.api.energy.ConverterBlock;
import com.hbm_m.api.energy.ConverterBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineConverterRfHeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Port of {@code MachineConverterRfHe} (1.7.10 Original). */
public class MachineConverterRfHeBlock extends ConverterBlock {

    public MachineConverterRfHeBlock(Properties properties) { super(properties); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineConverterRfHeBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.MACHINE_CONVERTER_RF_HE_BE.get(), ConverterBlockEntity::serverTick);
    }
}
