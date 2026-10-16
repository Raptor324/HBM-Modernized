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

    public RBMKNumitronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_NUMITRON_BE.get(), pos, state);
        Arrays.fill(channel, "");
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
        }
        setChanged();
        syncToClient();
    }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        tag.putString("channel" + i, channel[i]);
        tag.putDouble("value" + i, value[i]);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        value[i]   = tag.getDouble("value" + i);
        }
    }
}
