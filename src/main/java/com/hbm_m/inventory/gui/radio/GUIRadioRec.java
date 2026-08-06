package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.machines.RadioRecBlockEntity;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Port of {@code GUIRadioRec} (1.7.10 Original). Plain vanilla-widget config screen (no container). */
public class GUIRadioRec extends Screen {

    public static final int MAX_CHANNEL_LENGTH = 15;

    private final BlockPos pos;
    private final RadioRecBlockEntity blockEntity;

    private EditBox channelBox;
    private Checkbox onBox;

    public GUIRadioRec(BlockPos pos, RadioRecBlockEntity blockEntity) {
        super(Component.translatable("container.hbm_m.radiorec"));
        this.pos = pos;
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 30;

        channelBox = new EditBox(this.font, cx - 75, y, 150, 18, Component.literal("Channel"));
        channelBox.setMaxLength(MAX_CHANNEL_LENGTH);
        channelBox.setValue(blockEntity.channel);
        addRenderableWidget(channelBox);
        y += 24;

        onBox = new Checkbox(cx - 75, y, 150, 18, Component.literal("On"), blockEntity.isOn);
        addRenderableWidget(onBox);
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 40, y, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        data.putString("channel", channelBox.getValue());
        data.putBoolean("isOn", onBox.selected());
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
