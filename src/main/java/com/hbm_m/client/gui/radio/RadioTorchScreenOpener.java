package com.hbm_m.client.gui.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchBaseBlockEntity;
import com.hbm_m.inventory.gui.radio.GUIRadioTorchLogic;
import com.hbm_m.inventory.gui.radio.GUIRadioTorchReader;
import com.hbm_m.inventory.gui.radio.GUIRadioTorchSimple;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Client-only screen-opening hooks for the radio-torch family, called from block {@code use()} methods. */
public final class RadioTorchScreenOpener {

    private RadioTorchScreenOpener() {}

    public static void openSenderReceiver(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof RadioTorchBaseBlockEntity be) {
            mc.setScreen(new GUIRadioTorchSimple(pos, be, Component.literal("Radio Torch")));
        }
    }

    public static void openLogic(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.network.radio.RadioTorchLogicBlockEntity be) {
            mc.setScreen(new GUIRadioTorchLogic(pos, be));
        }
    }

    public static void openReader(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.network.radio.RadioTorchReaderBlockEntity be) {
            mc.setScreen(new GUIRadioTorchReader(pos, be));
        }
    }

    public static void openController(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.network.radio.RadioTorchControllerBlockEntity be) {
            mc.setScreen(new com.hbm_m.inventory.gui.radio.GUIRadioTorchController(pos, be));
        }
    }

    public static void openRadioRec(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.machines.RadioRecBlockEntity be) {
            mc.setScreen(new com.hbm_m.inventory.gui.radio.GUIRadioRec(pos, be));
        }
    }

    public static void openRadioTelex(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.network.RadioTelexBlockEntity be) {
            mc.setScreen(new com.hbm_m.inventory.gui.radio.GUIRadioTelex(pos, be));
        }
    }

    public static void openRadioAutocal(BlockPos pos) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.network.RadioAutocalBlockEntity be) {
            mc.setScreen(new com.hbm_m.inventory.gui.radio.GUIRadioAutocal(pos, be));
        }
    }
}
