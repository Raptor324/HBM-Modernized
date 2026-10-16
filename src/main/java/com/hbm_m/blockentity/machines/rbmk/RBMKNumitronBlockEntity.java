package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/** 1:1 port of {@code TileEntityRBMKNumitron}/{@code TileEntityRBMKDisplay}: 2 numeric 7-segment-style readouts. */
public class RBMKNumitronBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 2;
    public final String[] channel = new String[UNITS];
    public final double[] value   = new double[UNITS];

    /** DisplayUnit.leading_zeroes / shorten_number / active_digits, with the original's
     *  constructor defaults (all digits lit, SI-shortened, zero-padded). */
    public final boolean[] leadingZeroes = new boolean[UNITS];
    public final boolean[] shortenNumber = new boolean[UNITS];
    public final long[]    activeDigits  = new long[UNITS];
    /**
     * {@code polling}: a polling unit re-reads its channel every tick and falls back to zero when
     * the signal stops, so it reads as live. A non-polling one latches whatever it last saw. The
     * port had no polling flag, so every unit behaved as latching and a dead channel left the
     * readout frozen.
     */
    public final boolean[] polling = new boolean[UNITS];

    public RBMKNumitronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_NUMITRON_BE.get(), pos, state);
        java.util.Arrays.fill(activeDigits, 0b01111111L);
        java.util.Arrays.fill(shortenNumber, true);
        java.util.Arrays.fill(leadingZeroes, true);
        Arrays.fill(channel, "");
        for (int i = 0; i < UNITS; i++) unitLabel[i] = "Numitron " + (i + 1);
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        for (int i = 0; i < UNITS; i++) {
            // CE's DisplayUnit.update bails on an inactive unit.
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
            if (data.contains("zeroes" + i)) leadingZeroes[i] = data.getBoolean("zeroes" + i);
            if (data.contains("short" + i))  shortenNumber[i] = data.getBoolean("short" + i);
            if (data.contains("digits" + i)) activeDigits[i]  = data.getLong("digits" + i);
            if (data.contains("polling" + i)) polling[i] = data.getBoolean("polling" + i);
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
            tag.putDouble("value" + i, value[i]);
            tag.putBoolean("zeroes" + i, leadingZeroes[i]);
            tag.putBoolean("short" + i, shortenNumber[i]);
            tag.putLong("digits" + i, activeDigits[i]);
            tag.putBoolean("polling" + i, polling[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            value[i]   = tag.getDouble("value" + i);
            leadingZeroes[i] = !tag.contains("zeroes" + i) || tag.getBoolean("zeroes" + i);
            shortenNumber[i] = !tag.contains("short" + i)  || tag.getBoolean("short" + i);
            activeDigits[i]  = tag.contains("digits" + i) ? tag.getLong("digits" + i) : 0b01111111L;
            polling[i] = tag.getBoolean("polling" + i);
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            tag.putString("channel" + i, channel[i]);
            tag.putDouble("value" + i, value[i]);
            tag.putBoolean("zeroes" + i, leadingZeroes[i]);
            tag.putBoolean("short" + i, shortenNumber[i]);
            tag.putLong("digits" + i, activeDigits[i]);
            tag.putBoolean("polling" + i, polling[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            value[i]   = tag.getDouble("value" + i);
            leadingZeroes[i] = !tag.contains("zeroes" + i) || tag.getBoolean("zeroes" + i);
            shortenNumber[i] = !tag.contains("short" + i)  || tag.getBoolean("short" + i);
            activeDigits[i]  = tag.contains("digits" + i) ? tag.getLong("digits" + i) : 0b01111111L;
            polling[i] = tag.getBoolean("polling" + i);
        }
    }
    *///?}
}
