package com.hbm_m.platform;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Изолированный и максимально чистый API для работы с жидкостями.
 * Поддерживает 1.20.1 (Forge) и 1.21.1 (NeoForge)
 */
public final class FluidHooks {
    private FluidHooks() {}

    // ЕДИНСТВЕННЫЙ lookup IFluidHandlerItem для всего класса (был 4× копипаст гейтов).
    // Пакеты IFluidHandlerItem/FluidStack автозаменяются !loader_packages на neoforge.
    //? if < 1.21.1 {
    private static net.minecraftforge.fluids.capability.IFluidHandlerItem getItemFluidHandler(ItemStack stack) {
        return stack.isEmpty() ? null
                : stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
    }
    //?} else {
    /*private static net.minecraftforge.fluids.capability.IFluidHandlerItem getItemFluidHandler(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
    }
    *///?}

    /** Есть ли у предмета жидкостная capability (замена прямых ForgeCapabilities-проверок). */
    public static boolean isFluidContainer(ItemStack stack) {
        return stack != null && getItemFluidHandler(stack) != null;
    }

    public record FluidExtraction(Fluid fluid, int amount, ItemStack remainder) {
        public static final FluidExtraction EMPTY = new FluidExtraction(Fluids.EMPTY, 0, ItemStack.EMPTY);
    }

    public record FluidInsertion(int amountInserted, ItemStack remainder) {
        public static final FluidInsertion EMPTY = new FluidInsertion(0, ItemStack.EMPTY);
    }

