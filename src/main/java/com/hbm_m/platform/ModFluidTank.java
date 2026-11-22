package com.hbm_m.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

/**
 * Кросс-платформенный и кросс-версионный бак.
 * Без Stonecutter-гейтов в теле: пакеты net.minecraftforge.fluids.* автозаменяются
 * !loader_packages при neoforge-prepare.
 *
 * Слои «схлопнуты»: класс сам реализует {@link IPlatformFluidHandler} и держит
 * единственный объект — анонимный наследник loader-FluidTank. Делегат
 * из FluidHooks.createFluidHandler и промежуточный backend больше не нужны.
 */
public class ModFluidTank implements IPlatformFluidHandler {

    private Fluid conformedFluid = Fluids.EMPTY;

    /** Единственный платформенный объект: loader-FluidTank с валидатором. */
    private final net.minecraftforge.fluids.capability.templates.FluidTank tank;
    //? if < 1.21.1 {
    private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> lazyCap;
    //?}

    /** Кэш architectury-стека для {@link #getFluid()}; инвалидируется в onContentsChanged/setFluid/readNBT. */
    private dev.architectury.fluid.FluidStack cachedFluidStack;

    public ModFluidTank(int capacity) {
        this.tank = new net.minecraftforge.fluids.capability.templates.FluidTank(capacity) {
            @Override public boolean isFluidValid(net.minecraftforge.fluids.FluidStack stack) { return ModFluidTank.this.isFluidValid(stack.getFluid()); }
            @Override protected void onContentsChanged() {
                // Инвалидация ЗДЕСЬ, а не в fillMb/drainMb: через capability (трубы, FluidUtil)
                // зовут tank.fill/drain напрямую, минуя fillMb/drainMb. simulate-вызовы
                // содержимое не меняют и кэш не сбрасывают.
                ModFluidTank.this.cachedFluidStack = null;
                ModFluidTank.this.onContentsChanged();
            }
        };
        //? if < 1.21.1 {
        this.lazyCap = net.minecraftforge.common.util.LazyOptional.of(() -> tank);
        //?}
    }

