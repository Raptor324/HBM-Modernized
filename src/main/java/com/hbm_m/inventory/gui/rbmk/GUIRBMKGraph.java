package com.hbm_m.inventory.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKGraphBlockEntity;
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
 * Port of {@code GUIScreenRBMKGraph} - two rolling charts, each with an RTTY channel, a label, an
 * on/off toggle and the original {@code GraphUnit}'s optional axis bounds. A bound left switched
 * off lets that end of the axis auto-scale to the data, exactly like {@code minBound}/
 * {@code maxBound} in the original.
 */
public class GUIRBMKGraph extends Screen {

    private static final int UNITS = RBMKGraphBlockEntity.UNITS;

    private final BlockPos pos;
    private final RBMKGraphBlockEntity be;

    private final EditBox[] channelBoxes  = new EditBox[UNITS];
    private final EditBox[] labelBoxes    = new EditBox[UNITS];
    private final EditBox[] minBoxes      = new EditBox[UNITS];
    private final EditBox[] maxBoxes      = new EditBox[UNITS];
    private final Button[]  activeButtons = new Button[UNITS];
    private final Button[]  minButtons    = new Button[UNITS];
    private final Button[]  maxButtons    = new Button[UNITS];

    private final boolean[] unitActive = new boolean[UNITS];
    /** CE's per-unit {@code polling} flag - re-read the channel every tick and zero on silence. */
    private final net.minecraft.client.gui.components.Checkbox[] pollingBoxes = new net.minecraft.client.gui.components.Checkbox[UNITS];
    private final boolean[] minBound   = new boolean[UNITS];
    private final boolean[] maxBound   = new boolean[UNITS];

    public GUIRBMKGraph(BlockPos pos, RBMKGraphBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_graph"));
        this.pos = pos;
        this.be = be;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 70;

        for (int i = 0; i < UNITS; i++) {
            final int u = i;
            unitActive[i] = be.isUnitActive(i);
            minBound[i]   = be.minBound[i];
            maxBound[i]   = be.maxBound[i];

            EditBox channel = new EditBox(this.font, cx - 100, y, 130, 16, Component.literal("channel"));
            channel.setMaxLength(24);
            channel.setValue(be.channel[i] != null ? be.channel[i] : "");
            channelBoxes[i] = channel;
            addRenderableWidget(channel);

            activeButtons[i] = Button.builder(GUIRBMKGauge.activeLabel(unitActive[i]), b -> {
                unitActive[u] = !unitActive[u];
                activeButtons[u].setMessage(GUIRBMKGauge.activeLabel(unitActive[u]));
            }).bounds(cx + 36, y, 30, 16).build();
            addRenderableWidget(activeButtons[i]);
            y += 19;

            EditBox label = new EditBox(this.font, cx - 100, y, 130, 16, Component.literal("label"));
            label.setMaxLength(24);
            label.setValue(be.getUnitLabel(i));
            labelBoxes[i] = label;
            addRenderableWidget(label);
            y += 19;

            EditBox min = new EditBox(this.font, cx - 100, y, 55, 16, Component.literal("min"));
            min.setValue(Long.toString(be.graphMin[i]));
            minBoxes[i] = min;
            addRenderableWidget(min);

            minButtons[i] = Button.builder(boundLabel(minBound[i]), b -> {
                minBound[u] = !minBound[u];
                minButtons[u].setMessage(boundLabel(minBound[u]));
            }).bounds(cx - 42, y, 40, 16).build();
            addRenderableWidget(minButtons[i]);

            EditBox max = new EditBox(this.font, cx + 2, y, 55, 16, Component.literal("max"));
            max.setValue(Long.toString(be.graphMax[i]));
            maxBoxes[i] = max;
            addRenderableWidget(max);

            maxButtons[i] = Button.builder(boundLabel(maxBound[i]), b -> {
                maxBound[u] = !maxBound[u];
                maxButtons[u].setMessage(boundLabel(maxBound[u]));
            }).bounds(cx + 60, y, 40, 16).build();
            addRenderableWidget(maxButtons[i]);
            //? if < 1.21.1 {
            pollingBoxes[i] = new net.minecraft.client.gui.components.Checkbox(cx - 100, y, 90, 16, Component.literal("poll"), be.polling[i]);
            //?} else {
            /*pollingBoxes[i] = net.minecraft.client.gui.components.Checkbox.builder(Component.literal("poll"), this.font).pos(cx - 100, y).selected(be.polling[i]).build();
            *///?}
            addRenderableWidget(pollingBoxes[i]);
            y += 26;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.save"), b -> save())
                .bounds(cx - 40, y, 80, 20).build());
    }

    private static Component boundLabel(boolean fixed) {
        return Component.literal(fixed ? "fixed" : "auto");
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < UNITS; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("ulabel" + i, labelBoxes[i].getValue());
            data.putBoolean("active" + i, unitActive[i]);

            // The block entity treats the presence of gmin/gmax as "bound enabled" and the
            // gminOff/gmaxOff markers as the explicit off switch (see receiveControl there).
            if (minBound[i]) data.putLong("gmin" + i, parse(minBoxes[i].getValue()));
            else             data.putBoolean("gminOff" + i, true);
            if (maxBound[i]) data.putLong("gmax" + i, parse(maxBoxes[i].getValue()));
            else             data.putBoolean("gmaxOff" + i, true);
            data.putBoolean("polling" + i, pollingBoxes[i].selected());
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    private static long parse(String s) {
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 85, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
