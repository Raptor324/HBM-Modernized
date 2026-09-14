package com.hbm_m.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Изолированный и максимально чистый API для работы с жидкостями.
 * Поддерживает 1.20.1 (Forge) и 1.21.1 (NeoForge)
 */
public final class FluidHooks {
    private FluidHooks() {}

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
        
        //? if < 1.21.1 {
        net.minecraftforge.fluids.capability.IFluidHandlerItem handler = copy.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler == null) return FluidExtraction.EMPTY;
        net.minecraftforge.fluids.FluidStack drained = handler.drain(maxAmount, simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        //?} else {
        /*net.neoforged.neoforge.fluids.capability.IFluidHandlerItem handler = copy.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if (handler == null) return FluidExtraction.EMPTY;
        net.neoforged.neoforge.fluids.FluidStack drained = handler.drain(maxAmount, simulate ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        *///?}

        if (drained.isEmpty()) return FluidExtraction.EMPTY;
        return new FluidExtraction(drained.getFluid(), drained.getAmount(), handler.getContainer());
    }

    /** Вливание конкретной жидкости в предмет. */
    public static FluidInsertion insertFluidIntoItem(ItemStack stack, Fluid fluid, int amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0 || fluid == null || fluid == Fluids.EMPTY) return FluidInsertion.EMPTY;
        
        ItemStack copy = stack.copy();
        copy.setCount(1);
        
        //? if < 1.21.1 {
        net.minecraftforge.fluids.capability.IFluidHandlerItem handler = copy.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler == null) return FluidInsertion.EMPTY;
        int filled = handler.fill(new net.minecraftforge.fluids.FluidStack(fluid, amount), simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        //?} else {
        /*net.neoforged.neoforge.fluids.capability.IFluidHandlerItem handler = copy.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if (handler == null) return FluidInsertion.EMPTY;
        int filled = handler.fill(new net.neoforged.neoforge.fluids.FluidStack(fluid, amount), simulate ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        *///?}
        
        if (filled <= 0) return FluidInsertion.EMPTY;
        return new FluidInsertion(filled, handler.getContainer());
    }

    /** Максимальная вместимость предмета-контейнера. */
    public static int getItemFluidCapacity(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        //? if < 1.21.1 {
        net.minecraftforge.fluids.capability.IFluidHandlerItem handler = stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        //?} else {
        /*net.neoforged.neoforge.fluids.capability.IFluidHandlerItem handler = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        *///?}
        if (handler == null) return 0;
        int max = 0;
        for (int i = 0; i < handler.getTanks(); i++) max = Math.max(max, handler.getTankCapacity(i));
        return max;
    }

    /** 
     * Умная проверка стакаемости, исправляющая баг с бочками. 
     * Считает `null` тег и пустой `{}` тег абсолютно одинаковыми.
     */
    public static boolean areItemsStackable(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.getItem() != b.getItem()) return false;
        
        //? if < 1.21.1 {
        net.minecraft.nbt.CompoundTag tagA = a.getTag();
        net.minecraft.nbt.CompoundTag tagB = b.getTag();
        boolean emptyA = tagA == null || tagA.isEmpty();
        boolean emptyB = tagB == null || tagB.isEmpty();
        if (emptyA && emptyB) return true;
        if (emptyA || emptyB) return false;
        return tagA.equals(tagB);
        //?} else {
        /*return ItemStack.isSameItemSameComponents(a, b);
        *///?}
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
            //? if < 1.21.1 {
            private final net.minecraftforge.fluids.capability.templates.FluidTank forgeTank = new net.minecraftforge.fluids.capability.templates.FluidTank(capacity) {
                @Override public boolean isFluidValid(net.minecraftforge.fluids.FluidStack stack) { return fluidValidator.test(stack.getFluid()); }
                @Override protected void onContentsChanged() { if (onContentsChanged != null) onContentsChanged.run(); }
                @Override public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
                    if (resource.isEmpty() || !isFluidValid(resource)) return 0;
                    Fluid coerced = fillHarmonizer != null ? fillHarmonizer.apply(resource.getFluid()) : resource.getFluid();
                    return super.fill(new net.minecraftforge.fluids.FluidStack(coerced, resource.getAmount()), action);
                }
                @Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
                    net.minecraftforge.fluids.FluidStack d = super.drain(resource, action);
                    if (d.isEmpty()) return d;
                    Fluid coerced = drainHarmonizer != null ? drainHarmonizer.apply(d.getFluid()) : d.getFluid();
                    return new net.minecraftforge.fluids.FluidStack(coerced, d.getAmount());
                }
                @Override public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
                    net.minecraftforge.fluids.FluidStack d = super.drain(maxDrain, action);
                    if (d.isEmpty()) return d;
                    Fluid coerced = drainHarmonizer != null ? drainHarmonizer.apply(d.getFluid()) : d.getFluid();
                    return new net.minecraftforge.fluids.FluidStack(coerced, d.getAmount());
                }
            };
            private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> lazyCap = net.minecraftforge.common.util.LazyOptional.of(() -> forgeTank);
            //?} else {
            /*private final net.neoforged.neoforge.fluids.capability.templates.FluidTank forgeTank = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(capacity) {
                @Override public boolean isFluidValid(net.neoforged.neoforge.fluids.FluidStack stack) { return fluidValidator.test(stack.getFluid()); }
                @Override protected void onContentsChanged() { if (onContentsChanged != null) onContentsChanged.run(); }
                @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
                    if (resource.isEmpty() || !isFluidValid(resource)) return 0;
                    Fluid coerced = fillHarmonizer != null ? fillHarmonizer.apply(resource.getFluid()) : resource.getFluid();
                    return super.fill(new net.neoforged.neoforge.fluids.FluidStack(coerced, resource.getAmount()), action);
                }
                @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
                    net.neoforged.neoforge.fluids.FluidStack d = super.drain(resource, action);
                    if (d.isEmpty()) return d;
                    Fluid coerced = drainHarmonizer != null ? drainHarmonizer.apply(d.getFluid()) : d.getFluid();
                    return new net.neoforged.neoforge.fluids.FluidStack(coerced, d.getAmount());
                }
                @Override public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
                    net.neoforged.neoforge.fluids.FluidStack d = super.drain(maxDrain, action);
                    if (d.isEmpty()) return d;
                    Fluid coerced = drainHarmonizer != null ? drainHarmonizer.apply(d.getFluid()) : d.getFluid();
                    return new net.neoforged.neoforge.fluids.FluidStack(coerced, d.getAmount());
                }
            };
            *///?}

            @Override public int getFluidAmountMb() { return forgeTank.getFluidAmount(); }
            @Override public int getCapacityMb() { return forgeTank.getCapacity(); }
            @Override public void setCapacityMb(int cap) { forgeTank.setCapacity(cap); }
            @Override public Fluid getStoredFluid() { return forgeTank.getFluid().isEmpty() ? Fluids.EMPTY : forgeTank.getFluid().getFluid(); }
            
            @Override public int fillMb(Fluid fluid, int amount, boolean simulate) {
                if (amount <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
                //? if < 1.21.1 {
                return forgeTank.fill(new net.minecraftforge.fluids.FluidStack(fluid, amount), simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                //?} else {
                /*return forgeTank.fill(new net.neoforged.neoforge.fluids.FluidStack(fluid, amount), simulate ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                *///?}
            }
            @Override public int drainMb(int amount, boolean simulate) {
                if (amount <= 0) return 0;
                //? if < 1.21.1 {
                return forgeTank.drain(amount, simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount();
                //?} else {
                /*return forgeTank.drain(amount, simulate ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount();
                *///?}
            }
            @Override public void setFluid(Fluid fluid, int amount) {
                //? if < 1.21.1 {
                forgeTank.setFluid(fluid == null || fluid == Fluids.EMPTY ? net.minecraftforge.fluids.FluidStack.EMPTY : new net.minecraftforge.fluids.FluidStack(fluid, amount));
                //?} else {
                /*forgeTank.setFluid(fluid == null || fluid == Fluids.EMPTY ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : new net.neoforged.neoforge.fluids.FluidStack(fluid, amount));
                *///?}
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
            //?} else {
            /*@Override public net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.nbt.CompoundTag tag) { return forgeTank.writeToNBT(net.minecraft.core.RegistryAccess.EMPTY, tag); }
            @Override public void readNBT(net.minecraft.nbt.CompoundTag tag) { forgeTank.readFromNBT(net.minecraft.core.RegistryAccess.EMPTY, tag); }
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
}