package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.inventory.menu.MachineCrucibleMenu;
import com.hbm_m.inventory.recipes.CrucibleRecipes;
import com.hbm_m.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUICrucible}: Abfall- (links) und Rezeptstapel (Mitte) als farbige Saeulen je Material, Fortschritts- und
 * Hitzebalken mit TU-Tooltips, das Rezeptfeld (106,80) oeffnet den Rezeptwaehler und zeigt das gewaehlte Rezept.
 */
public class GUIMachineCrucible extends GuiInfoScreen<MachineCrucibleMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_crucible.png");

    private final MachineCrucibleBlockEntity crucible;

    public GUIMachineCrucible(MachineCrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.crucible = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 214;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (crucible != null) {
            drawStackInfo(g, crucible.wasteStack, x, y, 16, 17);
            drawStackInfo(g, crucible.recipeStack, x, y, 61, 17);

            drawCustomInfoStat(g, x, y, 125, 81, 34, 7, x, y, Component.literal(String.format(Locale.US, "%,d", crucible.progress) + " / " + String.format(Locale.US, "%,d", MachineCrucibleBlockEntity.processTime) + "TU"));
            drawCustomInfoStat(g, x, y, 125, 90, 34, 7, x, y, Component.literal(String.format(Locale.US, "%,d", crucible.heat) + " / " + String.format(Locale.US, "%,d", MachineCrucibleBlockEntity.maxHeat) + "TU"));

            if (leftPos + 106 <= x && leftPos + 106 + 18 > x && topPos + 80 < y && topPos + 80 + 18 >= y) {
                CrucibleRecipe recipe = CrucibleRecipes.get(this.crucible.recipe);
                if (recipe != null) {
                    g.renderComponentTooltip(this.font, recipe.print(Screen.hasShiftDown()), x, y);
                } else {
                    g.renderTooltip(this.font, Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW), x, y);
                }
            }
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (crucible != null && leftPos + 106 <= mx && leftPos + 106 + 18 > mx && topPos + 80 < my && topPos + 80 + 18 >= my) {
            playClickSound();
            Minecraft.getInstance().setScreen(new GUICrucibleRecipeSelector(crucible, this));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (crucible == null) return;

        int pGauge = crucible.progress * 33 / MachineCrucibleBlockEntity.processTime;
        if (pGauge > 0) g.blit(TEXTURE, leftPos + 126, topPos + 82, 176, 0, pGauge, 5);
        int hGauge = crucible.heat * 33 / MachineCrucibleBlockEntity.maxHeat;
        if (hGauge > 0) g.blit(TEXTURE, leftPos + 126, topPos + 91, 176, 5, hGauge, 5);

        CrucibleRecipe recipe = CrucibleRecipes.get(crucible.recipe);
        ItemStack icon = recipe != null ? recipe.getIcon() : new ItemStack(com.hbm_m.item.ModItems.TEMPLATE_FOLDER.get());
        g.renderItem(icon, leftPos + 107, topPos + 81);

        if (!crucible.recipeStack.isEmpty()) drawStack(g, crucible.recipeStack, MachineCrucibleBlockEntity.recipeZCapacity, 62, 97);
        if (!crucible.wasteStack.isEmpty()) drawStack(g, crucible.wasteStack, MachineCrucibleBlockEntity.wasteZCapacity, 17, 97);
    }

    protected void drawStackInfo(GuiGraphics g, List<MaterialStack> stack, int mouseX, int mouseY, int x, int y) {
        List<Component> list = new ArrayList<>();

        if (stack.isEmpty())
            list.add(Component.literal("Empty").withStyle(ChatFormatting.RED));

        for (MaterialStack sta : stack) {
            list.add(sta.material.getLocalizedName().copy().append(": " + Mats.formatAmount(sta.amount, Screen.hasShiftDown())).withStyle(ChatFormatting.YELLOW));
        }

        drawCustomInfoStat(g, mouseX, mouseY, x, y, 36, 81, mouseX, mouseY, list.toArray(new Component[0]));
    }

    protected void drawStack(GuiGraphics g, List<MaterialStack> stack, int capacity, int x, int y) {

        if (stack.isEmpty()) return;

        int lastHeight = 0;
        int lastQuant = 0;

        for (MaterialStack sta : stack) {

            int targetHeight = (lastQuant + sta.amount) * 79 / capacity;

            if (lastHeight == targetHeight) continue; //skip draw calls that would be 0 pixels high

            int offset = sta.material.smeltable == SmeltingBehavior.ADDITIVE ? 34 : 0; //additives use a differnt texture

            int hex = sta.material.moltenColor;
            RenderSystem.setShaderColor((hex >> 16 & 255) / 255F, (hex >> 8 & 255) / 255F, (hex & 255) / 255F, 1F);
            g.blit(TEXTURE, leftPos + x, topPos + y - targetHeight, 176 + offset, 89 - targetHeight, 34, targetHeight - lastHeight);

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1F, 1F, 1F, 0.3F);
            g.blit(TEXTURE, leftPos + x, topPos + y - targetHeight, 176 + offset, 89 - targetHeight, 34, targetHeight - lastHeight);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
}
