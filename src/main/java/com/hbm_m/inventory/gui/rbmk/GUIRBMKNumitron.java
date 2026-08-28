package com.hbm_m.inventory.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKNumitronBlockEntity;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Port of {@code GUIScreenRBMKNumitron} - two numitron tubes, each with an RTTY channel, a label,
 * an on/off toggle and the original {@code DisplayUnit}'s three formatting options: which of the
 * seven digits are lit, whether large values are SI-shortened, and whether the number is padded
 * with zeroes or spaces.
 */
public class GUIRBMKNumitron extends Screen {

    private static final int UNITS  = RBMKNumitronBlockEntity.UNITS;
    private static final int DIGITS = 7;

    private final BlockPos pos;
    private final RBMKNumitronBlockEntity be;

    private final EditBox[] channelBoxes  = new EditBox[UNITS];
    private final EditBox[] labelBoxes    = new EditBox[UNITS];
    private final Button[]  activeButtons = new Button[UNITS];
    private final Button[]  zeroButtons   = new Button[UNITS];
    private final Button[]  shortButtons  = new Button[UNITS];
    private final Button[][] digitButtons = new Button[UNITS][DIGITS];

    private final boolean[] unitActive    = new boolean[UNITS];
    /** CE's per-unit {@code polling} flag - re-read the channel every tick and zero on silence. */
    private final net.minecraft.client.gui.components.Checkbox[] pollingBoxes = new net.minecraft.client.gui.components.Checkbox[UNITS];
    private final boolean[] leadingZeroes = new boolean[UNITS];
    private final boolean[] shortenNumber = new boolean[UNITS];
    private final long[]    activeDigits  = new long[UNITS];

    public GUIRBMKNumitron(BlockPos pos, RBMKNumitronBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_numitron"));
        this.pos = pos;
        this.be = be;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 70;

        for (int i = 0; i < UNITS; i++) {
            final int u = i;
            unitActive[i]    = be.isUnitActive(i);
            leadingZeroes[i] = be.leadingZeroes[i];
            shortenNumber[i] = be.shortenNumber[i];
            activeDigits[i]  = be.activeDigits[i];

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

            zeroButtons[i] = Button.builder(zeroLabel(leadingZeroes[i]), b -> {
                leadingZeroes[u] = !leadingZeroes[u];
                zeroButtons[u].setMessage(zeroLabel(leadingZeroes[u]));
            }).bounds(cx - 100, y, 62, 16).build();
            addRenderableWidget(zeroButtons[i]);

            shortButtons[i] = Button.builder(shortLabel(shortenNumber[i]), b -> {
                shortenNumber[u] = !shortenNumber[u];
                shortButtons[u].setMessage(shortLabel(shortenNumber[u]));
            }).bounds(cx - 34, y, 62, 16).build();
            addRenderableWidget(shortButtons[i]);
            y += 19;

            // One toggle per tube digit; bit 0x40 is the leftmost, the same order the renderer walks.
            for (int d = 0; d < DIGITS; d++) {
                final int bit = d;
                digitButtons[i][d] = Button.builder(digitLabel(isDigitOn(u, bit)), b -> {
                    activeDigits[u] ^= (0x40L >> bit);
                    digitButtons[u][bit].setMessage(digitLabel(isDigitOn(u, bit)));
                }).bounds(cx - 100 + d * 20, y, 18, 16).build();
                addRenderableWidget(digitButtons[i][d]);
            }
            pollingBoxes[i] = new net.minecraft.client.gui.components.Checkbox(cx - 100, y, 90, 16, Component.literal("poll"), be.polling[i]);
            addRenderableWidget(pollingBoxes[i]);
            y += 26;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.save"), b -> save())
                .bounds(cx - 40, y, 80, 20).build());
    }

    private boolean isDigitOn(int unit, int digit) {
        return (activeDigits[unit] & (0x40L >> digit)) != 0;
    }

    private static Component digitLabel(boolean on) { return Component.literal(on ? "8" : "-"); }
    private static Component zeroLabel(boolean on)  { return Component.literal(on ? "0-pad" : "space"); }
    private static Component shortLabel(boolean on) { return Component.literal(on ? "SI" : "raw"); }

    private void save() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < UNITS; i++) {
            data.putString("channel" + i, channelBoxes[i].getValue());
            data.putString("ulabel" + i, labelBoxes[i].getValue());
            data.putBoolean("active" + i, unitActive[i]);
            data.putBoolean("zeroes" + i, leadingZeroes[i]);
            data.putBoolean("short" + i, shortenNumber[i]);
            data.putLong("digits" + i, activeDigits[i]);
            data.putBoolean("polling" + i, pollingBoxes[i].selected());
        }
        RadioTorchControlPacket.sendToServer(pos, data);
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 85, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
