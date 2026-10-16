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
 * Port of {@code GUIScreenRBMKGauge} (1.7.10 Original) - 4 gauge units, each configured with an
 * RTTY channel, a label and a min/max range. Vanilla-widget config screen, matching the
 * "SCOPE-Vereinfachung" already used for the radio-torch family's config screens.
 */
public class GUIRBMKGauge extends Screen {

    private final BlockPos pos;
    private final RBMKGaugeBlockEntity be;

    private final EditBox[] channelBoxes = new EditBox[RBMKGaugeBlockEntity.UNITS];
    private final EditBox[] labelBoxes   = new EditBox[RBMKGaugeBlockEntity.UNITS];
    private final EditBox[] minBoxes     = new EditBox[RBMKGaugeBlockEntity.UNITS];
    private final EditBox[] maxBoxes     = new EditBox[RBMKGaugeBlockEntity.UNITS];

    public GUIRBMKGauge(BlockPos pos, RBMKGaugeBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_gauge"));
        this.pos = pos;
        this.be = be;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 90;

        for (int i = 0; i < RBMKGaugeBlockEntity.UNITS; i++) {
            EditBox channel = new EditBox(this.font, cx - 100, y, 95, 16, Component.literal("channel"));
            channel.setMaxLength(24);
            channel.setValue(be.channel[i] != null ? be.channel[i] : "");
            channelBoxes[i] = channel;
            addRenderableWidget(channel);

            EditBox label = new EditBox(this.font, cx + 5, y, 95, 16, Component.literal("label"));
            label.setMaxLength(24);
            label.setValue(be.label[i] != null ? be.label[i] : "");
            labelBoxes[i] = label;
            addRenderableWidget(label);
            y += 18;

            EditBox min = new EditBox(this.font, cx - 100, y, 95, 16, Component.literal("min"));
            min.setValue(String.valueOf(be.min[i]));
            minBoxes[i] = min;
            addRenderableWidget(min);

            EditBox max = new EditBox(this.font, cx + 5, y, 95, 16, Component.literal("max"));
            max.setValue(String.valueOf(be.max[i]));
            maxBoxes[i] = max;
            addRenderableWidget(max);
            y += 22;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.save"), b -> save())
                .bounds(cx - 40, y + 4, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < RBMKGaugeBlockEntity.UNITS; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("label" + i, labelBoxes[i].getValue());
            data.putDouble("min" + i, parse(minBoxes[i].getValue()));
            data.putDouble("max" + i, parse(maxBoxes[i].getValue()));
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
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 105, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
