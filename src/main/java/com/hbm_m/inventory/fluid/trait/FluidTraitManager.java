package com.hbm_m.inventory.fluid.trait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

/**
 * Central registry of HBM fluid traits + tooltip temperature (°C), mirroring 1.7.10 {@code FluidType} + trait map.
 */
public class FluidTraitManager {

    public static final int ROOM_TEMPERATURE_C = 20;

    private static final Map<Fluid, Map<Class<? extends FluidTrait>, FluidTrait>> FLUID_TRAITS = new HashMap<>();
    private static final Map<Fluid, Integer> FLUID_TEMPERATURE_C = new HashMap<>();

    public static void setTemperatureCelsius(Fluid fluid, int tempC) {
        FLUID_TEMPERATURE_C.put(fluid, tempC);
    }

    /** Room temperature (20°C) when unset. */
    public static int getTemperatureCelsius(Fluid fluid) {
        return FLUID_TEMPERATURE_C.getOrDefault(fluid, ROOM_TEMPERATURE_C);
    }

    public static void addTrait(Fluid fluid, FluidTrait trait) {
        FLUID_TRAITS.computeIfAbsent(fluid, k -> new HashMap<>()).put(trait.getClass(), trait);
    }

    @SuppressWarnings("unchecked")
    public static <T extends FluidTrait> T getTrait(Fluid fluid, Class<T> traitClass) {
        if (!FLUID_TRAITS.containsKey(fluid)) return null;
        return (T) FLUID_TRAITS.get(fluid).get(traitClass);
    }

    public static boolean hasTrait(Fluid fluid, Class<? extends FluidTrait> traitClass) {
        return FLUID_TRAITS.containsKey(fluid) && FLUID_TRAITS.get(fluid).containsKey(traitClass);
    }

    /**
     * Mirrors 1.7.10 {@code FluidType.addInfo}: temperature line, trait {@code addInfo}, optional {@code addInfoHidden}
     * when shift is held, always accumulates hidden traits for the LSHIFT hint line.
     */
    public static void appendFluidTypeTooltip(Fluid fluid, boolean shiftDown, List<Component> lines) {
        int temp = getTemperatureCelsius(fluid);
        if (temp != ROOM_TEMPERATURE_C) {
            String tempStr = Math.abs(temp) >= 1000
                    ? EnergyFormatter.formatTooltipNumber((long) temp) + "°C"
                    : temp + "°C";
            if (temp < 0) {
                lines.add(Component.literal(tempStr).withStyle(ChatFormatting.BLUE));
            } else if (temp > 0) {
                lines.add(Component.literal(tempStr).withStyle(ChatFormatting.RED));
            }
        }

        List<Component> hidden = new ArrayList<>();
        for (Class<? extends FluidTrait> clazz : FluidTrait.traitList) {
            FluidTrait trait = getTrait(fluid, clazz);
            if (trait != null) {
                trait.addInfo(lines);
                if (shiftDown) {
                    trait.addInfoHidden(lines);
                }
                trait.addInfoHidden(hidden);
            }
        }

        if (!hidden.isEmpty() && !shiftDown) {
            lines.add(Component.translatable("gui.hbm_m.fluid_tank.hold_shift_more")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}