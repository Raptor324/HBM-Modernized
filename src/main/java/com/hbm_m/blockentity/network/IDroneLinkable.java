package com.hbm_m.blockentity.network;

import net.minecraft.core.BlockPos;

/**
 * Port von {@code com.hbm.tileentity.network.IDroneLinkable} (1.7.10 Original). Implementiert von
 * {@link MachineDroneCrateBlockEntity} und {@link MachineDroneWaypointBlockEntity} - den beiden
 * Pipeline-A-Bloecken ("manuelle Verlinkung" per {@link com.hbm_m.item.tools_and_armor.ItemDroneLinker}).
 */
public interface IDroneLinkable {
    /** Der Punkt, an dem eine {@link com.hbm_m.entity.drone.EntityDeliveryDrone} andockt (typischerweise 1 Block ueber dem Block). */
    BlockPos getDronePoint();

    void setNextTarget(BlockPos target);
}
