package com.hbm_m.inventory.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKKeyPadBlockEntity;
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

/** Port of {@code GUIScreenRBMKKeyPad} - 4 buttons, each with an RTTY channel, a command string and a polling toggle. */
public class GUIRBMKKeyPad extends Screen {

    private final BlockPos pos;
    private final RBMKKeyPadBlockEntity be;

    private final EditBox[]  channelBoxes = new EditBox[RBMKKeyPadBlockEntity.UNITS];
    private final EditBox[]  commandBoxes = new EditBox[RBMKKeyPadBlockEntity.UNITS];
    private final Checkbox[] pollingBoxes = new Checkbox[RBMKKeyPadBlockEntity.UNITS];

    public GUIRBMKKeyPad(BlockPos pos, RBMKKeyPadBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_keypad"));
        this.pos = pos;
        this.be = be;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 80;

        for (int i = 0; i < RBMKKeyPadBlockEntity.UNITS; i++) {
            EditBox channel = new EditBox(this.font, cx - 110, y, 100, 16, Component.literal("channel"));
            channel.setMaxLength(24);
            channel.setValue(be.channel[i] != null ? be.channel[i] : "");
            channelBoxes[i] = channel;
            addRenderableWidget(channel);

            EditBox command = new EditBox(this.font, cx - 5, y, 60, 16, Component.literal("command"));
            command.setValue(be.command[i] != null ? be.command[i] : "");
            commandBoxes[i] = command;
            addRenderableWidget(command);

            Checkbox polling = com.hbm_m.client.GuiCompat.checkbox(cx + 60, y, 55, 16, Component.literal("hold"), be.polling[i]);
            pollingBoxes[i] = polling;
            addRenderableWidget(polling);

            y += 20;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.save"), b -> save())
                .bounds(cx - 40, y + 4, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < RBMKKeyPadBlockEntity.UNITS; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("command" + i, commandBoxes[i].getValue());
            data.putBoolean("polling" + i, pollingBoxes[i].selected());
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 95, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
