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

    /^* Fuer die Anmeldung in {@code RegisterCapabilitiesEvent}: aufgeloester Wert oder null (nie rekursiv ueber die Welt). ^/
    public static <T> @Nullable T query(BlockEntity be, HbmCap<T> cap, @Nullable Direction side) {
        LazyCap<T> c = be instanceof HbmCapabilityProvider p ? p.getHbmCapability(cap, side) : defaults(be, cap, side);
        return c.orElse(null);
    }

    /^*
     * Standard ohne eigenen Forge-Zweig - wie die bisherige NeoForge-Anmeldung: HBM-Energie ueber die
     * implementierten Schnittstellen (Connector nur auf anschliessbaren Seiten), Items/Fluessigkeit/FE ueber die
     * Hilfsmethoden von {@link com.hbm_m.blockentity.BaseHbmBlockEntity}.
     ^/
    public static <T> LazyCap<T> defaults(BlockEntity be, HbmCap<T> cap, @Nullable Direction side) {
        if (cap == HbmCap.HBM_ENERGY_PROVIDER) return be instanceof IEnergyProvider p ? LazyCap.ofObj(p) : LazyCap.empty();
        if (cap == HbmCap.HBM_ENERGY_RECEIVER) return be instanceof IEnergyReceiver r ? LazyCap.ofObj(r) : LazyCap.empty();
        if (cap == HbmCap.HBM_ENERGY_CONNECTOR) {
            return be instanceof IEnergyConnector c && c.canConnectEnergy(side) ? LazyCap.ofObj(c) : LazyCap.empty();
        }
        if (be instanceof com.hbm_m.blockentity.BaseHbmBlockEntity hbm) {
            if (cap == HbmCap.ITEM_HANDLER) return LazyCap.ofObj(hbm.getItemHandler(side));
            if (cap == HbmCap.FLUID_HANDLER) return LazyCap.ofObj(hbm.getFluidHandler(side));
            if (cap == HbmCap.ENERGY) {
                if (be instanceof com.hbm_m.api.energy.ConverterBlockEntity conv) return LazyCap.ofObj(new com.hbm_m.api.energy.HbmForgeWrapper(conv));
                return LazyCap.ofObj(hbm.getEnergyStorage(side));
            }
        }
        return LazyCap.empty();
    }
}
*///?}
