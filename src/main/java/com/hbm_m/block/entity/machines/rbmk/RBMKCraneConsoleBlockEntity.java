package com.hbm_m.block.entity.machines.rbmk;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Console for controlling the RBMK crane/autoloader system.
 */
public class RBMKCraneConsoleBlockEntity extends RBMKColumnBlockEntity {

    public RBMKCraneConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKCraneConsoleBlockEntity be) {
        baseTick(level, pos, state, be);
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.BLANK; }
}

