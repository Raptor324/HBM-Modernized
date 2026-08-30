package com.hbm_m.inventory.gui.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKTerminalBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Port of {@code GUIScreenRBMKTerminal} - a free-text RTTY command console. Commands are
 * evaluated server-side ({@link RBMKTerminalBlockEntity#eval}); this screen only echoes what
 * was typed locally since there's no S2C reply channel for this port (see the block entity's
 * class doc for the accepted scope trim).
 */
public class GUIRBMKTerminal extends Screen {

    private static final int MAX_LOG_LINES = 10;

    private final BlockPos pos;
    private final RBMKTerminalBlockEntity be;
    private final Deque<String> log = new ArrayDeque<>();

    private EditBox inputBox;

    public GUIRBMKTerminal(BlockPos pos, RBMKTerminalBlockEntity be) {
        super(Component.translatable("gui.hbm_m.rbmk_terminal"));
        this.pos = pos;
        this.be = be;
        log.addFirst("chan: " + be.channel + (be.running ? " (sending: " + be.runningValue + ")" : ""));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 40;

        inputBox = new EditBox(this.font, cx - 100, y, 160, 18, Component.literal("command"));
        inputBox.setMaxLength(64);
        addRenderableWidget(inputBox);
        setInitialFocus(inputBox);

        addRenderableWidget(Button.builder(Component.literal(">"), b -> submit())
                .bounds(cx + 62, y, 20, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.hbm_m.close"), b -> onClose())
                .bounds(cx - 40, y + 26, 80, 20).build());
    }

    private void submit() {
        String line = inputBox.getValue();
        if (line.isEmpty()) return;

        CompoundTag data = new CompoundTag();
        data.putString("cmd", line);
        RadioTorchControlPacket.sendToServer(pos, data);

        // Local echo only; the authoritative scrollback comes back from the block entity, which
        // is also what the in-world panel renderer draws.
        log.addFirst("> " + line);
        while (log.size() > MAX_LOG_LINES) log.removeLast();
        inputBox.setValue("");
    }

    /**
     * The block entity's own scrollback, newest first, once the server has echoed it back. Until
     * the first sync arrives this is empty and the local echo above carries the display.
     */
    private java.util.List<String> serverLog() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String line : be.history) {
            if (line != null && !line.isEmpty()) lines.add("> " + line);
            if (lines.size() >= MAX_LOG_LINES) break;
        }
        return lines;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, this.height / 2 - 60, 0xFFFFFF);

        int lineY = this.height / 2 - 40;
        java.util.List<String> synced = serverLog();
        Iterable<String> lines = synced.isEmpty() ? log : synced;
        // Amber while the terminal is repeating a broadcast, green otherwise - same signal the
        // in-world renderer uses.
        int color = be.doesRepeat ? 0xFFB060 : 0xAAFFAA;
        for (String line : lines) {
            g.drawString(this.font, line, cx - 100, lineY, color);
            lineY += 10;
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
