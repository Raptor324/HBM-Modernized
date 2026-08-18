package com.hbm_m.item.industrial;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.hbm_m.platform.PlatformHooks;

/**
 * Direkter Java-Port von {@code ItemPlateFuel} (1.7.10 Original, {@code
 * com.hbm.items.machine.ItemPlateFuel}) - Brennstoffplatte fuer den Research Reactor. Die
 * verbleibende Lebensdauer wird (anders als im Original, das dafuer die geerbte {@code
 * ItemFuelRod}-Basisklasse nutzt) direkt als NBT-Tag {@code "lifetime"} auf dem Stack
 * gespeichert - funktional identisch, aber ohne die volle {@code ItemFuelRod}-Hierarchie
 * (Reparatur/Wiederaufbereitung dieser Klasse werden vom Research Reactor nicht benoetigt).
 */
public class ItemPlateFuel extends Item {

    public enum FunctionEnum {
        LOGARITHM, SQUARE_ROOT, NEGATIVE_QUADRATIC, LINEAR, PASSIVE
    }

    public final long lifeTime;
    public final FunctionEnum function;
    public final int reactivity;

    public ItemPlateFuel(Properties properties, long lifeTime, FunctionEnum function, int reactivity) {
        super(properties);
        this.lifeTime = lifeTime;
        this.function = function;
        this.reactivity = reactivity;
    }

    public static long getLifeTime(ItemStack stack) {
        return PlatformHooks.getLong(stack, "lifetime");
    }

    public static void setLifeTime(ItemStack stack, long value) {
        PlatformHooks.putLong(stack, "lifetime", value);
    }

    /** 1:1 aus dem Original ({@code react(World, ItemStack, int)}). */
    public int react(Level level, ItemStack stack, int flux) {
        if (function != FunctionEnum.PASSIVE) {
            setLifeTime(stack, getLifeTime(stack) + flux);
        }

        return switch (function) {
            case LOGARITHM -> (int) (Math.log10(flux + 1) * 0.5D * reactivity);
            case SQUARE_ROOT -> (int) (Math.sqrt(flux) * reactivity / 10D);
            case NEGATIVE_QUADRATIC -> (int) Math.max((flux - (flux * (long) flux / 10000D)) / 100D * reactivity, 0);
            case LINEAR -> (int) (flux / 100D * reactivity);
            case PASSIVE -> {
                setLifeTime(stack, getLifeTime(stack) + reactivity);
                yield reactivity;
            }
        };
    }
}
