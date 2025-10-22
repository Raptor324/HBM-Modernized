package com.hbm_m.item;

import com.hbm_m.capability.EnergyCapabilityProvider;
import com.hbm_m.util.EnergyFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Универсальный класс для батареек и аккумуляторов в стиле HBM NTM.
 * Поддерживает настраиваемую ёмкость, скорость зарядки и разрядки.
 * Использует EnergyCapabilityProvider для работы с Forge Energy.
 */
public class ModBatteryItem extends Item {
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;
    private final boolean isRechargeable;

    /**
     * Конструктор для создания перезаряжаемой батарейки/аккумулятора
     *
     * @param properties Свойства предмета
     * @param capacity Максимальная ёмкость энергии (в FE/RF)
     * @param maxReceive Максимальная скорость зарядки (FE/t)
     * @param maxExtract Максимальная скорость разрядки (FE/t)
     */
    public ModBatteryItem(Properties properties, int capacity, int maxReceive, int maxExtract) {
        super(properties.stacksTo(1)); // Батарейки не стакаются
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.isRechargeable = maxReceive > 0;
    }

    /**
     * Конструктор для одноразовых батареек (только разрядка, без зарядки)
     *
     * @param properties Свойства предмета
     * @param capacity Максимальная ёмкость энергии (в FE/RF)
     * @param maxExtract Максимальная скорость разрядки (FE/t)
     */
    public ModBatteryItem(Properties properties, int capacity, int maxExtract) {
        this(properties, capacity, 0, maxExtract);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // Используем существующий EnergyCapabilityProvider
        return new EnergyCapabilityProvider(stack, this.capacity, this.maxReceive, this.maxExtract);
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        // Всегда показываем полоску заряда
        return true;
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        // Ширина полоски зависит от заряда (0-13 пикселей)
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(energy -> {
                    int stored = energy.getEnergyStored();
                    int max = energy.getMaxEnergyStored();
                    return max > 0 ? Math.round(13.0F * stored / max) : 0;
                })
                .orElse(0);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        // Динамический цвет полоски в зависимости от уровня заряда
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(energy -> {
                    int stored = energy.getEnergyStored();
                    int max = energy.getMaxEnergyStored();
                    float ratio = max > 0 ? (float) stored / max : 0;

                    // Красный → Жёлтый → Зелёный
                    if (ratio <= 0.25f) {
                        return 0xFF0000; // Красный (критично)
                    } else if (ratio <= 0.5f) {
                        return 0xFF8800; // Оранжевый (низкий)
                    } else if (ratio <= 0.75f) {
                        return 0xFFFF00; // Жёлтый (средний)
                    } else {
                        return 0x44B027; // Зелёный (высокий)
                    }
                })
                .orElse(0xFF0000);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level,
                                @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
            int stored = energy.getEnergyStored();
            int max = energy.getMaxEnergyStored();

            // Хранится энергии (тёмно-синий цвет)
            tooltip.add(Component.translatable("tooltip.hbm_m.battery.stored")
                    .withStyle(ChatFormatting.DARK_BLUE));

            // Текущая энергия / Максимальная (с приставками СИ, золотой цвет)
            tooltip.add(Component.literal(
                            String.format("%s / %s FE",
                                    EnergyFormatter.format(stored),
                                    EnergyFormatter.format(max)))
                    .withStyle(ChatFormatting.GOLD));

            // Скорость зарядки/разрядки (с приставками СИ)
            if (maxReceive > 0 && maxExtract > 0) {
                tooltip.add(Component.translatable("tooltip.hbm_m.battery.transfer_rate",
                        Component.literal(EnergyFormatter.format(maxReceive)).withStyle(ChatFormatting.YELLOW),
                        Component.literal(EnergyFormatter.format(maxExtract)).withStyle(ChatFormatting.YELLOW)
                ).withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.translatable("tooltip.hbm_m.battery.discharge_rate",
                        Component.literal(EnergyFormatter.format(maxExtract)).withStyle(ChatFormatting.YELLOW)
                ).withStyle(ChatFormatting.WHITE));
            }
        });

        super.appendHoverText(stack, level, tooltip, flag);
    }

    // Геттеры для получения параметров батарейки
    public long getCapacity() {
        return capacity;
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public boolean isRechargeable() {
        return isRechargeable;
    }
}