package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchControllerBlockEntity;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Port of {@code GUIScreenRadioTorchController} (1.7.10 Original): channel + polling only. */
public class GUIRadioTorchController extends Screen {

    private final BlockPos pos;
    private final RadioTorchControllerBlockEntity blockEntity;

    private EditBox channelBox;
    private Checkbox pollingBox;

    public GUIRadioTorchController(BlockPos pos, RadioTorchControllerBlockEntity blockEntity) {
        super(Component.literal("Radio Controller"));
        this.pos = pos;
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        channelBox = new EditBox(this.font, cx - 75, y, 150, 18, Component.literal("Channel"));
        channelBox.setMaxLength(15);
        channelBox.setValue(blockEntity.channel);
        addRenderableWidget(channelBox);
        y += 24;

        pollingBox = new Checkbox(cx - 75, y, 150, 18, Component.literal("Polling"), blockEntity.polling);
        addRenderableWidget(pollingBox);
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 40, y, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        data.putString("channel", channelBox.getValue());
        data.putBoolean("polling", pollingBox.selected());
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
