package com.hbm_m.api.fluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Отображение жидкостей в тексте GUI (минуя сырые RL вроде {@code hbm_m:naphta}).
 */
public final class FluidLocalization {

    private FluidLocalization() {}

    /** Имя жидкости по id в реестре; при отсутствии — литеральный RL. */
    public static Component nameFromFluidId(ResourceLocation fluidId) {
        if (fluidId == null) {
            return Component.empty();
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Component.literal(fluidId.toString()).withStyle(ChatFormatting.BLUE);
        }
        return Component.literal(FluidStack.create(fluid, 1).getName().getString()).withStyle(ChatFormatting.BLUE);
    }
}