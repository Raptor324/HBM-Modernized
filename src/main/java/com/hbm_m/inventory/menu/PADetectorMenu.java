package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.albion.PADetectorBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/**
 * Menue des Bauteils "Detektor". Slotkoordinaten 1:1 aus {@code ContainerPADetector} (1.7.10).
 */
public class PADetectorMenu extends PAMenu {

    public PADetectorMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (PADetectorBlockEntity) readBlockEntity(inv, extraData));
    }

    public PADetectorMenu(int id, Inventory inv, PADetectorBlockEntity blockEntity) {
        super(ModMenuTypes.PA_DETECTOR_MENU.get(), id, inv, blockEntity, null,
                ModBlocks.PA_DETECTOR.get(),
                SlotSpec.in(PADetectorBlockEntity.SLOT_BATTERY, 8, 72),
                SlotSpec.out(PADetectorBlockEntity.SLOT_OUTPUT_A, 62, 45),
                SlotSpec.out(PADetectorBlockEntity.SLOT_OUTPUT_B, 80, 45));
    }
}
