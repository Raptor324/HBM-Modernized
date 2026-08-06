package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchReaderBlockEntity;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Port of {@code GUIScreenRadioTorchReader} (1.7.10 Original). 8 rows of Channel + Value-name. */
public class GUIRadioTorchReader extends Screen {

    private final BlockPos pos;
    private final RadioTorchReaderBlockEntity blockEntity;

    private Checkbox pollingBox;
    private final EditBox[] channelBoxes = new EditBox[8];
    private final EditBox[] nameBoxes = new EditBox[8];

    public GUIRadioTorchReader(BlockPos pos, RadioTorchReaderBlockEntity blockEntity) {
        super(Component.literal("Radio Reader"));
        this.pos = pos;
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 90;

        pollingBox = new Checkbox(cx - 75, y, 150, 18, Component.literal("Polling"), blockEntity.polling);
        addRenderableWidget(pollingBox);
        y += 24;

        for (int i = 0; i < 8; i++) {
            EditBox channelBox = new EditBox(this.font, cx - 100, y, 90, 18, Component.literal("Channel " + i));
            channelBox.setMaxLength(15);
            channelBox.setValue(blockEntity.channels[i]);
            channelBoxes[i] = channelBox;
            addRenderableWidget(channelBox);

            EditBox nameBox = new EditBox(this.font, cx - 4, y, 100, 18, Component.literal("Value name " + i));
            nameBox.setMaxLength(32);
            nameBox.setValue(blockEntity.names[i]);
            nameBoxes[i] = nameBox;
            addRenderableWidget(nameBox);

            y += 20;
        }
        y += 8;

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 40, y, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        data.putBoolean("polling", pollingBox.selected());
        for (int i = 0; i < 8; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("name" + i, nameBoxes[i].getValue());
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 110, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
