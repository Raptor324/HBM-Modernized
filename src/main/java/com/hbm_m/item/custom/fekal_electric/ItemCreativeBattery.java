package com.hbm_m.item.custom.fekal_electric;

import com.hbm_m.api.energy.IEnergyProvider;
import com.hbm_m.api.energy.LongEnergyWrapper;
import com.hbm_m.capability.ModCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemCreativeBattery extends ModBatteryItem {

    public ItemCreativeBattery(Properties pProperties) {
        super(pProperties.rarity(Rarity.EPIC).stacksTo(1), Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.hbm_m.creative_battery_desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        pTooltipComponents.add(Component.translatable("tooltip.hbm_m.creative_battery_flavor")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack pStack) {
        return false;
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack pStack) {
        return 13; // Всегда полная
    }

    @Override
    public int getBarColor(@Nonnull ItemStack pStack) {
        return 0xFF00FF; // Фиолетовый для креатива
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new CreativeEnergyProvider();
    }

    private static class CreativeEnergyProvider implements ICapabilityProvider {
        private final LazyOptional<IEnergyProvider> lazyProvider = LazyOptional.of(CreativeEnergyStorage::new);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ModCapabilities.HBM_ENERGY_PROVIDER) {
                return lazyProvider.cast();
            }
            if (cap == ForgeCapabilities.ENERGY) {
                return lazyProvider.lazyMap(p -> new LongEnergyWrapper(p, LongEnergyWrapper.BitMode.LOW)).cast();
            }
            return LazyOptional.empty();
        }

        private static class CreativeEnergyStorage implements IEnergyProvider {


            public long extractEnergy(long maxExtract, boolean simulate) {
                return maxExtract; // Всегда выдаем сколько просят
            }

            @Override
            public long getEnergyStored() {
                return Long.MAX_VALUE;
            }

            @Override
            public long getMaxEnergyStored() {
                return Long.MAX_VALUE;
            }

            @Override
            public void setEnergyStored(long energy) {
                // Игнорируем - креативная батарея не меняет заряд
            }

            @Override
            public long getProvideSpeed() {
                return Long.MAX_VALUE;
            }

            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canConnectEnergy(Direction side) {
                return true;
            }
        }
    }
}