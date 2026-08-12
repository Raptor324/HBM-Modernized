package com.hbm_m.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

/**
 * Кросс-платформенный и кросс-версионный бак (POJO).
 * Без Stonecutter!
 */
public class ModFluidTank {

    private Fluid conformedFluid = Fluids.EMPTY;
    private final IPlatformFluidHandler backend;

    public ModFluidTank(int capacity) {
        this.backend = FluidHooks.createFluidHandler(capacity, this::isFluidValid, this::onContentsChanged, null, null);
    }

    public boolean isFluidValid(Fluid fluid) {
        return true;
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

    // ── Платформенные хелперы ────────────────────────────────────────────────

    public int getFluidAmountMb() { return backend.getFluidAmountMb(); }
    public int getCapacityMb() { return backend.getCapacityMb(); }
    public boolean isEmpty() { return getFluidAmountMb() <= 0; }
    public int getSpaceMb() { return getCapacityMb() - getFluidAmountMb(); }
    public Fluid getStoredFluid() { return backend.getStoredFluid(); }

    // ──────────────── Fill/Drain в mB ────────────────

    public int fillMb(Fluid fluid, int amountMb) {
        return backend.fillMb(fluid, amountMb, false);
    }

    public int drainMb(int amountMb) {
        return backend.drainMb(amountMb, false);
    }

    public int fillInternal(Fluid fluid, int amount) {
        return fillMb(fluid, amount);
    }

    public int drainInternal(int amount) {
        return drainMb(amount);
    }

    /** 
     * Возвращает LazyOptional<IFluidHandler> (Forge), IFluidHandler (NeoForge)
     * или Storage<FluidVariant> (Fabric).
     */
    public Object getCapability() { return backend.getCapability(); }

    // ──────────────── NBT ────────────────

    public CompoundTag writeNBT(CompoundTag tag) {
        backend.writeNBT(tag);
        if (conformedFluid != Fluids.EMPTY) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(conformedFluid);
            if (loc != null) tag.putString("ConformedFluid", loc.toString());
        }
        return tag;
    }

    public void readNBT(CompoundTag tag) {
        backend.readNBT(tag);
        if (tag.contains("ConformedFluid")) {
            Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.tryParse(tag.getString("ConformedFluid")));
            conformedFluid = f != null ? f : Fluids.EMPTY;
        } else {
            conformedFluid = Fluids.EMPTY;
        }
    }

    public CompoundTag writeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        return writeNBT(tag);
    }

    public void readNBT(HolderLookup.Provider provider, CompoundTag tag) {
        readNBT(tag);
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

    public dev.architectury.fluid.FluidStack getFluid() {
        Fluid f = getStoredFluid();
        if (f == null || f == Fluids.EMPTY) return dev.architectury.fluid.FluidStack.empty();
        return dev.architectury.fluid.FluidStack.create(f, getFluidAmountMb());
    }

    public IPlatformFluidHandler getBackend() {
        return this.backend;
    }
}