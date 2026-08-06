package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchBaseBlockEntity;
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
 * Port of {@code GUIScreenRadioTorch} (1.7.10 Original, shared by Sender/Receiver/Logic's base
 * fields). Plain vanilla-widget config screen (no container) - channel string, polling toggle,
 * custom-mapping toggle plus 16 mapping text fields when custom-mapping is enabled.
 * <p>
 * SCOPE-Vereinfachung: Vanille-Widgets statt eigener texturierter Oberflaeche - funktional
 * vollstaendig aequivalent, spart die aufwendige Custom-Textur-Layout-Arbeit fuer eine reine
 * Text-Konfigurationsmaske.
 */
public class GUIRadioTorchSimple extends Screen {

    public static final int MAX_CHANNEL_LENGTH = 15;

    private final BlockPos pos;
    private final RadioTorchBaseBlockEntity blockEntity;

    private EditBox channelBox;
    private Checkbox pollingBox;
    private Checkbox customMapBox;
    private final EditBox[] mappingBoxes = new EditBox[16];

    public GUIRadioTorchSimple(BlockPos pos, RadioTorchBaseBlockEntity blockEntity, Component title) {
        super(title);
        this.pos = pos;
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 90;

        channelBox = new EditBox(this.font, cx - 75, y, 150, 18, Component.literal("Channel"));
        channelBox.setMaxLength(MAX_CHANNEL_LENGTH);
        channelBox.setValue(blockEntity.channel);
        addRenderableWidget(channelBox);
        y += 24;

        pollingBox = new Checkbox(cx - 75, y, 150, 18, Component.literal("Polling (always re-send/re-read)"), blockEntity.polling);
        addRenderableWidget(pollingBox);
        y += 22;

        customMapBox = new Checkbox(cx - 75, y, 150, 18, Component.literal("Custom string mapping"), blockEntity.customMap);
        addRenderableWidget(customMapBox);
        y += 22;

        for (int i = 0; i < 16; i++) {
            int col = i % 4;
            int row = i / 4;
            EditBox box = new EditBox(this.font, cx - 75 + col * 38, y + row * 20, 34, 18, Component.literal("Map " + i));
            box.setMaxLength(32);
            box.setValue(blockEntity.mapping[i] != null ? blockEntity.mapping[i] : "");
            mappingBoxes[i] = box;
            addRenderableWidget(box);
        }
        y += 4 * 20 + 8;

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 40, y, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        data.putString("channel", channelBox.getValue());
        data.putBoolean("polling", pollingBox.selected());
        data.putBoolean("customMap", customMapBox.selected());
        for (int i = 0; i < 16; i++) data.putString("mapping" + i, mappingBoxes[i].getValue());
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
