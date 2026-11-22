package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineWoodBurnerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

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
 * статус ON/OFF. Шкала горения рисуется как полный индикатор, пока {@code isBurning()}:
 * точное {@code burnTime}/{@code maxBurnTime} недоступны клиенту (приватные поля BE,
 * меню не синхронизирует ContainerData).
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
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (burner == null) return;

        if (burner.liquidBurn) {
            guiGraphics.blit(TEXTURE, leftPos + 16, topPos + 17, 176, 52, 60, 54);
            guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 17, 176, 106, 36, 54);
        }

        if (burner.isEnabled()) {
            guiGraphics.blit(TEXTURE, leftPos + 53, topPos + 17, 196, 0, 16, 15);
        }

        long maxPower = burner.getMaxEnergyStored();
        int p = maxPower > 0 ? (int) (burner.getEnergyStored() * 34 / maxPower) : 0;
        if (p > 0) guiGraphics.blit(TEXTURE, leftPos + 143, topPos + 52 - p, 176, 52 - p, 16, p);

        // Оригинал: burnTime/maxBurnTime 4x52. Клиенту недоступны (см. класс-комментарий) —
        // рисуем полный индикатор, пока топливо горит.
        if (burner.isBurning() && !burner.liquidBurn) {
            guiGraphics.blit(TEXTURE, leftPos + 17, topPos + 18, 192, 0, 4, 52);
        }

        if (burner.liquidBurn) {
            burner.tank.renderTank(guiGraphics, leftPos + 80, topPos + 18, 16, 52);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (burner == null) return;

        drawElectricityInfo(guiGraphics, mouseX, mouseY, 143, 18, 16, 34,
                burner.getEnergyStored(), burner.getMaxEnergyStored());

        // Бонусы топлива — по пустому слоту топлива при пустом курсоре (оригинал burnModule.getDesc()).
        if (this.menu.getCarried().isEmpty()) {
            var fuelSlot = this.menu.slots.get(MachineWoodBurnerMenu.SLOT_FUEL);
            if (isMouseOver(mouseX, mouseY, 26, 18, 16, 16) && !fuelSlot.hasItem()) {
                List<Component> bonuses = MachineWoodBurnerBlockEntity.burnModule.getDesc();
                if (!bonuses.isEmpty()) {
                    guiGraphics.renderComponentTooltip(this.font, bonuses, mouseX, mouseY);
                }
            }
        }

        if (burner.liquidBurn) {
            burner.tank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 80, this.topPos + 18, 16, 52);
        }

        if (isMouseOver(mouseX, mouseY, 53, 17, 16, 15)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(burner.isEnabled() ? "ON" : "OFF")
                            .withStyle(burner.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Оригинал: (53,17) 16×15 → receiveControl("toggle").
        if (isMouseOver(mouseX, mouseY, 53, 17, 16, 15)) {
            if (burner != null) {
                playButtonPress();
                CompoundTag data = new CompoundTag();
                data.putBoolean("toggle", false);
                NBTControlPacket.sendToServer(burner.getBlockPos(), data);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Оригинал: (46,37) 30×14 → receiveControl("switch").
        if (isMouseOver(mouseX, mouseY, 46, 37, 30, 14)) {
            if (burner != null) {
                playButtonPress();
                CompoundTag data = new CompoundTag();
                data.putBoolean("switch", false);
                NBTControlPacket.sendToServer(burner.getBlockPos(), data);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playButtonPress() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + sizeX
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + sizeY;
    }
}
