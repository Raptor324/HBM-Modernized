package com.hbm_m.capability;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

//? if neoforge {
/*import net.neoforged.neoforge.capabilities.*;
import net.neoforged.bus.api.SubscribeEvent;
*///?} else {
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}

public class ModCapabilities {
    public static final Capability<IEnergyProvider> HBM_ENERGY_PROVIDER = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IEnergyReceiver> HBM_ENERGY_RECEIVER = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IEnergyConnector> HBM_ENERGY_CONNECTOR = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IEnergyProvider.class);
        event.register(IEnergyReceiver.class);
        event.register(IEnergyConnector.class);
    }
}