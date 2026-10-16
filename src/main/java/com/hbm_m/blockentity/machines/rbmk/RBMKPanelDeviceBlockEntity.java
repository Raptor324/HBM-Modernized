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

    /**
     * Per-unit presentation state shared by every original panel unit type ({@code GaugeUnit},
     * {@code IndicatorUnit}, {@code KeyUnit}, {@code LeverUnit}, {@code DisplayUnit} and
     * {@code GraphUnit} all declare exactly these three): an explicit on/off toggle, a free-text
     * label drawn next to the unit, and a tint color. The renderers skip inactive units entirely,
     * matching the original's {@code if(!unit.active) continue;}.
     */
    public final boolean[] unitActive;
    public final String[]  unitLabel;
    public final int[]     unitColor;

    protected RBMKPanelDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        int n = unitCount();
        unitActive = new boolean[n];
        unitLabel  = new String[n];
        unitColor  = new int[n];
        java.util.Arrays.fill(unitLabel, "");
        for (int i = 0; i < n; i++) unitColor[i] = defaultUnitColor(i);
    }

    /** Number of independent units this panel shows (original: gauges[4], indicators[6], …). */
    public abstract int unitCount();

    /** Original per-unit constructor defaults; overridden where the original seeds colors. */
    protected int defaultUnitColor(int index) { return 0x00FF00; }

    public boolean isUnitActive(int i) { return i >= 0 && i < unitActive.length && unitActive[i]; }
    public String  getUnitLabel(int i)  { return i >= 0 && i < unitLabel.length && unitLabel[i] != null ? unitLabel[i] : ""; }
    public int     getUnitColor(int i)  { return i >= 0 && i < unitColor.length ? unitColor[i] : 0x00FF00; }

    /** Applies the shared "active"/"label"/"color" config keys; subclasses call this first. */
    protected void receiveSharedControl(net.minecraft.nbt.CompoundTag data) {
        for (int i = 0; i < unitCount(); i++) {
            if (data.contains("active" + i)) unitActive[i] = data.getBoolean("active" + i);
            if (data.contains("ulabel" + i)) unitLabel[i]  = data.getString("ulabel" + i);
            if (data.contains("ucolor" + i)) unitColor[i]  = data.getInt("ucolor" + i);
        }
    }

    private void saveShared(net.minecraft.nbt.CompoundTag tag) {
        for (int i = 0; i < unitCount(); i++) {
            tag.putBoolean("active" + i, unitActive[i]);
            tag.putString("ulabel" + i, unitLabel[i] == null ? "" : unitLabel[i]);
            tag.putInt("ucolor" + i, unitColor[i]);
        }
    }

    private void loadShared(net.minecraft.nbt.CompoundTag tag) {
        for (int i = 0; i < unitCount(); i++) {
            unitActive[i] = tag.getBoolean("active" + i);
            unitLabel[i]  = tag.contains("ulabel" + i) ? tag.getString("ulabel" + i) : "";
            unitColor[i]  = tag.contains("ucolor" + i) ? tag.getInt("ucolor" + i) : defaultUnitColor(i);
        }
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        saveShared(tag);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        loadShared(tag);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveShared(tag);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadShared(tag);
    }
    *///?}

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
