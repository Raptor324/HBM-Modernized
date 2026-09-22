package com.hbm_m.inventory.gui;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.client.GuiCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.hbm_m.inventory.menu.AnvilMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.AnvilCraftC2SPacket;
import com.hbm_m.network.AnvilSelectRecipeC2SPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AnvilIngredient;
import com.hbm_m.recipe.AnvilRecipeManager;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GUIAnvil extends AbstractContainerScreen<AnvilMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        RefStrings.MODID, "textures/gui/processing/gui_anvil.png");

    private static final int DISPLAY_SLOTS = 10;

    private final List<AnvilRecipe> originRecipes = new ArrayList<>();
    private final List<AnvilRecipe> filteredRecipes = new ArrayList<>();
    private EditBox searchBox;
    private AnvilRecipe.OverlayType filterState = AnvilRecipe.OverlayType.NONE;
    private int columnOffset;
    private int maxColumnOffset;
    private int selectionIndex = -1;
    private int lastTextWidth;
    private ItemStack hoveredRecipeStack = ItemStack.EMPTY;
    private boolean hoverFilterButton;
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
        // Поле поиска в фокусе сразу после открытия — иначе набор «не работает»
        focusSearch();

        if (menu.blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            cachedServerSelection = menu.blockEntity.getSelectedRecipeId().orElse(null);
        }
        applySearch("");
    }

    private void focusSearch() {
        searchBox.setFocused(true);
        this.setFocused(searchBox);
    }

    private void unfocusSearch() {
        if (this.getFocused() == searchBox) {
            this.setFocused(null);
        } else {
            searchBox.setFocused(false);
        }
    }

    /**
     * В списке GUI — только construction-рецепты (материалы из инвентаря игрока),
     * как в оригинале 1.7.10. Smithing-рецепты (два слота) в список не попадают.
     */
    private void loadAvailableRecipes() {
        originRecipes.clear();
        originRecipes.addAll(
            AnvilRecipeManager.getClientRecipes().stream()
                .filter(recipe -> !recipe.usesMachineInputs())
                .filter(recipe -> menu.blockEntity == null || recipe.canCraftOn(menu.blockEntity.getTier()))
                // Порядок оригинала: порядок регистрации (поле order), затем id
                .sorted(Comparator.comparingInt(AnvilRecipe::getOrder)
                    .thenComparing(recipe -> recipe.getRecipeId().toString()))
                .toList()
        );
    }

    private boolean matchFilter(AnvilRecipe recipe) {
        return filterState == AnvilRecipe.OverlayType.NONE || recipe.getOverlay() == filterState;
    }

    private void applySearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        filteredRecipes.clear();

        for (AnvilRecipe recipe : originRecipes) {
            if (!matchFilter(recipe)) {
                continue;
            }
            if (query.isEmpty()) {
                filteredRecipes.add(recipe);
                continue;
            }
            List<String> searchTerms = buildSearchTerms(recipe);
            for (String term : searchTerms) {
                if (term.contains(query)) {
                    filteredRecipes.add(recipe);
                    break;
                }
            }
        }

        resetPagination();
        if (menu.blockEntity != null) {
            cachedServerSelection = menu.blockEntity.getSelectedRecipeId().orElse(null);
        }
        followSelection = true;
        refreshSelectionFromCache(false);
    }

    private List<String> buildSearchTerms(AnvilRecipe recipe) {
        List<String> terms = new ArrayList<>();

        addOutputSearchTerms(terms, recipe.getResultItemSafe());
        for (AnvilRecipe.ResultEntry entry : recipe.getOutputs()) {
            addOutputSearchTerms(terms, entry.stack());
        }
        addIngredientSearchTerms(terms, recipe.getInputA());
        addIngredientSearchTerms(terms, recipe.getInputB());
        for (AnvilIngredient required : recipe.getInventoryInputs()) {
            addIngredientSearchTerms(terms, required);
        }

        return terms;
    }

    private void addOutputSearchTerms(List<String> terms, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        try {
            terms.add(output.getHoverName().getString().toLowerCase(Locale.ROOT));
        } catch (Exception ex) {
            terms.add("error");
        }
        addTagSearchTerms(terms, output);
    }

    private void addIngredientSearchTerms(List<String> terms, AnvilIngredient ingredient) {
        if (ingredient.isEmpty()) {
            return;
        }
        // Как в оригинале: ищутся все oredict-варианты входа
        terms.addAll(ingredient.displayNames());
        for (ItemStack variant : ingredient.variants()) {
            addTagSearchTerms(terms, variant);
        }
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
            //? if < 1.21.1 {
            searchBox.tick();
            //?}
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
        if (menu.blockEntity == null) return; // тайл может отсутствовать в реплее Flashback
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
            //? if < 1.21.1 {
            if (filteredRecipes.get(i).getId().equals(id)) {
            //?} else {
            /*if (id.equals(RecipeHooks.recipeId(this.minecraft.level.getRecipeManager(), AnvilRecipe.Type.INSTANCE, filteredRecipes.get(i)))) {
            *///?}
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
                focusSearch();
                return true;
            }

            // Рамка поля рисуется 88x16 при (8,108), а сам EditBox — 84x12 при (10,111):
            // клик по рамке тоже должен фокусировать поле
            if (isOverSearchBox(mouseX, mouseY)) {
                focusSearch();
                return true;
            }

            if (searchBox.isFocused()) {
                unfocusSearch();
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

        if (isOverCraftButton(mouseX, mouseY) && isCraftButtonEnabled() && menu.blockEntity != null) {
            playClickSound();
            boolean craftAll = hasShiftDown();
            ModPacketHandler.sendToServer(
                ModPacketHandler.ANVIL_CRAFT,
                new AnvilCraftC2SPacket(menu.blockEntity.getBlockPos(), craftAll));
            return true;
        }

        // Кнопка фильтра категорий (оригинал: цикл All/Construction/Recycling/Smithing)
        if (isOverFilterButton(mouseX, mouseY)) {
            playClickSound();
            AnvilRecipe.OverlayType[] values = AnvilRecipe.OverlayType.values();
            filterState = values[(filterState.ordinal() + 1) % values.length];
            applySearch(searchBox.getValue());
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
        ResourceLocation id = recipe != null ? RecipeHooks.recipeId(this.minecraft.level.getRecipeManager(), AnvilRecipe.Type.INSTANCE, recipe) : null;
        cachedServerSelection = id;
        if (menu.blockEntity == null) return; // тайл может отсутствовать в реплее Flashback
        menu.blockEntity.setSelectedRecipeId(id);
        ModPacketHandler.sendToServer(
            ModPacketHandler.ANVIL_SELECT_RECIPE,
            new AnvilSelectRecipeC2SPacket(menu.blockEntity.getBlockPos(), id));
    }

    //? if < 1.21.1 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Как в оригинале: скролл в любом месте GUI, кроме слотов
        if (isOverGuiArea(mouseX, mouseY) && slotAt(mouseX, mouseY) == null) {
            if (delta > 0) {
                scrollColumns(-1);
            } else if (delta < 0) {
                scrollColumns(1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Как в оригинале: скролл в любом месте GUI, кроме слотов
        if (isOverGuiArea(mouseX, mouseY) && slotAt(mouseX, mouseY) == null) {
            if (scrollY > 0) {
                scrollColumns(-1);
            } else if (scrollY < 0) {
                scrollColumns(1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    *///?}

    private boolean isOverGuiArea(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft && mouseX < guiLeft + imageWidth &&
               mouseY > guiTop && mouseY <= guiTop + imageHeight;
    }

    /** Аналог оригинального getSlotAtPosition: слот под курсором (с 1px запасом, как в 1.7.10). */
    @Nullable
    private Slot slotAt(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            if (mouseX >= sx - 1 && mouseX < sx + 16 + 1 && mouseY >= sy - 1 && mouseY < sy + 16 + 1) {
                return slot;
            }
        }
        return null;
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

    private boolean isOverFilterButton(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 88 && mouseX < guiLeft + 106 &&
               mouseY >= guiTop + 53 && mouseY < guiTop + 71;
    }

    private boolean isOverSearchButton(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 97 && mouseX < guiLeft + 115 &&
               mouseY >= guiTop + 107 && mouseY < guiTop + 125;
    }

    private boolean isOverSearchBox(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        // Видимая рамка поля: (8,108) 88x16
        return mouseX >= guiLeft + 8 && mouseX < guiLeft + 96 &&
               mouseY >= guiTop + 108 && mouseY < guiTop + 124;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        hoveredRecipeStack = ItemStack.EMPTY;
        hoverFilterButton = isOverFilterButton(mouseX, mouseY);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, imageWidth, imageHeight);

        renderSearchFieldBackground(guiGraphics, guiLeft, guiTop);
        drawButtonHighlights(guiGraphics, guiLeft, guiTop, mouseX, mouseY);
        renderFilterButton(guiGraphics, guiLeft, guiTop);
        renderRecipeGrid(guiGraphics, guiLeft, guiTop, mouseX, mouseY);

        renderSidePanel(guiGraphics, guiLeft, guiTop);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component titleText = menu.blockEntity != null
                ? Component.translatable("container.hbm_m.anvil", menu.blockEntity.getTier().getDisplayName())
                : Component.translatable("container.hbm_m.anvil"); // тайл может отсутствовать в реплее Flashback
        int titleWidth = this.font.width(titleText);
        int x = 61 - titleWidth / 2;
        int y = 8;
        guiGraphics.drawString(this.font, titleText, x, y, 0x404040, false);

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);

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

    /**
     * Иконка активного фильтра на кнопке (оригинал: SMITHING → u200, CONSTRUCTION → u218,
     * RECYCLING → u236; при NONE видна иконка «все рецепты», запечённая в фоне).
     */
    private void renderFilterButton(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        int u;
        switch (filterState) {
            case SMITHING -> u = 200;
            case CONSTRUCTION -> u = 218;
            case RECYCLING -> u = 236;
            default -> {
                return;
            }
        }
        guiGraphics.blit(TEXTURE, guiLeft + 88, guiTop + 53, u, hoverFilterButton ? 18 : 0, 18, 18);
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

            // Для разборки показываем входной предмет, для остальных — результат
            ItemStack displayStack;
            if (recipe.isRecycling()) {
                displayStack = recipe.getRecyclingInputStack();
                if (displayStack.isEmpty()) {
                    displayStack = recipe.getDisplayStack(); // Фоллбэк
                }
            } else {
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

        List<AnvilIngredient> machineInputs = new ArrayList<>();
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
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (!hoveredRecipeStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredRecipeStack, mouseX, mouseY);
        }

        if (hoverFilterButton) {
            guiGraphics.renderTooltip(this.font, filterTooltip(), mouseX, mouseY);
        }
    }

    private Component filterTooltip() {
        return switch (filterState) {
            case SMITHING -> Component.translatable("gui.hbm_m.anvil.filter.smithing");
            case CONSTRUCTION -> Component.translatable("gui.hbm_m.anvil.filter.construction");
            case RECYCLING -> Component.translatable("gui.hbm_m.anvil.filter.recycling");
            default -> Component.translatable("gui.hbm_m.anvil.filter.all");
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            // ESC закрывает экран, как в 1.7.10 (textboxKeyTyped не глотал ESC)
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            // Оригинал: сфокусированное поле глотает ВСЕ клавиши — иначе буквы утекают
            // в контейнер (E закрывает GUI, 1-9 двигают хотбар, Q выбрасывает предметы)
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            applySearch(searchBox.getValue());
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_SLASH && searchBox != null) {
            focusSearch();
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

    private int getPlayerItemCount(AnvilIngredient required) {
        if (minecraft == null || minecraft.player == null || required.isEmpty()) {
            return 0;
        }

        int count = 0;
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack invStack = inventory.getItem(i);
            if (required.matches(invStack)) {
                count += invStack.getCount();
            }
        }
        return count;
    }

    private void appendIngredientLines(List<Component> lines, List<AnvilIngredient> stacks, IngredientSource source) {
        for (AnvilIngredient ingredient : stacks) {
            if (ingredient.isEmpty()) {
                continue;
            }

            int owned = source == IngredientSource.MACHINE
                ? getMachineItemCount(ingredient)
                : getPlayerItemCount(ingredient);
            boolean hasEnough = owned >= ingredient.count();

            Component line = Component.literal("> " + ingredient.count() + "x " +
                ingredient.display().getHoverName().getString());
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
                if (!"forge".equals(id.getNamespace()) && !"c".equals(id.getNamespace())
                    && !"neoforge".equals(id.getNamespace())) {
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

    private int getMachineItemCount(AnvilIngredient required) {
        if (required.isEmpty() || menu.blockEntity == null) {
            return 0;
        }

        ModItemStackHandler handler = menu.blockEntity.getItemHandler();
        int count = 0;
        int slotLimit = Math.min(handler.getSlots(), 2);

        for (int slot = 0; slot < slotLimit; slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (required.matches(slotStack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }

    private enum IngredientSource {
        MACHINE,
        PLAYER
    }

    @Override
    public void removed() {
        super.removed();
    }
}
