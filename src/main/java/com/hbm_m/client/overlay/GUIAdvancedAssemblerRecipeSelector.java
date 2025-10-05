package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.SetAssemblerRecipeC2SPacket;
import com.hbm_m.recipe.AssemblerRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI для выбора рецепта в продвинутой сборочной машине.
 * При выборе рецепта или нажатии ESC возвращается в основное меню машины.
 */
public class GUIAdvancedAssemblerRecipeSelector extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        RefStrings.MODID, "textures/gui/gui_recipe_selector.png");

    public static final String NULL_SELECTION = "null";
    
    private final int xSize = 176;
    private final int ySize = 132;
    private int guiLeft;
    private int guiTop;
    
    private EditBox searchBox;
    private int pageIndex = 0;
    private int pageCount;
    
    private final List<RecipeEntry> allRecipes = new ArrayList<>();
    private List<RecipeEntry> filteredRecipes = new ArrayList<>();
    
    private final BlockPos machinePos;
    private final List<ResourceLocation> availableRecipes;
    private final Screen parentScreen;
    
    // Текущий выбранный рецепт (может быть null)
    private ResourceLocation selectedRecipe = null;
    private final ResourceLocation initialRecipe;
    
    private record RecipeEntry(ResourceLocation id, ItemStack icon) {}
    
    public GUIAdvancedAssemblerRecipeSelector(BlockPos machinePos, List<ResourceLocation> availableRecipes,
        ResourceLocation currentRecipe, Screen parentScreen) {
            super(Component.translatable("gui.hbm_m.assembler_recipe_selector"));
            this.machinePos = machinePos;
            this.availableRecipes = availableRecipes;
            this.parentScreen = parentScreen;
            this.selectedRecipe = currentRecipe;
            this.initialRecipe = currentRecipe;
    }
    
    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        
        // Заполняем список рецептов
        if (this.allRecipes.isEmpty() && this.minecraft != null && this.minecraft.level != null) {
            for (ResourceLocation recipeId : availableRecipes) {
                this.minecraft.level.getRecipeManager().byKey(recipeId).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        ItemStack icon = assemblerRecipe.getResultItem(null);
                        allRecipes.add(new RecipeEntry(recipeId, icon));
                    }
                });
            }
            
            this.filteredRecipes.clear();
            this.filteredRecipes.addAll(allRecipes);
            resetPaging();
        }
        
        // Поле поиска - координаты как в оригинале
        this.searchBox = new EditBox(this.font, this.guiLeft + 28, this.guiTop + 111, 102, 12, Component.empty());
        this.searchBox.setMaxLength(32);
        this.searchBox.setResponder(this::onSearch);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setFocused(true);
        this.addRenderableWidget(this.searchBox);
    }
    
    private void resetPaging() {
        this.pageIndex = 0;
        // Формула из оригинала: (количество рецептов - 40) / 8
        this.pageCount = Math.max(0, (int) Math.ceil((this.filteredRecipes.size() - 40) / 8.0));
    }
    
    private void onSearch(String query) {
        filteredRecipes.clear();
        if (query.isEmpty()) {
            filteredRecipes.addAll(allRecipes);
        } else {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            for (RecipeEntry entry : allRecipes) {
                if (entry.icon.getHoverName().getString().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    filteredRecipes.add(entry);
                }
            }
        }
        resetPaging();
    }
    
    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Рендерим родительский экран на фоне
        if (this.parentScreen != null) {
            this.parentScreen.render(guiGraphics, -1, -1, partialTick);
        }
        
        // Затемнение
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);
        
        // Отрисовка фона GUI
        drawGuiBackground(guiGraphics, mouseX, mouseY);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Отрисовка рецептов и тултипов
        renderRecipes(guiGraphics, mouseX, mouseY);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }
    
    private void drawGuiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Основная текстура
        guiGraphics.blit(TEXTURE, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        
        // Фон поля поиска при фокусе
        if (this.searchBox.isFocused()) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 26, this.guiTop + 108, 0, 132, 106, 16);
        }
        
        // Ховер-эффекты для кнопок (из оригинала)
        // Кнопка "Вверх"
        if (isHovering(mouseX, mouseY, 152, 18, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 152, this.guiTop + 18, 176, 0, 16, 16);
        }
        
        // Кнопка "Вниз"
        if (isHovering(mouseX, mouseY, 152, 36, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 152, this.guiTop + 36, 176, 16, 16, 16);
        }
        
        // Кнопка "Закрыть"
        if (isHovering(mouseX, mouseY, 152, 90, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 152, this.guiTop + 90, 176, 32, 16, 16);
        }
        
        // Кнопка "Очистить поиск"
        if (isHovering(mouseX, mouseY, 134, 108, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 134, this.guiTop + 108, 176, 48, 16, 16);
        }
        
        // Кнопка "Фокус поиска"
        if (isHovering(mouseX, mouseY, 8, 108, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 8, this.guiTop + 108, 176, 64, 16, 16);
        }
        
        // Отрисовка рамки выбранного рецепта в сетке
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= filteredRecipes.size()) break;
            int ind = i - pageIndex * 8;
            RecipeEntry entry = filteredRecipes.get(i);
            
            if (entry.id.equals(this.selectedRecipe)) {
                guiGraphics.blit(TEXTURE, this.guiLeft + 7 + 18 * (ind % 8), 
                               this.guiTop + 17 + 18 * (ind / 8), 192, 0, 18, 18);
            }
        }
    }
    
    private void renderRecipes(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Сетка 8×5 как в оригинале
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= filteredRecipes.size()) break;
            
            int ind = i - pageIndex * 8;
            int col = ind % 8;
            int row = ind / 8;
            int x = this.guiLeft + 8 + 18 * col;
            int y = this.guiTop + 18 + 18 * row;
            
            RecipeEntry entry = filteredRecipes.get(i);
            guiGraphics.renderItem(entry.icon, x, y);
        }
        
        // Отрисовка выбранного рецепта в отдельном слоте
        if (this.selectedRecipe != null) {
            for (RecipeEntry entry : filteredRecipes) {
                if (entry.id.equals(this.selectedRecipe)) {
                    guiGraphics.renderItem(entry.icon, this.guiLeft + 152, this.guiTop + 72);
                    break;
                }
            }
        }
    }
    
    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Тултипы для сетки рецептов
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= this.filteredRecipes.size()) break;
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8);
            int iy = 17 + 18 * (ind / 8);
            
            if (isHovering(mouseX, mouseY, ix, iy, 18, 18)) {
                RecipeEntry entry = filteredRecipes.get(i);
                
                // ИСПОЛЬЗУЕМ ПРОДВИНУТЫЙ ТУЛТИП С ДЕТАЛЯМИ РЕЦЕПТА
                if (this.minecraft != null && this.minecraft.level != null) {
                    this.minecraft.level.getRecipeManager().byKey(entry.id).ifPresent(recipe -> {
                        if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                            List<Component> tooltip = new ArrayList<>();
                            
                            // Название предмета
                            tooltip.add(entry.icon.getHoverName());
                            
                            // Детали рецепта (БЕЗ строки о папке шаблонов)
                            com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(assemblerRecipe, tooltip);
                            
                            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), 
                                                    mouseX, mouseY);
                        }
                    });
                }
                
                return; // Важно: выходим после показа тултипа
            }
        }
        
        // Тултип для слота предпросмотра
        if (isHovering(mouseX, mouseY, 151, 71, 18, 18) && this.selectedRecipe != null) {
            if (this.minecraft != null && this.minecraft.level != null) {
                this.minecraft.level.getRecipeManager().byKey(this.selectedRecipe).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        List<Component> tooltip = new ArrayList<>();
                        
                        ItemStack output = assemblerRecipe.getResultItem(null);
                        tooltip.add(output.getHoverName());
                        
                        // Детали рецепта
                        com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(assemblerRecipe, tooltip);
                        
                        guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), 
                                                mouseX, mouseY);
                    }
                });
            }
            return;
        }
        
        // Остальные тултипы для кнопок
        if (isHovering(mouseX, mouseY, 152, 90, 16, 16)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Close").withStyle(ChatFormatting.YELLOW), 
                                    mouseX, mouseY);
        }
        
        if (isHovering(mouseX, mouseY, 134, 108, 16, 16)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Clear search").withStyle(ChatFormatting.YELLOW), 
                                    mouseX, mouseY);
        }
        
        if (isHovering(mouseX, mouseY, 8, 108, 16, 16)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Press ENTER to toggle focus").withStyle(ChatFormatting.ITALIC), 
                                    mouseX, mouseY);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Кнопка "Вверх"
        if (isHovering((int)mouseX, (int)mouseY, 152, 18, 16, 16)) {
            playClickSound();
            if (this.pageIndex > 0) this.pageIndex--;
            return true;
        }
        
        // Кнопка "Вниз"
        if (isHovering((int)mouseX, (int)mouseY, 152, 36, 16, 16)) {
            playClickSound();
            if (this.pageIndex < this.pageCount) this.pageIndex++;
            return true;
        }
        
        // Кнопка "Очистить поиск"
        if (isHovering((int)mouseX, (int)mouseY, 134, 108, 16, 16)) {
            this.searchBox.setValue("");
            this.onSearch("");
            this.searchBox.setFocused(true);
            return true;
        }
        
        // Клик по рецептам в сетке
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= this.filteredRecipes.size()) break;
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8);
            int iy = 17 + 18 * (ind / 8);
            
            if (isHovering((int)mouseX, (int)mouseY, ix, iy, 18, 18)) {
                RecipeEntry entry = filteredRecipes.get(i);
                
                // Переключение выбора (как в оригинале)
                if (!entry.id.equals(selectedRecipe)) {
                    this.selectedRecipe = entry.id;
                } else {
                    this.selectedRecipe = null;
                }
                
                playClickSound();
                return true;
            }
        }
        
        // Клик по слоту выбранного рецепта - очистка
        if (isHovering((int)mouseX, (int)mouseY, 151, 71, 18, 18)) {
            if (this.selectedRecipe != null) {
                this.selectedRecipe = null;
                playClickSound();
                return true;
            }
        }
        
        // Кнопка "Закрыть" - применение выбора
        if (isHovering((int)mouseX, (int)mouseY, 152, 90, 16, 16)) {
            applySelection();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && pageIndex > 0) {
            pageIndex--;
        } else if (delta < 0 && pageIndex < pageCount) {
            pageIndex++;
        }
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ENTER переключает фокус поиска
        if (keyCode == 257) { // GLFW_KEY_ENTER
            this.searchBox.setFocused(!this.searchBox.isFocused());
            return true;
        }
        
        // Навигация клавишами
        if (keyCode == 265) pageIndex--; // UP
        if (keyCode == 264) pageIndex++; // DOWN
        if (keyCode == 266) pageIndex -= 5; // PAGE_UP
        if (keyCode == 267) pageIndex += 5; // PAGE_DOWN
        if (keyCode == 268) pageIndex = 0; // HOME
        if (keyCode == 269) pageIndex = pageCount; // END
        
        pageIndex = Math.max(0, Math.min(pageIndex, pageCount));
        
        // ESC закрывает с применением выбора
        if (keyCode == 256) {
            applySelection();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void applySelection() {
        // ВСЕГДА отправляем пакет (как в оригинале через onGuiClosed)
        ModPacketHandler.INSTANCE.sendToServer(
            new SetAssemblerRecipeC2SPacket(machinePos, selectedRecipe)); // null разрешён!
        
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
    
    @Override
    public void onClose() {
        applySelection();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private boolean isHovering(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= guiLeft + x && mouseX < guiLeft + x + width && 
                mouseY >= guiTop + y && mouseY < guiTop + y + height;
    }
    
    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
