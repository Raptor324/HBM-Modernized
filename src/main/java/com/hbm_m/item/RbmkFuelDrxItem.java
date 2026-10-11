package com.hbm_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.rbmk.RBMKRodItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Digamma ("DRX") RBMK fuel - a real, loadable {@link RBMKRodItem} (see stat block at its
 * {@code ModItems.RBMK_FUEL_DRX} registration) with the original's over-the-top flavor tooltip
 * kept as-is. Previously registered as a plain {@code Item}, which meant it could never actually
 * be inserted into a fuel channel; now behaves like the other 32 RBMK fuels.
 */
public class RbmkFuelDrxItem extends RBMKRodItem {

    public RbmkFuelDrxItem(Properties properties) {
        super("Digamma Fuel Rod", properties);
    }

    /**
     * 1:1 {@code ItemRBMKRod.addInformation}, Zweig {@code this == rbmk_fuel_drx}: dieselben Werte wie die
     * uebrigen Staebe (Formatierung wie {@link RBMKRodItem}), aber mit den {@code trait.rbmx.*}-Texten,
     * Neutronenarten {@code <name>.x} und "m" statt "°C". Strahlungszeilen kommen aus dem Hazard-System.
     */
    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal(ChatFormatting.ITALIC + fullName));

        double hull = getHullHeat(stack);
        double core = getCoreHeat(stack);
        if (hull >= 50 || core >= 50)
            list.add(Component.translatable("desc.item.wasteCooling").withStyle(ChatFormatting.GOLD));

        if (selfRate > 0 || function == EnumBurnFunc.SIGMOID)
            list.add(Component.translatable("trait.rbmx.source").withStyle(ChatFormatting.RED));

        list.add(Component.translatable("trait.rbmx.depletion",
            String.format("%.3f%%", (1.0 - getEnrichment(stack)) * 100)).withStyle(ChatFormatting.GREEN));
        list.add(Component.translatable("trait.rbmx.xenon",
            String.format("%.3f%%", getPoison(stack))).withStyle(ChatFormatting.DARK_PURPLE));
        list.add(Component.translatable("trait.rbmx.splitsWith",
            Component.translatable(nType.unlocalized + ".x")).withStyle(ChatFormatting.BLUE));
        list.add(Component.translatable("trait.rbmx.splitsInto",
            Component.translatable(rType.unlocalized + ".x")).withStyle(ChatFormatting.BLUE));
        list.add(Component.translatable("trait.rbmx.fluxFunc",
            Component.literal(ChatFormatting.WHITE + getFuncDescription(stack))).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmx.funcType",
            function.title).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmx.xenonGen",
            Component.literal(ChatFormatting.WHITE + "x * " + xGen)).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmx.xenonBurn",
            Component.literal(ChatFormatting.WHITE + "x² / " + xBurn)).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmx.heat",
            heat + "°C").withStyle(ChatFormatting.GOLD));
        list.add(Component.translatable("trait.rbmx.diffusion",
            diffusion + "¹/²").withStyle(ChatFormatting.GOLD));
        list.add(Component.translatable("trait.rbmx.skinTemp",
            String.format("%.1fm", hull)).withStyle(ChatFormatting.RED));
        list.add(Component.translatable("trait.rbmx.coreTemp",
            String.format("%.1fm", core)).withStyle(ChatFormatting.RED));
        list.add(Component.translatable("trait.rbmx.melt",
            meltingPoint + "m").withStyle(ChatFormatting.DARK_RED));
    }
}
