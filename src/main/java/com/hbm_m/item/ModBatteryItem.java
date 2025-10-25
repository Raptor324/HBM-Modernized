package com.hbm_m.item;

import com.hbm_m.capability.EnergyCapabilityProvider;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.util.EnergyFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Универсальный класс для батареек и аккумуляторов в стиле HBM NTM.
 * Поддерживает long-энергосистему через ILongEnergyStorage.
 * Совместим с Forge Energy через обёртки.
 */
public class ModBatteryItem extends Item {
    private final long capacity;
    private final long maxReceive;
    private final long maxExtract;
    private final boolean isRechargeable;

    /**
     * Конструктор для создания перезаряжаемой батарейки/аккумулятора
     */
    public ModBatteryItem(Properties properties, long capacity, long maxReceive, long maxExtract) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.isRechargeable = maxReceive > 0;
    }

    /**
     * Конструктор для одноразовых батареек (только разрядка)
     */
    public ModBatteryItem(Properties properties, long capacity, long maxExtract) {
        this(properties, capacity, 0, maxExtract);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyCapabilityProvider(stack, this.capacity, this.maxReceive, this.maxExtract);
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        // ИСПРАВЛЕНО: Читаем через ILongEnergyStorage для корректного отображения
        return stack.getCapability(ModCapabilities.LONG_ENERGY)
                .map(energy -> {
                    long stored = energy.getEnergyStored();
                    long max = energy.getMaxEnergyStored();
                    return max > 0 ? Math.round(13.0F * stored / max) : 0;
                })
                .orElse(0);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        // ИСПРАВЛЕНО: Читаем через ILongEnergyStorage
        return stack.getCapability(ModCapabilities.LONG_ENERGY)
                .map(energy -> {
                    long stored = energy.getEnergyStored();
                    long max = energy.getMaxEnergyStored();
                    float ratio = max > 0 ? (float) stored / max : 0;

                    // Красный → Оранжевый → Жёлтый → Зелёный
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
        // ИСПРАВЛЕНО: Приоритет ILongEnergyStorage для корректного отображения больших значений
        stack.getCapability(ModCapabilities.LONG_ENERGY).ifPresent(energy -> {
            long stored = energy.getEnergyStored();
            long max = energy.getMaxEnergyStored();

            // Заголовок
            tooltip.add(Component.translatable("tooltip.hbm_m.battery.stored")
                    .withStyle(ChatFormatting.DARK_BLUE));

            // Текущая энергия / Максимальная (с приставками СИ)
            tooltip.add(Component.literal(
                            String.format("%s / %s FE",
                                    EnergyFormatter.format(stored),
                                    EnergyFormatter.format(max)))
                    .withStyle(ChatFormatting.GOLD));

            // Скорость зарядки/разрядки
            if (maxReceive > 0 && maxExtract > 0) {
                // Перезаряжаемая батарея
                tooltip.add(Component.translatable("tooltip.hbm_m.battery.transfer_rate",
                        Component.literal(EnergyFormatter.format(maxReceive)).withStyle(ChatFormatting.GREEN)
                ).withStyle(ChatFormatting.WHITE));

                tooltip.add(Component.translatable("tooltip.hbm_m.battery.discharge_rate",
                        Component.literal(EnergyFormatter.format(maxExtract)).withStyle(ChatFormatting.YELLOW)
                ).withStyle(ChatFormatting.WHITE));
            }

            // Процент заряда
            float percent = max > 0 ? (stored * 100.0f / max) : 0;
            ChatFormatting percentColor = percent > 75 ? ChatFormatting.GREEN :
                    percent > 50 ? ChatFormatting.YELLOW :
                            percent > 25 ? ChatFormatting.GOLD : ChatFormatting.RED;

            tooltip.add(Component.literal(String.format("%.1f%%", percent))
                    .withStyle(percentColor));
        });

        super.appendHoverText(stack, level, tooltip, flag);
    }

    // Утилитарные методы для получения энергии напрямую из ItemStack
    // (полезно для рецептов или других механик)

    /**
     * Получает текущее количество энергии в батарейке
     */
    public static long getEnergy(ItemStack stack) {
        return stack.getCapability(ModCapabilities.LONG_ENERGY)
                .map(ILongEnergyStorage::getEnergyStored)
                .orElse(0L);
    }

    /**
     * Устанавливает количество энергии в батарейке
     */
    public static void setEnergy(ItemStack stack, long energy) {
        if (stack.getItem() instanceof ModBatteryItem battery) {
            energy = Math.max(0, Math.min(energy, battery.capacity));
            stack.getOrCreateTag().putLong("energy", energy);
        }
    }

    /**
     * Проверяет, разряжена ли батарейка
     */
    public static boolean isDepleted(ItemStack stack) {
        return getEnergy(stack) <= 0;
    }

    /**
     * Проверяет, полностью ли заряжена батарейка
     */
    public static boolean isFullyCharged(ItemStack stack) {
        if (!(stack.getItem() instanceof ModBatteryItem battery)) return false;
        return getEnergy(stack) >= battery.capacity;
    }

    // Геттеры для параметров батарейки
    public long getCapacity() {
        return capacity;
    }

    public long getMaxReceive() {
        return maxReceive;
    }

    public long getMaxExtract() {
        return maxExtract;
    }

    public boolean isRechargeable() {
        return isRechargeable;
    }
}