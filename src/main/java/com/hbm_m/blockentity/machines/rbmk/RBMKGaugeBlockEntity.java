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
    public final double[] min     = new double[UNITS];
    public final double[] max     = new double[UNITS];
    public final double[] value   = new double[UNITS];
    /** GaugeUnit.lastRenderValue - previous tick's value, for needle interpolation. */
    public final double[] lastValue = new double[UNITS];

    /**
     * {@code GaugeUnit.polling}: a polling gauge re-reads its channel every tick and drops back to
     * zero the moment the signal stops, so it reads as a live instrument. A non-polling one latches
     * the last value it saw. The port had no polling flag at all, so every gauge behaved as
     * latching and a dead channel left the needle frozen at its last reading.
     */
    public final boolean[] polling = new boolean[UNITS];

    public RBMKGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_GAUGE_BE.get(), pos, state);
        Arrays.fill(channel, "");
        Arrays.fill(max, 100.0);
        for (int i = 0; i < UNITS; i++) unitLabel[i] = "Gauge " + (i + 1);
    }

    /** CE's per-index needle colours (GaugeUnit constructor). */
    @Override
    protected int defaultUnitColor(int index) {
        return switch (index) {
            case 0 -> 0x800000;
            case 1 -> 0x804000;
            case 2 -> 0x808000;
            default -> 0x000080;
        };
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        for (int i = 0; i < UNITS; i++) {
            lastValue[i] = value[i];

            // CE's GaugeUnit.update bails on an inactive unit; the port updated every unit whether
            // the operator had switched it on or not.
            if (!isUnitActive(i)) continue;
            if (channel[i] == null || channel[i].isEmpty()) continue;

            RTTYNetwork.RttyChannel ch = RTTYNetwork.listen(level, channel[i]);
            if (ch != null && ch.signal != null) {
                value[i] = parseNum(String.valueOf(ch.signal), value[i]);
            } else if (polling[i]) {
                value[i] = 0;
            }
        }
    }

    /** Original per-unit array size (see the matching *Unit inner class). */
    @Override public int unitCount() { return UNITS; }

    @Override
    public void receiveControl(CompoundTag data) {
        receiveSharedControl(data);
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i)) channel[i] = data.getString("channel" + i);
            if (data.contains("min" + i))     min[i]     = data.getDouble("min" + i);
            if (data.contains("max" + i))     max[i]     = data.getDouble("max" + i);
            if (data.contains("polling" + i)) polling[i] = data.getBoolean("polling" + i);
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
            tag.putDouble("value" + i, value[i]);
            tag.putBoolean("polling" + i, polling[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            min[i]     = tag.getDouble("min" + i);
            max[i]     = tag.contains("max" + i) ? tag.getDouble("max" + i) : 100.0;
            value[i]   = tag.getDouble("value" + i);
            polling[i] = tag.getBoolean("polling" + i);
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
            tag.putDouble("value" + i, value[i]);
            tag.putBoolean("polling" + i, polling[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            min[i]     = tag.getDouble("min" + i);
            max[i]     = tag.contains("max" + i) ? tag.getDouble("max" + i) : 100.0;
            value[i]   = tag.getDouble("value" + i);
            polling[i] = tag.getBoolean("polling" + i);
        }
    }
    *///?}
}
