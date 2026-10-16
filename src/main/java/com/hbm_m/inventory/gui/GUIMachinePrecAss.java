package com.hbm_m.inventory.gui;

import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.client.GuiCompat;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.menu.MachinePrecAssMenu;
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

/**
 * Port of {@code GUIMachinePrecAss} (1.7.10 original, "Precision Assembler"). Per the original's
 * own {@code TileEntityMachinePrecAss} comment ("horribly copy-pasted crap device"), the PrecAss is
 * mechanically a near-clone of the Advanced Assembler (same 176x256 layout, same blueprint-folder
 * recipe selection, same slot coordinates, energy bar and progress bar UVs) - only the multiblock
 * shell/animations differ, which this port omits (see {@link com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity}).
 * This screen mirrors {@link GUIMachineAdvancedAssembler} 1:1 but targets {@link MachinePrecAssMenu}
 * and its own {@code gui_precass.png} texture.
 */
public class GUIMachinePrecAss extends AbstractContainerScreen<MachinePrecAssMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_precass.png");
    *///?} else {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_precass.png");
    //?}

    public GUIMachinePrecAss(MachinePrecAssMenu menu, Inventory playerInventory, Component title) {
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

        // Основная текстура
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Энергия
        long energyStored = this.menu.getEnergyLong();
        long maxEnergy = this.menu.getMaxEnergyLong();
        if (maxEnergy > 0) {
            int energyBarHeight = (int) (energyStored * 61L / maxEnergy);
            if (energyBarHeight > 61) energyBarHeight = 61;

            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - energyBarHeight,
                    176, 61 - energyBarHeight, 16, energyBarHeight);
        }

        // Прогресс крафта
        int progress = this.menu.getProgress();
        if (progress > 0) {
            int maxProgress = this.menu.getMaxProgress();
            if (maxProgress > 0) {
                int progressWidth = (int) Math.ceil(70.0 * progress / maxProgress);
                guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progressWidth, 16);
            }
        }

        // Текущий рецепт
        var precBe = this.menu.getBlockEntity(); // тайл может отсутствовать в реплее Flashback
        ResourceLocation selectedRecipeId = precBe != null ? precBe.getSelectedRecipeId() : null;
        AssemblerRecipe recipe = null;
        if (selectedRecipeId != null && this.minecraft != null && this.minecraft.level != null) {
            recipe = com.hbm_m.platform.recipe.RecipeHooks.getRecipeByKey(this.minecraft.level.getRecipeManager(), selectedRecipeId)
                    .filter(r -> r instanceof AssemblerRecipe)
                    .map(r -> (AssemblerRecipe) r)
                    .orElse(null);
        }

        boolean hasRecipe = recipe != null;
        boolean canProcess = hasRecipe && energyStored >= 100;

        // LEDs
        if (this.menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (hasRecipe) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (canProcess) {
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        // Призрачные предметы в пустых входных слотах
        renderGhostItems(guiGraphics);
    }

    /**
     * Отрисовывает призрачные предметы в пустых входных слотах.
     * Группирует одинаковые ингредиенты и показывает суммарное количество.
     */
    private void renderGhostItems(GuiGraphics guiGraphics) {
        var be = this.menu.getBlockEntity();
        // тайл может отсутствовать в реплее Flashback
        NonNullList<ItemStack> ghostItems = be != null ? be.getGhostItems() : NonNullList.create();

        if (ghostItems.isEmpty()) {
            return;
        }

        java.util.Map<ItemStack, Integer> groupedItems = new java.util.LinkedHashMap<>();
        for (ItemStack stack : ghostItems) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack found = null;
            for (ItemStack key : groupedItems.keySet()) {
                if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(key, stack)) {
                    found = key;
                    break;
                }
            }

            if (found != null) {
                groupedItems.put(found, groupedItems.get(found) + stack.getCount());
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                groupedItems.put(copy, stack.getCount());
            }
        }

        int inputSlotsStart = 4;
        int inputSlotsCount = 12;

        int slotOffset = 0;
        for (java.util.Map.Entry<ItemStack, Integer> entry : groupedItems.entrySet()) {
            if (slotOffset >= inputSlotsCount) {
                break;
            }

            ItemStack ghostStack = entry.getKey().copy();
            ghostStack.setCount(entry.getValue());

            int slotIndex = inputSlotsStart + slotOffset;
            if (slotIndex >= this.menu.slots.size()) break;

            net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);

            if (!slot.hasItem()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 100);

                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;

                guiGraphics.renderItem(ghostStack, x, y);

                if (ghostStack.getCount() > 1) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    guiGraphics.renderItemDecorations(this.font, ghostStack, x, y);
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();

                guiGraphics.pose().popPose();
            }

            slotOffset++;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderTooltip(guiGraphics, pMouseX, pMouseY);

        // Подсказка для шкалы энергии
        if (isMouseOver(pMouseX, pMouseY, 152, 18, 16, 61)) {
            List<Component> tooltip = new ArrayList<>();

            long energy = this.menu.getEnergyLong();
            long maxEnergy = this.menu.getMaxEnergyLong();

            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);

            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE")
                    .withStyle(ChatFormatting.GREEN));

            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pMouseX, pMouseY);
        }

        // Подсказка для кнопки выбора рецепта
        if (isMouseOver(pMouseX, pMouseY, 7, 125, 18, 18)) {
            var be = this.menu.getBlockEntity(); // тайл может отсутствовать в реплее Flashback
            ResourceLocation selectedRecipeId = be != null ? be.getSelectedRecipeId() : null;
            if (selectedRecipeId == null) {
                guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    pMouseX, pMouseY);
            } else if (this.minecraft != null && this.minecraft.level != null) {
                com.hbm_m.platform.recipe.RecipeHooks.getRecipeByKey(this.minecraft.level.getRecipeManager(), selectedRecipeId).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        List<Component> tooltip = new ArrayList<>();

                        ItemStack output = assemblerRecipe.getResultItem(null);
                        tooltip.add(output.getHoverName());

                        com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(assemblerRecipe, tooltip);

                        guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(),
                                                pMouseX, pMouseY);
                    }
                });
            } else {
                guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    pMouseX, pMouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver((int) mouseX, (int) mouseY, 7, 125, 18, 18)) {
            openRecipeSelector();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openRecipeSelector() {
        if (this.minecraft == null || this.minecraft.level == null) return;

        var be = this.menu.getBlockEntity();
        // тайл может отсутствовать в реплее Flashback
        if (be == null) return;
        ResourceLocation currentRecipe = be.getSelectedRecipeId();

        this.minecraft.setScreen(new GUIScreenRecipeSelector(
            be.getBlockPos(),
            currentRecipe,
            this
        ));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name,
                            70 - this.font.width(name) / 2, 6, 0x404040, false);

        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                            8, this.imageHeight - 96 + 2, 0x404040, false);

        if (this.minecraft != null && this.minecraft.screen == this) {
            var labelBe = this.menu.getBlockEntity(); // тайл может отсутствовать в реплее Flashback
            ResourceLocation selectedRecipeId = labelBe != null ? labelBe.getSelectedRecipeId() : null;
            if (selectedRecipeId != null && this.minecraft.level != null) {
                com.hbm_m.platform.recipe.RecipeHooks.getRecipeByKey(this.minecraft.level.getRecipeManager(), selectedRecipeId).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        ItemStack icon = assemblerRecipe.getResultItem(null);
                        guiGraphics.renderItem(icon, 8, 126);
                    }
                });
            } else {
                ItemStack folderIcon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
                guiGraphics.renderItem(folderIcon, 8, 126);
            }
        }
    }

    private boolean isMouseOver(int pMouseX, int pMouseY, int pX, int pY, int pWidth, int pHeight) {
        return pMouseX >= this.leftPos + pX && pMouseX < this.leftPos + pX + pWidth &&
                pMouseY >= this.topPos + pY && pMouseY < this.topPos + pY + pHeight;
    }
}
