package com.hbm_m.client.gui;

import com.hbm_m.api.energy.CableDiodeBlockEntity;
import com.hbm_m.inventory.gui.GUIDiode;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client-Einstieg fuer {@code CableDiode.onBlockActivated} (GUI ohne Container). */
public final class DiodeScreenOpener {

    private DiodeScreenOpener() {}

    public static void open(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof CableDiodeBlockEntity be) mc.setScreen(new GUIDiode(be));
    }
}
