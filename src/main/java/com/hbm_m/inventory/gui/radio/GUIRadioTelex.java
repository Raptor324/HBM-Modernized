package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.RadioTelexBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Port of {@code GUIScreenRadioTelex} (1.7.10 Original). Plain vanilla-widget screen (no container). */
public class GUIRadioTelex extends Screen {

    private static final int LINE_COUNT = 5;

    private final BlockPos pos;
    private final RadioTelexBlockEntity blockEntity;

    private EditBox txChannelBox;
    private EditBox rxChannelBox;
    private final EditBox[] txLineBoxes = new EditBox[LINE_COUNT];

    public GUIRadioTelex(BlockPos pos, RadioTelexBlockEntity blockEntity) {
        super(Component.translatable("container.hbm_m.radio_telex"));
        this.pos = pos;
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 110;

        txChannelBox = new EditBox(this.font, cx - 100, y, 90, 16, Component.literal("TX Channel"));
        txChannelBox.setMaxLength(15);
        txChannelBox.setValue(blockEntity.txChannel);
        addRenderableWidget(txChannelBox);

        rxChannelBox = new EditBox(this.font, cx + 10, y, 90, 16, Component.literal("RX Channel"));
        rxChannelBox.setMaxLength(15);
        rxChannelBox.setValue(blockEntity.rxChannel);
        addRenderableWidget(rxChannelBox);
        y += 22;

        for (int i = 0; i < LINE_COUNT; i++) {
            EditBox box = new EditBox(this.font, cx - 100, y, 200, 16, Component.literal("TX Line " + i));
            box.setMaxLength(33);
            box.setValue(blockEntity.txLines[i] != null ? blockEntity.txLines[i] : "");
            txLineBoxes[i] = box;
            addRenderableWidget(box);
            y += 18;
        }
        y += 6;

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 100, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Send"), b -> { save(); send(); })
                .bounds(cx - 34, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Print"), b -> print())
                .bounds(cx + 32, y, 60, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Clear RX"), b -> clearRx())
                .bounds(cx - 50, y, 100, 20).build());
    }

    private CompoundTag baseData() {
        CompoundTag data = new CompoundTag();
        data.putString("txChannel", txChannelBox.getValue());
        data.putString("rxChannel", rxChannelBox.getValue());
        for (int i = 0; i < LINE_COUNT; i++) data.putString("tx" + i, txLineBoxes[i].getValue());
        return data;
    }

    private void save() {
        RadioTorchControlPacket.sendToServer(pos, baseData());
    }

    private void send() {
        CompoundTag data = baseData();
        data.putString("cmd", "snd");
        RadioTorchControlPacket.sendToServer(pos, data);
    }

    private void print() {
        CompoundTag data = new CompoundTag();
        data.putString("cmd", "rxprt");
        RadioTorchControlPacket.sendToServer(pos, data);
    }

    private void clearRx() {
        CompoundTag data = new CompoundTag();
        data.putString("cmd", "rxcls");
        RadioTorchControlPacket.sendToServer(pos, data);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int y = this.height / 2 - 130;
        guiGraphics.drawCenteredString(this.font, this.title, cx, y, 0xFFFFFF);

        y = this.height / 2 - 60;
        for (int i = 0; i < LINE_COUNT; i++) {
            String line = blockEntity.rxLines[i] != null ? blockEntity.rxLines[i] : "";
            guiGraphics.drawString(this.font, "RX " + i + ": " + line, cx - 100, y, 0x55FF55, false);
            y += 12;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
