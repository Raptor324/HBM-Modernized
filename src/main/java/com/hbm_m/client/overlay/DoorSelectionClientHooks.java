package com.hbm_m.client.overlay;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.inventory.gui.GUIDoorModelSelection;

import net.minecraft.client.Minecraft;

public final class DoorSelectionClientHooks {

    private DoorSelectionClientHooks() {}

    public static void openSelectionMenu(DoorBlockEntity doorEntity) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new GUIDoorModelSelection(
                doorEntity.getBlockPos(),
                doorEntity.getDoorDeclId(),
                doorEntity.getModelSelection()
        ));
    }
}
