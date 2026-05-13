package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineCrucibleMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Crucible machine GUI.
 *
 * Progress gauge  — 33 px wide, at (126, 82) relative to GUI origin
 * Heat gauge      — 33 px wide, at (126, 91) relative to GUI origin
 * Recipe button   — 18×18 at (106, 80) relative to GUI origin
 *
 * Material-stack rendering (wasteStack / recipeStack) is stubbed out and will be
 * filled in once the MaterialStack / Mats system is ported to the modern codebase.
 */
public class GUIMachineCrucible extends GuiInfoScreen<MachineCrucibleMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_crucible.png");

    // UV offsets for the gauge sprites (same as legacy: x=176, y=0 for progress; y=5 for heat)
    private static final int GAUGE_U = 176;
    private static final ItemStack RECIPE_BUTTON_FALLBACK = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
    private static final int LIQ_GAUGE_HEIGHT = 101;

    public GUIMachineCrucible(MachineCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    // -------------------------------------------------------------------------
    // Background layer
    // -------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // Base GUI texture
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Progress gauge (0..33 px wide)
        int pGauge = menu.getScaledProgress();
        if (pGauge > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 82, GAUGE_U, 0, pGauge, 5);
        }

        // Heat gauge (0..33 px wide)
        int hGauge = menu.getScaledHeat();
        if (hGauge > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 91, GAUGE_U, 5, hGauge, 5);
        }

        // Legacy parity: show a recipe icon on the recipe selector button.
        guiGraphics.renderItem(RECIPE_BUTTON_FALLBACK, this.leftPos + 107, this.topPos + 81);

        // Liquid storage bars (left = waste placeholder, middle = molten storage)
        int liquidH = menu.getScaledLiquidHeight();
        if (liquidH > 0) {
            int yTop = this.topPos + 115 - liquidH; // bottom aligns near old stack area
            guiGraphics.fill(this.leftPos + 17, yTop, this.leftPos + 62, this.topPos + 115, 0xAA5A2F0F);
            guiGraphics.fill(this.leftPos + 62, yTop, this.leftPos + 107, this.topPos + 115, 0xAAC47A2C);
        }
    }

    // -------------------------------------------------------------------------
    // Labels
    // -------------------------------------------------------------------------

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title (centered)
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        // Inventory label
        guiGraphics.drawString(this.font,
                Component.translatable("container.inventory"),
                8, this.imageHeight - 96 + 2, 4210752, false);
    }

    // -------------------------------------------------------------------------
    // Tooltips / hover
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Progress tooltip
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                125, 81, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", menu.getProgress()) +
                        " / " + String.format(Locale.US, "%,d", menu.getProcessTime()) + " TU"));

        // Heat tooltip
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                125, 90, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", menu.getHeat()) +
                        " / " + String.format(Locale.US, "%,d", menu.getMaxHeat()) + " TU"));

        // Liquid tooltip over left/middle tank area
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
            16, 16, 91, LIQ_GAUGE_HEIGHT, mouseX, mouseY,
            Component.literal(String.format(Locale.US, "%,d", menu.getLiquidStored()) +
                " / " + String.format(Locale.US, "%,d", menu.getLiquidCap()) + " mB"));

        // Recipe button tooltip
        if (isPointInRect(106, 80, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.recipe.setRecipe"), mouseX, mouseY);
        }

        // TODO: drawStackInfo by material type once MaterialStack is ported.
    }

    // -------------------------------------------------------------------------
    // Mouse click — recipe selector
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isPointInRect(106, 80, 18, 18, (int) mouseX, (int) mouseY)) {
            playClickSound();
            // TODO: open recipe selector once CrucibleRecipes is ported
            // GUIScreenRecipeSelector.openSelector(CrucibleRecipes.INSTANCE, ...);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
