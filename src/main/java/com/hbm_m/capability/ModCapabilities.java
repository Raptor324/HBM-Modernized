package com.hbm_m.capability;// package com.hbm_m.api.capability; (или где у тебя хранятся capabilities)

import com.hbm_m.energy.ILongEnergyStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {

    // Наш новый capability для long-энергии
    public static final Capability<ILongEnergyStorage> LONG_ENERGY =
            CapabilityManager.get(new CapabilityToken<>() {});

    // Метод для регистрации
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(ILongEnergyStorage.class);
    }
}
