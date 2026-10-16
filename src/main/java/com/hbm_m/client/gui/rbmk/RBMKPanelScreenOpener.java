package com.hbm_m.client.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKGaugeBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKGraphBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKIndicatorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKKeyPadBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKNumitronBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKTerminalBlockEntity;
import com.hbm_m.inventory.gui.rbmk.GUIRBMKGauge;
import com.hbm_m.inventory.gui.rbmk.GUIRBMKGraph;
import com.hbm_m.inventory.gui.rbmk.GUIRBMKIndicator;
import com.hbm_m.inventory.gui.rbmk.GUIRBMKKeyPad;
import com.hbm_m.inventory.gui.rbmk.GUIRBMKLever;
import com.hbm_m.inventory.gui.rbmk.GUIRBMKNumitron;
import com.hbm_m.inventory.gui.rbmk.GUIRBMKTerminal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client-only screen-opening dispatcher for the 7 RTTY panel devices, called from
 * {@code RBMKPanelDeviceBlock#use}. Mirrors {@code RadioTorchScreenOpener}'s pattern - kept as a
 * separate dispatcher (string key -&gt; screen) rather than storing method references in the
 * (common-code) block registration, so {@code ModBlocks} never touches a client-only class.
 */
public final class RBMKPanelScreenOpener {

    private RBMKPanelScreenOpener() {}

    public static void open(String screenId, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        switch (screenId) {
            case "gauge" -> {
                if (mc.level.getBlockEntity(pos) instanceof RBMKGaugeBlockEntity be) mc.setScreen(new GUIRBMKGauge(pos, be));
            }
            case "indicator" -> {
                if (mc.level.getBlockEntity(pos) instanceof RBMKIndicatorBlockEntity be) mc.setScreen(new GUIRBMKIndicator(pos, be));
            }
            case "numitron" -> {
                if (mc.level.getBlockEntity(pos) instanceof RBMKNumitronBlockEntity be) mc.setScreen(new GUIRBMKNumitron(pos, be));
            }
            case "graph" -> {
                if (mc.level.getBlockEntity(pos) instanceof RBMKGraphBlockEntity be) mc.setScreen(new GUIRBMKGraph(pos, be));
            }
            case "lever" -> {
                if (mc.level.getBlockEntity(pos) instanceof RBMKLeverBlockEntity be) mc.setScreen(new GUIRBMKLever(pos, be));
            }
            case "keypad" -> {
                if (mc.level.getBlockEntity(pos) instanceof RBMKKeyPadBlockEntity be) mc.setScreen(new GUIRBMKKeyPad(pos, be));
            }
            case "terminal" -> {
                if (mc.level.getBlockEntity(pos) instanceof RBMKTerminalBlockEntity be) mc.setScreen(new GUIRBMKTerminal(pos, be));
            }
            default -> {}
        }
    }
}
