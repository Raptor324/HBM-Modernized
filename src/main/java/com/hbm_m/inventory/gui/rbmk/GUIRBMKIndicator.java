package com.hbm_m.inventory.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKIndicatorBlockEntity;
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

/** Port of {@code GUIScreenRBMKIndicator} - 6 lamp units, each with an RTTY channel, min/max trigger range and invert toggle. */
public class GUIRBMKIndicator extends Screen {

    private final BlockPos pos;
    private final RBMKIndicatorBlockEntity be;

    /** Label, tint and on/off toggle - the three fields every original *Unit carries. */
    private final EditBox[] labelBoxes    = new EditBox[RBMKIndicatorBlockEntity.UNITS];
    private final EditBox[] colorBoxes    = new EditBox[RBMKIndicatorBlockEntity.UNITS];
    private final Button[]  activeButtons = new Button[RBMKIndicatorBlockEntity.UNITS];
    private final boolean[] unitActive    = new boolean[RBMKIndicatorBlockEntity.UNITS];
    /** CE's per-unit {@code polling} flag - re-read the channel every tick and zero on silence. */
    private final net.minecraft.client.gui.components.Checkbox[] pollingBoxes = new net.minecraft.client.gui.components.Checkbox[RBMKIndicatorBlockEntity.UNITS];
    private final EditBox[] channelBoxes = new EditBox[RBMKIndicatorBlockEntity.UNITS];
    private final EditBox[] minBoxes     = new EditBox[RBMKIndicatorBlockEntity.UNITS];
    private final EditBox[] maxBoxes     = new EditBox[RBMKIndicatorBlockEntity.UNITS];
    private final Checkbox[] invertBoxes = new Checkbox[RBMKIndicatorBlockEntity.UNITS];

    public GUIRBMKIndicator(BlockPos pos, RBMKIndicatorBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_indicator"));
        this.pos = pos;
        this.be = be;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 100;

        for (int i = 0; i < RBMKIndicatorBlockEntity.UNITS; i++) {
            EditBox channel = new EditBox(this.font, cx - 110, y, 90, 16, Component.literal("channel"));
            channel.setMaxLength(24);
            channel.setValue(be.channel[i] != null ? be.channel[i] : "");
            channelBoxes[i] = channel;
            addRenderableWidget(channel);

            EditBox min = new EditBox(this.font, cx - 10, y, 45, 16, Component.literal("min"));
            min.setValue(String.valueOf(be.min[i]));
            minBoxes[i] = min;
            addRenderableWidget(min);

            EditBox max = new EditBox(this.font, cx + 40, y, 45, 16, Component.literal("max"));
            max.setValue(String.valueOf(be.max[i]));
            maxBoxes[i] = max;
            addRenderableWidget(max);

            Checkbox invert = com.hbm_m.client.GuiCompat.checkbox(cx + 92, y, 60, 16, Component.literal("inv"), be.invert[i]);
            invertBoxes[i] = invert;
            addRenderableWidget(invert);

            y += 19;

            final int idx = i;
            unitActive[i] = be.isUnitActive(i);
            
            EditBox lbl = new EditBox(this.font, cx - 100, y, 95, 16, Component.literal("label"));
            lbl.setMaxLength(24);
            lbl.setValue(be.getUnitLabel(i));
            labelBoxes[i] = lbl;
            addRenderableWidget(lbl);
            
            EditBox col = new EditBox(this.font, cx + 5, y, 45, 16, Component.literal("color"));
            col.setMaxLength(7);
            col.setValue(String.format("%06X", be.getUnitColor(i) & 0xFFFFFF));
            colorBoxes[i] = col;
            addRenderableWidget(col);
            
            activeButtons[i] = Button.builder(GUIRBMKGauge.activeLabel(unitActive[i]), b -> {
                unitActive[idx] = !unitActive[idx];
                activeButtons[idx].setMessage(GUIRBMKGauge.activeLabel(unitActive[idx]));
            }).bounds(cx + 78, y, 22, 16).build();
            addRenderableWidget(activeButtons[i]);
            pollingBoxes[i] = new net.minecraft.client.gui.components.Checkbox(cx - 100, y, 90, 16, Component.literal("poll"), be.polling[i]);
            addRenderableWidget(pollingBoxes[i]);
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.save"), b -> save())
                .bounds(cx - 40, y + 4, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < RBMKIndicatorBlockEntity.UNITS; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("ulabel" + i, labelBoxes[i].getValue());
            data.putBoolean("active" + i, unitActive[i]);
            data.putInt("ucolor" + i, GUIRBMKGauge.parseColor(colorBoxes[i].getValue()));
            data.putDouble("min" + i, parse(minBoxes[i].getValue()));
            data.putDouble("max" + i, parse(maxBoxes[i].getValue()));
            data.putBoolean("invert" + i, invertBoxes[i].selected());
            data.putBoolean("polling" + i, pollingBoxes[i].selected());
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    private static double parse(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 115, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
