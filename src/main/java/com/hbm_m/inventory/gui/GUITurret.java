package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.TurretMenu;
import com.hbm_m.network.TurretControlPacket;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Generischer GUI-Screen fuer alle Turret-Varianten - Textur kommt aus {@link com.hbm_m.blockentity.machines.TurretStats}. */
public class GUITurret extends AbstractContainerScreen<TurretMenu> {

    private final ResourceLocation texture;

    /** Toggle-Buttons: On/Off + 4 Ziel-Kategorien (Spieler/Tiere/Mobs/Maschinen), siehe {@link TurretControlPacket}. */
    private static final int BTN_SIZE = 11;
    private static final int BTN_X = 8;
    private static final int BTN_Y_START = 18;
    private static final String[] BTN_LABELS = {"On", "P", "A", "M", "Mc"};
    private static final int[] BTN_ACTIONS = {
            TurretControlPacket.ACTION_TOGGLE_ON,
            TurretControlPacket.ACTION_TOGGLE_PLAYERS,
            TurretControlPacket.ACTION_TOGGLE_ANIMALS,
            TurretControlPacket.ACTION_TOGGLE_MOBS,
            TurretControlPacket.ACTION_TOGGLE_MACHINES
    };

    public GUITurret(TurretMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.texture = pMenu.blockEntity.getGuiTexture();
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, texture);
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int energyHeight = menu.getEnergyScaled(34);
        if (energyHeight > 0) {
            gui.blit(texture, x + 152, y + 65 + 34 - energyHeight, 176, 34 - energyHeight, 16, energyHeight);
        }
    }

    private static final String[] ARTY_MODE_NAMES = {"Artillery", "Cannon", "Manual"};
    private static final int MODE_BTN_Y = BTN_Y_START + 5 * (BTN_SIZE + 2) + 4;

    private boolean isArty() {
        return menu.blockEntity.getStats() == com.hbm_m.blockentity.machines.TurretStats.ARTY;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);
        renderToggleButtons(gui, mouseX, mouseY);
        if (isArty()) renderModeButton(gui);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    private void renderModeButton(GuiGraphics gui) {
        int x = this.leftPos + BTN_X;
        int y = this.topPos + MODE_BTN_Y;
        gui.fill(x, y, x + BTN_SIZE * 3, y + BTN_SIZE, 0xFF000000);
        gui.fill(x + 1, y + 1, x + BTN_SIZE * 3 - 1, y + BTN_SIZE - 1, 0xFF4A6FA5);
        gui.drawCenteredString(this.font, ARTY_MODE_NAMES[menu.blockEntity.getFireMode()],
                x + (BTN_SIZE * 3) / 2, y + 2, 0xFFFFFFFF);
    }

    private boolean isButtonActive(int index) {
        return switch (index) {
            case 0 -> menu.blockEntity.isOn();
            case 1 -> menu.blockEntity.isTargetingPlayers();
            case 2 -> menu.blockEntity.isTargetingAnimals();
            case 3 -> menu.blockEntity.isTargetingMobs();
            case 4 -> menu.blockEntity.isTargetingMachines();
            default -> false;
        };
    }

    private void renderToggleButtons(GuiGraphics gui, int mouseX, int mouseY) {
        for (int i = 0; i < BTN_LABELS.length; i++) {
            int x = this.leftPos + BTN_X;
            int y = this.topPos + BTN_Y_START + i * (BTN_SIZE + 2);
            boolean active = isButtonActive(i);
            int color = active ? 0xFF3CB043 : 0xFF7A7A7A;
            gui.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, 0xFF000000);
            gui.fill(x + 1, y + 1, x + BTN_SIZE - 1, y + BTN_SIZE - 1, color);
            gui.drawCenteredString(this.font, BTN_LABELS[i], x + BTN_SIZE / 2, y + 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < BTN_LABELS.length; i++) {
            int x = BTN_X;
            int y = BTN_Y_START + i * (BTN_SIZE + 2);
            if (isMouseOver(mouseX, mouseY, x, y, BTN_SIZE, BTN_SIZE)) {
                TurretControlPacket.sendToServer(menu.blockEntity.getBlockPos(), BTN_ACTIONS[i]);
                return true;
            }
        }
        if (isArty() && isMouseOver(mouseX, mouseY, BTN_X, MODE_BTN_Y, BTN_SIZE * 3, BTN_SIZE)) {
            TurretControlPacket.sendToServer(menu.blockEntity.getBlockPos(), TurretControlPacket.ACTION_CYCLE_FIRE_MODE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static final String[] BTN_TOOLTIP_NAMES = {"On/Off", "Players", "Animals", "Mobs", "Machines"};

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);

        if (isMouseOver(mouseX, mouseY, 152, 99, 16, 16)) {
            List<Component> tooltip = new ArrayList<>();
            String energyStr = EnergyFormatter.format(menu.getEnergyLong());
            String maxEnergyStr = EnergyFormatter.format(menu.getMaxEnergyLong());
            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE").withStyle(ChatFormatting.GREEN));
            gui.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
            return;
        }

        for (int i = 0; i < BTN_LABELS.length; i++) {
            if (isMouseOver(mouseX, mouseY, BTN_X, BTN_Y_START + i * (BTN_SIZE + 2), BTN_SIZE, BTN_SIZE)) {
                boolean active = isButtonActive(i);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(BTN_TOOLTIP_NAMES[i] + ": " + (active ? "On" : "Off"))
                        .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
                gui.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX
                && mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY;
    }
}
