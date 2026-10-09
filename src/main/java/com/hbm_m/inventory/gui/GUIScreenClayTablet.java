package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.Random;

import com.hbm_m.inventory.recipes.PedestalRecipes;
import com.hbm_m.inventory.recipes.PedestalRecipes.PedestalExtraCondition;
import com.hbm_m.inventory.recipes.PedestalRecipes.PedestalInput;
import com.hbm_m.inventory.recipes.PedestalRecipes.PedestalRecipe;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.special.ItemClayTablet;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUIScreenClayTablet}: Tontafel 142x84 aus {@code guide_pedestal.png}; mit {@code tabletSeed} wird ein
 * Sockelrezept des Satzes {@code tabletMeta} gewaehlt und jedes Feld mit Wahrscheinlichkeit 50 % (Meta 1: 25 %)
 * aufgedeckt, darueber Ausgabe, Name und das Sonderbedingungs-Symbol (Voll-/Neumond, Sonne).
 */
public class GUIScreenClayTablet extends Screen {

    protected int xSize = 142;
    protected int ySize = 84;
    protected int guiLeft;
    protected int guiTop;
    protected int tabletMeta;

    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/guide_pedestal.png");

    public GUIScreenClayTablet(int tabletMeta) {
        super(Component.empty());
        this.tabletMeta = tabletMeta;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        this.drawGuiContainerBackgroundLayer(g);
    }

    protected void drawGuiContainerBackgroundLayer(GuiGraphics g) {

        Player player = Minecraft.getInstance().player;
        ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        if (held.getItem() instanceof ItemClayTablet tablet) tabletMeta = tablet.tabletMeta;

        int tabletOffset = tabletMeta == 1 ? 84 : 0;
        int iconOffset = tabletMeta == 1 ? 16 : 0;
        float revealChance = tabletMeta == 1 ? 0.25F : 0.5F;
        g.blit(texture, guiLeft, guiTop, 0, tabletOffset, xSize, ySize);

        ArrayList<PedestalRecipe> recipeSet = PedestalRecipes.recipeSets[Math.abs(tabletMeta) % PedestalRecipes.recipeSets.length];

        if (!held.isEmpty() && PlatformHooks.contains(held, "tabletSeed") && !recipeSet.isEmpty()) {
            Random rand = new Random(PlatformHooks.getLong(held, "tabletSeed"));
            PedestalRecipe recipe = recipeSet.get(rand.nextInt(recipeSet.size()));

            if (recipe.extra == PedestalExtraCondition.FULL_MOON) g.blit(texture, guiLeft + 120, guiTop + 62, 142 + iconOffset, 32, 16, 16);
            if (recipe.extra == PedestalExtraCondition.NEW_MOON) g.blit(texture, guiLeft + 120, guiTop + 62, 142 + iconOffset, 48, 16, 16);
            if (recipe.extra == PedestalExtraCondition.SUN) g.blit(texture, guiLeft + 120, guiTop + 62, 142 + iconOffset, 64, 16, 16);

            for (int l = 0; l < 3; l++) {
                for (int r = 0; r < 3; r++) {
                    if (rand.nextFloat() > revealChance) {
                        g.blit(texture, guiLeft + 7 + r * 27, guiTop + 7 + l * 27, 142 + iconOffset, 16, 16, 16);
                    } else {

                        PedestalInput ingredient = r + l * 3 < recipe.input.length ? recipe.input[r + l * 3] : null;

                        if (ingredient == null) {
                            g.blit(texture, guiLeft + 7 + r * 27, guiTop + 7 + l * 27, 142 + iconOffset, 0, 16, 16);
                            continue;
                        }

                        ItemStack input = ingredient.toStack();
                        if (input.isEmpty()) input = new ItemStack(ModItems.NOTHING.get());

                        g.renderItem(input, guiLeft + 7 + r * 27, guiTop + 7 + l * 27);
                        g.renderItemDecorations(this.font, input, guiLeft + 7 + r * 27, guiTop + 7 + l * 27, input.getCount() > 1 ? (input.getCount() + "") : null);
                    }
                }
            }

            ItemStack output = recipe.getOutput();
            if (output == null) output = ItemStack.EMPTY;

            g.renderItem(output, guiLeft + xSize / 2 - 8, guiTop - 20);
            g.renderItemDecorations(this.font, output, guiLeft + xSize / 2 - 8, guiTop - 20, output.getCount() > 1 ? (output.getCount() + "") : null);

            String label = output.getHoverName().getString();
            g.drawString(this.font, label, guiLeft + (xSize - this.font.width(label)) / 2, guiTop - 30, 0xffffff, false);

        } else {

            for (int l = 0; l < 3; l++) {
                for (int r = 0; r < 3; r++) {
                    g.blit(texture, guiLeft + 7 + r * 27, guiTop + 7 + l * 27, 142 + iconOffset, 16, 16, 16);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
