package com.hbm_m.compat.jei;

//? if forge || neoforge {
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.world.level.material.Fluid;

/**
 * Единственное место, где категории JEI строят жидкостный ингредиент: типы {@code FluidStack}
 * у Forge и NeoForge разные, а всё остальное одинаково.
 */
public final class JeiFluidSlots {

    private JeiFluidSlots() {}

    /** Ёмкость по умолчанию для полоски жидкости — по ней считается заполнение слота. */
    public static final int DEFAULT_CAPACITY_MB = 24_000;

    public static void addFluid(IRecipeSlotBuilder slot, Fluid fluid, int amountMb, int capacityMb) {
        int capacity = Math.max(capacityMb, Math.max(1, amountMb));
        slot.setFluidRenderer(capacity, false, 16, 16)
                .setCustomRenderer(JeiTypes.FLUID, new HbmFluidJeiRenderer(16, 16));
        //? if forge {
        /*slot.addIngredient(JeiTypes.FLUID, new net.minecraftforge.fluids.FluidStack(fluid, amountMb));
        *///?} elif neoforge {
        slot.addIngredient(JeiTypes.FLUID, new net.neoforged.neoforge.fluids.FluidStack(fluid, amountMb));
        //?}
    }

    public static void addFluid(IRecipeSlotBuilder slot, dev.architectury.fluid.FluidStack stack, int capacityMb) {
        addFluid(slot, stack.getFluid(), (int) Math.min(Integer.MAX_VALUE, stack.getAmount()), capacityMb);
    }
}
//?}
