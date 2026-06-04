package com.hbm_m.compat.jei;

import com.hbm_m.inventory.recipes.SolderingRecipes;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

//? if forge {
public class SolderingStationJeiRecipe {

    private final List<List<ItemStack>> toppings;
    private final List<List<ItemStack>> pcb;
    private final List<List<ItemStack>> solder;
    private final ItemStack output;
    private final @org.jetbrains.annotations.Nullable SolderingRecipes.FluidRequirement fluid;

    public SolderingStationJeiRecipe(SolderingRecipes.SolderingRecipe recipe) {
        this.toppings = Arrays.stream(recipe.toppings)
                .map(ing -> List.of(ing.ingredient().getItems())).toList();
        this.pcb = Arrays.stream(recipe.pcb)
                .map(ing -> List.of(ing.ingredient().getItems())).toList();
        this.solder = Arrays.stream(recipe.solder)
                .map(ing -> List.of(ing.ingredient().getItems())).toList();
        this.output = recipe.output.copy();
        this.fluid  = recipe.fluid;
    }

    public List<List<ItemStack>> getToppings() { return toppings; }
    public List<List<ItemStack>> getPcb()      { return pcb;      }
    public List<List<ItemStack>> getSolder()   { return solder;   }
    public ItemStack getOutput()               { return output;   }
    public @org.jetbrains.annotations.Nullable SolderingRecipes.FluidRequirement getFluid() { return fluid; }

    public static List<SolderingStationJeiRecipe> fromRecipes() {
        return SolderingRecipes.recipes.stream()
                .map(SolderingStationJeiRecipe::new)
                .toList();
    }
}
//?} else {
/*public final class SolderingStationJeiRecipe { private SolderingStationJeiRecipe() {} }
*///?}
