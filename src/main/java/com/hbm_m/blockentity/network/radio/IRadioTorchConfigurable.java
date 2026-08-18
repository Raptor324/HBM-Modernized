package com.hbm_m.blockentity.network.radio;

import net.minecraft.nbt.CompoundTag;

/** Implemented by every radio-torch block entity so {@code RadioTorchControlPacket} can dispatch generically. */
public interface IRadioTorchConfigurable {
    void receiveControl(CompoundTag data);
}
