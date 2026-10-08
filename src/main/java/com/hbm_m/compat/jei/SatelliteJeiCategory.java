package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.itempool.ItemPool;
import com.hbm_m.itempool.ItemPool.WeightedContent;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.satellite.SatelliteMiner;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code SatelliteHandler}: Bergbausatellit links, sein Beutepool rechts im 6x3-Raster der
 * Amboss-Textur, die Satelliten-Andockstation dazwischen. Jedes Ergebnis traegt wie im Original
 * seine Wahrscheinlichkeit als rote Zeile; mehr als 18 Eintraege wechseln im selben Feld durch.
 */
public class SatelliteJeiCategory implements IRecipeCategory<SatelliteJeiCategory.Recipe> {

    public record Outcome(ItemStack stack, float chance) { }
    public record Recipe(ItemStack satellite, List<Outcome> outputs) { }

    public static final RecipeType<Recipe> RECIPE_TYPE = RecipeType.create(RefStrings.MODID, "satellite", Recipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public SatelliteJeiCategory(IGuiHelper guiHelper) {
        this.background = JeiAnvilTextures.createRecipeBackground(guiHelper);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.SAT_DOCK.get()));
    }

    /** Original {@code getMiningSatellites}; Gegenstaende ohne Ladung ({@code getCargoForItem == null}) fallen weg. */
    public static List<Recipe> recipes() {
        List<Recipe> list = new ArrayList<>();
        ItemStack[] sats = {
                new ItemStack(ModItems.SATELLITE_MINER_ASTRO.get()),
                new ItemStack(ModItems.SATELLITE_MINER_LUNAR.get()),
                new ItemStack(ModItems.SAT_MINER.get()),
                new ItemStack(ModItems.SAT_LUNAR_MINER.get()),
        };
        for (ItemStack sat : sats) {
            String poolName = SatelliteMiner.getCargoForItem(sat);
            if (poolName == null) continue;
            WeightedContent[] pool = ItemPool.getPool(poolName);
            int weight = 0;
            for (WeightedContent e : pool) weight += e.weight;
            List<Outcome> outs = new ArrayList<>();
            for (WeightedContent e : pool) {
                outs.add(new Outcome(e.stack.copy(), weight > 0 ? 100F * e.weight / weight : 0F));
            }
            list.add(new Recipe(sat, outs));
        }
        return list;
    }

    @Override public RecipeType<Recipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatableWithFallback("jei.hbm_m.satellite", "Satellite"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 65; }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 24).addItemStack(recipe.satellite());

        List<Outcome> out = recipe.outputs();
        int overflowCount = out.size() / 18;
        for (int i = 0; i < Math.min(out.size(), 18); i++) {
            List<Outcome> cell = new ArrayList<>();
            for (int j = 0; j < overflowCount + 1 && j * 18 + i < out.size(); j++) cell.add(out.get(j * 18 + i));
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 48 + 18 * (i % 6), 6 + 18 * (i / 6));
            for (Outcome o : cell) slot.addItemStack(o.stack());
            slot.addRichTooltipCallback((view, tooltip) -> {
                ItemStack shown = view.getDisplayedItemStack().orElse(ItemStack.EMPTY);
                for (Outcome o : cell) {
                    if (com.hbm_m.platform.StackNbt.sameItemSameTags(o.stack(), shown)) {
                        tooltip.add(Component.literal(((int) (o.chance() * 10F) / 10F) + "%").withStyle(ChatFormatting.RED));
                        break;
                    }
                }
            });
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 30, 31).addItemStack(new ItemStack(ModBlocks.SAT_DOCK.get()));
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        JeiAnvilRendering.blit(guiGraphics, 11, 23, 113, 105, 18, 18);  // in
        JeiAnvilRendering.blit(guiGraphics, 47, 5, 5, 87, 108, 54);     // out
        JeiAnvilRendering.blit(guiGraphics, 29, 14, 131, 96, 18, 36);   // operation
    }
}
//?} else {
/*public final class SatelliteJeiCategory {
    private SatelliteJeiCategory() {}
}*///?}
