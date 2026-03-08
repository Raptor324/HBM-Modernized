package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ItemDesignatorPacket;
import com.hbm_m.network.ModPacketHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * GUI for manual designator: adjust target X/Z with buttons.
 * Port from 1.7.10 GUIScreenDesignator. No container/menu.
 */
public class DesignatorScreen extends Screen {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/gui_designator.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 178;

    private final Player player;
    private int leftPos;
    private int topPos;
    private int shownX;
    private int shownZ;
    private final List<FolderButton> buttons = new ArrayList<>();

    public DesignatorScreen(Player player) {
        super(Component.translatable("gui.hbm_m.designator"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - WIDTH) / 2;
        topPos = (height - HEIGHT) / 2;
        shownX = 0;
        shownZ = 0;
        ItemStack stack = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!stack.isEmpty() && stack.is(ModItems.DESIGNATOR_MANUAL.get()) && stack.hasTag()) {
            var tag = stack.getTag();
            if (tag != null && tag.contains("xCoord")) {
                shownX = tag.getInt("xCoord");
                shownZ = tag.getInt("zCoord");
            }
        }
        updateButtons();
    }

    private void updateButtons() {
        buttons.clear();
        int gL = leftPos;
        int gT = topPos;
        // X row: add 1,5,10,50,100
        buttons.add(new FolderButton(gL + 25, gT + 26, 0, 0, 0, 1, null));
        buttons.add(new FolderButton(gL + 52, gT + 26, 1, 0, 0, 5, null));
        buttons.add(new FolderButton(gL + 79, gT + 26, 2, 0, 0, 10, null));
        buttons.add(new FolderButton(gL + 106, gT + 26, 3, 0, 0, 50, null));
        buttons.add(new FolderButton(gL + 133, gT + 26, 4, 0, 0, 100, null));
        // X row: subtract 1,5,10,50,100
        buttons.add(new FolderButton(gL + 25, gT + 62, 5, 1, 0, 1, null));
        buttons.add(new FolderButton(gL + 52, gT + 62, 6, 1, 0, 5, null));
        buttons.add(new FolderButton(gL + 79, gT + 62, 7, 1, 0, 10, null));
        buttons.add(new FolderButton(gL + 106, gT + 62, 8, 1, 0, 50, null));
        buttons.add(new FolderButton(gL + 133, gT + 62, 9, 1, 0, 100, null));
        buttons.add(new FolderButton(gL + 133, gT + 44, 10, 2, 0, 0, "gui.hbm_m.designator.set_x"));
        // Z row: add
        buttons.add(new FolderButton(gL + 25, gT + 98, 0, 0, 1, 1, null));
        buttons.add(new FolderButton(gL + 52, gT + 98, 1, 0, 1, 5, null));
        buttons.add(new FolderButton(gL + 79, gT + 98, 2, 0, 1, 10, null));
        buttons.add(new FolderButton(gL + 106, gT + 98, 3, 0, 1, 50, null));
        buttons.add(new FolderButton(gL + 133, gT + 98, 4, 0, 1, 100, null));
        // Z row: subtract
        buttons.add(new FolderButton(gL + 25, gT + 134, 5, 1, 1, 1, null));
        buttons.add(new FolderButton(gL + 52, gT + 134, 6, 1, 1, 5, null));
        buttons.add(new FolderButton(gL + 79, gT + 134, 7, 1, 1, 10, null));
        buttons.add(new FolderButton(gL + 106, gT + 134, 8, 1, 1, 50, null));
        buttons.add(new FolderButton(gL + 133, gT + 134, 9, 1, 1, 100, null));
        buttons.add(new FolderButton(gL + 133, gT + 116, 10, 2, 1, 0, "gui.hbm_m.designator.set_z"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        for (FolderButton b : buttons) {
            b.drawButton(guiGraphics, b.isMouseOnButton(mouseX, mouseY));
        }
        String xStr = "X: " + shownX;
        String zStr = "Z: " + shownZ;
        guiGraphics.drawString(font, xStr, leftPos + WIDTH / 2 - font.width(xStr) / 2, topPos + 50, 0x404040, false);
        guiGraphics.drawString(font, zStr, leftPos + WIDTH / 2 - font.width(zStr) / 2, topPos + 50 + 72, 0x404040, false);
        for (FolderButton b : buttons) {
            if (b.isMouseOnButton(mouseX, mouseY)) {
                b.drawTooltip(guiGraphics, mouseX, mouseY);
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (FolderButton b : buttons) {
            if (b.isMouseOnButton((int) mouseX, (int) mouseY)) {
                b.executeAction();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        ItemStack stack = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (stack.isEmpty() || !stack.is(ModItems.DESIGNATOR_MANUAL.get())) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class FolderButton {
        final int xPos;
        final int yPos;
        final int type;
        final int operator;
        final int value;
        final int reference;
        final String tooltipKey;

        FolderButton(int x, int y, int t, int o, int r, int v, String tooltipKey) {
            xPos = x;
            yPos = y;
            type = t;
            operator = o;
            value = v;
            reference = r;
            this.tooltipKey = tooltipKey;
        }

        boolean isMouseOnButton(int mouseX, int mouseY) {
            return xPos <= mouseX && xPos + 18 > mouseX && yPos <= mouseY && yPos + 18 > mouseY;
        }

        void drawButton(GuiGraphics guiGraphics, boolean hovered) {
            guiGraphics.blit(TEXTURE, xPos, yPos, hovered ? 176 + 18 : 176, type * 18, 18, 18);
        }

        void drawTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (tooltipKey != null && !tooltipKey.isEmpty()) {
                guiGraphics.renderTooltip(font, Component.translatable(tooltipKey), mouseX, mouseY);
            }
        }

        void executeAction() {
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            }
            ModPacketHandler.INSTANCE.sendToServer(new ItemDesignatorPacket(operator, value, reference));
            int result = 0;
            if (operator == 0) result += value;
            if (operator == 1) result -= value;
            if (operator == 2) {
                if (reference == 0) shownX = (int) Math.round(player.getX());
                else shownZ = (int) Math.round(player.getZ());
                return;
            }
            if (reference == 0) shownX += result;
            else shownZ += result;
        }
    }
}
