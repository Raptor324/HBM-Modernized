package com.hbm_m.capability;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import net.minecraft.world.level.block.entity.BlockEntity;

//? if neoforge {
/*import net.neoforged.neoforge.capabilities.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import com.hbm_m.lib.RefStrings;
*///?}
//? if forge {
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}

//? if forge {
public class ModCapabilities {
    public static final Capability<IEnergyProvider>  HBM_ENERGY_PROVIDER  = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IEnergyReceiver>  HBM_ENERGY_RECEIVER  = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IEnergyConnector> HBM_ENERGY_CONNECTOR = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IEnergyProvider.class);
        event.register(IEnergyReceiver.class);
        event.register(IEnergyConnector.class);
    }

    public static boolean hasEnergyComponent(BlockEntity be) {
        return be.getCapability(HBM_ENERGY_CONNECTOR).isPresent()
            || be.getCapability(HBM_ENERGY_PROVIDER).isPresent()
            || be.getCapability(HBM_ENERGY_RECEIVER).isPresent();
    }
}
//?}

//? if neoforge {
/*public class ModCapabilities {
    public static final BlockCapability<IEnergyProvider, Direction> HBM_ENERGY_PROVIDER =
        BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "energy_provider"), IEnergyProvider.class);

    public static final BlockCapability<IEnergyReceiver, Direction> HBM_ENERGY_RECEIVER =
        BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "energy_receiver"), IEnergyReceiver.class);

    public static final BlockCapability<IEnergyConnector, Direction> HBM_ENERGY_CONNECTOR =
        BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "energy_connector"), IEnergyConnector.class);

    public static final ItemCapability<IEnergyProvider, Void> HBM_ITEM_ENERGY_PROVIDER =
        ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "item_energy_provider"), IEnergyProvider.class);

    public static final ItemCapability<IEnergyReceiver, Void> HBM_ITEM_ENERGY_RECEIVER =
        ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "item_energy_receiver"), IEnergyReceiver.class);

    public static void register(RegisterCapabilitiesEvent event) {
        // Fallbacks automatically map your BlockEntity implemented interfaces to capabilities
        event.registerBlockEntityFallback(HBM_ENERGY_PROVIDER, (be, side) -> be instanceof IEnergyProvider p ? p : null);
        event.registerBlockEntityFallback(HBM_ENERGY_RECEIVER, (be, side) -> be instanceof IEnergyReceiver r ? r : null);
        event.registerBlockEntityFallback(HBM_ENERGY_CONNECTOR, (be, side) -> be instanceof IEnergyConnector c ? c : null);
    }

    public static boolean hasEnergyComponent(BlockEntity be) {
        return be instanceof IEnergyConnector
            || be instanceof IEnergyProvider
            || be instanceof IEnergyReceiver;
    }
}
*///?}

//? if fabric {
/*public class ModCapabilities {
    public static boolean hasEnergyComponent(BlockEntity be) {
        return be instanceof IEnergyConnector
                || be instanceof IEnergyProvider
                || be instanceof IEnergyReceiver;
    }
}
*///?}