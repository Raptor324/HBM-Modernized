package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.albion.PADipoleBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/**
 * Menue des Bauteils "Dipol". Slotkoordinaten 1:1 aus {@code ContainerPADipole} (1.7.10).
 */
public class PADipoleMenu extends PAMenu {

    public PADipoleMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (PADipoleBlockEntity) readBlockEntity(inv, extraData));
    }

    public PADipoleMenu(int id, Inventory inv, PADipoleBlockEntity blockEntity) {
        super(ModMenuTypes.PA_DIPOLE_MENU.get(), id, inv, blockEntity, null,
                ModBlocks.DIPOLE.get(),
                SlotSpec.in(PADipoleBlockEntity.SLOT_BATTERY, 8, 72),
                SlotSpec.in(PADipoleBlockEntity.SLOT_COIL, 89, 26));
    }
}
