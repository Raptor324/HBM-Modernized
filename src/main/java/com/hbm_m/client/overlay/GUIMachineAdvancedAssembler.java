package com.hbm_m.client.overlay;

// GUI для продвинутой сборочной машины.
// Отвечает за отрисовку прогресса, энергии, подсказок и взаимодействие с пользователем.
// Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.

import com.hbm_m.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.recipe.AssemblerRecipe;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.hbm_m.util.EnergyFormatter;


// TODO: Нужен утилитарный класс для отрисовки подсказок и жидкостей.
// Этот функционал сейчас встроен в этот класс

public class GUIMachineAdvancedAssembler extends AbstractContainerScreen<MachineAdvancedAssemblerMenu> {

    // Текстура из старого GUI
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_assembler.png");
    // private static final ResourceLocation TEMPLATE_FOLDER_ICON = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/item/template_folder.png");

    public GUIMachineAdvancedAssembler(MachineAdvancedAssemblerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Отрисовка основной текстуры
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Отрисовка энергии
        long energyStored = this.menu.getEnergyLong();
        long maxEnergy = this.menu.getMaxEnergyLong();
        if (maxEnergy > 0) {
            int energyBarHeight = (int) (energyStored * 61L / maxEnergy);
            if (energyBarHeight > 61) energyBarHeight = 61; // Защита

            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - energyBarHeight,
                    176, 61 - energyBarHeight, 16, energyBarHeight);
        }

