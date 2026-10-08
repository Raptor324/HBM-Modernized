package com.hbm_m.client.gui.structure;

import com.hbm_m.blockentity.generic.WandJigsawBlockEntity;
import com.hbm_m.blockentity.generic.WandTandemBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Clientseitiges Oeffnen der Jigsaw-/Tandem-GUIs (Original {@code provideGUI}). */
public final class WandScreenOpener {

    private WandScreenOpener() {}

    public static void openJigsaw(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof WandJigsawBlockEntity jigsaw) {
            mc.setScreen(new GuiWandJigsaw(jigsaw));
        }
    }

    public static void openTandem(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof WandTandemBlockEntity tandem) {
            mc.setScreen(new GuiWandTandem(tandem));
        }
    }
}
