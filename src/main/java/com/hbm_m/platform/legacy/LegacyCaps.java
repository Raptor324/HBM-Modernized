package com.hbm_m.platform.legacy;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.legacy.energy.IEnergyStorage;
import com.hbm_m.platform.legacy.fluids.capability.IFluidHandler;
import com.hbm_m.platform.legacy.fluids.capability.IFluidHandlerItem;
import com.hbm_m.platform.legacy.items.IItemHandler;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * [NeoForge 26.x] Ersatz für {@code Capabilities.ItemHandler/FluidHandler/EnergyStorage}.
 *
 * <p>NeoForge 26 hat die alten Handler-Capabilities zugunsten der Transfer API
 * ({@code ResourceHandler}, {@code EnergyHandler}) entfernt. Der Mod spricht intern weiter die
 * alten Schnittstellen aus {@code com.hbm_m.platform.legacy}; Stonecutter biegt für 26.x
 * {@code Capabilities.ItemHandler.BLOCK} usw. auf die Konstanten hier um
 * (siehe {@code mc26_legacy_caps} in stonecutter.gradle.kts).
 *
 * <p>TODO(26.x): Brücke zu {@code Capabilities.Item/Fluid/Energy} für Fremd-Mods
 * (Hopper, Rohre anderer Mods). Ohne sie sehen nur HBM-Blöcke einander.
 */
public final class LegacyCaps {

    private LegacyCaps() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(RefStrings.MODID, "legacy_" + path);
    }

    public static final BlockCapability<IItemHandler, Direction> ITEM_HANDLER_BLOCK =
            BlockCapability.createSided(id("item_handler"), IItemHandler.class);
    public static final ItemCapability<IItemHandler, Void> ITEM_HANDLER_ITEM =
            ItemCapability.createVoid(id("item_handler"), IItemHandler.class);
    public static final EntityCapability<IItemHandler, Void> ITEM_HANDLER_ENTITY =
            EntityCapability.createVoid(id("item_handler"), IItemHandler.class);

    public static final BlockCapability<IFluidHandler, Direction> FLUID_HANDLER_BLOCK =
            BlockCapability.createSided(id("fluid_handler"), IFluidHandler.class);
    public static final ItemCapability<IFluidHandlerItem, Void> FLUID_HANDLER_ITEM =
            ItemCapability.createVoid(id("fluid_handler"), IFluidHandlerItem.class);

    public static final BlockCapability<IEnergyStorage, Direction> ENERGY_BLOCK =
            BlockCapability.createSided(id("energy"), IEnergyStorage.class);
    public static final ItemCapability<IEnergyStorage, Void> ENERGY_ITEM =
            ItemCapability.createVoid(id("energy"), IEnergyStorage.class);
}
