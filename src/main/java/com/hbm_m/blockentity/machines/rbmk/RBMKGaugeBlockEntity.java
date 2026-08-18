package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/** 1:1 port of {@code TileEntityRBMKGauge}: 4 needle gauges, each polling a numeric RTTY channel. */
public class RBMKGaugeBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 4;
    public final String[] channel = new String[UNITS];
    public final String[] label   = new String[UNITS];
    public final int[]    color   = new int[UNITS];
    public final double[] min     = new double[UNITS];
    public final double[] max     = new double[UNITS];
    public final double[] value   = new double[UNITS];

    public RBMKGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_GAUGE_BE.get(), pos, state);
        Arrays.fill(channel, "");
        Arrays.fill(label, "");
        Arrays.fill(max, 100.0);
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        for (int i = 0; i < UNITS; i++) {
            if (channel[i] == null || channel[i].isEmpty()) continue;
            RTTYNetwork.RttyChannel ch = RTTYNetwork.listen(level, channel[i]);
            if (ch != null && ch.signal != null) value[i] = parseNum(String.valueOf(ch.signal), value[i]);
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i)) channel[i] = data.getString("channel" + i);
            if (data.contains("label" + i))   label[i]   = data.getString("label" + i);
            if (data.contains("color" + i))   color[i]   = data.getInt("color" + i);
            if (data.contains("min" + i))     min[i]     = data.getDouble("min" + i);
            if (data.contains("max" + i))     max[i]     = data.getDouble("max" + i);
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
        tag.putString("label" + i, label[i]);
        tag.putInt("color" + i, color[i]);
        tag.putDouble("min" + i, min[i]);
        tag.putDouble("max" + i, max[i]);
        tag.putDouble("value" + i, value[i]);
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        tag.putString("channel" + i, channel[i]);
        tag.putString("label" + i, label[i]);
        tag.putInt("color" + i, color[i]);
        tag.putDouble("min" + i, min[i]);
        tag.putDouble("max" + i, max[i]);
        tag.putDouble("value" + i, value[i]);
        }
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < UNITS; i++) {
        channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        label[i]   = tag.contains("label" + i)   ? tag.getString("label" + i)   : "";
        color[i]   = tag.getInt("color" + i);
        min[i]     = tag.getDouble("min" + i);
        max[i]     = tag.contains("max" + i) ? tag.getDouble("max" + i) : 100.0;
        value[i]   = tag.getDouble("value" + i);
        }
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        label[i]   = tag.contains("label" + i)   ? tag.getString("label" + i)   : "";
        color[i]   = tag.getInt("color" + i);
        min[i]     = tag.getDouble("min" + i);
        max[i]     = tag.contains("max" + i) ? tag.getDouble("max" + i) : 100.0;
        value[i]   = tag.getDouble("value" + i);
        }
    }
    *///?}
}
