package com.hbm_m.item.liquids;

import java.util.List;
//? if fabric {
import java.util.Iterator;
//?}

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

//? if forge {
/*import org.jetbrains.annotations.NotNull;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
//?}

public class InfiniteWaterItem extends Item {

    private final int transferRate; // Скорость передачи (mB/tick)

    public InfiniteWaterItem(Properties properties, int transferRate) {
        super(properties);
        this.transferRate = transferRate;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§bInfinite Water Source"));
        tooltip.add(Component.literal("§7Output Rate: §e" + transferRate + " mB/t"));
    }

    // ================================================================== //
    //  FORGE — ICapabilityProvider + IFluidHandlerItem                   //
    // ================================================================== //

    //? if forge {
    /*@Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new InfiniteWaterCapabilityProvider(stack, transferRate);
    }

    private static class InfiniteWaterCapabilityProvider implements ICapabilityProvider {
        private final InfiniteWaterHandler handler;
        private final LazyOptional<IFluidHandlerItem> optional;

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        InfiniteWaterCapabilityProvider(ItemStack stack, int rate) {
            this.handler = new InfiniteWaterHandler(stack, rate);
            this.optional = LazyOptional.of(() -> handler);
        }
    }

    private static class InfiniteWaterHandler implements IFluidHandlerItem {
        private final ItemStack container;
        private final int rate;

        InfiniteWaterHandler(ItemStack container, int rate) {
            this.container = container;
            this.rate = rate;
        }

        @NotNull
        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {
            // Визуально показываем, что бак всегда полон воды
            return new FluidStack(Fluids.WATER, Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            // Нельзя залить ничего другого
            return stack.getFluid() == Fluids.WATER;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Это источник, его нельзя наполнить (или можно сказать что он уже полон)
            return 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != Fluids.WATER) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            // ВОТ ТУТ МАГИЯ
            // 1. Мы ограничиваем выход скоростью (rate) ИЛИ запросом (maxDrain)
            int amountToDrain = Math.min(maxDrain, rate);

            // 2. Мы всегда возвращаем воду
            // 3. Мы игнорируем action.EXECUTE, потому что источник бесконечный
            // (мы не уменьшаем никакие переменные внутри)
            return new FluidStack(Fluids.WATER, amountToDrain);
        }
    }
    *///?}

    // ================================================================== //
    //  FABRIC — Fabric Transfer API (Storage)                            //
    //  Регистрацию делай через FluidStorage.ITEM.registerForItems(...)    //
    // ================================================================== //

    //? if fabric {
    public Storage<FluidVariant> createFabricStorage(ContainerItemContext context) {
        return new Storage<FluidVariant>() {
            @Override
            public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                // Источник воды не принимает жидкость
                return 0;
            }

            @Override
            public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                if (resource.isBlank() || resource.getFluid() != Fluids.WATER) return 0;
                return Math.min(maxAmount, transferRate);
            }

            @Override
            public Iterator<StorageView<FluidVariant>> iterator() {
                return List.<StorageView<FluidVariant>>of(new StorageView<FluidVariant>() {
                    @Override
                    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                        if (resource.isBlank() || resource.getFluid() != Fluids.WATER) return 0;
                        return Math.min(maxAmount, transferRate);
                    }

                    @Override
                    public boolean isResourceBlank() {
                        return false;
                    }

                    @Override
                    public FluidVariant getResource() {
                        return FluidVariant.of(Fluids.WATER);
                    }

                    @Override
                    public long getAmount() {
                        // Визуально показываем бесконечность
                        return Long.MAX_VALUE;
                    }

                    @Override
                    public long getCapacity() {
                        return Long.MAX_VALUE;
                    }
                }).iterator();
            }
        };
    }
    //?}
}
