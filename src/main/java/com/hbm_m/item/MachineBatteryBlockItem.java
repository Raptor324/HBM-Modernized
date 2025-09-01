package com.hbm_m.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MachineBatteryBlockItem extends BlockItem {

    private final int maxPower;

    public MachineBatteryBlockItem(Block pBlock, Properties pProperties, int maxPower) {
        super(pBlock, pProperties);
        this.maxPower = maxPower;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {

        pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.capacity", maxPower).withStyle(ChatFormatting.GOLD));
        pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.charge_speed", maxPower / 200).withStyle(ChatFormatting.GOLD));
        pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.discharge_speed", maxPower / 600).withStyle(ChatFormatting.GOLD));

        // Если в предмете сохранена энергия (после разрушения блока)
        if (pStack.hasTag()) {
            CompoundTag blockEntityTag = pStack.getTagElement("BlockEntityTag");
            if (blockEntityTag != null && blockEntityTag.contains("energy")) {
                int energy = blockEntityTag.getInt("energy");
                pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.stored", energy, maxPower).withStyle(ChatFormatting.YELLOW));
            }
        }
        
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }
}