    /**
     * Валидация заливаемой жидкости. Тип, зафиксированный через {@link #conform(Fluid)},
     * принимается ВСЕГДА и только он — независимо от заполненности бака (внешние
     * трубы/хендлеры проверяют isFluidValid ДО заливки и не должны видеть «бак пуст
     * и принимает что угодно» или «бак полон, но валидно всё»).
     */
    public boolean isFluidValid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        return conformedFluid == Fluids.EMPTY || fluid == conformedFluid;
    }

    protected void onContentsChanged() {}

    public void conform(Fluid type) {
        if (type == null) type = Fluids.EMPTY;
        if (getStoredFluid() != type && !isEmpty()) {
            drainMb(getFluidAmountMb());
        }
        this.conformedFluid = type;
    }

    public void resetTank() {
        if (!isEmpty()) drainMb(getFluidAmountMb());
        this.conformedFluid = Fluids.EMPTY;
    }

    @NotNull
    public Fluid getConfiguredFluid() {
        Fluid stored = getStoredFluid();
        return stored != Fluids.EMPTY ? stored : conformedFluid;
    }

    // ── Платформенные хелперы (прямые, без делегата) ────────────────────────

    public int getFluidAmountMb() { return tank.getFluidAmount(); }
    public int getCapacityMb() { return tank.getCapacity(); }
    public boolean isEmpty() { return getFluidAmountMb() <= 0; }
    public int getSpaceMb() { return getCapacityMb() - getFluidAmountMb(); }
    public Fluid getStoredFluid() { return tank.getFluid().isEmpty() ? Fluids.EMPTY : tank.getFluid().getFluid(); }

    // ──────────────── Fill/Drain в mB ────────────────

    public int fillMb(Fluid fluid, int amountMb) {
        return fillMb(fluid, amountMb, false);
    }

    public int drainMb(int amountMb) {
        return drainMb(amountMb, false);
    }

    @Override
    public int fillMb(Fluid fluid, int amount, boolean simulate) {
        if (amount <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        return tank.fill(new net.minecraftforge.fluids.FluidStack(fluid, amount),
                simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                         : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int drainMb(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        return tank.drain(amount,
                simulate ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                         : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount();
    }

    @Override
    public void setFluid(Fluid fluid, int amount) {
        cachedFluidStack = null;
        tank.setFluid(fluid == null || fluid == Fluids.EMPTY
                ? net.minecraftforge.fluids.FluidStack.EMPTY
                : new net.minecraftforge.fluids.FluidStack(fluid, amount));
    }

    @Override
    public void setCapacityMb(int cap) { tank.setCapacity(cap); }

    /**
     * Возвращает LazyOptional<IFluidHandler> (Forge) или IFluidHandler (NeoForge).
     */
    @Override
    public Object getCapability() {
        //? if < 1.21.1 {
        return lazyCap;
        //?} else {
        /*return tank;
        *///?}
    }

    // ──────────────── NBT ────────────────

    public CompoundTag writeNBT(CompoundTag tag) {
        return writeNBT(null, tag);
    }

    public void readNBT(CompoundTag tag) {
        readNBT(null, tag);
    }

    public CompoundTag writeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        //? if < 1.21.1 {
        tank.writeToNBT(tag);
        //?} else {
        /*net.minecraft.core.HolderLookup.Provider p = provider != null ? provider : PlatformHooks.bestEffortProvider();
        tank.writeToNBT(p != null ? p : net.minecraft.core.RegistryAccess.EMPTY, tag);
        *///?}
        if (conformedFluid != Fluids.EMPTY) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(conformedFluid);
            if (loc != null) tag.putString("ConformedFluid", loc.toString());
        }
        return tag;
    }

    public void readNBT(HolderLookup.Provider provider, CompoundTag tag) {
        //? if < 1.21.1 {
        tank.readFromNBT(tag);
        //?} else {
        /*net.minecraft.core.HolderLookup.Provider p = provider != null ? provider : PlatformHooks.bestEffortProvider();
        tank.readFromNBT(p != null ? p : net.minecraft.core.RegistryAccess.EMPTY, tag);
        *///?}
        cachedFluidStack = null;
        if (tag.contains("ConformedFluid")) {
            Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.tryParse(tag.getString("ConformedFluid")));
            conformedFluid = f != null ? f : Fluids.EMPTY;
        } else {
            conformedFluid = Fluids.EMPTY;
        }
    }

    // ──────────────── МЕТОДЫ ОБРАТНОЙ СОВМЕСТИМОСТИ ────────────────

    public CompoundTag writeToNBT(CompoundTag tag, String key) {
        CompoundTag subTag = new CompoundTag();
        this.writeNBT(subTag);
        tag.put(key, subTag);
        return tag;
    }

    public void readFromNBT(CompoundTag tag, String key) {
        if (tag.contains(key)) {
            this.readNBT(tag.getCompound(key));
        }
    }

    /**
     * Architectury-стек. Кэшируется: аллокация только при первом обращении после
     * изменения содержимого (fill/drain/setFluid/readNBT), а не на каждый вызов.
     */
    public dev.architectury.fluid.FluidStack getFluid() {
        if (cachedFluidStack != null) return cachedFluidStack;
        Fluid f = getStoredFluid();
        if (f == null || f == Fluids.EMPTY) {
            // Пустой стек — константа, кэшировать нечего.
            return dev.architectury.fluid.FluidStack.empty();
        }
        dev.architectury.fluid.FluidStack stack = dev.architectury.fluid.FluidStack.create(f, getFluidAmountMb());
        cachedFluidStack = stack;
        return stack;
    }

    /**
     * Этот класс и есть платформенный бэкенд (слои схлопнуты).
     */
    public IPlatformFluidHandler getBackend() {
        return this;
    }
}
