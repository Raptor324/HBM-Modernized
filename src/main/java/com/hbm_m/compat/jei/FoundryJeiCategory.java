package com.hbm_m.compat.jei;
//? if forge {

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.inventory.recipes.CrucibleRecipes;
import com.hbm_m.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.hbm_m.item.material.ItemMold;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI-Gegenstueck zu {@code CrucibleSmeltingHandler}, {@code CrucibleAlloyingHandler} und {@code CrucibleCastingHandler}
 * des Originals: Schmelzwerte (Item -> fluessiger Schrott), Legierungsrezepte (Schmelzen -> Schmelzen) und Gussformen
 * (Schmelze + Form -> Gussstueck im passenden Becken).
 */
public class FoundryJeiCategory extends JeiUniversalRecipeCategory<FoundryJeiCategory.Entry> {

    /** Ein Eintrag: Eingaben (je Platz Alternativen), Ausgaben, Maschine(n). */
    public record Entry(List<List<ItemStack>> inputs, List<ItemStack> outputs, ItemStack[] machines) { }

    public static final RecipeType<Entry> SMELTING = RecipeType.create(RefStrings.MODID, "crucible_smelting", Entry.class);
    public static final RecipeType<Entry> ALLOYING = RecipeType.create(RefStrings.MODID, "crucible_alloying", Entry.class);
    public static final RecipeType<Entry> CASTING = RecipeType.create(RefStrings.MODID, "crucible_casting", Entry.class);

    private final RecipeType<Entry> type;
    private final Component title;

    public FoundryJeiCategory(IGuiHelper guiHelper, RecipeType<Entry> type, Component title, ItemStack... machines) {
        super(guiHelper, machines);
        this.type = type;
        this.title = title;
    }

    @Override public RecipeType<Entry> getRecipeType() { return type; }
    @Override public Component getTitle() { return title; }
    @Override protected int getInputCount(Entry recipe) { return recipe.inputs().size(); }
    @Override protected int getOutputCount(Entry recipe) { return recipe.outputs().size(); }
    @Override protected List<List<ItemStack>> getInputStacks(Entry recipe) { return recipe.inputs(); }
    @Override protected List<ItemStack> getOutputStacks(Entry recipe) { return recipe.outputs(); }
    @Override protected ItemStack[] getMachines(Entry recipe) { return recipe.machines().length > 0 ? recipe.machines() : machines; }

    // ═══════════════════════════ Rezeptlisten ═══════════════════════════

    /** Original {@code CrucibleRecipes.getSmeltingRecipes()}: jedes Item, das im Tiegel zu Schmelze wird. */
    public static List<Entry> smeltingRecipes() {
        List<Entry> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (ItemScraps.isScrap(stack)) continue;
            List<MaterialStack> mats = Mats.getSmeltingMaterialsFromItem(stack);
            if (mats.isEmpty()) continue;
            List<ItemStack> outs = new ArrayList<>();
            for (MaterialStack mat : mats) {
                if (mat.material.smeltable != SmeltingBehavior.SMELTABLE && mat.material.smeltable != SmeltingBehavior.ADDITIVE) continue;
                ItemStack scrap = ItemScraps.create(mat, true);
                if (!scrap.isEmpty()) outs.add(scrap);
            }
            if (!outs.isEmpty()) list.add(new Entry(List.of(List.of(stack)), outs, new ItemStack[0]));
        }
        return list;
    }

    /** Original {@code CrucibleAlloyingHandler}: die Legierungsrezepte mit ihrem Symbol. */
    public static List<Entry> alloyingRecipes() {
        List<Entry> list = new ArrayList<>();
        for (CrucibleRecipe recipe : CrucibleRecipes.all()) {
            List<List<ItemStack>> in = new ArrayList<>();
            for (MaterialStack mat : recipe.input) in.add(List.of(ItemScraps.create(mat, true)));
            List<ItemStack> out = new ArrayList<>();
            for (MaterialStack mat : recipe.output) out.add(ItemScraps.create(mat, true));
            list.add(new Entry(in, out, new ItemStack[] { new ItemStack(ModBlocks.CRUCIBLE.get()), recipe.getIcon() }));
        }
        return list;
    }

    /** Original {@code CrucibleRecipes.getMoldRecipes()}: Schrott + Form -> Gussstueck im passenden Becken. */
    public static List<Entry> castingRecipes() {
        ItemMold.init();
        List<Entry> list = new ArrayList<>();
        for (NTMMaterial material : Mats.orderedList) {

            if (material.smeltable != SmeltingBehavior.SMELTABLE)
                continue;

            for (ItemMold.Mold mold : ItemMold.molds) {
                ItemStack out = mold.getOutput(material);
                Item moldItem = ItemMold.itemOf(mold);
                if (out != null && moldItem != null) {
                    ItemStack scrap = ItemScraps.create(new MaterialStack(material, mold.getCost()), true);
                    ItemStack basin = new ItemStack(mold.size == 0 ? ModBlocks.FOUNDRY_MOLD.get() : ModBlocks.FOUNDRY_BASIN.get());
                    list.add(new Entry(List.of(List.of(scrap), List.of(new ItemStack(moldItem))), List.of(out), new ItemStack[] { basin }));
                }
            }
        }
        return list;
    }
}
//?} else {
/*public final class FoundryJeiCategory {
    private FoundryJeiCategory() {}
}*///?}
