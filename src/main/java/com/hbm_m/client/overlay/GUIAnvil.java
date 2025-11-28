package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.AnvilMenu;
import com.hbm_m.network.AnvilCraftC2SPacket;
import com.hbm_m.network.AnvilSelectRecipeC2SPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraftforge.items.ItemStackHandler;

public class GUIAnvil extends AbstractContainerScreen<AnvilMenu> {
    
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_anvil.png");
    private static final int DISPLAY_SLOTS = 10;
    
    private final List<AnvilRecipe> originRecipes = new ArrayList<>();
    private final List<AnvilRecipe> filteredRecipes = new ArrayList<>();
    private EditBox searchBox;
    private int columnOffset;
    private int maxColumnOffset;
    private int selectionIndex = -1;
    private int lastTextWidth;
    private ItemStack hoveredRecipeStack = ItemStack.EMPTY;
    private boolean followSelection = true;
    
    @Nullable
    private ResourceLocation cachedServerSelection;
    
    public GUIAnvil(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }
    
    @Override
    protected void init() {
        super.init();
        loadAvailableRecipes();
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        searchBox = new EditBox(this.font, x + 10, y + 111, 84, 12, Component.empty());
        searchBox.setMaxLength(25);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setTextColorUneditable(0xFFFFFFFF);
        searchBox.setResponder(this::applySearch);
        
        // Устанавливаем подсказку
        String hint = Component.translatable("gui.hbm_m.anvil.search_hint").getString();
        if ("gui.hbm_m.anvil.search_hint".equals(hint)) {
            hint = Component.translatable("gui.hbm_m.anvil.search").getString();
        }
        searchBox.setSuggestion(hint);
        searchBox.setValue("");
        
        this.addRenderableWidget(searchBox);
        
        cachedServerSelection = menu.blockEntity.getSelectedRecipeId().orElse(null);
        applySearch("");
    }
    
    private void loadAvailableRecipes() {
        originRecipes.clear();
        RegistryAccess access = getRegistryAccess();
        originRecipes.addAll(
            AnvilRecipeManager.getClientRecipes().stream()
                .filter(recipe -> recipe.canCraftOn(menu.blockEntity.getTier()))
                .sorted(Comparator.comparing(recipe ->
                    recipe.getResultItem(access).getHoverName().getString().toLowerCase(Locale.ROOT)))
                .toList()
        );
    }
    
    private void applySearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        filteredRecipes.clear();
        
        if (query.isEmpty()) {
            filteredRecipes.addAll(originRecipes);
        } else {
            RegistryAccess access = getRegistryAccess();
            for (AnvilRecipe recipe : originRecipes) {
                List<String> searchTerms = buildSearchTerms(recipe, access);
                for (String term : searchTerms) {
                    if (term.contains(query)) {
                        filteredRecipes.add(recipe);
                        break;
                    }
                }
            }
        }
        
