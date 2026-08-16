package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for the 7 RTTY-driven RBMK control-room panels (gauge/indicator/lever/numitron/
 * graph/terminal/keypad). In the original these were 8 entirely separate block+tile+GUI+renderer
 * sets talking to the shared {@code RTTYSystem} radio bus; this port keeps that same wiring
 * (through {@link RTTYNetwork}, the already-ported equivalent) but factors the common
 * tick/config/NBT plumbing into one base class per {@code RBMKPanelDeviceBlock} (see that class
 * for the shared block-level interaction handling).
 * <p>
 * Panels are informational/control devices, not reactor columns - they never have a lid and
 * never contribute to the neutron simulation.
 */
public abstract class RBMKPanelDeviceBlockEntity extends RBMKColumnBlockEntity implements IRadioTorchConfigurable {

    protected RBMKPanelDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public final void tickPanel(Level level, BlockPos pos) {
        baseTick(level, pos, getBlockState(), this);
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());
        onPanelTick(level, pos);
    }

    protected abstract void onPanelTick(Level level, BlockPos pos);

    @Override public boolean hasLid()           { return false; }
    @Override public boolean isLidRemovable()   { return false; }
    @Override public RBMKType getRBMKType()     { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType(){ return ColumnType.BLANK; }
    @Override protected boolean participatesInHeatNetwork() { return false; }

    protected static double parseNum(String s, double fallback) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return fallback; }
    }

    protected void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
