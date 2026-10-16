package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.albion.PASourceBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/**
 * Menue des Bauteils "Teilchenquelle". Slotkoordinaten 1:1 aus {@code ContainerPASource} (1.7.10).
 */
public class PASourceMenu extends PAMenu {

    public PASourceMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (PASourceBlockEntity) readBlockEntity(inv, extraData));
    }

    public PASourceMenu(int id, Inventory inv, PASourceBlockEntity blockEntity) {
        super(ModMenuTypes.PA_SOURCE_MENU.get(), id, inv, blockEntity, blockEntity.getContainerData(),
                ModBlocks.PA_SOURCE.get(),
                SlotSpec.in(PASourceBlockEntity.SLOT_BATTERY, 8, 72),
                SlotSpec.in(PASourceBlockEntity.SLOT_INPUT_A, 62, 16),
                SlotSpec.in(PASourceBlockEntity.SLOT_INPUT_B, 80, 16));
    }

    /** Zustand des Strahls als Index in {@code PAState.values()}. */
    public int getStateOrdinal() { return data().get(0); }
    /** Zuletzt gemessener Impuls. */
    public int getLastSpeed()    { return data().get(1); }
    /** Aktuelle Streuung des Strahls. */
    public int getDefocus()      { return data().get(2); }
}
