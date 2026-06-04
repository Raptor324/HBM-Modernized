package com.hbm_m.compat.jei;

import com.hbm_m.recipe.CrystallizerRecipe;
import com.hbm_m.recipe.CrystallizerRecipes;
import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

//? if forge {
public class CrystallizerJeiRecipe {

    private final List<ItemStack> inputs;
    private final ItemStack output;
    private final FluidStack acid;
    private final int duration;

    public CrystallizerJeiRecipe(CrystallizerRecipe recipe) {
        this.inputs   = List.of(recipe.getInput().getItems());
        this.output   = recipe.getOutput().copy();
        this.acid     = recipe.getAcid() != null ? recipe.getAcid() : FluidStack.empty();
        this.duration = recipe.getDuration();
    }

    public List<ItemStack> getInputs()  { return inputs;   }
    public ItemStack       getOutput()  { return output;   }
    public FluidStack      getAcid()    { return acid;     }
    public int             getDuration(){ return duration; }

    public static List<CrystallizerJeiRecipe> fromAll() {
        return CrystallizerRecipes.getAll().stream()
                .filter(r -> !r.getOutput().isEmpty())
                .map(CrystallizerJeiRecipe::new)
                .toList();
    }
}
//?} else {
/*public final class CrystallizerJeiRecipe { private CrystallizerJeiRecipe() {} }
*///?}