        // ИСПРАВЛЕНО: Отрисовка прогресса - используем ContainerData через menu
        int progress = this.menu.getProgress(); // <-- ИЗМЕНЕНО: используем menu вместо blockEntity
        if (progress > 0) {
            int maxProgress = this.menu.getMaxProgress(); // <-- ИЗМЕНЕНО
            if (maxProgress > 0) {
                int progressWidth = (int) Math.ceil(70.0 * progress / maxProgress);
                guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progressWidth, 16);
            }
        }

        // Получаем текущий рецепт
        ResourceLocation selectedRecipeId = this.menu.getBlockEntity().getSelectedRecipeId();
        AssemblerRecipe recipe = null;
        if (selectedRecipeId != null && this.minecraft != null && this.minecraft.level != null) {
            recipe = this.minecraft.level.getRecipeManager()
                    .byKey(selectedRecipeId)
                    .filter(r -> r instanceof AssemblerRecipe)
                    .map(r -> (AssemblerRecipe) r)
                    .orElse(null);
        }

        boolean hasRecipe = recipe != null;
        boolean canProcess = hasRecipe && energyStored >= 100;
        
        // Отрисовка светодиодов (LEDs) - используем isCrafting() из menu
        if (this.menu.isCrafting()) { // <-- ПРАВИЛЬНО: используется menu
            // Левый LED (зеленый)
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            // Правый LED (зеленый)
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (hasRecipe) {
            // Левый LED (желтый)
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (canProcess) {
                // Правый LED (желтый)
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        // Отрисовка "призрачных" предметов в пустых слотах
        renderGhostItems(guiGraphics);
        
        // TODO: Отрисовка жидкостей в танках
        // Когда у BlockEntity будут методы getInputTank() и getOutputTank(), раскомментируйте:
        /*
        FluidTank inputTank = this.menu.getBlockEntity().getInputTank();
        FluidTank outputTank = this.menu.getBlockEntity().getOutputTank();
        
        if (inputTank != null) {
            renderFluidTank(guiGraphics, inputTank, this.leftPos + 8, this.topPos + 115, 52, 16);
        }
        if (outputTank != null) {
            renderFluidTank(guiGraphics, outputTank, this.leftPos + 80, this.topPos + 115, 52, 16);
        }
        */
    }

    /**
     * Отрисовывает призрачные предметы в пустых входных слотах.
     * Группирует одинаковые ингредиенты и показывает суммарное количество.
     */

    private void renderGhostItems(GuiGraphics guiGraphics) {
        // ИСПОЛЬЗУЕМ метод из BlockEntity, который получает данные из модуля
        NonNullList<ItemStack> ghostItems = this.menu.getBlockEntity().getGhostItems();

        if (ghostItems.isEmpty()) {
            return;
        }

        // Группируем одинаковые предметы и суммируем их количество
        java.util.Map<ItemStack, Integer> groupedItems = new java.util.LinkedHashMap<>();
        for (ItemStack stack : ghostItems) {
            if (stack.isEmpty()) {
                continue;
            }
            
            // Ищем уже существующий предмет в группе
            ItemStack found = null;
            for (ItemStack key : groupedItems.keySet()) {
                if (ItemStack.isSameItemSameTags(key, stack)) {
                    found = key;
                    break;
                }
            }
            
            if (found != null) {
                // Увеличиваем количество
                groupedItems.put(found, groupedItems.get(found) + stack.getCount());
            } else {
                // Добавляем новый предмет
                ItemStack copy = stack.copy();
                copy.setCount(1); // Нормализуем количество для ключа
                groupedItems.put(copy, stack.getCount());
            }
        }

        // Слоты 4-15 (handler) соответствуют слотам 40-51 в menu
        int inputSlotsStart = 4; // 40
        int inputSlotsCount = 12;

        // Отрисовываем сгруппированные предметы
        int slotOffset = 0;
        for (java.util.Map.Entry<ItemStack, Integer> entry : groupedItems.entrySet()) {
            if (slotOffset >= inputSlotsCount) {
                break; // Превышен лимит слотов
            }
            
            ItemStack ghostStack = entry.getKey().copy();
            ghostStack.setCount(entry.getValue()); // Устанавливаем суммарное количество

            // Получаем слот
            int slotIndex = inputSlotsStart + slotOffset;
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
            
            slotOffset++;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderTooltip(guiGraphics, pMouseX, pMouseY);
        
        // ИСПРАВЛЕНО: Подсказка для шкалы энергии - используем ContainerData
        if (isMouseOver(pMouseX, pMouseY, 152, 18, 16, 61)) {
            List<Component> tooltip = new ArrayList<>();

            // Получаем long значения
            long energy = this.menu.getEnergyLong();
            long maxEnergy = this.menu.getMaxEnergyLong();

            // Форматируем их
            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);

            // Первая строка: текущая / максимальная энергия
            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE")
                    .withStyle(ChatFormatting.GREEN));

            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pMouseX, pMouseY);

        }
        
        // Подсказка для шкалы энерги
        
        // ПРОДВИНУТАЯ ПОДСКАЗКА ДЛЯ КНОПКИ ВЫБОРА РЕЦЕПТА
        if (isMouseOver(pMouseX, pMouseY, 7, 125, 18, 18)) {
            ResourceLocation selectedRecipeId = this.menu.getBlockEntity().getSelectedRecipeId();
            if (selectedRecipeId != null && this.minecraft != null && this.minecraft.level != null) {
                this.minecraft.level.getRecipeManager().byKey(selectedRecipeId).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        List<Component> tooltip = new ArrayList<>();
                        
                        // Название выходного предмета
                        ItemStack output = assemblerRecipe.getResultItem(null);
                        tooltip.add(output.getHoverName());
                        
                        // ДОБАВЛЯЕМ ПРОДВИНУТЫЙ ТУЛТИП С ДЕТАЛЯМИ РЕЦЕПТА
                        com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(assemblerRecipe, tooltip);
                        
                        guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(),
                                                pMouseX, pMouseY);
                    }
                });
            } else {
                // Если рецепт не выбран, показываем подсказку "Set Recipe"
                guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    pMouseX, pMouseY);
            }
        }
        
        // TODO: Подсказки для танков - ВОССТАНОВЛЕНО из оригинала
        /*
        if (isMouseOver(pMouseX, pMouseY, 8, 99, 52, 16)) {
            FluidTank inputTank = this.menu.getBlockEntity().getInputTank();
            if (inputTank != null) {
                inputTank.renderTankInfo(guiGraphics, pMouseX, pMouseY);
            }
        }
        
        if (isMouseOver(pMouseX, pMouseY, 80, 99, 52, 16)) {
            FluidTank outputTank = this.menu.getBlockEntity().getOutputTank();
            if (outputTank != null) {
                outputTank.renderTankInfo(guiGraphics, pMouseX, pMouseY);
            }
        }
        */
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по кнопке выбора рецепта
        if (isMouseOver((int)mouseX, (int)mouseY, 7, 125, 18, 18)) {
            openRecipeSelector();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openRecipeSelector() {
        if (this.minecraft == null || this.minecraft.level == null) return;
        
        ResourceLocation currentRecipe = this.menu.getBlockEntity().getSelectedRecipeId();
        
        // Убираем передачу списка рецептов
        this.minecraft.setScreen(new GUIAdvancedAssemblerRecipeSelector(
            this.menu.getBlockEntity().getBlockPos(),
            currentRecipe,
            this
        ));
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Центрированное название
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 
                            70 - this.font.width(name) / 2, 6, 0x404040, false);
        
        // Название инвентаря игрока
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                            8, this.imageHeight - 96 + 2, 0x404040, false);
        
        // ОТРИСОВКА ИКОНКИ РЕЦЕПТА - ТОЛЬКО ЕСЛИ ЭТО ОСНОВНОЙ ЭКРАН
        // Проверяем, что текущий активный экран - это именно этот экран
        if (this.minecraft != null && this.minecraft.screen == this) {
            ResourceLocation selectedRecipeId = this.menu.getBlockEntity().getSelectedRecipeId();
            if (selectedRecipeId != null && this.minecraft.level != null) {
                this.minecraft.level.getRecipeManager().byKey(selectedRecipeId).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        ItemStack icon = assemblerRecipe.getResultItem(null);
                        guiGraphics.renderItem(icon, 8, 126);
                    }
                });
            } else {
                // Отрисовываем иконку папки шаблонов
                ItemStack folderIcon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
                guiGraphics.renderItem(folderIcon, 8, 126);
            }
        }
    }
    
    // TODO: Метод для отрисовки жидкостей в танках
    /*
    private void renderFluidTank(GuiGraphics guiGraphics, FluidTank tank, int x, int y, int width, int height) {
        if (tank.getFluid().isEmpty()) return;
        
        // Логика рендеринга жидкости
        // Используйте RenderSystem и FluidRenderer из Forge
    }
    */
    private boolean isMouseOver(int pMouseX, int pMouseY, int pX, int pY, int pWidth, int pHeight) {
        return pMouseX >= this.leftPos + pX && pMouseX < this.leftPos + pX + pWidth &&
                pMouseY >= this.topPos + pY && pMouseY < this.topPos + pY + pHeight;
    }
}