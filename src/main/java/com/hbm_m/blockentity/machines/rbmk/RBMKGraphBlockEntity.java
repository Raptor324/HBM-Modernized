package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * 1:1 port of {@code TileEntityRBMKGraph}: 2 rolling 30-sample history traces, each sampling its
 * RTTY channel every 10 ticks - matches the original's {@code pushValue} cadence.
 */
public class RBMKGraphBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 2;
    public static final int HISTORY_LENGTH = 30;

    public final String[] channel = new String[UNITS];
    public final long[][] history = new long[UNITS][HISTORY_LENGTH];

    /** GraphUnit.min/max plus their enable flags - when unset the axis auto-scales to the data. */
    public final long[]    graphMin  = new long[UNITS];
    public final long[]    graphMax  = new long[UNITS];
    public final boolean[] minBound  = new boolean[UNITS];
    public final boolean[] maxBound  = new boolean[UNITS];
    private int tickCounter = 0;

    public RBMKGraphBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_GRAPH_BE.get(), pos, state);
        Arrays.fill(channel, "");
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        tickCounter++;
        if (tickCounter < 10) return;
        tickCounter = 0;

        for (int i = 0; i < UNITS; i++) {
            if (channel[i] == null || channel[i].isEmpty()) continue;
            RTTYNetwork.RttyChannel ch = RTTYNetwork.listen(level, channel[i]);
            long value = ch != null && ch.signal != null ? (long) parseNum(String.valueOf(ch.signal), 0) : 0;
            pushValue(i, value);
        }
    }

    private void pushValue(int unit, long value) {
        System.arraycopy(history[unit], 1, history[unit], 0, HISTORY_LENGTH - 1);
        history[unit][HISTORY_LENGTH - 1] = value;
    }

    /** Original per-unit array size (see the matching *Unit inner class). */
    @Override public int unitCount() { return UNITS; }

    @Override
    public void receiveControl(CompoundTag data) {
        receiveSharedControl(data);
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("gmin" + i))    { graphMin[i] = data.getLong("gmin" + i); minBound[i] = true; }
            if (data.contains("gmax" + i))    { graphMax[i] = data.getLong("gmax" + i); maxBound[i] = true; }
            if (data.contains("gminOff" + i)) minBound[i] = false;
            if (data.contains("gmaxOff" + i)) maxBound[i] = false;
        }
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i)) channel[i] = data.getString("channel" + i);
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
            tag.putLongArray("history" + i, history[i]);
            tag.putLong("gmin" + i, graphMin[i]);
            tag.putLong("gmax" + i, graphMax[i]);
            tag.putBoolean("gminB" + i, minBound[i]);
            tag.putBoolean("gmaxB" + i, maxBound[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            graphMin[i] = tag.getLong("gmin" + i);
            graphMax[i] = tag.getLong("gmax" + i);
            minBound[i] = tag.getBoolean("gminB" + i);
            maxBound[i] = tag.getBoolean("gmaxB" + i);
            if (tag.contains("history" + i)) {
                long[] h = tag.getLongArray("history" + i);
                System.arraycopy(h, 0, history[i], 0, Math.min(h.length, HISTORY_LENGTH));
            }
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            tag.putString("channel" + i, channel[i]);
            tag.putLongArray("history" + i, history[i]);
            tag.putLong("gmin" + i, graphMin[i]);
            tag.putLong("gmax" + i, graphMax[i]);
            tag.putBoolean("gminB" + i, minBound[i]);
            tag.putBoolean("gmaxB" + i, maxBound[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            graphMin[i] = tag.getLong("gmin" + i);
            graphMax[i] = tag.getLong("gmax" + i);
            minBound[i] = tag.getBoolean("gminB" + i);
            maxBound[i] = tag.getBoolean("gmaxB" + i);
            if (tag.contains("history" + i)) {
                long[] h = tag.getLongArray("history" + i);
                System.arraycopy(h, 0, history[i], 0, Math.min(h.length, HISTORY_LENGTH));
            }
        }
    }
    *///?}
}
