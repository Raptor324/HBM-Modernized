package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.menu.MachineCrucibleMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.ScrapItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Порт {@code GUICrucible} (1.7.10): столбцы расплава рисуются текстурой GUI
 * (колонка u=176, добавки — u=176+34) с тинтом цвета материала, аддитивным
 * блендингом SRC_ALPHA/ONE и полупрозрачным оверлеем 0.3; тултипы — разбивка
 * по материалам (порт drawStackInfo / Mats.formatAmount); иконка рецепта на
 * (107,81), клик по (106,80) 18×18 открывает селектор рецептов.
 */
public class GUIMachineCrucible extends GuiInfoScreen<MachineCrucibleMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_crucible.png");

    // drawStack: recipeStack в (62,97), wasteStack в (17,97), бар 34×79
    private static final int BAR_W = 34, BAR_MAX_H = 79, BAR_ANCHOR_Y = 97;
    private static final int RECIPE_BAR_X = 62, WASTE_BAR_X = 17;
    // drawStackInfo: хитбоксы 36×81 — waste (16,17), recipe (61,17)
    private static final int INFO_W = 36, INFO_H = 81;
    private static final int WASTE_INFO_X = 16, RECIPE_INFO_X = 61, INFO_Y = 17;

    private static final int PROGRESS_X = 126, PROGRESS_Y = 82, HEAT_Y = 91, GAUGE_W = 33, GAUGE_H = 5, GAUGE_U = 176;
    private static final int SELECTOR_X = 106, SELECTOR_Y = 80, SELECTOR_SIZE = 18;

    private final MachineCrucibleMenu menu;

    public GUIMachineCrucible(MachineCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int pGauge = menu.getScaledProgress();
        if (pGauge > 0) {
            g.blit(TEXTURE, this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y, GAUGE_U, 0, pGauge, GAUGE_H);
        }
        int hGauge = menu.getScaledHeat();
        if (hGauge > 0) {
            g.blit(TEXTURE, this.leftPos + PROGRESS_X, this.topPos + HEAT_Y, GAUGE_U, 5, hGauge, GAUGE_H);
        }

        // Иконка текущего рецепта (или папка шаблонов) — renderItem на (107,81)
        MachineCrucibleBlockEntity be = menu.blockEntity;
        ItemStack selectorIcon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
        if (be != null && be.hasRecipe() && this.minecraft != null && this.minecraft.level != null) {
            com.hbm_m.recipe.MoltenAlloyRecipe loaded = be.getLoadedRecipe(this.minecraft.level);
            if (loaded != null) {
                selectorIcon = GUIScreenRecipeSelector.crucibleRecipeIcon(loaded);
            }
        }
        g.renderItem(selectorIcon, this.leftPos + SELECTOR_X + 1, this.topPos + SELECTOR_Y + 1);

        // Столбцы расплава (в конце background layer, поверх иконки — как в оригинале)
        if (be != null && this.minecraft != null && this.minecraft.level != null) {
            if (!be.recipeStack.isEmpty()) drawStack(g, be.recipeStack, be.getRecipeCap(), RECIPE_BAR_X, BAR_ANCHOR_Y);
            if (!be.wasteStack.isEmpty()) drawStack(g, be.wasteStack, be.getWasteCap(), WASTE_BAR_X, BAR_ANCHOR_Y);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /**
     * Порт {@code GUICrucible.drawStack}: сегменты снизу вверх; цвет материала
     * через shaderColor, блендинг SRC_ALPHA/ONE, затем белый оверлей alpha 0.3.
     * Добавки (ADDITIVE) рисуются по смещённой текстурной колонке +34.
     */
    private void drawStack(GuiGraphics g, List<MaterialStack> stacks, int capacity, int x, int y) {
        int lastHeight = 0;
        int lastQuant = 0;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        for (MaterialStack sta : stacks) {
            int targetHeight = (lastQuant + sta.amount) * BAR_MAX_H / capacity;
            if (lastHeight == targetHeight) continue; // сегменты нулевой высоты не рисуем
            int offset = sta.type.isAdditive() ? 34 : 0;

            int color = sta.type.color;
            float r = ((color >> 16) & 0xFF) / 255F;
            float gr = ((color >> 8) & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;

            RenderSystem.setShaderColor(r, gr, b, 1f);
            g.blit(TEXTURE, this.leftPos + x, this.topPos + y - targetHeight,
                    176 + offset, 89 - targetHeight, BAR_W, targetHeight - lastHeight);
            RenderSystem.setShaderColor(1F, 1F, 1F, 0.3F);
            g.blit(TEXTURE, this.leftPos + x, this.topPos + y - targetHeight,
                    176 + offset, 89 - targetHeight, BAR_W, targetHeight - lastHeight);

            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /** Порт drawStackInfo: "Empty" красным или построчный список материалов с количеством. */
    private void drawStackInfo(GuiGraphics g, List<MaterialStack> stack, int x, int y, double mouseX, double mouseY) {
        List<Component> list = new ArrayList<>();
        if (stack.isEmpty()) {
            list.add(Component.translatable("gui.hbm_m.crucible.empty").withStyle(ChatFormatting.RED));
        }
        boolean shift = hasShiftDown();
        for (MaterialStack sta : stack) {
            list.add(Component.literal("").withStyle(ChatFormatting.YELLOW)
                    .append(GUIScreenRecipeSelector.materialNameOf(sta))
                    .append(Component.literal(": " + ScrapItem.formatAmount(
                            (int) ((long) sta.amount * ScrapItem.QUANTA_PER_INGOT / MaterialStack.MB_PER_INGOT),
                            shift).getString())));
        }
        drawCustomInfoStat(g, (int) mouseX, (int) mouseY,
                x, y, INFO_W, INFO_H, (int) mouseX, (int) mouseY,
                list.toArray(new Component[0]));
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        MachineCrucibleBlockEntity be = menu.blockEntity;
        if (be == null) return;

        drawStackInfo(g, be.wasteStack, WASTE_INFO_X, INFO_Y, mouseX, mouseY);
        drawStackInfo(g, be.recipeStack, RECIPE_INFO_X, INFO_Y, mouseX, mouseY);

        drawCustomInfoStat(g, mouseX, mouseY, 125, 81, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", menu.getProgress()) + " / "
                        + String.format(Locale.US, "%,d", menu.getProcessTime()) + "TU"));
        drawCustomInfoStat(g, mouseX, mouseY, 125, 90, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", menu.getHeat()) + " / "
                        + String.format(Locale.US, "%,d", menu.getMaxHeat()) + "TU"));

        // Тултип селектора: print() рецепта или подсказка выбора (порт drawCreativeTabHoveringText)
        if (isOverSelector(mouseX, mouseY) && this.minecraft != null && this.minecraft.level != null) {
            if (be.hasRecipe() && this.minecraft != null && this.minecraft.level != null) {
                com.hbm_m.recipe.MoltenAlloyRecipe loaded = be.getLoadedRecipe(this.minecraft.level);
                if (loaded != null) {
                    List<Component> tip = new ArrayList<>(List.of(GUIScreenRecipeSelector.recipeName(loaded)));
                    GUIScreenRecipeSelector.recipePrint(loaded, tip);
                    g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
                }
            } else if (be.hasRecipe()) {
                // no-op: recipe id не резолвится на клиенте
            } else {
                g.renderComponentTooltip(this.font,
                        List.of(Component.translatable("gui.hbm_m.crucible.recipe.set").withStyle(ChatFormatting.YELLOW)),
                        mouseX, mouseY);
            }
        }
    }

    private boolean isOverSelector(double mouseX, double mouseY) {
        return this.leftPos + SELECTOR_X <= mouseX && this.leftPos + SELECTOR_X + SELECTOR_SIZE > mouseX
                && this.topPos + SELECTOR_Y < mouseY && this.topPos + SELECTOR_Y + SELECTOR_SIZE >= mouseY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Порт mouseClicked: открытие селектора рецептов (index 0)
        if (isOverSelector(mouseX, mouseY) && menu.blockEntity != null && this.minecraft != null) {
            ResourceLocation current = null;
            String sel = menu.blockEntity.getSelectedRecipe();
            if (sel != null && !"null".equals(sel)) {
                current = ResourceLocation.tryParse(sel);
            }
            this.minecraft.setScreen(new GUIScreenRecipeSelector(menu.blockEntity.getBlockPos(), current, this));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, delta);
        super.render(g, mouseX, mouseY, delta);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        g.drawString(this.font, this.playerInventoryTitle,
                8, this.inventoryLabelY, 0x404040, false);
    }
}
