package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.List;

// GUI для сборочной машины. Отвечает за отрисовку прогресса, энергии и подсказок.
// Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import javax.annotation.Nonnull;

import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineAssemblerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GUIMachineAssembler extends AbstractContainerScreen<MachineAssemblerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_assembler_old.png");
    
    // Константа для корректного индекса слота шаблона
    private static final int TEMPLATE_SLOT_GUI_INDEX = 36 + 4; // 36 слотов игрока + 4 слота машины

    public GUIMachineAssembler(MachineAssemblerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;

        // Рисуем фон
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Отрисовка энергии
        int energyBarHeight = 52;
        int energy = this.menu.getEnergyScaled(energyBarHeight);
        guiGraphics.blit(TEXTURE, x + 116, y + 70 - energy, 176, 52 - energy, 16, energy);

        // Отрисовка прогресса
        if(menu.isCrafting()) {
            int progressWidth = 83;
            guiGraphics.blit(TEXTURE, x + 45, y + 82, 2, 222, menu.getProgressScaled(progressWidth), 32);
        }

        // Информационные панели (ВАЖНО: привязываем текстуру заново)
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawInfoPanels(guiGraphics, x, y);
        renderGhostItems(guiGraphics);
    }

    /**
     * Отрисовывает информационные панели (иконки за пределами GUI)
     */
    private void drawInfoPanels(GuiGraphics guiGraphics, int x, int y) {
        // Панель шаблона (всегда видна) - ID 11
        drawInfoPanel(guiGraphics, x - 16, y + 16, 16, 16, 11);

        // Панель предупреждения (видна только если нет шаблона) - ID 6
        ItemStack templateStack = menu.getSlot(TEMPLATE_SLOT_GUI_INDEX).getItem();
        if (templateStack.isEmpty() || !(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            drawInfoPanel(guiGraphics, x - 16, y + 36, 16, 16, 6);
        }
    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
        NonNullList<ItemStack> ghostItems = this.menu.getBlockEntity().getGhostItems();
        
        if (ghostItems.isEmpty()) {
            return;
        }
        
        // Получаем время для анимации циклической смены предметов (для Ingredient с несколькими вариантами)
        long time = System.currentTimeMillis();
        int cycleIndex = (int) ((time / 1000) % 20); // Меняется каждую секунду
        
        // Слоты 6-17 (handler) соответствуют слотам 42-53 в menu (36 слотов игрока + 6 машины)
        int inputSlotsStart = 36 + 6; // 42
        int inputSlotsCount = 12;
        
        // Отрисовываем сгруппированные предметы
        for (int i = 0; i < Math.min(ghostItems.size(), inputSlotsCount); i++) {
            ItemStack ghostStack = ghostItems.get(i);
            
            if (ghostStack.isEmpty()) {
                continue;
            }
            
            // Получаем слот
            int slotIndex = inputSlotsStart + i;
            if (slotIndex >= this.menu.slots.size()) break;
            
            net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
            
            // Отрисовываем призрак только если слот пуст
            if (!slot.hasItem()) {
                // Отрисовка призрачного предмета с полупрозрачностью
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 100); // z-level 100
                
                // Полупрозрачность
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
                
                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;
                
                guiGraphics.renderItem(ghostStack, x, y);
                
                // ОТРИСОВКА КОЛИЧЕСТВА (если > 1)
                if (ghostStack.getCount() > 1) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Полная непрозрачность для текста
                    guiGraphics.renderItemDecorations(this.font, ghostStack, x, y);
                }
                
                // Восстанавливаем цвет
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
                
                guiGraphics.pose().popPose();
            }
        }
    }

    /**
     * Отрисовывает одну информационную панель по ID
     * @param id Тип панели: 6 = предупреждение, 11 = шаблон
     */
    private void drawInfoPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int id) {
        int u = 192; // UV координаты в текстуре для иконок
        int v = 0;
        
        // Определяем UV смещение в зависимости от ID панели
        switch(id) {
            case 6:  // Предупреждение
                v = 16;
                break;
            case 11: // Шаблон
                v = 0;
                break;
            default:
                return;
        }
        
        guiGraphics.blit(TEXTURE, x, y, u, v, width, height);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        // Подсказка для энергии
        renderEnergyTooltip(pGuiGraphics, pX, pY);
        
        // Подсказки для информационных панелей
        renderInfoPanelTooltips(pGuiGraphics, pX, pY);
    }

    /**
     * Отрисовывает подсказку для шкалы энергии
     */
    private void renderEnergyTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        int energyBarX = this.leftPos + 116;
        int energyBarY = this.topPos + 18;
        int energyBarWidth = 16;
        int energyBarHeight = 52;

        if (pX >= energyBarX && pX < energyBarX + energyBarWidth &&
                pY >= energyBarY && pY < energyBarY + energyBarHeight) {

            List<Component> tooltip = new ArrayList<>();

            // --- ИЗМЕНЕНИЕ: Используем Long-геттеры и Форматер ---
            long energy = this.menu.getEnergyLong();
            long maxEnergy = this.menu.getMaxEnergyLong();
            long delta = this.menu.getEnergyDeltaLong();

            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);
            String deltaStr = EnergyFormatter.formatRate(delta); // (уже добавляет /t)

            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE"));

            // ---

            pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
        }
    }

    /**
     * Отрисовывает подсказки для информационных панелей
     */
    private void renderInfoPanelTooltips(GuiGraphics pGuiGraphics, int pX, int pY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Панель шаблона
        if (isMouseOver(pX, pY, x - 16, y + 16, 16, 16)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("desc.gui.template"));
            pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
        }

        // Панель предупреждения (только если нет шаблона)
        ItemStack templateStack = menu.getSlot(TEMPLATE_SLOT_GUI_INDEX).getItem();
        if (templateStack.isEmpty() || !(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            if (isMouseOver(pX, pY, x - 16, y + 36, 16, 16)) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("desc.gui.assembler.warning").withStyle(ChatFormatting.RED));
                pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
            }
        }
    }

    /**
     * Проверяет, находится ли курсор мыши над областью
     */
    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}