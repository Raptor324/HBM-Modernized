package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.RadioAutocalBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Port of {@code GUIScreenRadioAUTOCAL} (1.7.10 Original). Plain vanilla-widget screen (no
 * container) - the original's rich text editor is simplified to a fixed set of single-line
 * {@link EditBox} script-line fields (no multi-line text widget exists in this Minecraft version's
 * vanilla component set), matching the same-line-count convention used by {@code GUIRadioTelex}.
 */
public class GUIRadioAutocal extends Screen {

    private static final int SCRIPT_LINES = 16;

    private final BlockPos pos;
    private final RadioAutocalBlockEntity blockEntity;

    private final EditBox[] lineBoxes = new EditBox[SCRIPT_LINES];
    private Checkbox onBox;
    private Checkbox ignoreBox;
    private Checkbox autoBox;

    public GUIRadioAutocal(BlockPos pos, RadioAutocalBlockEntity blockEntity) {
        super(Component.translatable("container.hbm_m.radio_autocal"));
        this.pos = pos;
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = this.height / 2 - 110;

        for (int i = 0; i < SCRIPT_LINES; i++) {
            EditBox box = new EditBox(this.font, cx - 120, top + i * 13, 240, 12, Component.literal("Line " + i));
            box.setMaxLength(48);
            box.setValue(i < blockEntity.script.size() ? blockEntity.script.get(i) : "");
            lineBoxes[i] = box;
            addRenderableWidget(box);
        }

        int y = top + SCRIPT_LINES * 13 + 6;
        onBox = com.hbm_m.client.GuiCompat.checkbox(cx - 120, y, 70, 18, Component.literal("On"), blockEntity.isOn);
        addRenderableWidget(onBox);
        ignoreBox = com.hbm_m.client.GuiCompat.checkbox(cx - 40, y, 90, 18, Component.literal("Ignore err"), blockEntity.ignoreError);
        addRenderableWidget(ignoreBox);
        autoBox = com.hbm_m.client.GuiCompat.checkbox(cx + 60, y, 90, 18, Component.literal("Auto reboot"), blockEntity.autoReboot);
        addRenderableWidget(autoBox);
        y += 24;

        addRenderableWidget(Button.builder(Component.literal("Save & Run"), b -> save())
                .bounds(cx - 60, y, 120, 20).build());
    }

    private void save() {
        StringBuilder payload = new StringBuilder();
        for (int i = 0; i < SCRIPT_LINES; i++) {
            payload.append(lineBoxes[i].getValue());
            if (i < SCRIPT_LINES - 1) payload.append('\n');
        }

        CompoundTag data = new CompoundTag();
        data.putString("payload", payload.toString());
        data.putBoolean("on", onBox.selected());
        data.putBoolean("ignore", ignoreBox.selected());
        data.putBoolean("auto", autoBox.selected());
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 128, 0xFFFFFF);

        int hx = this.width / 2 + 130;
        int hy = this.height / 2 - 110;
        guiGraphics.drawString(this.font, "History:", hx, hy, 0xAAAAAA, false);
        hy += 12;
        for (String line : blockEntity.history) {
            guiGraphics.drawString(this.font, line, hx, hy, 0x55FF55, false);
            hy += 10;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
