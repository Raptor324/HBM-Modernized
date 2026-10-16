package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.albion.PAQuadrupoleBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/**
 * Menue des Bauteils "Quadrupol". Slotkoordinaten 1:1 aus {@code ContainerPAQuadrupole} (1.7.10).
 */
public class PAQuadrupoleMenu extends PAMenu {

    public PAQuadrupoleMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (PAQuadrupoleBlockEntity) readBlockEntity(inv, extraData));
    }

    public PAQuadrupoleMenu(int id, Inventory inv, PAQuadrupoleBlockEntity blockEntity) {
        super(ModMenuTypes.PA_QUADRUPOLE_MENU.get(), id, inv, blockEntity, null,
                ModBlocks.QUADRUPOLE.get(),
                SlotSpec.in(PAQuadrupoleBlockEntity.SLOT_BATTERY, 26, 72),
                SlotSpec.in(PAQuadrupoleBlockEntity.SLOT_COIL, 71, 36));
    }
}
