package com.hbm_m.compat.jei;

//? if forge || neoforge {
/** Fluessigkeits-Zutatentyp von JEI je Loader (Forge {@code ForgeTypes}, NeoForge {@code NeoForgeTypes}). */
public final class JeiFluidTypes {
    private JeiFluidTypes() {}

    //? if forge {
    public static final mezz.jei.api.ingredients.IIngredientTypeWithSubtypes<net.minecraft.world.level.material.Fluid, net.minecraftforge.fluids.FluidStack> FLUID_STACK =
            mezz.jei.api.forge.ForgeTypes.FLUID_STACK;
    //?} else {
    /*public static final mezz.jei.api.ingredients.IIngredientTypeWithSubtypes<net.minecraft.world.level.material.Fluid, net.neoforged.neoforge.fluids.FluidStack> FLUID_STACK =
            mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK;
    *///?}
}
//?} else {
/*public final class JeiFluidTypes {
    private JeiFluidTypes() {}
}
*///?}
