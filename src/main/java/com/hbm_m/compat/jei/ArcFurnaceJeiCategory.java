package com.hbm_m.compat.jei;
//? if forge {

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.inventory.recipes.ArcFurnaceRecipes;
import com.hbm_m.inventory.recipes.ArcFurnaceRecipes.ArcFurnaceRecipe;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Original {@code ArcFurnaceRecipes.getSolidRecipes()} / {@code getFluidRecipes()}: je eine Kategorie fuer den Fest- und
 * den Fluessig-Modus des grossen Lichtbogenofens. Die Liste entsteht aus {@link ArcFurnaceRecipes#getOutput} ueber alle
 * Items, damit Autogenerierung und Ofenrezepte genau so erscheinen wie in der Maschine.
 */
public class ArcFurnaceJeiCategory extends JeiUniversalRecipeCategory<ArcFurnaceJeiCategory.Entry> {

    public record Entry(ItemStack input, List<ItemStack> outputs) { }

    public static final RecipeType<Entry> SOLID = RecipeType.create(RefStrings.MODID, "arc_furnace_solid", Entry.class);
    public static final RecipeType<Entry> FLUID = RecipeType.create(RefStrings.MODID, "arc_furnace_fluid", Entry.class);

    private final boolean liquid;

    public ArcFurnaceJeiCategory(IGuiHelper guiHelper, boolean liquid) {
        super(guiHelper, new ItemStack[] { new ItemStack(ModBlocks.ARC_FURNACE.get()) });
        this.liquid = liquid;
    }

    public static List<Entry> recipes(Level level, boolean liquid) {
        List<Entry> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (ItemScraps.isScrap(stack)) continue;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(stack, liquid, level);
            if (recipe == null) continue;
            List<ItemStack> outs = new ArrayList<>();
            if (!liquid && recipe.solidOutput != null && !recipe.solidOutput.isEmpty()) outs.add(recipe.solidOutput.copy());
            if (liquid && recipe.fluidOutput != null) for (MaterialStack m : recipe.fluidOutput) outs.add(ItemScraps.create(m, true));
            if (!outs.isEmpty()) list.add(new Entry(stack, outs));
        }
        if (liquid) {
            for (NTMMaterial mat : Mats.orderedList) {
                if (mat.smeltable == SmeltingBehavior.SMELTABLE) {
                    MaterialStack ingot = new MaterialStack(mat, MaterialShapes.INGOT.q(1));
                    ItemStack scrap = ItemScraps.create(ingot);
                    if (!scrap.isEmpty()) list.add(new Entry(scrap, List.of(ItemScraps.create(ingot, true))));
                }
            }
        }
        return list;
    }

    @Override public RecipeType<Entry> getRecipeType() { return liquid ? FLUID : SOLID; }
    @Override public Component getTitle() { return Component.translatable("container.machineArcFurnaceLarge"); }
    @Override protected int getInputCount(Entry e) { return 1; }
    @Override protected int getOutputCount(Entry e) { return e.outputs().size(); }
    @Override protected List<List<ItemStack>> getInputStacks(Entry e) { return List.of(List.of(e.input())); }
    @Override protected List<ItemStack> getOutputStacks(Entry e) { return e.outputs(); }
}
//?} else {
/*public final class ArcFurnaceJeiCategory {
    private ArcFurnaceJeiCategory() {}
}*///?}
