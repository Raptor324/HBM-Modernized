package com.hbm_m.recipe;

import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.item.material.ItemCastMold;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Central recipe registry for the crucible machine.
 *
 * <p>Modern port of legacy {@code CrucibleRecipes extends GenericRecipes<CrucibleRecipe>}.
 * The three recipe types are held in separate dedicated registries, and this class
 * just wires their JEI-facing display representations up from the same data the
 * actual game logic uses:
 * <ul>
 *   <li>{@link CrucibleSmeltingRecipes} — item → molten material (used by {@code MachineCrucibleBlockEntity})</li>
 *   <li>{@link MoltenAlloyRecipes}      — molten material combinations → molten material (used by {@code MachineCrucibleBlockEntity})</li>
 *   <li>{@link MoldCastingRecipes}      — molten material + mold → item (used by {@code MachineFoundryBasinBlockEntity})</li>
 * </ul>
 * {@link CrucibleAlloyingRecipes} and {@link CrucibleMoldRecipes} are pure ItemStack-based
 * JEI display mirrors of {@link MoltenAlloyRecipes} / {@link MoldCastingRecipes}, generated here.
 */
public class CrucibleRecipes {

    /** Singleton — mirrors legacy {@code CrucibleRecipes.INSTANCE}. */
    public static final CrucibleRecipes INSTANCE = new CrucibleRecipes();
    private static boolean defaultsRegistered = false;

    private CrucibleRecipes() {}

    /**
     * Registers all built-in crucible recipes and their JEI display mirrors.
     * Call once during mod initialisation (e.g. from the common setup event).
     */
    public void registerDefaults() {
        if (defaultsRegistered) return;
        defaultsRegistered = true;

        CrucibleSmeltingRecipes.registerDefaults();
        MoltenAlloyRecipes.registerDefaults();

        registerAlloyingDisplay();
        registerMoldDisplay();
    }

    /** Mirrors {@link MoltenAlloyRecipes} as ItemStack-based recipes for JEI. */
    private void registerAlloyingDisplay() {
        for (MoltenAlloyRecipe recipe : MoltenAlloyRecipes.getRecipes()) {
            ItemStack[] inputs  = new ItemStack[recipe.inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                MaterialStack in = recipe.inputs[i];
                inputs[i] = scaledIcon(in.type, in.amount);
            }
            ItemStack[] outputs = new ItemStack[recipe.outputs.length];
            for (int i = 0; i < outputs.length; i++) {
                MaterialStack out = recipe.outputs[i];
                outputs[i] = scaledIcon(out.type, out.amount);
            }

            CrucibleAlloyingRecipes.register(new CrucibleAlloyingRecipe(recipe.name)
                    .setup(recipe.frequency, outputs.length > 0 ? outputs[0] : ItemStack.EMPTY)
                    .inputs(inputs)
                    .outputs(outputs));
        }
    }

    /** Displays a material as a stack of its representative icon item, scaled by mb amount (roughly, for JEI only). */
    private static ItemStack scaledIcon(MaterialType mat, int amountMb) {
        ItemStack icon = switch (mat) {
            // Molten-only intermediates with no castable output: show their real-world smelting source instead.
            case CARBON   -> new ItemStack(net.minecraft.world.item.Items.COAL);
            case REDSTONE -> new ItemStack(net.minecraft.world.item.Items.REDSTONE);
            default       -> MoldCastingRecipes.getMaterialIcon(mat);
        };
        if (icon.isEmpty()) return ItemStack.EMPTY;
        int count = Math.max(1, Math.min(64, Math.round(amountMb / (float) MaterialStack.MB_PER_INGOT)));
        ItemStack copy = icon.copy();
        copy.setCount(count);
        return copy;
    }

    /** Mirrors {@link MoldCastingRecipes} (× every mold item × every material) for JEI. */
    private void registerMoldDisplay() {
        for (MaterialType mat : MaterialType.values()) {
            for (ItemCastMold.MoldType moldType : ItemCastMold.MoldType.values()) {
                ItemStack output = MoldCastingRecipes.getOutput(moldType, mat);
                if (output.isEmpty()) continue;

                ItemStack material = MoldCastingRecipes.getMaterialIcon(mat);
                ItemStack mold = moldItem(moldType);
                if (material.isEmpty() || mold.isEmpty()) continue;

                CrucibleMoldRecipes.register(material, mold, output);
            }
        }
    }

    private static ItemStack moldItem(ItemCastMold.MoldType moldType) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", "mold_" + moldType.name().toLowerCase());
        if (!BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    // -------------------------------------------------------------------------
    // Accessor façade — delegates to the split registries
    // -------------------------------------------------------------------------

    /** Returns all alloying recipes in insertion order. Mirrors legacy {@code CrucibleRecipes.INSTANCE.recipeOrderedList}. */
    public List<CrucibleAlloyingRecipe> getAlloyingRecipes() {
        return CrucibleAlloyingRecipes.getRecipes();
    }

    /** Returns the smelting recipe list. Mirrors legacy {@code CrucibleRecipes.getSmeltingRecipes()}. */
    public static List<CrucibleSmeltingRecipes.SmeltingEntry> getSmeltingRecipes() {
        return CrucibleSmeltingRecipes.getRecipes();
    }

    /** Returns all mold-casting recipes as [material, mold, placeholder, output] arrays. Mirrors legacy {@code CrucibleRecipes.getMoldRecipes()}. */
    public static List<ItemStack[]> getMoldRecipes() {
        return CrucibleMoldRecipes.getMoldRecipes();
    }
}
