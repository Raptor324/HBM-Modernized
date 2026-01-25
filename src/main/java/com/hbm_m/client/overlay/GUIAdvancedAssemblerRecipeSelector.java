package com.hbm_m.client.overlay;

import com.hbm_m.block.entity.custom.machines.MachineAdvancedAssemblerBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GUIAdvancedAssemblerRecipeSelector extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_recipe_selector.png");
    
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
    private final Screen parentScreen;
    private ItemStack lastFolderStack = ItemStack.EMPTY;
    
    private ResourceLocation selectedRecipe = null;
    
    private record RecipeEntry(ResourceLocation id, ItemStack icon, @Nullable AssemblerRecipe recipe) {}
    
    public GUIAdvancedAssemblerRecipeSelector(BlockPos machinePos, ResourceLocation currentRecipe, Screen parentScreen) {
        super(Component.translatable("gui.hbm_m.assembler_recipe_selector"));
        this.machinePos = machinePos;
        this.parentScreen = parentScreen;
        this.selectedRecipe = currentRecipe;
    }
    
    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        // Удалите весь блок с availableRecipes
        // Вместо этого вызовите reloadRecipes() напрямую
        if (this.allRecipes.isEmpty()) {
            reloadRecipes();
        }

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
        this.renderBackground(guiGraphics);
        drawGuiBackground(guiGraphics, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderRecipes(guiGraphics, mouseX, mouseY);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }
    
    private void drawGuiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        
        if (this.searchBox.isFocused()) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 26, this.guiTop + 108, 0, 132, 106, 16);
        }
        
        // Ховер-эффекты для кнопок
        if (isHovering(mouseX, mouseY, 152, 18, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 152, this.guiTop + 18, 176, 0, 16, 16);
        }
        if (isHovering(mouseX, mouseY, 152, 36, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 152, this.guiTop + 36, 176, 16, 16, 16);
        }
        if (isHovering(mouseX, mouseY, 152, 90, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 152, this.guiTop + 90, 176, 32, 16, 16);
        }
        if (isHovering(mouseX, mouseY, 134, 108, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 134, this.guiTop + 108, 176, 48, 16, 16);
        }
        if (isHovering(mouseX, mouseY, 8, 108, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.guiLeft + 8, this.guiTop + 108, 176, 64, 16, 16);
        }
        
        // Рамка выбранного рецепта
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
        // Тултипы для рецептов в сетке
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= this.filteredRecipes.size()) break;
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8);
            int iy = 17 + 18 * (ind / 8);
            
            if (isHovering(mouseX, mouseY, ix, iy, 18, 18)) {
                RecipeEntry entry = filteredRecipes.get(i);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(entry.icon.getHoverName());
                
                // НОВОЕ: Добавляем информацию о группе
                if (entry.recipe != null) {
                    String pool = entry.recipe.getBlueprintPool();
                    if (pool != null && !pool.isEmpty()) {
                        tooltip.add(Component.empty());
                        tooltip.add(Component.translatable("gui.hbm_m.recipe_from_group")
                            .withStyle(ChatFormatting.AQUA));
                        tooltip.add(Component.literal("  " + pool)
                            .withStyle(ChatFormatting.GOLD));
                    }
                    
                    tooltip.add(Component.empty());
                    com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(entry.recipe, tooltip);
                }
                
                guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(),
                    mouseX, mouseY);
                return;
            }
        }
        
        // Тултип для выбранного рецепта
        if (isHovering(mouseX, mouseY, 151, 71, 18, 18) && this.selectedRecipe != null) {
            if (this.minecraft != null && this.minecraft.level != null) {
                this.minecraft.level.getRecipeManager().byKey(this.selectedRecipe).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        List<Component> tooltip = new ArrayList<>();
                        ItemStack output = assemblerRecipe.getResultItem(null);
                        tooltip.add(output.getHoverName());
                        
                        // НОВОЕ: Добавляем информацию о группе
                        String pool = assemblerRecipe.getBlueprintPool();
                        if (pool != null && !pool.isEmpty()) {
                            tooltip.add(Component.empty());
                            tooltip.add(Component.translatable("gui.hbm_m.recipe_from_group")
                                .withStyle(ChatFormatting.AQUA));
                            tooltip.add(Component.literal("  " + pool)
                                .withStyle(ChatFormatting.GOLD));
                        }
                        
                        tooltip.add(Component.empty());
                        com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(assemblerRecipe, tooltip);
                        guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(),
                            mouseX, mouseY);
                    }
                });
            }
            return;
        }
        
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
        if (isHovering((int)mouseX, (int)mouseY, 152, 18, 16, 16)) {
            playClickSound();
            if (this.pageIndex > 0) this.pageIndex--;
            return true;
        }
        
        if (isHovering((int)mouseX, (int)mouseY, 152, 36, 16, 16)) {
            playClickSound();
            if (this.pageIndex < this.pageCount) this.pageIndex++;
            return true;
        }
        
        if (isHovering((int)mouseX, (int)mouseY, 134, 108, 16, 16)) {
            this.searchBox.setValue("");
            this.onSearch("");
            this.searchBox.setFocused(true);
            return true;
        }
        
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= this.filteredRecipes.size()) break;
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8);
            int iy = 17 + 18 * (ind / 8);
            if (isHovering((int)mouseX, (int)mouseY, ix, iy, 18, 18)) {
                RecipeEntry entry = filteredRecipes.get(i);
                if (!entry.id.equals(selectedRecipe)) {
                    this.selectedRecipe = entry.id;
                } else {
                    this.selectedRecipe = null;
                }
                playClickSound();
                return true;
            }
        }
        
        if (isHovering((int)mouseX, (int)mouseY, 151, 71, 18, 18)) {
            if (this.selectedRecipe != null) {
                this.selectedRecipe = null;
                playClickSound();
                return true;
            }
        }
        
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
        if (keyCode == 257) {
            this.searchBox.setFocused(!this.searchBox.isFocused());
            return true;
        }
        
        if (keyCode == 265) pageIndex--;
        if (keyCode == 264) pageIndex++;
        if (keyCode == 266) pageIndex -= 5;
        if (keyCode == 267) pageIndex += 5;
        if (keyCode == 268) pageIndex = 0;
        if (keyCode == 269) pageIndex = pageCount;
        pageIndex = Math.max(0, Math.min(pageIndex, pageCount));
        
        if (keyCode == 256) {
            applySelection();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void applySelection() {
        ModPacketHandler.INSTANCE.sendToServer(
                new SetAssemblerRecipeC2SPacket(machinePos, selectedRecipe));
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

    @Override
    public void tick() {
        super.tick();
        
        // Проверяем изменение папки через клиента
        if (this.minecraft != null && this.minecraft.level != null) {
            // Получаем BlockEntity напрямую на клиенте
            BlockEntity be = this.minecraft.level.getBlockEntity(this.machinePos);
            if (be instanceof MachineAdvancedAssemblerBlockEntity assembler) {
                ItemStack currentFolder = assembler.getBlueprintFolder();
                
                // Если папка изменилась — перезагружаем рецепты
                if (!ItemStack.matches(lastFolderStack, currentFolder)) {
                    this.lastFolderStack = currentFolder.copy();
                    reloadRecipes();
                }
            }
        }
    }

    private void reloadRecipes() {
        this.allRecipes.clear();
        this.filteredRecipes.clear();
        
        if (this.minecraft != null && this.minecraft.level != null) {
            BlockEntity be = this.minecraft.level.getBlockEntity(this.machinePos);
            if (be instanceof MachineAdvancedAssemblerBlockEntity assembler) {
                List<AssemblerRecipe> available = assembler.getAvailableRecipes();
                
                // Разделяем рецепты на две группы
                List<RecipeEntry> poolRecipes = new ArrayList<>();
                List<RecipeEntry> baseRecipes = new ArrayList<>();
                
                for (AssemblerRecipe recipe : available) {
                    ItemStack icon = recipe.getResultItem(null);
                    RecipeEntry entry = new RecipeEntry(recipe.getId(), icon, recipe);
                    
                    // Если рецепт имеет pool — добавляем в приоритетную группу
                    if (recipe.getBlueprintPool() != null && !recipe.getBlueprintPool().isEmpty()) {
                        poolRecipes.add(entry);
                    } else {
                        baseRecipes.add(entry);
                    }
                }
                
                // Сначала добавляем рецепты с пулом, затем базовые
                allRecipes.addAll(poolRecipes);
                allRecipes.addAll(baseRecipes);
                
                if (this.searchBox != null && !this.searchBox.getValue().isEmpty()) {
                    onSearch(this.searchBox.getValue());
                } else {
                    this.filteredRecipes.addAll(this.allRecipes);
                }
                
                resetPaging();
            }
        }
    }
}
