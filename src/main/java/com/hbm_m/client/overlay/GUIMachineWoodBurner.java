package com.hbm_m.client.overlay;

// ИМПОРТЫ
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineWoodBurnerMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.ToggleWoodBurnerPacket;
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
import java.util.Optional;

public class GUIMachineWoodBurner extends AbstractContainerScreen<MachineWoodBurnerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/wood_burner/345345.png");

    private static final ResourceLocation BURN_TIME_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/wood_burner/fuel_bar.png");

    private static final ResourceLocation ENERGY_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/wood_burner/energy_bar.png");

    public GUIMachineWoodBurner(MachineWoodBurnerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        this.topPos -= 20;
        this.leftPos = (this.width - this.imageWidth) / 2;

        this.titleLabelY = 6;
        this.titleLabelX = 17;
        this.inventoryLabelY = this.imageHeight - 110;
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        RenderSystem.setShaderTexture(0, TEXTURE);
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        renderBurnTimeBar(pGuiGraphics, x, y);
        renderEnergyBar(pGuiGraphics, x, y);
        renderToggleButton(pGuiGraphics, x, y);
    }

    // (Этот метод использует ту же логику, что и старый файл)
    private void renderBurnTimeBar(GuiGraphics graphics, int x, int y) {
        if (menu.getBurnTime() > 0) {
            int totalHeight = 52;
            // [OK] menu.getBurnTimeScaled() теперь работает правильно
            int barHeight = menu.getBurnTimeScaled(totalHeight);

            RenderSystem.setShaderTexture(0, BURN_TIME_BAR_TEXTURE);

            int startY = y + 18 + (totalHeight - barHeight);
            int textureStartY = totalHeight - barHeight;

            // [OK] Координаты (x + 17) те же, что и в старом файле
            graphics.blit(BURN_TIME_BAR_TEXTURE, x + 17, startY, 0, textureStartY, 4, barHeight);
        }
    }

    // (Этот метод использует ту же логику, что и старый файл)
    private void renderEnergyBar(GuiGraphics graphics, int x, int y) {
        if (menu.getEnergyLong() > 0) { // Используем long
            int totalHeight = 34;
            // [OK] menu.getEnergyScaled() теперь работает правильно (с long)
            int barHeight = menu.getEnergyScaled(totalHeight);

            RenderSystem.setShaderTexture(0, ENERGY_BAR_TEXTURE);

            int startY = y + 18 + (totalHeight - barHeight);
            int textureStartY = totalHeight - barHeight;

            // [OK] Координаты (x + 143) те же, что и в старом файле
            graphics.blit(
                    ENERGY_BAR_TEXTURE,
                    x + 143,
                    startY,
                    0,
                    textureStartY,
                    16,
                    barHeight
            );
        }
    }

    // (Этот метод использует ту же логику, что и старый файл)
    private void renderToggleButton(GuiGraphics graphics, int x, int y) {
        // [OK] menu.isEnabled() теперь работает правильно
        if (!menu.isEnabled()) {
            RenderSystem.setShaderTexture(0, TEXTURE);
            // [OK] Координаты (x + 53, y + 17) те же, что и в старом файле
            graphics.blit(TEXTURE, x + 53, y + 17, 196, 0, 16, 16);
        }
    }

    // (Этот метод использует ту же логику, что и старый файл)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [OK] Координаты (53, 17) те же, что и в старом файле
        if (isMouseOver((int)mouseX, (int)mouseY, 53, 17, 16, 16)) {
            // [OK] Пакет тот же
            ModPacketHandler.INSTANCE.sendToServer(
                    new ToggleWoodBurnerPacket(menu.getBlockEntity().getBlockPos())
            );
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        // (Тултип для кнопки)
        // [OK] Координаты (53, 17) те же
        if (isMouseOver(pX, pY, 53, 17, 16, 16)) {
            List<Component> tooltip = new ArrayList<>();
            // [OK] menu.isEnabled() теперь работает
            if (menu.isEnabled()) {
                tooltip.add(Component.literal("Generator: ON").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Click to turn OFF").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal("Generator: OFF").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("Click to turn ON").withStyle(ChatFormatting.GRAY));
            }
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
            return;
        }

        // (Тултип для шкалы времени горения)
        // [OK] Координаты (17, 17) те же
        if (isMouseOver(pX, pY, 17, 17, 4, 52)) {
            List<Component> tooltip = new ArrayList<>();
            // [OK] menu.isLit() и menu.getBurnTime() теперь работают
            if (menu.isLit()) {
                int burnTimeSeconds = menu.getBurnTime() / 20;
                tooltip.add(Component.literal("Burn Time: " + burnTimeSeconds + "s")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.literal("Not burning").withStyle(ChatFormatting.GRAY));
            }
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }

        // (Тултип для шкалы энергии)
        // [OK] Координаты (143, 18) те же
        if (isMouseOver(pX, pY, 143, 18, 16, 34)) {
            List<Component> tooltip = new ArrayList<>();

            // [OK] menu.getEnergyLong() и getMaxEnergyLong() теперь работают
            long energy = menu.getEnergyLong();
            long maxEnergy = menu.getMaxEnergyLong();

            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);

            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE")
                    .withStyle(ChatFormatting.GREEN));

            // [OK] menu.isLit() теперь работает
            if (menu.isLit()) {
                tooltip.add(Component.literal("+" + EnergyFormatter.formatRate(50)).withStyle(ChatFormatting.YELLOW));
                long deltaPerSecond = 50 * 20;
                String deltaPerSecondText = "+" + EnergyFormatter.formatWithUnit(deltaPerSecond, "HE/s");
                tooltip.add(Component.literal(deltaPerSecondText).withStyle(ChatFormatting.YELLOW));
            } else {
                tooltip.add(Component.literal("Not generating").withStyle(ChatFormatting.GRAY));
            }

            long percentage = maxEnergy > 0 ? (energy * 100 / maxEnergy) : 0;
            tooltip.add(Component.literal(percentage + "%").withStyle(ChatFormatting.AQUA));

            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }

        // (Тултип для пламени)
        // [OK] Координаты (56, 36) те же
        if (isMouseOver(pX, pY, 56, 36, 14, 14)) {
            if (menu.isLit()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("Burning fuel").withStyle(ChatFormatting.RED));
                pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
            }
        }
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return (mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX &&
                mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY);
    }
}