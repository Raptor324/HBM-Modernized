package com.hbm_m.item.custom.liquids;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Universal infinite fluid source - provides any requested fluid type.
 * Analog of ItemInfiniteFluid from 1.7.10 (fluid_barrel_infinite).
 */
public class InfiniteFluidItem extends Item {

    private final int transferRate; // mB per transfer (e.g. 1_000_000_000 like 1.7.10)

    public InfiniteFluidItem(Properties properties, int transferRate) {
        super(properties);
        this.transferRate = transferRate;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§bInfinite Fluid Source"));
        tooltip.add(Component.literal("§7Output Rate: §e" + transferRate + " mB/t"));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new InfiniteFluidCapabilityProvider(stack, transferRate);
    }

    private static class InfiniteFluidCapabilityProvider implements ICapabilityProvider {
        private final InfiniteFluidHandler handler;
        private final LazyOptional<IFluidHandlerItem> optional;

        public InfiniteFluidCapabilityProvider(ItemStack stack, int rate) {
            this.handler = new InfiniteFluidHandler(stack, rate);
            this.optional = LazyOptional.of(() -> handler);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }
    }

    private static class InfiniteFluidHandler implements IFluidHandlerItem {
        private final ItemStack container;
        private final int rate;

        public InfiniteFluidHandler(ItemStack container, int rate) {
            this.container = container;
            this.rate = rate;
        }

        @Nonnull
        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            int amountToDrain = Math.min(resource.getAmount(), rate);
            return new FluidStack(resource.getFluid(), amountToDrain);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
