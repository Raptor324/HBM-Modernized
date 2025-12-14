package com.hbm_m.item.custom.fekal_electric;

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

    private final long maxPower; // Меняем int на long

    public MachineBatteryBlockItem(Block pBlock, Properties pProperties, long maxPower) {
        super(pBlock, pProperties);
        this.maxPower = maxPower;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        // Используем long для вычислений
        // Если у тебя есть утилита EnergyFormatter.format(long), лучше использовать её для красивых чисел (1M, 1G и т.д.)

        pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.capacity", maxPower).withStyle(ChatFormatting.GOLD));
        pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.charge_speed", maxPower / 200).withStyle(ChatFormatting.GOLD));
        pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.discharge_speed", maxPower / 600).withStyle(ChatFormatting.GOLD));

        // Читаем энергию из NBT
        if (pStack.hasTag()) {
            CompoundTag blockEntityTag = pStack.getTagElement("BlockEntityTag");
            // Важно: в MachineBatteryBlockEntity мы сохраняем как "Energy" (с большой буквы), проверь это!
            // В старом коде было "Energy", здесь "energy". Лучше проверять оба варианта или привести к одному.
            if (blockEntityTag != null) {
                long energy = 0;
                if (blockEntityTag.contains("Energy")) {
                    energy = blockEntityTag.getLong("Energy");
                } else if (blockEntityTag.contains("energy")) {
                    energy = blockEntityTag.getInt("energy"); // Поддержка старых сохранений
                }

                if (energy > 0) {
                    pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.stored", energy, maxPower).withStyle(ChatFormatting.YELLOW));
                }
            }
        }

        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }
}