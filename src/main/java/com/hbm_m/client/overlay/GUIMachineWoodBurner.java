package com.hbm_m.client.overlay;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineWoodBurnerMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.ToggleWoodBurnerPacket;
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
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/wood_burner/wood_burner_gui.png");

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
        this.inventoryLabelY = this.imageHeight - 104;
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

    private void renderBurnTimeBar(GuiGraphics graphics, int x, int y) {
        if (menu.getBurnTime() > 0) {
            int totalHeight = 52;
            int barHeight = menu.getBurnTimeScaled(totalHeight);

            RenderSystem.setShaderTexture(0, BURN_TIME_BAR_TEXTURE);

            int startY = y + 18 + (totalHeight - barHeight);
            int textureStartY = totalHeight - barHeight;

            graphics.blit(BURN_TIME_BAR_TEXTURE, x + 17, startY, 0, textureStartY, 4, barHeight);
        }
    }

    private void renderEnergyBar(GuiGraphics graphics, int x, int y) {
        if (menu.getEnergy() > 0) {
            int totalHeight = 34;
            int barHeight = menu.getEnergyScaled(totalHeight);

            RenderSystem.setShaderTexture(0, ENERGY_BAR_TEXTURE);

            int startY = y + 52 - barHeight;

            graphics.blit(
                    ENERGY_BAR_TEXTURE,
                    x + 143,
                    startY,
                    0,
                    0,
                    16,
                    barHeight
            );
        }
    }

    private void renderToggleButton(GuiGraphics graphics, int x, int y) {
        // Рендерим текстуру выключенной кнопки ТОЛЬКО если генератор выключен
        if (!menu.isEnabled()) {
            RenderSystem.setShaderTexture(0, TEXTURE);
            // Координаты кнопки: x=47, y=17
            // Координаты текстуры выключенной кнопки: x=196, y=0
            graphics.blit(TEXTURE, x + 53, y + 17, 196, 0, 16, 16);
        }
        // Если enabled=true, текстура включенной кнопки уже есть в основном GUI
    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по кнопке переключателя (x=47, y=17, размер 16x16)
        if (isMouseOver((int)mouseX, (int)mouseY, 53, 17, 16, 16)) {
            // Отправляем пакет на сервер для переключения состояния
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

        // Тултип для кнопки переключателя
        if (isMouseOver(pX, pY, 53, 17, 16, 16)) {
            List<Component> tooltip = new ArrayList<>();
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

        // Тултип для шкалы времени горения
        if (isMouseOver(pX, pY, 17, 17, 4, 52)) {
            List<Component> tooltip = new ArrayList<>();
            if (menu.isLit()) {
                int burnTimeSeconds = menu.getBurnTime() / 20;
                tooltip.add(Component.literal("Burn Time: " + burnTimeSeconds + "s")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.literal("Not burning").withStyle(ChatFormatting.GRAY));
            }
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }

        // Тултип для шкалы энергии
        if (isMouseOver(pX, pY, 143, 18, 16, 34)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(String.format("%,d / %,d FE", menu.getEnergy(), menu.getMaxEnergy()))
                    .withStyle(ChatFormatting.GREEN));

            if (menu.isLit()) {
                tooltip.add(Component.literal("+50 FE/t").withStyle(ChatFormatting.YELLOW));
            } else {
                tooltip.add(Component.literal("Not generating").withStyle(ChatFormatting.GRAY));
            }

            int percentage = (int) ((float) menu.getEnergy() / menu.getMaxEnergy() * 100);
            tooltip.add(Component.literal(percentage + "%").withStyle(ChatFormatting.AQUA));

            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }

        // Тултип для области пламени
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