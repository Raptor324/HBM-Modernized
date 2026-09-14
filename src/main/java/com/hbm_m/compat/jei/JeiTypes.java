package com.hbm_m.compat.jei;

//? if forge || neoforge {
//? if forge {
import net.minecraftforge.fluids.FluidStack;
//?} elif neoforge {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?}
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import net.minecraft.world.level.material.Fluid;

/**
 * Единственное место, где JEI-интеграция знает про лоадер: тип жидкостного ингредиента.
 * В {@code platform/*Hooks} его вынести нельзя — те классы грузятся всегда, а JEI лежит
 * в {@code compileOnly} и в рантайме может отсутствовать.
 */
public final class JeiTypes {

    private JeiTypes() {}

    //? if forge {
    public static final IIngredientTypeWithSubtypes<Fluid, FluidStack> FLUID =
            mezz.jei.api.forge.ForgeTypes.FLUID_STACK;
    //?} elif neoforge {
    /*public static final IIngredientTypeWithSubtypes<Fluid, FluidStack> FLUID =
            mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK;
    *///?}
}
//?}