    /**
     * Попытка извлечь ЛЮБУЮ жидкость из предмета (аналог drain(amount)).
     * Мы больше не передаем targetFluid, чтобы избежать конфликтов ванильных ведер и HBM-жидкостей.
     */
    public static FluidExtraction extractFluidFromItem(ItemStack stack, int maxAmount, boolean simulate) {
        if (stack.isEmpty() || maxAmount <= 0) return FluidExtraction.EMPTY;

        ItemStack copy = stack.copy();
        copy.setCount(1); // Работаем только с одним предметом из стака

        net.minecraftforge.fluids.capability.IFluidHandlerItem handler = getItemFluidHandler(copy);
        if (handler == null) return FluidExtraction.EMPTY;
        net.minecraftforge.fluids.FluidStack drained = handler.drain(maxAmount,
                simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                         : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        if (drained.isEmpty()) return FluidExtraction.EMPTY;
        return new FluidExtraction(drained.getFluid(), drained.getAmount(), handler.getContainer());
    }

    /** Вливание конкретной жидкости в предмет. */
    public static FluidInsertion insertFluidIntoItem(ItemStack stack, Fluid fluid, int amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0 || fluid == null || fluid == Fluids.EMPTY) return FluidInsertion.EMPTY;

        ItemStack copy = stack.copy();
        copy.setCount(1);

        net.minecraftforge.fluids.capability.IFluidHandlerItem handler = getItemFluidHandler(copy);
        if (handler == null) return FluidInsertion.EMPTY;
        int filled = handler.fill(new net.minecraftforge.fluids.FluidStack(fluid, amount),
                simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                         : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        if (filled <= 0) return FluidInsertion.EMPTY;
        return new FluidInsertion(filled, handler.getContainer());
    }

    /** Максимальная вместимость предмета-контейнера. */
    public static int getItemFluidCapacity(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        net.minecraftforge.fluids.capability.IFluidHandlerItem handler = getItemFluidHandler(stack);
        if (handler == null) return 0;
        int max = 0;
        for (int i = 0; i < handler.getTanks(); i++) max = Math.max(max, handler.getTankCapacity(i));
        return max;
    }

    /**
     * 64-битный фингерпринт Architectury FluidStack для per-tick подписей
     * (бывший PlatformHooks.fluidFingerprint). 0 для пустого стека;
     * различает variant (fluid) и amount.
     */
    public static long fingerprint(dev.architectury.fluid.FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return 0L;
        return fluid.getFluid().hashCode() * 31L + fluid.getAmount();
    }

    /** Фабрика платформенного бака (скрывает инициализацию capabilities/storages). */
    public static IPlatformFluidHandler createFluidHandler(
            int capacity, 
            java.util.function.Predicate<Fluid> fluidValidator, 
            Runnable onContentsChanged,
            java.util.function.UnaryOperator<Fluid> fillHarmonizer,
            java.util.function.UnaryOperator<Fluid> drainHarmonizer
    ) {
        return new IPlatformFluidHandler() {
            // Тело единое для обеих версий: пакеты FluidStack/FluidTank/FluidAction
            // переписываются автозаменой !loader_packages при neoforge-prepare.
            private final net.minecraftforge.fluids.capability.templates.FluidTank forgeTank = new net.minecraftforge.fluids.capability.templates.FluidTank(capacity) {
                @Override public boolean isFluidValid(net.minecraftforge.fluids.FluidStack stack) { return fluidValidator.test(stack.getFluid()); }
                @Override protected void onContentsChanged() { if (onContentsChanged != null) onContentsChanged.run(); }
                @Override public int fill(net.minecraftforge.fluids.FluidStack resource, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
                    if (resource.isEmpty()) return 0;
                    // ВАЛИДАЦИЯ ПОСЛЕ гармонизатора: валидатор настроен на результирующую
                    // жидкость, сырой pre-check отбрасывал валидные заливки (0 до fillHarmonizer).
                    Fluid targetFluid = fillHarmonizer != null ? fillHarmonizer.apply(resource.getFluid()) : resource.getFluid();
                    net.minecraftforge.fluids.FluidStack toFill = targetFluid == resource.getFluid()
                            ? resource : new net.minecraftforge.fluids.FluidStack(targetFluid, resource.getAmount());
                    if (!isFluidValid(toFill)) return 0;
                    return super.fill(toFill, action);
                }
                // Пост-обработка drain'а без аллокации, когда гармонизатор не задан/не меняет жидкость
                private net.minecraftforge.fluids.FluidStack harmonizeDrained(net.minecraftforge.fluids.FluidStack d) {
                    if (d.isEmpty() || drainHarmonizer == null) return d;
                    Fluid coerced = drainHarmonizer.apply(d.getFluid());
                    return coerced == d.getFluid() ? d : new net.minecraftforge.fluids.FluidStack(coerced, d.getAmount());
                }
                @Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
                    return harmonizeDrained(super.drain(resource, action));
                }
                @Override public net.minecraftforge.fluids.FluidStack drain(int maxDrain, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
                    return harmonizeDrained(super.drain(maxDrain, action));
                }
            };
            //? if < 1.21.1 {
            private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> lazyCap = net.minecraftforge.common.util.LazyOptional.of(() -> forgeTank);
            //?}

            @Override public int getFluidAmountMb() { return forgeTank.getFluidAmount(); }
            @Override public int getCapacityMb() { return forgeTank.getCapacity(); }
            @Override public void setCapacityMb(int cap) { forgeTank.setCapacity(cap); }
            @Override public Fluid getStoredFluid() { return forgeTank.getFluid().isEmpty() ? Fluids.EMPTY : forgeTank.getFluid().getFluid(); }

            @Override public int fillMb(Fluid fluid, int amount, boolean simulate) {
                if (amount <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
                return forgeTank.fill(new net.minecraftforge.fluids.FluidStack(fluid, amount),
                        simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                                 : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
            @Override public int drainMb(int amount, boolean simulate) {
                if (amount <= 0) return 0;
                return forgeTank.drain(amount,
                        simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                                 : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
            @Override public void setFluid(Fluid fluid, int amount) {
                forgeTank.setFluid(fluid == null || fluid == Fluids.EMPTY
                        ? net.minecraftforge.fluids.FluidStack.EMPTY
                        : new net.minecraftforge.fluids.FluidStack(fluid, amount));
            }
            @Override public Object getCapability() {
                //? if < 1.21.1 {
                return lazyCap;
                //?} else {
                /*return forgeTank;
                *///?}
            }
            //? if < 1.21.1 {
            @Override public net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.nbt.CompoundTag tag) { return forgeTank.writeToNBT(tag); }
            @Override public void readNBT(net.minecraft.nbt.CompoundTag tag) { forgeTank.readFromNBT(tag); }
            @Override public net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag tag) { return forgeTank.writeToNBT(tag); }
            @Override public void readNBT(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag tag) { forgeTank.readFromNBT(tag); }
            //?} else {
            /*@Override public net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.nbt.CompoundTag tag) {
                return writeNBT(null, tag);
            }
            @Override public void readNBT(net.minecraft.nbt.CompoundTag tag) {
                readNBT(null, tag);
            }
            @Override public net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag tag) {
                net.minecraft.core.HolderLookup.Provider p = provider != null ? provider : PlatformHooks.bestEffortProvider();
                return forgeTank.writeToNBT(p != null ? p : net.minecraft.core.RegistryAccess.EMPTY, tag);
            }
            @Override public void readNBT(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag tag) {
                net.minecraft.core.HolderLookup.Provider p = provider != null ? provider : PlatformHooks.bestEffortProvider();
                forgeTank.readFromNBT(p != null ? p : net.minecraft.core.RegistryAccess.EMPTY, tag);
            }
            *///?}
        };
    }

    public record FluidActionResult(boolean isSuccess, ItemStack getResult) {}

    public static FluidActionResult tryEmptyContainer(ItemStack container, IPlatformFluidHandler tank, int maxAmount, boolean simulate) {
        if (container.isEmpty()) return new FluidActionResult(false, container);
        FluidExtraction extraction = extractFluidFromItem(container, maxAmount, true);
        if (extraction.amount() <= 0) return new FluidActionResult(false, container);
        
        int filled = tank.fillMb(extraction.fluid(), extraction.amount(), true);
        if (filled <= 0) return new FluidActionResult(false, container);
        
        if (!simulate) {
            FluidExtraction exec = extractFluidFromItem(container, filled, false);
            tank.fillMb(exec.fluid(), exec.amount(), false);
            return new FluidActionResult(true, exec.remainder());
        }
        return new FluidActionResult(true, extraction.remainder());
    }

    public static Object getRawFluidHandler(Object capabilityObj) {
        //? if < 1.21.1 {
        if (capabilityObj instanceof net.minecraftforge.common.util.LazyOptional<?> opt) {
            return opt.orElse(null);
        }
        //?}
        return capabilityObj;
    }

    /**
     * Loader fluid-handler lookup on a block face (analog of NEXT {@code Services.CAPS
     * .findFluidHandler(world, pos, side)} in {@code Library.canConnectFluid}).
     *
     * @return the block's fluid handler for that face, or {@code null}
     */
    @Nullable
    public static Object getBlockFluidHandler(net.minecraft.world.level.Level level,
                                              net.minecraft.core.BlockPos pos,
                                              @Nullable net.minecraft.core.Direction side) {
        //? if forge {
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.isRemoved()) return null;
        return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
        //?} elif neoforge {
        /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
        *///?}
    }
}