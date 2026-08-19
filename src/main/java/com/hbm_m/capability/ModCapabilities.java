package com.hbm_m.capability;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import net.minecraft.world.level.block.entity.BlockEntity;

//? if neoforge {
/*import net.neoforged.neoforge.capabilities.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    @SuppressWarnings("unchecked")
    public static void register(RegisterCapabilitiesEvent event) {
        for (var supplier : com.hbm_m.blockentity.ModBlockEntities.BLOCK_ENTITIES) {
            if (!supplier.isPresent()) continue;
            var type = (BlockEntityType<BlockEntity>) supplier.get();

            // 1. Предметы (ItemHandler)
            event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, type,
                (be, side) -> (be instanceof BaseHbmBlockEntity hbm) ? (net.neoforged.neoforge.items.IItemHandler) hbm.getItemHandler(side) : null
            );

            // 2. Жидкости (FluidHandler)
            event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, type,
                (be, side) -> (be instanceof BaseHbmBlockEntity hbm) ? (net.neoforged.neoforge.fluids.capability.IFluidHandler) hbm.getFluidHandler(side) : null
            );

            // 3. Энергия (EnergyStorage)
            event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, type,
                (be, side) -> {
                    if (be instanceof com.hbm_m.api.energy.ConverterBlockEntity conv)
                        return new com.hbm_m.api.energy.HbmForgeWrapper(conv);
                    return (be instanceof BaseHbmBlockEntity hbm) ? (net.neoforged.neoforge.energy.IEnergyStorage) hbm.getEnergyStorage(side) : null;
                }
            );

            // 4. HBM Custom Energy
            event.registerBlockEntity(HBM_ENERGY_PROVIDER, type, (be, side) -> (be instanceof IEnergyProvider p) ? p : null);
            event.registerBlockEntity(HBM_ENERGY_RECEIVER, type, (be, side) -> (be instanceof IEnergyReceiver r) ? r : null);
            event.registerBlockEntity(HBM_ENERGY_CONNECTOR, type, (be, side) -> (be instanceof IEnergyConnector c && c.canConnectEnergy(side)) ? c : null);
        }

        registerBatteryItemCaps(event);
    }

    private static void registerBatteryItemCaps(RegisterCapabilitiesEvent event) {
        for (var supplier : com.hbm_m.item.ModItems.ITEMS) {
            if (!supplier.isPresent()) continue;
            net.minecraft.world.item.Item item = supplier.get();
            if (!(item instanceof com.hbm_m.item.fekal_electric.ModBatteryItem battery)) continue;

            event.registerItem(HBM_ITEM_ENERGY_PROVIDER, (stack, ctx) ->
                    new com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage(
                            stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()), item);
            event.registerItem(HBM_ITEM_ENERGY_RECEIVER, (stack, ctx) ->
                    new com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage(
                            stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()), item);

            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) ->
                    new com.hbm_m.api.energy.LongEnergyWrapper(
                            new com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage(
                                    stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()),
                            com.hbm_m.api.energy.LongEnergyWrapper.BitMode.LOW), item);
        }
    }

    public static boolean hasEnergyComponent(BlockEntity be) {
        return be instanceof IEnergyConnector
            || be instanceof IEnergyProvider
            || be instanceof IEnergyReceiver;
    }
}
*///?}