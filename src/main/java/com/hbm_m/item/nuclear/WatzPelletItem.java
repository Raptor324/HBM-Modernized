package com.hbm_m.item.nuclear;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A fresh (non-depleted) Watz fuel/absorber pellet. Ported from
 * {@code com.hbm.items.machine.ItemWatzPellet} (1.7.10), but as a plain item-per-type
 * (matching this port's {@code ZirnoxRodItem} convention) instead of an NBT-enum-meta item.
 * <p>
 * Remaining "yield" (burn/absorb budget) is stored as a double in NBT and depletes every
 * reactor tick; when it reaches zero {@link MachineWatzPowerplantBlockEntityHooks} (see
 * {@code MachineWatzPowerplantBlockEntity}) swaps the stack for the matching depleted item.
 */
public class WatzPelletItem extends Item {

    private static final String NBT_YIELD = "watz_yield";

    private final WatzPelletType type;

    public WatzPelletItem(Properties properties, WatzPelletType type) {
        super(properties.stacksTo(16));
        this.type = type;
    }

    public WatzPelletType getType() {
        return type;
    }

    public static double getYield(ItemStack stack) {
        if (!(stack.getItem() instanceof WatzPelletItem pellet)) return 0D;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_YIELD)) return pellet.type.yield;
        return tag.getDouble(NBT_YIELD);
    }

    public static void setYield(ItemStack stack, double yield) {
        stack.getOrCreateTag().putDouble(NBT_YIELD, Math.max(0D, yield));
    }

    public static double getEnrichment(ItemStack stack) {
        if (!(stack.getItem() instanceof WatzPelletItem pellet)) return 0D;
        return getYield(stack) / pellet.type.yield;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnrichment(stack) < 0.999D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double remaining = Math.max(0D, Math.min(1D, getEnrichment(stack)));
        return Math.round((float) (13.0D * remaining));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return type.isFuel() ? 0xE55B4B : 0x4B9FE5;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        double depletion = (1D - getEnrichment(stack)) * 100D;
        tooltip.add(Component.literal(String.format(Locale.US, "Depletion: %.1f%%", depletion)).withStyle(ChatFormatting.YELLOW));

        if (type.passiveFlux > 0) {
            tooltip.add(Component.literal("Base fission rate: " + (int) type.passiveFlux).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("Self-igniting!").withStyle(ChatFormatting.RED));
        }
        if (type.isFuel()) {
            tooltip.add(Component.literal("Heat per flux: " + type.heatEmission + " TU").withStyle(ChatFormatting.GOLD));
        }
        if (type.isAbsorber()) {
            tooltip.add(Component.literal("Neutron absorber / moderator").withStyle(ChatFormatting.GRAY));
        }
    }
}
