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
/*import com.hbm_m.blockentity.BaseHbmBlockEntity;

public class ModCapabilities {
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

            // Alle Seiten-/Proxy-Regeln stehen in getHbmCapability (1:1-Spiegel des Forge-getCapability),
            // Standard ohne Forge-Zweig: HbmCaps.defaults (bisherige NeoForge-Anmeldung).
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.ITEM_HANDLER, side));
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.FLUID_HANDLER, side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.ENERGY, side));
            event.registerBlockEntity(HBM_ENERGY_PROVIDER, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_PROVIDER, side));
            event.registerBlockEntity(HBM_ENERGY_RECEIVER, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_RECEIVER, side));
            event.registerBlockEntity(HBM_ENERGY_CONNECTOR, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_CONNECTOR, side));
        }

        registerBatteryItemCaps(event);
    }

    private static void registerBatteryItemCaps(RegisterCapabilitiesEvent event) {
        for (var supplier : com.hbm_m.item.ModItems.ITEMS) {
            if (!supplier.isPresent()) continue;
            net.minecraft.world.item.Item item = supplier.get();

            // Infinite Fluid Barrel: NeoForge-аналог forge initCapabilities.
            if (item instanceof com.hbm_m.item.liquids.InfiniteFluidItem infinite) {
                event.registerItem(Capabilities.FluidHandler.ITEM,
                        (stack, ctx) -> new com.hbm_m.item.liquids.InfiniteFluidItem.InfiniteFluidCapabilityHandler(
                                stack, infinite.getTransferRate(), infinite.getFixedFluid()), item);
                continue;
            }

            // Fluessigkeitsfass (Forge: initCapabilities -> FluidBarrelCapabilityProvider)
            if (item instanceof com.hbm_m.item.liquids.FluidBarrelItem) {
                event.registerItem(Capabilities.FluidHandler.ITEM,
                        (stack, ctx) -> new com.hbm_m.item.liquids.FluidBarrelItem.FluidBarrelCapabilityHandler(stack), item);
                continue;
            }
            // Loetlampe (Forge: FillOnlyCapability, nur Befuellen)
            if (item instanceof com.hbm_m.item.tool.ItemBlowtorch blowtorch) {
                event.registerItem(Capabilities.FluidHandler.ITEM,
                        (stack, ctx) -> new com.hbm_m.item.tool.ItemBlowtorch.FillOnlyCapability(stack, blowtorch), item);
                continue;
            }
            // Pipette (Forge: PipetteCapability)
            if (item instanceof com.hbm_m.item.tool.ItemPipette) {
                event.registerItem(Capabilities.FluidHandler.ITEM,
                        (stack, ctx) -> new com.hbm_m.item.tool.ItemPipette.PipetteCapability(stack), item);
                continue;
            }

            // Energie-Items: Forge initCapabilities -> EnergyCapabilityProvider(stack, capacity, maxReceive, maxExtract)
            if (item instanceof com.hbm_m.item.fekal_electric.ModBatteryItem battery) {
                registerEnergyItem(event, item, stack -> new com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage(
                        stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()));
            } else if (item instanceof com.hbm_m.item.tool.ItemSwordAbilityPower sword) {
                registerEnergyItem(event, item, stack -> new com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage(
                        stack, sword.maxPower, sword.chargeRate, 0));
            } else if (item instanceof com.hbm_m.item.tool.ItemToolAbilityPower tool) {
                registerEnergyItem(event, item, stack -> new com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage(
                        stack, tool.maxPower, tool.chargeRate, 0));
            } else if (item instanceof com.hbm_m.powerarmor.ModArmorFSBPowered armor) {
                // Kapazitaet haengt von den Ruestungsmods ab; NeoForge fragt Item-Caps bei jedem Zugriff neu ab,
                // daher entfaellt das Forge-invalidateCaps aus ArmorModificationHelper.
                registerEnergyItem(event, item, stack -> {
                    long modifiedCapacity = armor.getMaxCharge(stack);
                    return new com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage(
                            stack, modifiedCapacity, armor.chargeRate, modifiedCapacity);
                });
            }
        }
    }

    /^*
     * 1:1 Forge {@code EnergyCapabilityProvider.getCapability}: HBM-Provider nur wenn entnehmbar, HBM-Receiver nur wenn
     * aufladbar, FE immer (untere Bits).
     ^/
    private static void registerEnergyItem(RegisterCapabilitiesEvent event, net.minecraft.world.item.Item item,
            java.util.function.Function<net.minecraft.world.item.ItemStack, com.hbm_m.api.energy.EnergyCapabilityProvider.ItemEnergyStorage> storage) {
        event.registerItem(HBM_ITEM_ENERGY_PROVIDER, (stack, ctx) -> {
            var s = storage.apply(stack);
            return s.canExtract() ? s : null;
        }, item);
        event.registerItem(HBM_ITEM_ENERGY_RECEIVER, (stack, ctx) -> {
            var s = storage.apply(stack);
            return s.canReceive() ? s : null;
        }, item);
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) ->
                new com.hbm_m.api.energy.LongEnergyWrapper(storage.apply(stack), com.hbm_m.api.energy.LongEnergyWrapper.BitMode.LOW), item);
    }

    public static boolean hasEnergyComponent(BlockEntity be) {
        // wie Forge be.getCapability(..) ohne Seite
        return com.hbm_m.platform.HbmCaps.get(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_CONNECTOR, null).isPresent()
            || com.hbm_m.platform.HbmCaps.get(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_PROVIDER, null).isPresent()
            || com.hbm_m.platform.HbmCaps.get(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_RECEIVER, null).isPresent();
    }
}
*///?}