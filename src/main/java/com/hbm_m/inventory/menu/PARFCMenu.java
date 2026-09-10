package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.albion.PARFCBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/**
 * Menue des Bauteils "Beschleunigerzelle". Slotkoordinaten 1:1 aus {@code ContainerPARFC} (1.7.10).
 */
public class PARFCMenu extends PAMenu {

    public PARFCMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (PARFCBlockEntity) readBlockEntity(inv, extraData));
    }

    public PARFCMenu(int id, Inventory inv, PARFCBlockEntity blockEntity) {
        super(ModMenuTypes.PA_RFC_MENU.get(), id, inv, blockEntity, null,
                ModBlocks.RFC.get(),
                SlotSpec.in(PARFCBlockEntity.SLOT_BATTERY, 53, 72));
    }
}
