package com.hbm_m.inventory.gui;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.SetChemPlantRecipeC2SPacket;
import com.hbm_m.recipe.ChemicalPlantRecipes;
import com.hbm_m.recipe.ChemicalPlantRecipes.ChemicalRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GUIChemicalPlantRecipeSelector extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_recipe_selector.png");

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

    @Nullable
    private String selectedRecipeId;

    private record RecipeEntry(String id, String displayName, ChemicalRecipe recipe) {}

    public GUIChemicalPlantRecipeSelector(BlockPos machinePos, @Nullable String currentRecipeId, Screen parentScreen) {
        super(Component.translatable("gui.hbm_m.chemplant_recipe_selector"));
        this.machinePos = machinePos;
        this.parentScreen = parentScreen;
        this.selectedRecipeId = currentRecipeId;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

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

    private void reloadRecipes() {
        this.allRecipes.clear();
        this.filteredRecipes.clear();

        List<ChemicalRecipe> recipes = ChemicalPlantRecipes.getRecipeList();
        for (ChemicalRecipe recipe : recipes) {
            allRecipes.add(new RecipeEntry(recipe.getId(), recipe.getDisplayName(), recipe));
        }
        filteredRecipes.addAll(allRecipes);
        resetPaging();
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
            String lower = query.toLowerCase(Locale.ROOT);
            for (RecipeEntry entry : allRecipes) {
                if (entry.displayName.toLowerCase(Locale.ROOT).contains(lower)
                        || entry.id.toLowerCase(Locale.ROOT).contains(lower)) {
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

        // Highlight selected recipe
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= filteredRecipes.size()) break;
            int ind = i - pageIndex * 8;
            RecipeEntry entry = filteredRecipes.get(i);
            if (entry.id.equals(this.selectedRecipeId)) {
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
            ItemStack icon = getRecipeIcon(entry.recipe);
            guiGraphics.renderItem(icon, x, y);
        }

        // Render selected recipe icon in the preview area
        if (this.selectedRecipeId != null) {
            for (RecipeEntry entry : filteredRecipes) {
                if (entry.id.equals(this.selectedRecipeId)) {
                    ItemStack icon = getRecipeIcon(entry.recipe);
                    guiGraphics.renderItem(icon, this.guiLeft + 152, this.guiTop + 72);
                    break;
                }
            }
        }
    }

    private ItemStack getRecipeIcon(ChemicalRecipe recipe) {
        if (!recipe.getItemOutputs().isEmpty()) {
            for (ItemStack output : recipe.getItemOutputs()) {
                if (!output.isEmpty()) return output;
            }
        }
        if (!recipe.getItemInputs().isEmpty()) {
            List<ItemStack> display = recipe.getItemInputs().get(0).getDisplayStacks();
            if (!display.isEmpty()) return display.get(0);
        }
        return ItemStack.EMPTY;
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= this.filteredRecipes.size()) break;
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8);
            int iy = 17 + 18 * (ind / 8);

            if (isHovering(mouseX, mouseY, ix, iy, 18, 18)) {
                RecipeEntry entry = filteredRecipes.get(i);
                List<Component> tooltip = buildRecipeTooltip(entry);
                guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
                return;
            }
        }

        if (isHovering(mouseX, mouseY, 151, 71, 18, 18) && this.selectedRecipeId != null) {
            for (RecipeEntry entry : allRecipes) {
                if (entry.id.equals(this.selectedRecipeId)) {
                    List<Component> tooltip = buildRecipeTooltip(entry);
                    guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
                    return;
                }
            }
        }

        if (isHovering(mouseX, mouseY, 152, 90, 16, 16)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Close").withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }
        if (isHovering(mouseX, mouseY, 134, 108, 16, 16)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Clear search").withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }
    }

    private List<Component> buildRecipeTooltip(RecipeEntry entry) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(entry.displayName).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal(entry.id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());

        ChemicalRecipe r = entry.recipe;
        if (!r.getItemInputs().isEmpty() || !r.getFluidInputs().isEmpty()) {
            tooltip.add(Component.literal("Inputs:").withStyle(ChatFormatting.YELLOW));
            for (var input : r.getItemInputs()) {
                List<ItemStack> display = input.getDisplayStacks();
                String name = display.isEmpty() ? "?" : display.get(0).getHoverName().getString();
                tooltip.add(Component.literal("  " + input.getCount() + "x " + name).withStyle(ChatFormatting.GRAY));
            }
            for (FluidStack fluid : r.getFluidInputs()) {
                tooltip.add(Component.literal("  " + fluid.getAmount() + " mB ").withStyle(ChatFormatting.AQUA)
                        .append(fluid.getDisplayName()));
            }
        }

        if (!r.getItemOutputs().isEmpty() || !r.getFluidOutputs().isEmpty()) {
            tooltip.add(Component.literal("Outputs:").withStyle(ChatFormatting.GREEN));
            for (ItemStack output : r.getItemOutputs()) {
                if (output.isEmpty()) continue;
                tooltip.add(Component.literal("  " + output.getCount() + "x ").withStyle(ChatFormatting.GRAY)
                        .append(output.getHoverName()));
            }
            for (FluidStack fluid : r.getFluidOutputs()) {
                tooltip.add(Component.literal("  " + fluid.getAmount() + " mB ").withStyle(ChatFormatting.AQUA)
                        .append(fluid.getDisplayName()));
            }
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Duration: " + r.getDuration() + " ticks").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Power: " + r.getPowerConsumption() + " HE/t").withStyle(ChatFormatting.RED));

        return tooltip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering((int) mouseX, (int) mouseY, 152, 18, 16, 16)) {
            playClickSound();
            if (this.pageIndex > 0) this.pageIndex--;
            return true;
        }

        if (isHovering((int) mouseX, (int) mouseY, 152, 36, 16, 16)) {
            playClickSound();
            if (this.pageIndex < this.pageCount) this.pageIndex++;
            return true;
        }

        if (isHovering((int) mouseX, (int) mouseY, 134, 108, 16, 16)) {
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
            if (isHovering((int) mouseX, (int) mouseY, ix, iy, 18, 18)) {
                RecipeEntry entry = filteredRecipes.get(i);
                if (!entry.id.equals(selectedRecipeId)) {
                    this.selectedRecipeId = entry.id;
                } else {
                    this.selectedRecipeId = null;
                }
                playClickSound();
                return true;
            }
        }

        if (isHovering((int) mouseX, (int) mouseY, 151, 71, 18, 18)) {
            if (this.selectedRecipeId != null) {
                this.selectedRecipeId = null;
                playClickSound();
                return true;
            }
        }

        if (isHovering((int) mouseX, (int) mouseY, 152, 90, 16, 16)) {
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
        if (keyCode == 257) { // ENTER
            this.searchBox.setFocused(!this.searchBox.isFocused());
            return true;
        }

        if (keyCode == 265) pageIndex--; // UP
        if (keyCode == 264) pageIndex++; // DOWN
        if (keyCode == 266) pageIndex -= 5; // PAGE_UP
        if (keyCode == 267) pageIndex += 5; // PAGE_DOWN
        if (keyCode == 268) pageIndex = 0; // HOME
        if (keyCode == 269) pageIndex = pageCount; // END
        pageIndex = Math.max(0, Math.min(pageIndex, pageCount));

        if (keyCode == 256) { // ESC
            applySelection();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applySelection() {
        ModPacketHandler.INSTANCE.sendToServer(
                new SetChemPlantRecipeC2SPacket(machinePos, selectedRecipeId));
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
