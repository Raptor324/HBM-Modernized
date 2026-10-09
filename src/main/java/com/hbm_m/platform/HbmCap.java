//? if neoforge {
/*package com.hbm_m.platform;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;

import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

/^*
 * [NeoForge 1.21.1] Schluessel der Capability-Bruecke, Gegenstueck zu Forges {@code Capability<T>}.
 * Namen wie in den Forge-Zweigen ({@code ForgeCapabilities.ITEM_HANDLER}, {@code ModCapabilities.HBM_ENERGY_*}),
 * damit die gespiegelten {@code getHbmCapability}-Rumpfe zeilengleich bleiben. {@link #block} ist die
 * NeoForge-Block-Capability (bzw. {@link #item} fuer ItemStacks), unter der {@link com.hbm_m.capability.ModCapabilities#register} den Wert anmeldet.
 ^/
public final class HbmCap<T> {

    public static final HbmCap<net.neoforged.neoforge.items.IItemHandler> ITEM_HANDLER =
            new HbmCap<>("item_handler", Capabilities.ItemHandler.BLOCK, Capabilities.ItemHandler.ITEM);
    public static final HbmCap<net.neoforged.neoforge.fluids.capability.IFluidHandler> FLUID_HANDLER =
            new HbmCap<>("fluid_handler", Capabilities.FluidHandler.BLOCK, null);
    public static final HbmCap<net.neoforged.neoforge.fluids.capability.IFluidHandlerItem> FLUID_HANDLER_ITEM =
            new HbmCap<>("fluid_handler_item", null, Capabilities.FluidHandler.ITEM);
    public static final HbmCap<net.neoforged.neoforge.energy.IEnergyStorage> ENERGY =
            new HbmCap<>("energy", Capabilities.EnergyStorage.BLOCK, Capabilities.EnergyStorage.ITEM);
    public static final HbmCap<IEnergyProvider> HBM_ENERGY_PROVIDER =
            new HbmCap<>("hbm_energy_provider", ModCapabilities.HBM_ENERGY_PROVIDER, ModCapabilities.HBM_ITEM_ENERGY_PROVIDER);
    public static final HbmCap<IEnergyReceiver> HBM_ENERGY_RECEIVER =
            new HbmCap<>("hbm_energy_receiver", ModCapabilities.HBM_ENERGY_RECEIVER, ModCapabilities.HBM_ITEM_ENERGY_RECEIVER);
    public static final HbmCap<IEnergyConnector> HBM_ENERGY_CONNECTOR =
            new HbmCap<>("hbm_energy_connector", ModCapabilities.HBM_ENERGY_CONNECTOR, null);

    private final String name;
    /^* NeoForge-Block-Capability (null = nur Item). ^/
    public final @org.jetbrains.annotations.Nullable BlockCapability<T, Direction> block;
    /^* NeoForge-Item-Capability (null = nur Block). ^/
    public final @org.jetbrains.annotations.Nullable ItemCapability<T, Void> item;

    private HbmCap(String name, @org.jetbrains.annotations.Nullable BlockCapability<T, Direction> block,
                   @org.jetbrains.annotations.Nullable ItemCapability<T, Void> item) {
        this.name = name;
        this.block = block;
        this.item = item;
    }

    /^* Forge {@code Capability.orEmpty(other, opt)}. ^/
    public <R> LazyCap<R> orEmpty(HbmCap<R> other, LazyCap<T> opt) {
        return this == other ? opt.cast() : LazyCap.empty();
    }

    @Override
    public String toString() {
        return "HbmCap[" + name + "]";
    }
}
*///?}
