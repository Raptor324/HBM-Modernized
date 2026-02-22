package com.hbm_m.item.custom.liquids;

import com.hbm_m.api.fluids.HbmFluidRegistry;
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
 * Fluid Barrel - A portable fluid container that can hold 16,000 mB (16 buckets) of any fluid.
 * Similar to a tank but as an item.
 */
public class FluidBarrelItem extends Item {

    public static final int CAPACITY = 16000; // 16 buckets
    public static final String NBT_FLUID = "Fluid";

    public FluidBarrelItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) {
            return Component.translatable("item.hbm_m.fluid_barrel.empty");
        }
        return Component.translatable("item.hbm_m.fluid_barrel", fluid.getDisplayName().getString());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.literal("§7Empty"));
        } else {
            tooltip.add(Component.literal("§7Fluid: §b" + fluid.getDisplayName().getString()));
            tooltip.add(Component.literal("§7Amount: §e" + fluid.getAmount() + " §7/ §e" + CAPACITY + " mB"));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return !fluid.isEmpty() && fluid.getAmount() < CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return 0;
        return Math.round(13.0F * fluid.getAmount() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFFF00;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidBarrelCapabilityProvider(stack);
    }

    // Static helper methods for NBT access
    public static FluidStack getFluid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_FLUID)) {
            return FluidStack.loadFluidStackFromNBT(tag.getCompound(NBT_FLUID));
        }
        return FluidStack.EMPTY;
    }

    public static void setFluid(ItemStack stack, FluidStack fluid) {
        CompoundTag tag = stack.getOrCreateTag();
        if (fluid.isEmpty()) {
            tag.remove(NBT_FLUID);
        } else {
            tag.put(NBT_FLUID, fluid.writeToNBT(new CompoundTag()));
        }
    }

    /** Returns tint color for overlay layer (for ItemColor). */
    public static int getTintColor(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return 0xFFFFFF;
        return HbmFluidRegistry.getTintColor(fluid.getFluid());
    }

    // --- Capability Provider ---
    private static class FluidBarrelCapabilityProvider implements ICapabilityProvider {
        private final FluidBarrelHandler handler;
        private final LazyOptional<IFluidHandlerItem> optional;

        public FluidBarrelCapabilityProvider(ItemStack stack) {
            this.handler = new FluidBarrelHandler(stack);
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

    // --- Fluid Handler Implementation ---
    private static class FluidBarrelHandler implements IFluidHandlerItem {
        private final ItemStack container;

        public FluidBarrelHandler(ItemStack container) {
            this.container = container;
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
            return FluidBarrelItem.getFluid(container);
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            FluidStack current = getFluidInTank(tank);
            // Can accept if empty or same fluid type
            return current.isEmpty() || current.isFluidEqual(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;

            FluidStack current = getFluidInTank(0);
            
            // If not empty, must be same fluid
            if (!current.isEmpty() && !current.isFluidEqual(resource)) {
                return 0;
            }

            int currentAmount = current.isEmpty() ? 0 : current.getAmount();
            int space = CAPACITY - currentAmount;
            int toFill = Math.min(space, resource.getAmount());

            if (toFill > 0 && action.execute()) {
                FluidStack newFluid = new FluidStack(resource.getFluid(), currentAmount + toFill);
                FluidBarrelItem.setFluid(container, newFluid);
            }

            return toFill;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            
            FluidStack current = getFluidInTank(0);
            if (current.isEmpty() || !current.isFluidEqual(resource)) {
                return FluidStack.EMPTY;
            }

            return drain(resource.getAmount(), action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack current = getFluidInTank(0);
            if (current.isEmpty() || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }

            int toDrain = Math.min(current.getAmount(), maxDrain);
            FluidStack drained = new FluidStack(current.getFluid(), toDrain);

            if (action.execute()) {
                int remaining = current.getAmount() - toDrain;
                if (remaining > 0) {
                    FluidBarrelItem.setFluid(container, new FluidStack(current.getFluid(), remaining));
                } else {
                    FluidBarrelItem.setFluid(container, FluidStack.EMPTY);
                }
            }

            return drained;
        }
    }
}
