package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared block entity for all RBMK panel/display blocks (gauge, indicator, lever, numitron, etc.).
 * Stores a linked console position and a display value.
 */
public class RBMKPanelBlockEntity extends RBMKColumnBlockEntity {

    /** Position of the linked RBMK console, or null if not linked. */
    public BlockPos linkedConsole = null;
    /** Display value (int) â€" interpretation depends on block type. */
    public double displayValue = 0;
    /** Panel-specific mode/channel index. */
    public int channel = 0;

    public RBMKPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_PANEL_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKPanelBlockEntity be) {
        baseTick(level, pos, state, be);
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.BLANK; }
    @Override protected boolean participatesInHeatNetwork() { return false; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedConsole != null) {
            tag.putInt("lx", linkedConsole.getX());
            tag.putInt("ly", linkedConsole.getY());
            tag.putInt("lz", linkedConsole.getZ());
        }
        tag.putDouble("displayValue", displayValue);
        tag.putInt("channel", channel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("lx")) {
            linkedConsole = new BlockPos(tag.getInt("lx"), tag.getInt("ly"), tag.getInt("lz"));
        }
        displayValue = tag.getDouble("displayValue");
        channel      = tag.getInt("channel");
    }
}

