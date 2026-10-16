package com.hbm_m.inventory.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKGaugeBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Port of {@code GUIScreenRBMKGauge} (1.7.10 Original) - four gauge units, each configured with an
 * RTTY channel, a label, a needle tint, a min/max range and an on/off toggle. Vanilla-widget
 * config screen, matching the "SCOPE-Vereinfachung" already used for the radio-torch family's
 * config screens.
 */
public class GUIRBMKGauge extends Screen {

    private static final int UNITS = RBMKGaugeBlockEntity.UNITS;

    private final BlockPos pos;
    private final RBMKGaugeBlockEntity be;

    private final EditBox[] channelBoxes  = new EditBox[UNITS];
    private final EditBox[] labelBoxes    = new EditBox[UNITS];
    private final EditBox[] colorBoxes    = new EditBox[UNITS];
    private final EditBox[] minBoxes      = new EditBox[UNITS];
    private final EditBox[] maxBoxes      = new EditBox[UNITS];
    private final Button[]  activeButtons = new Button[UNITS];
    private final boolean[] unitActive    = new boolean[UNITS];
    /** CE's per-unit {@code polling} flag - re-read the channel every tick and zero on silence. */
    private final net.minecraft.client.gui.components.Checkbox[] pollingBoxes = new net.minecraft.client.gui.components.Checkbox[UNITS];

    public GUIRBMKGauge(BlockPos pos, RBMKGaugeBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_gauge"));
        this.pos = pos;
        this.be = be;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 100;

        for (int i = 0; i < UNITS; i++) {
            final int idx = i;
            unitActive[i] = be.isUnitActive(i);

            EditBox channel = new EditBox(this.font, cx - 100, y, 95, 16, Component.literal("channel"));
            channel.setMaxLength(24);
            channel.setValue(be.channel[i] != null ? be.channel[i] : "");
            channelBoxes[i] = channel;
            addRenderableWidget(channel);

            EditBox label = new EditBox(this.font, cx + 5, y, 95, 16, Component.literal("label"));
            label.setMaxLength(24);
            label.setValue(be.getUnitLabel(i));
            labelBoxes[i] = label;
            addRenderableWidget(label);
            y += 18;

            EditBox min = new EditBox(this.font, cx - 100, y, 60, 16, Component.literal("min"));
            min.setValue(String.valueOf(be.min[i]));
            minBoxes[i] = min;
            addRenderableWidget(min);

            EditBox max = new EditBox(this.font, cx - 35, y, 60, 16, Component.literal("max"));
            max.setValue(String.valueOf(be.max[i]));
            maxBoxes[i] = max;
            addRenderableWidget(max);

            EditBox color = new EditBox(this.font, cx + 30, y, 45, 16, Component.literal("color"));
            color.setMaxLength(7);
            color.setValue(String.format("%06X", be.getUnitColor(i) & 0xFFFFFF));
            colorBoxes[i] = color;
            addRenderableWidget(color);

            activeButtons[i] = Button.builder(activeLabel(unitActive[i]), b -> {
                unitActive[idx] = !unitActive[idx];
                activeButtons[idx].setMessage(activeLabel(unitActive[idx]));
            }).bounds(cx + 78, y, 22, 16).build();
            addRenderableWidget(activeButtons[i]);

            //? if < 1.21.1 {
            pollingBoxes[i] = new net.minecraft.client.gui.components.Checkbox(cx - 100, y, 90, 16, Component.literal("poll"), be.polling[i]);
            //?} else {
            /*pollingBoxes[i] = net.minecraft.client.gui.components.Checkbox.builder(Component.literal("poll"), this.font).pos(cx - 100, y).selected(be.polling[i]).build();
            *///?}
            addRenderableWidget(pollingBoxes[i]);
            y += 18;

            y += 22;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.save"), b -> save())
                .bounds(cx - 40, y + 4, 80, 20).build());
    }

    static Component activeLabel(boolean on) {
        return Component.literal(on ? "ON" : "OFF");
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < UNITS; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("ulabel" + i, labelBoxes[i].getValue());
            data.putInt("ucolor" + i, parseColor(colorBoxes[i].getValue()));
            data.putBoolean("active" + i, unitActive[i]);
            data.putDouble("min" + i, parse(minBoxes[i].getValue()));
            data.putDouble("max" + i, parse(maxBoxes[i].getValue()));
            data.putBoolean("polling" + i, pollingBoxes[i].selected());
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    private static double parse(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    /** Accepts "RRGGBB" or "#RRGGBB"; anything else falls back to the default green. */
    static int parseColor(String s) {
        try { return Integer.parseInt(s.trim().replace("#", ""), 16); }
        catch (NumberFormatException e) { return 0x00FF00; }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 115, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
