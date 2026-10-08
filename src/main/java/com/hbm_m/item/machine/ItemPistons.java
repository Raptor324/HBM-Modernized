package com.hbm_m.item.machine;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.item.ITooltipProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemPistons} (piston_set_*): Tooltip mit dem Wirkungsgrad je Treibstoffgrad. Die Werte
 * ({@code EnumPistonType.eff}) liegen im Port in {@link MachineCombustionEngineBlockEntity#PISTON_EFF}.
 */
public class ItemPistons extends Item implements ITooltipProvider {

    /** {@code EnumPistonType}-Ordinal: STEEL, DURA, DESH, STARMETAL. */
    private final int type;

    public ItemPistons(int type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        double[] eff = MachineCombustionEngineBlockEntity.PISTON_EFF[type];

        list.add(Component.literal("Fuel efficiency:").withStyle(ChatFormatting.YELLOW));
        for (int i = 0; i < eff.length && i < FuelGrade.values().length; i++) {
            // FuelGrade.getLocalizedName(): hbmfluid.trait.fuel.<grade>
            String grade = Component.translatable("hbmfluid.trait.fuel." + FuelGrade.values()[i].getGrade().toLowerCase(Locale.US)).getString();
            list.add(Component.literal("-" + grade + ": ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("" + (int) (eff[i] * 100) + "%").withStyle(ChatFormatting.RED)));
        }
    }
}
