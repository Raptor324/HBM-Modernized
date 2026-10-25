package com.hbm_m.client.gui.structure;

import com.hbm_m.blockentity.generic.WandStructureBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Clientseitiges Oeffnen der Struktur-GUIs (Original {@code BlockWandStructure.provideGUI}). */
public final class StructureScreenOpener {

    private StructureScreenOpener() {}

    public static void open(BlockPos pos, boolean load) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof WandStructureBlockEntity structure) {
            mc.setScreen(load ? new GuiStructureLoad(structure) : new GuiStructureSave(structure));
        }
    }
}
