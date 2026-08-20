package com.hbm_m.inventory.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Port of {@code GUIScreenRBMKLever} - 2 flip-switches, each with an RTTY channel and on/off command strings. */
public class GUIRBMKLever extends Screen {

    private final BlockPos pos;
    private final RBMKLeverBlockEntity be;

    /** Label, tint and on/off toggle - the three fields every original *Unit carries. */
    private final EditBox[] labelBoxes    = new EditBox[RBMKLeverBlockEntity.UNITS];
    private final Button[]  activeButtons = new Button[RBMKLeverBlockEntity.UNITS];
    private final boolean[] unitActive    = new boolean[RBMKLeverBlockEntity.UNITS];
    private final net.minecraft.client.gui.components.Checkbox[] pollingBoxes = new net.minecraft.client.gui.components.Checkbox[RBMKLeverBlockEntity.UNITS];
    private final EditBox[] channelBoxes = new EditBox[RBMKLeverBlockEntity.UNITS];
    private final EditBox[] onBoxes      = new EditBox[RBMKLeverBlockEntity.UNITS];
    private final EditBox[] offBoxes     = new EditBox[RBMKLeverBlockEntity.UNITS];

    public GUIRBMKLever(BlockPos pos, RBMKLeverBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_lever"));
        this.pos = pos;
        this.be = be;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 60;

        for (int i = 0; i < RBMKLeverBlockEntity.UNITS; i++) {
            EditBox channel = new EditBox(this.font, cx - 110, y, 220, 16, Component.literal("channel"));
            channel.setMaxLength(24);
            channel.setValue(be.channel[i] != null ? be.channel[i] : "");
            channelBoxes[i] = channel;
            addRenderableWidget(channel);
            y += 19;

            final int idx = i;
            unitActive[i] = be.isUnitActive(i);
            
            EditBox lbl = new EditBox(this.font, cx - 100, y, 95, 16, Component.literal("label"));
            lbl.setMaxLength(24);
            lbl.setValue(be.getUnitLabel(i));
            labelBoxes[i] = lbl;
            addRenderableWidget(lbl);
            
            activeButtons[i] = Button.builder(GUIRBMKGauge.activeLabel(unitActive[i]), b -> {
                unitActive[idx] = !unitActive[idx];
                activeButtons[idx].setMessage(GUIRBMKGauge.activeLabel(unitActive[idx]));
            }).bounds(cx + 78, y, 22, 16).build();
            addRenderableWidget(activeButtons[i]);
            y += 19;

            EditBox on = new EditBox(this.font, cx - 110, y, 105, 16, Component.literal("on"));
            on.setValue(be.commandOn[i] != null ? be.commandOn[i] : "");
            onBoxes[i] = on;
            addRenderableWidget(on);

            EditBox off = new EditBox(this.font, cx + 5, y, 105, 16, Component.literal("off"));
            off.setValue(be.commandOff[i] != null ? be.commandOff[i] : "");
            offBoxes[i] = off;
            addRenderableWidget(off);
            y += 19;

            pollingBoxes[i] = new net.minecraft.client.gui.components.Checkbox(
                    cx - 110, y, 90, 16, Component.literal("repeat"), be.polling[i]);
            addRenderableWidget(pollingBoxes[i]);
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.save"), b -> save())
                .bounds(cx - 40, y + 4, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < RBMKLeverBlockEntity.UNITS; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("ulabel" + i, labelBoxes[i].getValue());
            data.putBoolean("active" + i, unitActive[i]);
            data.putString("commandOn" + i, onBoxes[i].getValue());
            data.putString("commandOff" + i, offBoxes[i].getValue());
            data.putBoolean("lpolling" + i, pollingBoxes[i].selected());
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 75, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
