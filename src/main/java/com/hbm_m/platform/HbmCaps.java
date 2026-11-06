//? if neoforge {
/*package com.hbm_m.platform;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

/^*
 * [NeoForge 1.21.1] Helfer der Capability-Bruecke (siehe {@link HbmCapabilityProvider}, {@link HbmCap}, {@link LazyCap}).
 ^/
public final class HbmCaps {

    private HbmCaps() {}

    /^*
     * Gegenstueck zu Forge {@code be.getCapability(cap, side)} auf einem beliebigen BlockEntity (z.B. Kern einer
     * Mehrblockstruktur): eigene BEs direkt ueber die Bruecke, fremde ueber die NeoForge-Capability.
     ^/
    public static <T> LazyCap<T> get(@Nullable BlockEntity be, HbmCap<T> cap, @Nullable Direction side) {
        if (be == null) return LazyCap.empty();
        if (be instanceof HbmCapabilityProvider p) return p.getHbmCapability(cap, side);
        Level level = be.getLevel();
        if (level == null || cap.block == null) return LazyCap.empty();
        T v = level.getCapability(cap.block, be.getBlockPos(), be.getBlockState(), be, side);
        return LazyCap.ofObj(v);
    }

    /^* Gegenstueck zu Forge {@code stack.getCapability(cap)} (Item-Capabilities, angemeldet in ModCapabilities). ^/
    public static <T> LazyCap<T> item(@Nullable net.minecraft.world.item.ItemStack stack, HbmCap<T> cap) {
        if (stack == null || stack.isEmpty() || cap.item == null) return LazyCap.empty();
        return LazyCap.ofObj(stack.getCapability(cap.item));
    }

    /^* Forge {@code stack.getCapability(cap, side)} - Items kennen auf NeoForge keine Seite. ^/
    public static <T> LazyCap<T> item(@Nullable net.minecraft.world.item.ItemStack stack, HbmCap<T> cap, @Nullable Direction side) {
        return item(stack, cap);
    }

    /^* Fuer die Anmeldung in {@code RegisterCapabilitiesEvent}: aufgeloester Wert oder null (nie rekursiv ueber die Welt).
     * Ohne LazyCap-Wrap/Unwrap — {@code defaults} wird ueber denselben Raw-Resolver bedient. ^/
    public static <T> @Nullable T query(BlockEntity be, HbmCap<T> cap, @Nullable Direction side) {
        if (be instanceof HbmCapabilityProvider p) return p.getHbmCapability(cap, side).orElse(null);
        return resolveDefault(be, cap, side);
    }

    /^*
     * Standard ohne eigenen Forge-Zweig - wie die bisherige NeoForge-Anmeldung: HBM-Energie ueber die
     * implementierten Schnittstellen (Connector nur auf anschliessbaren Seiten), Items/Fluessigkeit/FE ueber die
     * Hilfsmethoden von {@link com.hbm_m.blockentity.BaseHbmBlockEntity}.
     ^/
    public static <T> LazyCap<T> defaults(BlockEntity be, HbmCap<T> cap, @Nullable Direction side) {
        T v = resolveDefault(be, cap, side);
        return v != null ? LazyCap.ofObj(v) : LazyCap.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable T resolveDefault(BlockEntity be, HbmCap<T> cap, @Nullable Direction side) {
        if (cap == HbmCap.HBM_ENERGY_PROVIDER) return be instanceof IEnergyProvider p ? (T) p : null;
        if (cap == HbmCap.HBM_ENERGY_RECEIVER) return be instanceof IEnergyReceiver r ? (T) r : null;
        if (cap == HbmCap.HBM_ENERGY_CONNECTOR) {
            return be instanceof IEnergyConnector c && c.canConnectEnergy(side) ? (T) c : null;
        }
        if (be instanceof com.hbm_m.blockentity.BaseHbmBlockEntity hbm) {
            if (cap == HbmCap.ITEM_HANDLER) return (T) hbm.getItemHandler(side);
            if (cap == HbmCap.FLUID_HANDLER) return (T) hbm.getFluidHandler(side);
            if (cap == HbmCap.ENERGY) {
                return (T) hbm.getEnergyStorage(side);
            }
        }
        return null;
    }
}
*///?}
