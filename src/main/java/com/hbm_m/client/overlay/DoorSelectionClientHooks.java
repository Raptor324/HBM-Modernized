package com.hbm_m.client.overlay;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import net.minecraft.client.Minecraft;

public final class DoorSelectionClientHooks {

    private DoorSelectionClientHooks() {}

    public static void openSelectionMenu(DoorBlockEntity doorEntity) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new DoorModelSelectionScreen(
                doorEntity.getBlockPos(),
                doorEntity.getDoorDeclId(),
                doorEntity.getModelSelection()
        ));
    }
}
