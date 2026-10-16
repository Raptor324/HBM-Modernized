package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachineFlareStackBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.menu.MachineFlareStackMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.MachineControlC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

/**
 * Порт {@code GUIMachineGasFlare} (1.7.10): клапан ("valve") и поджиг ("dial") —
 * кликабельные области с оверлеями из текстуры, иконка пламени при горении,
 * бак и батарея. Позиции и регионы текстуры 1:1 с оригиналом.
 */
public class GUIMachineFlareStack extends GuiInfoScreen<MachineFlareStackMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_flare_stack.png");

    private final MachineFlareStackBlockEntity flareStack;

    public GUIMachineFlareStack(MachineFlareStackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.flareStack = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (flareStack != null) { // тайл может отсутствовать в реплее Flashback
            int x = this.leftPos;
            int y = this.topPos;

            // Оригинал: j = power * 52 / maxPower, бар на (143, 69-j) из (176, 94-j).
            int power = (int) (flareStack.getEnergyStored() * 52L / Math.max(flareStack.getMaxEnergyStored(), 1L));
            if (power > 52) power = 52;
            if (power > 0) {
                guiGraphics.blit(TEXTURE, x + 143, y + 69 - power, 176, 94 - power, 16, power);
            }

            // Оверлей клапана (176,0 / 35x10) и поджига (176,10 / 35x14).
            if (flareStack.isOn()) {
                guiGraphics.blit(TEXTURE, x + 79, y + 15, 176, 0, 35, 10);
            }
            if (flareStack.doesBurn()) {
                guiGraphics.blit(TEXTURE, x + 79, y + 49, 176, 10, 35, 14);
            }

            // Иконка пламени при горении (176,24 / 18x18).
            var tank = flareStack.getTank();
            boolean flammable = tank.getTankType() != Fluids.EMPTY
                    && FluidType.hasTrait(tank.getTankType(), FT_Flammable.class);
            if (flareStack.isOn() && flareStack.doesBurn() && tank.getFill() > 0 && flammable) {
                guiGraphics.blit(TEXTURE, x + 88, y + 29, 176, 24, 18, 18);
            }

            // Бак: окраска по цвету флюида (аналог renderTank (35,69) 16x52).
            if (tank.getFill() > 0) {
                int color = 0xFF000000 | FluidType.forFluid(tank.getTankType()).getColor();
                int fluidH = tank.getFill() * 52 / tank.getMaxFill();
                guiGraphics.fill(x + 35, y + 17 + (52 - fluidH), x + 51, y + 69, color);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (flareStack != null) {
            int x = this.leftPos;
            int y = this.topPos;

            // Оригинал: тултипы клапана (79,16 35x10), поджига (79,50 35x14),
            // бака (35,17 16x52) и батареи (143,17 16x52).
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, x + 79, y + 16, 35, 10, mouseX, mouseY,
                    Component.translatable("gui.hbm_m.flare.valve"));
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, x + 79, y + 50, 35, 14, mouseX, mouseY,
                    Component.translatable("gui.hbm_m.flare.ignition"));
            drawElectricityInfo(guiGraphics, mouseX, mouseY, x + 143, y + 17, 16, 52,
                    flareStack.getEnergyStored(), flareStack.getMaxEnergyStored());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (flareStack != null) {
            // Оригинал: клапан (89,16,16x10), поджиг (89,50,16x14).
            if (mouseX >= this.leftPos + 89 && mouseX < this.leftPos + 89 + 16
                    && mouseY >= this.topPos + 16 && mouseY < this.topPos + 16 + 10) {
                playButtonClick();
                sendToggle(true, false);
            } else if (mouseX >= this.leftPos + 89 && mouseX < this.leftPos + 89 + 16
                    && mouseY >= this.topPos + 50 && mouseY < this.topPos + 50 + 14) {
                playButtonClick();
                sendToggle(false, true);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playButtonClick() {
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void sendToggle(boolean valve, boolean dial) {
        CompoundTag data = new CompoundTag();
        if (valve) data.putBoolean("valve", true);
        if (dial) data.putBoolean("dial", true);
        MachineControlC2SPacket.send(flareStack.getBlockPos(), data);
    }
}
