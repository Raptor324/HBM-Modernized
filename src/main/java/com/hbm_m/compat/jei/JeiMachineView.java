package com.hbm_m.compat.jei;

//? if forge || neoforge {
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

/**
 * Разбор рецепта на четыре списка, которых хватает подавляющему большинству машин мода.
 * Позволяет держать одну категорию ({@link JeiSimpleMachineCategory}) вместо десятка копий.
 */
public interface JeiMachineView<R> {

    record ItemInput(Ingredient ingredient, int count) {}

    record FluidAmount(Fluid fluid, int mb) {
        static FluidAmount of(dev.architectury.fluid.FluidStack stack) {
            return new FluidAmount(stack.getFluid(), (int) Math.min(Integer.MAX_VALUE, stack.getAmount()));
        }
    }

    default List<ItemInput> itemInputs(R recipe) { return List.of(); }

    default List<FluidAmount> fluidInputs(R recipe) { return List.of(); }

    default List<ItemStack> itemOutputs(R recipe) { return List.of(); }

    default List<FluidAmount> fluidOutputs(R recipe) { return List.of(); }

    /** Строки под рецептом: длительность, потребление, шансы. */
    default List<Component> notes(R recipe) { return List.of(); }
}
//?}
