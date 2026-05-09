package com.hbm_m.item.fekal_electric;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyCapabilityProvider;
import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

//? if forge {
/*import net.minecraftforge.common.capabilities.ICapabilityProvider;
*///?}

public class ModBatteryItem extends Item {
    protected final long capacity;
    protected final long maxReceive;
    protected final long maxExtract;

    public ModBatteryItem(Properties properties, long capacity, long maxReceive, long maxExtract) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public ModBatteryItem(Properties properties, long capacity, long maxExtract) {
        this(properties, capacity, maxExtract, maxExtract);
    }

    // --- Геттеры для параметров батареи ---
    public long getCapacity() {
        return capacity;
    }

    public long getMaxReceive() {
        return maxReceive;
    }

    public long getMaxExtract() {
        return maxExtract;
    }

    //? if forge {
    /*@Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {

        return new EnergyCapabilityProvider(stack, this.capacity, this.maxReceive, this.maxExtract);
    }
    *///?}

    // --- Статический метод для установки энергии ---
    /**
     * Устанавливает энергию в ItemStack батареи напрямую через NBT.
     * Используется для создания предзаряженных батарей в креатив табе.
     *
     * @param stack ItemStack батареи
     * @param energy Количество энергии для установки
     */
    public static void setEnergy(ItemStack stack, long energy) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ModBatteryItem battery)) {
            return;
        }

        long clampedEnergy = Math.max(0, Math.min(energy, battery.getCapacity()));
        stack.getOrCreateTag().putLong("energy", clampedEnergy);
    }

    /**
     * Получает текущий заряд батареи из NBT.
     *
     * @param stack ItemStack батареи
     * @return Количество энергии
     */
    public static long getEnergy(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0;
        }
        return stack.getTag().getLong("energy");
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var provider = ItemEnergyAccess.getHbmProvider(stack);
        if (provider.isPresent()) {
            var energy = provider.get();
            if (energy.getMaxEnergyStored() <= 0) return 0;
            return (int) Math.round(13.0 * energy.getEnergyStored() / (double) energy.getMaxEnergyStored());
        }

        return ItemEnergyAccess.getHbmReceiver(stack)
                .map(energy -> {
                    if (energy.getMaxEnergyStored() <= 0) return 0;
                    return (int) Math.round(13.0 * energy.getEnergyStored() / (double) energy.getMaxEnergyStored());
                })
                .orElse(0);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        float ratio = ItemEnergyAccess.getHbmProvider(stack)
                .map(energy -> {
                    if (energy.getMaxEnergyStored() <= 0) return 0.0f;
                    return (float) energy.getEnergyStored() / energy.getMaxEnergyStored();
                })
                .orElseGet(() -> ItemEnergyAccess.getHbmReceiver(stack)
                        .map(energy -> {
                            if (energy.getMaxEnergyStored() <= 0) return 0.0f;
                            return (float) energy.getEnergyStored() / energy.getMaxEnergyStored();
                        })
                        .orElse(0.0f));

        return Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        ItemEnergyAccess.getHbmProvider(stack)
                .ifPresent(energy -> addEnergyTooltip(tooltip, energy.getEnergyStored(), energy.getMaxEnergyStored(), ChatFormatting.AQUA));

        if (ItemEnergyAccess.getHbmProvider(stack).isEmpty()) {
            ItemEnergyAccess.getHbmReceiver(stack)
                    .ifPresent(energy -> addEnergyTooltip(tooltip, energy.getEnergyStored(), energy.getMaxEnergyStored(), ChatFormatting.AQUA));
        }

        if (maxReceive > 0) {
            // [🔥 ИЗМЕНЕНО: Вся строка теперь ChatFormatting.GOLD 🔥]
            tooltip.add(Component.translatable("tooltip.hbm_m.battery.transfer_rate",
                    EnergyFormatter.format(maxReceive)).withStyle(ChatFormatting.GOLD));
        }
        if (maxExtract > 0) {
            // [🔥 ИЗМЕНЕНО: Вся строка теперь ChatFormatting.GOLD 🔥]
            tooltip.add(Component.translatable("tooltip.hbm_m.battery.discharge_rate",
                    EnergyFormatter.format(maxExtract)).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private void addEnergyTooltip(List<Component> tooltip, long stored, long max, ChatFormatting color) {
        // [🔥 ИЗМЕНЕНО: Теперь обе строки используют переданный 'color' 🔥]

        // Строка 1: "Хранится энергии:"
        tooltip.add(Component.translatable("tooltip.hbm_m.battery.stored").withStyle(color));

        // Строка 2: " X / Y HE"
        tooltip.add(Component.literal(String.format(" %s / %s HE",
                        EnergyFormatter.format(stored),
                        EnergyFormatter.format(max)))
                .withStyle(color));
    }
}