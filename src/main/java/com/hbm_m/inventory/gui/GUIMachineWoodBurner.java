package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineWoodBurnerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ToggleWoodBurnerPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Порт {@code GUIMachineWoodBurner} (1.7.10 Original), текстура
 * {@code gui_wood_burner_alt.png} 176×186: энергоколонна (143,18) 16×34,
 * шкала горения (17,70) 4×52, кнопка ON (53,17) 16×15, в жидком режиме —
 * оверлеи (16,17)←(176,52) 60×54 и (79,17)←(176,106) 36×54 + бак (80,18) 16×52.
 *
 * <p>Клики: (53,17) — вкл/выкл ({@code receiveControl("toggle")}),
 * (46,37) 30×14 — смена режима твёрдое/жидкое ({@code receiveControl("switch")}),
 * оба с {@code gui.button.press}. Тултипы: энергия, бонусы топлива
 * ({@code burnModule.getDesc()} по пустому слоту топлива), бак (жидкий режим),
 * секунды горения (твёрдый режим), статус ON/OFF.
 */
public class GUIMachineWoodBurner extends GuiInfoScreen<MachineWoodBurnerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_wood_burner_alt.png");

    private final MachineWoodBurnerBlockEntity burner;

    public GUIMachineWoodBurner(MachineWoodBurnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.burner = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (burner == null) return;

        if (burner.isLiquidBurn()) {
            guiGraphics.blit(TEXTURE, x + 16, y + 17, 176, 52, 60, 54);
            guiGraphics.blit(TEXTURE, x + 79, y + 17, 176, 106, 36, 54);
        }

        if (burner.isOn()) {
            guiGraphics.blit(TEXTURE, x + 53, y + 17, 196, 0, 16, 15);
        }

        long maxPower = MachineWoodBurnerBlockEntity.MAX_POWER;
        int p = (int) (burner.getEnergyStored() * 34 / maxPower);
        guiGraphics.blit(TEXTURE, x + 143, y + 52 - p, 176, 52 - p, 16, p);

        if (burner.maxBurnTime > 0 && !burner.isLiquidBurn()) {
            int b = burner.burnTime * 52 / burner.maxBurnTime;
            guiGraphics.blit(TEXTURE, x + 17, y + 70 - b, 192, 52 - b, 4, b);
        }

        if (burner.isLiquidBurn()) {
            // Оригинал: tank.renderTank(guiLeft + 80, guiTop + 70, zLevel, 16, 52) — в порте
            // renderTank принимает верхний левый угол: y = 70 - 52 = 18.
            burner.getTank().renderTank(guiGraphics, x + 80, y + 18, 16, 52);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Оригинал: имя по центру x=70, y=6, белый.
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (burner == null) return;

        // Оригинал: drawElectricityInfo(143, 18, 16, 34, power, maxPower).
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 143, 18, 16, 34,
                burner.getEnergyStored(), MachineWoodBurnerBlockEntity.MAX_POWER);

        // Бонусы топлива — по пустому слоту топлива при пустом курсоре (оригинал burnModule.getDesc()).
        if (this.menu.getCarried().isEmpty()) {
            var fuelSlot = this.menu.slots.get(MachineWoodBurnerMenu.SLOT_FUEL);
            if (isMouseOver(mouseX, mouseY, 26, 18, 16, 16) && !fuelSlot.hasItem()) {
                List<Component> bonuses = burner.getBurnModule().getDesc();
                if (!bonuses.isEmpty()) {
                    guiGraphics.renderComponentTooltip(this.font, bonuses, mouseX, mouseY);
                }
            }
        }

        if (burner.isLiquidBurn()) {
            // Оригинал: tank.renderTankInfo(80, 18, 16, 52).
            burner.getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 80, this.topPos + 18, 16, 52);
        } else if (isMouseOver(mouseX, mouseY, 16, 17, 8, 54)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal((burner.burnTime / 20) + "s"), mouseX, mouseY);
        }

        if (isMouseOver(mouseX, mouseY, 53, 17, 16, 15)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(burner.isOn() ? "ON" : "OFF")
                            .withStyle(burner.isOn() ? ChatFormatting.GREEN : ChatFormatting.RED),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Оригинал: (53,17) 16×15 → NBTControlPacket "toggle".
        if (isMouseOver(mouseX, mouseY, 53, 17, 16, 15)) {
            if (burner != null) {
                playButtonPress();
                ToggleWoodBurnerPacket.sendToServer(burner.getBlockPos(), false);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Оригинал: (46,37) 30×14 → NBTControlPacket "switch".
        if (isMouseOver(mouseX, mouseY, 46, 37, 30, 14)) {
            if (burner != null) {
                playButtonPress();
                ToggleWoodBurnerPacket.sendToServer(burner.getBlockPos(), true);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playButtonPress() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + sizeX
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + sizeY;
    }
}
