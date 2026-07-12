package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Passive floor block placed below RBMK columns to serve as a mechanical interface for the crane/autoloader.
 */
public class RBMKLoaderBlockEntity extends RBMKColumnBlockEntity {

    public RBMKLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_LOADER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKLoaderBlockEntity be) {
        baseTick(level, pos, state, be);
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.BLANK; }
}

