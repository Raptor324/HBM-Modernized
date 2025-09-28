package com.hbm_m.capability;

// Утилитарный класс для работы с capability в Minecraft Forge.
// Позволяет безопасно получать Capability<T> по классу T.
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class CapabilityUtils {
    public static <T> Capability<T> getCapability(Class<T> type) {
        return CapabilityManager.get(new CapabilityToken<T>() {});
    }
}