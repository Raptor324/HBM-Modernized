package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchLogicBlockEntity;
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
 * Port of {@code GUIScreenRadioTorchLogic} (1.7.10 Original). 16 rule rows, each a comparison-value
 * text field plus a cycle-button for the operator (0-9: &lt; &lt;= &gt;= &gt; == != equals !equals contains !contains).
 */
public class GUIRadioTorchLogic extends Screen {

    private static final String[] OPS = {"<", "<=", ">=", ">", "==", "!=", "equals", "!equals", "contains", "!contains"};

    private final BlockPos pos;
    private final RadioTorchLogicBlockEntity blockEntity;

    private EditBox channelBox;
    private Checkbox pollingBox;
    private Checkbox descendingBox;
    private final EditBox[] valueBoxes = new EditBox[16];
    private final Button[] opButtons = new Button[16];
    private final int[] conditions = new int[16];

    public GUIRadioTorchLogic(BlockPos pos, RadioTorchLogicBlockEntity blockEntity) {
        super(Component.literal("Radio Logic"));
        this.pos = pos;
        this.blockEntity = blockEntity;
        System.arraycopy(blockEntity.conditions, 0, conditions, 0, 16);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 110;

        channelBox = new EditBox(this.font, cx - 75, y, 150, 18, Component.literal("Channel"));
        channelBox.setMaxLength(15);
        channelBox.setValue(blockEntity.channel);
        addRenderableWidget(channelBox);
        y += 22;

        pollingBox = com.hbm_m.client.GuiCompat.checkbox(cx - 100, y, 90, 18, Component.literal("Polling"), blockEntity.polling);
        addRenderableWidget(pollingBox);
        descendingBox = com.hbm_m.client.GuiCompat.checkbox(cx + 10, y, 100, 18, Component.literal("Descending"), blockEntity.descending);
        addRenderableWidget(descendingBox);
        y += 24;

        for (int i = 0; i < 16; i++) {
            final int idx = i;
            int row = i / 2;
            int col = i % 2;
            int rowX = cx - 155 + col * 160;
            int rowY = y + row * 20;

            EditBox box = new EditBox(this.font, rowX, rowY, 90, 18, Component.literal("Value " + i));
            box.setMaxLength(32);
            box.setValue(blockEntity.mapping[i]);
            valueBoxes[i] = box;
            addRenderableWidget(box);

            Button opButton = Button.builder(Component.literal(OPS[conditions[i]]), b -> cycleOp(idx))
                    .bounds(rowX + 94, rowY, 60, 18).build();
            opButtons[i] = opButton;
            addRenderableWidget(opButton);
        }
        y += 8 * 20 + 8;

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 40, y, 80, 20).build());
    }

    private void cycleOp(int idx) {
        conditions[idx] = (conditions[idx] + 1) % OPS.length;
        opButtons[idx].setMessage(Component.literal(OPS[conditions[idx]]));
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        data.putString("channel", channelBox.getValue());
        data.putBoolean("polling", pollingBox.selected());
        data.putBoolean("descending", descendingBox.selected());
        for (int i = 0; i < 16; i++) {
            data.putString("mapping" + i, valueBoxes[i].getValue());
            data.putInt("cond" + i, conditions[i]);
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 128, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