        resetPagination();
        cachedServerSelection = menu.blockEntity.getSelectedRecipeId().orElse(null);
        followSelection = true;
        refreshSelectionFromCache(false);
    }
    
    private List<String> buildSearchTerms(AnvilRecipe recipe, RegistryAccess access) {
        List<String> terms = new ArrayList<>();
        
        ItemStack output = recipe.getResultItem(access);
        if (!output.isEmpty()) {
            try {
                terms.add(output.getHoverName().getString().toLowerCase(Locale.ROOT));
            } catch (Exception ex) {
                terms.add("error");
            }
            addTagSearchTerms(terms, output);
        }
        
        for (AnvilRecipe.ResultEntry entry : recipe.getOutputs()) {
            ItemStack stack = entry.stack();
            if (stack.isEmpty()) continue;
            try {
                terms.add(stack.getHoverName().getString().toLowerCase(Locale.ROOT));
            } catch (Exception ex) {
                terms.add("error");
            }
            addTagSearchTerms(terms, stack);
        }
        
        for (ItemStack input : List.of(recipe.getInputA(), recipe.getInputB())) {
            if (input.isEmpty()) continue;
            try {
                terms.add(input.getHoverName().getString().toLowerCase(Locale.ROOT));
            } catch (Exception ex) {
                terms.add("error");
            }
            addTagSearchTerms(terms, input);
        }
        
        for (ItemStack required : recipe.getInventoryInputs()) {
            if (required.isEmpty()) continue;
            try {
                terms.add(required.getHoverName().getString().toLowerCase(Locale.ROOT));
            } catch (Exception ex) {
                terms.add("error");
            }
            addTagSearchTerms(terms, required);
        }
        
        return terms;
    }
    
    private void resetPagination() {
        this.columnOffset = 0;
        this.selectionIndex = -1;
        recalculateBounds();
    }
    
    private void recalculateBounds() {
        int size = filteredRecipes.size();
        double raw = (size - DISPLAY_SLOTS) / 2.0D;
        maxColumnOffset = Math.max(0, (int) Math.ceil(raw));
        columnOffset = Mth.clamp(columnOffset, 0, maxColumnOffset);
        if (selectionIndex >= size) {
            selectionIndex = -1;
        }
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        if (searchBox != null) {
            searchBox.tick();
            if (searchBox.isFocused() && searchBox.getValue().isEmpty()) {
                searchBox.setSuggestion("");
            } else if (!searchBox.isFocused() && searchBox.getValue().isEmpty()) {
                // Возвращаем подсказку когда теряем фокус и поле пустое
                String hint = Component.translatable("gui.hbm_m.anvil.search_hint").getString();
                if ("gui.hbm_m.anvil.search_hint".equals(hint)) {
                    hint = Component.translatable("gui.hbm_m.anvil.search").getString();
                }
                searchBox.setSuggestion(hint);
            }
        }
        
        syncSelectionFromServer();
        if (followSelection) {
            ensureSelectionVisible();
        }
    }
    
    private void syncSelectionFromServer() {
        ResourceLocation serverSelection = menu.blockEntity.getSelectedRecipeId().orElse(null);
        if (!Objects.equals(serverSelection, cachedServerSelection)) {
            cachedServerSelection = serverSelection;
            refreshSelectionFromCache(false);
        }
    }
    
    private void refreshSelectionFromCache(boolean forceDefault) {
        if (filteredRecipes.isEmpty()) {
            selectionIndex = -1;
            followSelection = false;
            return;
        }
        
        if (cachedServerSelection == null) {
            if (forceDefault) {
                selectionIndex = 0;
                followSelection = true;
                notifyServerAboutSelection(filteredRecipes.get(0));
            } else {
                selectionIndex = -1;
                followSelection = false;
            }
            return;
        }
        
        int index = findRecipeIndex(cachedServerSelection);
        if (index >= 0) {
            selectionIndex = index;
            followSelection = true;
        } else if (forceDefault) {
            selectionIndex = 0;
            followSelection = true;
            notifyServerAboutSelection(filteredRecipes.get(0));
        } else {
            selectionIndex = -1;
            followSelection = false;
        }
    }
    
    private void ensureSelectionVisible() {
        if (selectionIndex < 0) {
            return;
        }
        
        int targetColumn = selectionIndex / 2;
        int columnsVisible = DISPLAY_SLOTS / 2;
        int maxVisibleColumn = columnOffset + columnsVisible - 1;
        
        if (targetColumn < columnOffset) {
            columnOffset = targetColumn;
        } else if (targetColumn > maxVisibleColumn) {
            columnOffset = targetColumn - columnsVisible + 1;
        } else {
            return;
        }
        
        columnOffset = Mth.clamp(columnOffset, 0, maxColumnOffset);
    }
    
    private int findRecipeIndex(ResourceLocation id) {
        for (int i = 0; i < filteredRecipes.size(); i++) {
            if (filteredRecipes.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
    
    public void focusRecipe(@Nullable AnvilRecipe target) {
        if (target == null || !originRecipes.contains(target)) {
            return;
        }
        
        searchBox.setValue("");
        applySearch("");
        
        int pos = filteredRecipes.indexOf(target);
        if (pos >= 0) {
            selectionIndex = pos;
            followSelection = true;
            ensureSelectionVisible();
            notifyServerAboutSelection(target);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null) {
            if (searchBox.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(searchBox);
                return true;
            }
            
            if (searchBox.isFocused() && !isMouseOverSearchBox(mouseX, mouseY)) {
                searchBox.setFocused(false);
            }
        }
        
        if (isOverLeftArrow(mouseX, mouseY)) {
            playClickSound();
            scrollColumns(-1);
            return true;
        }
        
        if (isOverRightArrow(mouseX, mouseY)) {
            playClickSound();
            scrollColumns(1);
            return true;
        }
        
        if (isOverCraftButton(mouseX, mouseY) && isCraftButtonEnabled()) {
            playClickSound();
            boolean craftAll = hasShiftDown();
            ModPacketHandler.INSTANCE.sendToServer(
                new AnvilCraftC2SPacket(menu.blockEntity.getBlockPos(), craftAll));
            return true;
        }
        
        if (isOverSearchButton(mouseX, mouseY)) {
            playClickSound();
            applySearch(searchBox.getValue());
            return true;
        }
        
        if (handleRecipeClick(mouseX, mouseY)) {
            playClickSound();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
    
    private void scrollColumns(int delta) {
        columnOffset = Mth.clamp(columnOffset + delta, 0, maxColumnOffset);
        followSelection = false;
    }
    
    private boolean handleRecipeClick(double mouseX, double mouseY) {
        int startIndex = columnOffset * 2;
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        
        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            int recipeIndex = startIndex + slot;
            if (recipeIndex >= filteredRecipes.size()) {
                break;
            }
            
            int col = slot / 2;
            int row = slot % 2;
            int overlayX = guiLeft + 16 + col * 18;
            int overlayY = guiTop + 71 + row * 18;
            
            if (mouseX >= overlayX && mouseX < overlayX + 18 &&
                mouseY >= overlayY && mouseY < overlayY + 18) {
                
                if (selectionIndex == recipeIndex) {
                    selectionIndex = -1;
                    followSelection = false;
                    notifyServerAboutSelection(null);
                } else {
                    selectionIndex = recipeIndex;
                    followSelection = true;
                    notifyServerAboutSelection(filteredRecipes.get(recipeIndex));
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isCraftButtonEnabled() {
        if (selectionIndex < 0 || selectionIndex >= filteredRecipes.size()) {
            return false;
        }
        return !filteredRecipes.get(selectionIndex).usesMachineInputs();
    }
    
    private void notifyServerAboutSelection(@Nullable AnvilRecipe recipe) {
        ResourceLocation id = recipe != null ? recipe.getId() : null;
        cachedServerSelection = id;
        menu.blockEntity.setSelectedRecipeId(id);
        ModPacketHandler.INSTANCE.sendToServer(
            new AnvilSelectRecipeC2SPacket(menu.blockEntity.getBlockPos(), id));
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isOverRecipeGrid(mouseX, mouseY)) {
            if (delta > 0) {
                scrollColumns(-1);
            } else if (delta < 0) {
                scrollColumns(1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    private boolean isOverRecipeGrid(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 16 && mouseX < guiLeft + 106 &&
               mouseY >= guiTop + 71 && mouseY < guiTop + 107;
    }
    
    private boolean isOverLeftArrow(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 7 && mouseX < guiLeft + 16 &&
               mouseY >= guiTop + 71 && mouseY < guiTop + 107;
    }
    
    private boolean isOverRightArrow(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 106 && mouseX < guiLeft + 115 &&
               mouseY >= guiTop + 71 && mouseY < guiTop + 107;
    }
    
    private boolean isOverCraftButton(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 52 && mouseX < guiLeft + 70 &&
               mouseY >= guiTop + 53 && mouseY < guiTop + 71;
    }
    
    private boolean isOverSearchButton(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 97 && mouseX < guiLeft + 115 &&
               mouseY >= guiTop + 107 && mouseY < guiTop + 125;
    }
    
    private boolean isMouseOverSearchBox(double mouseX, double mouseY) {
        if (searchBox == null) return false;
        int x = searchBox.getX();
        int y = searchBox.getY();
        int width = searchBox.getWidth();
        int height = searchBox.getHeight();
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        hoveredRecipeStack = ItemStack.EMPTY;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, imageWidth, imageHeight);
        
        renderSearchFieldBackground(guiGraphics, guiLeft, guiTop);
        drawButtonHighlights(guiGraphics, guiLeft, guiTop, mouseX, mouseY);
        renderRecipeGrid(guiGraphics, guiLeft, guiTop, mouseX, mouseY);
        
        renderSidePanel(guiGraphics, guiLeft, guiTop);
    }
    
    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component titleText = Component.translatable("container.hbm_m.anvil", menu.blockEntity.getTier().getDisplayName());
        int titleWidth = this.font.width(titleText);
        int x = 61 - titleWidth / 2;
        int y = 8;
        guiGraphics.drawString(this.font, titleText, x, y, 0x404040, false);
        
        Component inventoryLabel = Component.translatable("container.inventory");
        guiGraphics.drawString(this.font, inventoryLabel, 8, imageHeight - 96 + 2, 0x404040, false);
        
        // Рисуем текст деталей рецепта (если выбран)
        renderRecipeDetailsText(guiGraphics);
    }
    
    private void drawButtonHighlights(GuiGraphics guiGraphics, int guiLeft, int guiTop, int mouseX, int mouseY) {
        if (isOverLeftArrow(mouseX, mouseY) && columnOffset > 0) {
            guiGraphics.blit(TEXTURE, guiLeft + 7, guiTop + 71, 176, 186, 9, 36);
        }
        
        if (isOverRightArrow(mouseX, mouseY) && columnOffset < maxColumnOffset) {
            guiGraphics.blit(TEXTURE, guiLeft + 106, guiTop + 71, 185, 186, 9, 36);
        }
        
        if (isOverCraftButton(mouseX, mouseY) && isCraftButtonEnabled()) {
            guiGraphics.blit(TEXTURE, guiLeft + 52, guiTop + 53, 176, 150, 18, 18);
        }
        
        if (isOverSearchButton(mouseX, mouseY)) {
            guiGraphics.blit(TEXTURE, guiLeft + 97, guiTop + 107, 176, 168, 18, 18);
        }
    }
    
    private void renderSearchFieldBackground(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        if (searchBox == null) {
            return;
        }
        
        if (searchBox.isFocused()) {
            guiGraphics.blit(TEXTURE, guiLeft + 8, guiTop + 108, 168, 222, 88, 16);
        }
    }
    
    private void renderRecipeGrid(GuiGraphics guiGraphics, int guiLeft, int guiTop, int mouseX, int mouseY) {
        int startIndex = columnOffset * 2;
        
        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            int recipeIndex = startIndex + slot;
            if (recipeIndex >= filteredRecipes.size()) {
                break;
            }
            
            int col = slot / 2;
            int row = slot % 2;
            int itemX = guiLeft + 17 + col * 18;
            int itemY = guiTop + 72 + row * 18;
            int overlayX = itemX - 1;
            int overlayY = itemY - 1;
            
            AnvilRecipe recipe = filteredRecipes.get(recipeIndex);
            
            // ИСПРАВЛЕНИЕ: Определяем иконку в зависимости от типа рецепта
            ItemStack displayStack;
            if (recipe.isRecycling()) {
                // Для разборки - показываем входной предмет
                displayStack = recipe.getRecyclingInputStack();
                if (displayStack.isEmpty()) {
                    displayStack = recipe.getDisplayStack(); // Фоллбэк
                }
            } else {
                // Для обычных рецептов - стандартная иконка
                displayStack = recipe.getDisplayStack();
            }
            
            guiGraphics.renderItem(displayStack, itemX, itemY);
            guiGraphics.renderItemDecorations(this.font, displayStack, itemX, itemY);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            
            int overlayU = 18 + 18 * recipe.getOverlay().ordinal();
            guiGraphics.blit(TEXTURE, overlayX, overlayY, overlayU, 222, 18, 18);
            
            if (recipeIndex == selectionIndex) {
                guiGraphics.pose().translate(0, 0, 10);
                guiGraphics.blit(TEXTURE, overlayX, overlayY, 0, 222, 18, 18);
            }
            
            guiGraphics.pose().popPose();
            
            if (mouseX >= overlayX && mouseX < overlayX + 18 &&
                mouseY >= overlayY && mouseY < overlayY + 18) {
                hoveredRecipeStack = displayStack;
            }
        }
    }
    
    private void renderSidePanel(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        int slide = Mth.clamp(lastTextWidth - 42, 0, 1000);
        
        // СНАЧАЛА рисуем дополнительные сегменты (слева направо)
        int mul = 1;
        while (slide >= 51 * mul) {
            guiGraphics.blit(TEXTURE, guiLeft + 125 + 51 * mul, guiTop + 17, 125, 17, 54, 108);
            mul++;
        }
        
        // ПОТОМ рисуем основной сегмент (справа)
        guiGraphics.blit(TEXTURE, guiLeft + 125 + slide, guiTop + 17, 125, 17, 54, 108);
    }
    
    /**
     * Отрисовка текста деталей рецепта
     */
    private void renderRecipeDetailsText(GuiGraphics guiGraphics) {
        List<Component> lines = buildRecipeDetails();
        
        // Вычисляем ширину текста и обновляем lastTextWidth
        int longest = 0;
        if (!lines.isEmpty()) {
            for (Component line : lines) {
                longest = Math.max(longest, this.font.width(line));
            }
            float scale = 0.5F;
            lastTextWidth = (int) (longest * scale);
        } else {
            lastTextWidth = 0;
        }
        
        // Если нет текста - выходим
        if (lines.isEmpty()) {
            return;
        }
        
        // Рисуем текст
        float scale = 0.5F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.pose().translate(130 / scale, 25 / scale, 0.0F);
        
        int yOffset = 0;
        for (Component line : lines) {
            guiGraphics.drawString(this.font, line, 0, yOffset, 0xFFFFFF, false);
            yOffset += 9;
        }
        
        guiGraphics.pose().popPose();
    }
    
    private List<Component> buildRecipeDetails() {
        if (selectionIndex < 0 || selectionIndex >= filteredRecipes.size() ||
            minecraft == null || minecraft.player == null) {
            return List.of();
        }
        
        AnvilRecipe recipe = filteredRecipes.get(selectionIndex);
        List<Component> lines = new ArrayList<>();
        
        lines.add(Component.translatable("gui.hbm_m.anvil.inputs")
            .withStyle(ChatFormatting.YELLOW));
        
        List<ItemStack> machineInputs = new ArrayList<>();
        if (!recipe.getInputA().isEmpty()) {
            machineInputs.add(recipe.getInputA());
        }
        if (!recipe.getInputB().isEmpty()) {
            machineInputs.add(recipe.getInputB());
        }
        
        if (!machineInputs.isEmpty()) {
            appendIngredientLines(lines, machineInputs, IngredientSource.MACHINE);
        }
        
        if (!recipe.getInventoryInputs().isEmpty()) {
            appendIngredientLines(lines, recipe.getInventoryInputs(), IngredientSource.PLAYER);
        }
        
        lines.add(Component.empty());
        lines.add(Component.translatable("gui.hbm_m.anvil.outputs")
            .withStyle(ChatFormatting.YELLOW));
        
        for (AnvilRecipe.ResultEntry entry : recipe.getOutputs()) {
            ItemStack stack = entry.stack();
            float chance = entry.chance();
            String chanceText = chance < 1.0F ? " (" + (int) (chance * 100) + "%)" : "";
            lines.add(Component.literal("> " + stack.getCount() + "x " +
                stack.getHoverName().getString() + chanceText));
        }
        
        return lines;
    }
    
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        hoveredRecipeStack = ItemStack.EMPTY;
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        if (!hoveredRecipeStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredRecipeStack, mouseX, mouseY);
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            applySearch(searchBox.getValue());
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_SLASH && searchBox != null && !searchBox.isFocused()) {
            searchBox.setFocused(true);
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.charTyped(codePoint, modifiers)) {
            applySearch(searchBox.getValue());
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    private int getPlayerItemCount(ItemStack stack) {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        
        int count = 0;
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack invStack = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(invStack, stack)) {
                count += invStack.getCount();
            }
        }
        return count;
    }
    
    private void appendIngredientLines(List<Component> lines, List<ItemStack> stacks, IngredientSource source) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            
            int owned = source == IngredientSource.MACHINE
                ? getMachineItemCount(stack)
                : getPlayerItemCount(stack);
            boolean hasEnough = owned >= stack.getCount();
            
            Component line = Component.literal("> " + stack.getCount() + "x " +
                stack.getHoverName().getString());
            line = line.copy().withStyle(hasEnough ? ChatFormatting.WHITE : ChatFormatting.RED);
            lines.add(line);
        }
    }
    
    private void addTagSearchTerms(List<String> terms, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        try {
            stack.getItem().builtInRegistryHolder().tags().forEach(tagKey -> {
                ResourceLocation id = tagKey.location();
                if (!"forge".equals(id.getNamespace())) {
                    return;
                }
                
                String full = id.toString().toLowerCase(Locale.ROOT);
                String path = id.getPath().toLowerCase(Locale.ROOT);
                terms.add(full);
                terms.add(path);
                
                for (String part : path.split("[/_]")) {
                    if (!part.isEmpty()) {
                        terms.add(part);
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }
    
    private int getMachineItemCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        
        ItemStackHandler handler = menu.blockEntity.getItemHandler();
        int count = 0;
        int slotLimit = Math.min(handler.getSlots(), 2);
        
        for (int slot = 0; slot < slotLimit; slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }
    
    private enum IngredientSource {
        MACHINE,
        PLAYER
    }
    
    private RegistryAccess getRegistryAccess() {
        return minecraft != null && minecraft.level != null ?
            minecraft.level.registryAccess() : RegistryAccess.EMPTY;
    }
    
    @Override
    public void removed() {
        super.removed();
    }
}
