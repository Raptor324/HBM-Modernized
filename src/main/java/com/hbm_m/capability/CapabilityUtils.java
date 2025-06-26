package com.hbm_m.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class CapabilityUtils {
    public static <T> Capability<T> getCapability(Class<T> type) {
        return CapabilityManager.get(new CapabilityToken<T>() {});
    }
}