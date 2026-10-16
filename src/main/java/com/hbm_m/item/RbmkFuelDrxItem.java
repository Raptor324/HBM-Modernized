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

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHbmTooltip(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§4Self-combusting").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§2Crustyness! 0.0%").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("§5Lead poison! 0.0%").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§9Arrives from: Hyperbolic non-euclidean shapes").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("§9Departs to: Elliptic non-euclidean shapes").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("§eDestruction function: Cx + 10.0² / 10000 × 1000.0").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§cFunction specification: DANGEROUS / QUADRATIC").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§eYield creation function: x × 0.5").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eYield destruction function: x² / 50.0").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eCrust per tick at full power: 0.1 °C").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eFlow: 0.02/2").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eSkin entropy: 28.0m").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eCore entropy: 28.0m").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eCrush depth: 1000000.0m").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§4[Radioactive]").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§c1200000.0mAD/s").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§5[Digamma Radiation]").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§c333.3mDRx/s").withStyle(ChatFormatting.RED));
    }
}
