package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * 1:1 port of {@code TileEntityRBMKIndicator}: 6 lamps, each lit whenever its polled RTTY value
 * falls inside {@code [min,max]} (or outside, if {@code invert} is set).
 */
public class RBMKIndicatorBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 6;
    public final String[]  channel = new String[UNITS];
    public final double[]  min     = new double[UNITS];
    public final double[]  max     = new double[UNITS];
    public final boolean[] invert  = new boolean[UNITS];
    public final boolean[] state   = new boolean[UNITS];

    public RBMKIndicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_INDICATOR_BE.get(), pos, state);
        Arrays.fill(channel, "");
        Arrays.fill(max, 1.0);
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        for (int i = 0; i < UNITS; i++) {
            if (channel[i] == null || channel[i].isEmpty()) { state[i] = false; continue; }
            RTTYNetwork.RttyChannel ch = RTTYNetwork.listen(level, channel[i]);
            double value = ch != null && ch.signal != null ? parseNum(String.valueOf(ch.signal), 0) : 0;
            boolean inRange = value >= min[i] && value <= max[i];
            state[i] = invert[i] != inRange;
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i)) channel[i] = data.getString("channel" + i);
            if (data.contains("min" + i))     min[i]     = data.getDouble("min" + i);
            if (data.contains("max" + i))     max[i]     = data.getDouble("max" + i);
            if (data.contains("invert" + i))  invert[i]  = data.getBoolean("invert" + i);
        }
        setChanged();
        syncToClient();
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < UNITS; i++) {
        tag.putString("channel" + i, channel[i]);
        tag.putDouble("min" + i, min[i]);
        tag.putDouble("max" + i, max[i]);
        tag.putBoolean("invert" + i, invert[i]);
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        tag.putString("channel" + i, channel[i]);
        tag.putDouble("min" + i, min[i]);
        tag.putDouble("max" + i, max[i]);
        tag.putBoolean("invert" + i, invert[i]);
        }
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < UNITS; i++) {
        channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        min[i]     = tag.getDouble("min" + i);
        max[i]     = tag.contains("max" + i) ? tag.getDouble("max" + i) : 1.0;
        invert[i]  = tag.getBoolean("invert" + i);
        }
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        min[i]     = tag.getDouble("min" + i);
        max[i]     = tag.contains("max" + i) ? tag.getDouble("max" + i) : 1.0;
        invert[i]  = tag.getBoolean("invert" + i);
        }
    }
    *///?}
}
