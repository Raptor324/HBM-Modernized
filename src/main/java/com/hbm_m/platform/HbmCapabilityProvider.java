package com.hbm_m.platform;

/**
 * Capability-Bruecke Forge -> NeoForge. Auf 1.20.1 (Forge) nur ein Marker ohne Methoden, die BEs antworten dort
 * weiter ueber {@code getCapability}. Auf 1.21.1 (NeoForge) spiegelt {@code getHbmCapability} den Forge-Zweig
 * {@code getCapability} 1:1 (seitenabhaengig, inkl. Delegation von Proxy-/Dummy-Zellen an den Kern);
 * {@link com.hbm_m.capability.ModCapabilities#register} meldet das fuer alle eigenen BlockEntity-Typen an.
 */
public interface HbmCapabilityProvider {
    //? if neoforge {
    /*/^* Gegenstueck zu Forge {@code getCapability(cap, side)}; Standard wie bisher auf NeoForge (siehe {@link HbmCaps#defaults}). ^/
    default <T> LazyCap<T> getHbmCapability(HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        return HbmCaps.defaults((net.minecraft.world.level.block.entity.BlockEntity) this, cap, side);
    }

    /^* Gegenstueck zu Forge {@code invalidateCaps()}; wird beim Entfernen des BlockEntity aufgerufen. ^/
    default void invalidateHbmCaps() {
    }
    *///?}
}
