package com.hbm_m.client.overlay;

import com.hbm_m.main.MainRegistry; // ЗАМЕНИ НА СВОЙ КЛАСС
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

public class GUIMachineWoodBurner extends AbstractContainerScreen<MachineWoodBurnerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/generators/gui_wood_burner_alt.png");

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

        // Позиция заголовка (по умолчанию 6)
        this.titleLabelY = 6;
        this.titleLabelX = 17;// Можете изменить на нужное значение
        this.inventoryLabelY = this.imageHeight - 110;
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        // Рисуем заголовок белым цветом (0xFFFFFF)
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);

        // Рисуем надпись инвентаря стандартным цветом
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (menu.isLit()) {
            gui.blit(TEXTURE, x +46, y +37, 206, 72, 30, 14); // Flame
        }

        int burnHeight = menu.getBurnTimeScaled(52);
        if (burnHeight > 0) {
            gui.blit(TEXTURE, x + 17, y + 18 + 52 - burnHeight, 192, 52 - burnHeight, 4, burnHeight); // Fuel bar
        }

        int energyHeight = menu.getEnergyScaled(34);
        if (energyHeight > 0) {
            gui.blit(TEXTURE, x + 143, y + 18 + 34 - energyHeight, 176, 34 - energyHeight, 16, energyHeight); // Energy bar
        }

        if (!menu.isEnabled()) {
            gui.blit(TEXTURE, x + 53, y + 17, 196, 0, 16, 16); // Disabled overlay
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);
        this.renderTooltip(gui, mouseX, mouseY);
    }


    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);

        // Тултип для энергии
        if (isMouseOver(mouseX, mouseY, 143, 18, 16, 34)) {
            List<Component> tooltip = new ArrayList<>();

            long energy = menu.getEnergyLong();
            long maxEnergy = menu.getMaxEnergyLong();

            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);

            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE")
                    .withStyle(ChatFormatting.GREEN));

            if (menu.isLit()) {
                tooltip.add(Component.literal("+50 HE/t").withStyle(ChatFormatting.YELLOW));
            }

            gui.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }

        if (isMouseOver(mouseX, mouseY, 17, 18, 4, 52)) {
            List<Component> tooltip = new ArrayList<>();
            int burnTime = menu.getBurnTime();
            int maxBurnTime = menu.getMaxBurnTime();

            if (maxBurnTime > 0) {
                int seconds = burnTime / 20;
                tooltip.add(Component.literal("Burn Time: " + seconds + "s")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.literal("No Fuel")
                        .withStyle(ChatFormatting.RED));
            }

            gui.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }

        if (isMouseOver(mouseX, mouseY, 53, 17, 16, 16)) {
            String status = menu.isEnabled() ? "Enabled" : "Disabled";
            ChatFormatting color = menu.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED;
            gui.renderTooltip(this.font,
                    Component.literal(status).withStyle(color), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY, 53, 17, 16, 16)) {
            // Отправка пакета на сервер для переключения 'enabled'
            ModPacketHandler.INSTANCE.sendToServer(new ToggleWoodBurnerPacket(menu.blockEntity.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return (mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX &&
                mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY);
    }
}