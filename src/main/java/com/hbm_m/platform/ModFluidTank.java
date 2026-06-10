package com.hbm_m.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

//? if forge {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;


// * На Forge ModFluidTank — тонкая обёртка над FluidTank.
// * isFluidValid переопределяется анонимным классом или подклассом.
 
public class ModFluidTank extends FluidTank {

    private Fluid conformedFluid = Fluids.EMPTY;

    public ModFluidTank(int capacity) {
        super(capacity);
    }

    public void conform(Fluid type) {
        if (type == null) type = Fluids.EMPTY;
        if (getStoredFluid() != type && !isEmpty()) {
            drain(getFluidAmountMb(), FluidAction.EXECUTE);
        }
        this.conformedFluid = type;
    }

    public void resetTank() {
        if (!isEmpty()) {
            drain(getFluidAmountMb(), FluidAction.EXECUTE);
        }
        this.conformedFluid = Fluids.EMPTY;
    }

    @NotNull
    public Fluid getConfiguredFluid() {
        Fluid stored = getStoredFluid();
        return stored != Fluids.EMPTY ? stored : conformedFluid;
    }

    // ── Платформенные хелперы (для единообразия с Fabric API) ────────────────

//   Кол-во жидкости в мБ.
    public int getFluidAmountMb() {
        return fluid.getAmount();
    }

    //   Емкость танка в мБ.
    public int getCapacityMb() {
        return capacity;
    }

    public boolean isEmpty() {
        return fluid.isEmpty();
    }

//     Свободное место в мБ.
    public int getSpaceMb() {
        return capacity - fluid.getAmount();
    }

//   Тип жидкости (Fluids.EMPTY если пусто).
    public Fluid getStoredFluid() {
        return fluid.isEmpty() ? Fluids.EMPTY : fluid.getFluid();
    }

    // ──────────────── Fill/Drain в mB (как на Fabric) ────────────────

    public int fillMb(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == Fluids.EMPTY) return 0;
        return fill(new FluidStack(fluid, amountMb), FluidAction.EXECUTE);
    }

    public int drainMb(int amountMb) {
        if (amountMb <= 0) return 0;
        return drain(amountMb, FluidAction.EXECUTE).getAmount();
    }


//     * Заполнить tanком amount мБ указанной жидкости (без проверки isFluidValid).
//     * Возвращает реально добавленное количество.
     
    public int fillInternal(Fluid fluid, int amount) {
        if (amount <= 0 || fluid == Fluids.EMPTY) return 0;
        FluidStack stack = new FluidStack(fluid, amount);
        return fill(stack, FluidAction.EXECUTE);
    }


//     * Слить amount мБ (любой жидкости из танка).
//     * Возвращает реально слитое количество.
     
    public int drainInternal(int amount) {
        if (amount <= 0) return 0;
        FluidStack drained = drain(amount, FluidAction.EXECUTE);
        return drained.getAmount();
    }

//     NBT: сохранить.
    public CompoundTag writeNBT(CompoundTag tag) {
        writeToNBT(tag);
        if (conformedFluid != Fluids.EMPTY) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(conformedFluid);
            if (loc != null) tag.putString("ConformedFluid", loc.toString());
        }
        return tag;
    }

//    NBT: загрузить.
    public void readNBT(CompoundTag tag) {
        readFromNBT(tag);
        if (tag.contains("ConformedFluid")) {
            Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.tryParse(tag.getString("ConformedFluid")));
            conformedFluid = f != null ? f : Fluids.EMPTY;
        } else {
            conformedFluid = Fluids.EMPTY;
        }
    }
}
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/^*
 * На Fabric ModFluidTank — обёртка над SingleVariantStorage<FluidVariant>.
 *
 * Единицы: Fabric Transfer API использует «droplets» (1 мБ = 81 droplets).
 * Все публичные методы принимают/возвращают миллибакеты (int/long) для
 * совместимости с логикой машин, написанной под Forge mB.
 ^/
@SuppressWarnings("UnstableApiUsage")
public abstract class ModFluidTank {

    public static final long DROPLETS_PER_MB = 81L;

    private final long capacityDroplets;
    private Fluid conformedFluid = Fluids.EMPTY;

    private final SingleVariantStorage<FluidVariant> storage;

    public ModFluidTank(int capacityMb) {
        this.capacityDroplets = (long) capacityMb * DROPLETS_PER_MB;

        this.storage = new SingleVariantStorage<>() {
            @Override
            protected FluidVariant getBlankVariant() {
                return FluidVariant.blank();
            }

            @Override
            protected long getCapacity(FluidVariant variant) {
                return capacityDroplets;
            }

            @Override
            protected boolean canInsert(FluidVariant variant) {
                return ModFluidTank.this.isFluidValid(variant.getFluid());
            }

            @Override
            protected void onFinalCommit() {
                ModFluidTank.this.onContentsChanged();
            }
        };
    }

    public boolean isFluidValid(Fluid fluid) {
        return true;
    }

    protected void onContentsChanged() {}

    // ──────────────── Conform (порт 1.7.10 FluidTank.conform) ────────────────

    public void conform(Fluid type) {
        if (type == null) type = Fluids.EMPTY;
        Fluid stored = getStoredFluid();
        if (stored != type && !isEmpty()) {
            drainMb(getFluidAmountMb());
        }
        this.conformedFluid = type;
    }

    public void resetTank() {
        if (!isEmpty()) {
            drainMb(getFluidAmountMb());
        }
        this.conformedFluid = Fluids.EMPTY;
    }

    @NotNull
    public Fluid getConfiguredFluid() {
        Fluid stored = getStoredFluid();
        return stored != Fluids.EMPTY ? stored : conformedFluid;
    }

    // ──────────────── Fill ────────────────

    public int fillMb(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == Fluids.EMPTY) return 0;
        if (!isFluidValid(fluid)) return 0;

        FluidVariant variant = FluidVariant.of(fluid);
        long droplets = (long) amountMb * DROPLETS_PER_MB;

        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(variant, droplets, tx);
            tx.commit();
            return (int) (inserted / DROPLETS_PER_MB);
        }
    }

    // ──────────────── Drain ────────────────

    public int drainMb(int amountMb) {
        if (amountMb <= 0) return 0;

        long droplets = (long) amountMb * DROPLETS_PER_MB;

        try (Transaction tx = Transaction.openOuter()) {
            long extracted = storage.extract(storage.getResource(), droplets, tx);
            tx.commit();
            return (int) (extracted / DROPLETS_PER_MB);
        }
    }

    // ──────────────── State ────────────────

    @NotNull
    public Fluid getStoredFluid() {
        FluidVariant variant = storage.getResource();
        return variant.isBlank() ? Fluids.EMPTY : variant.getFluid();
    }

    public int getFluidAmountMb() {
        return (int) (storage.getAmount() / DROPLETS_PER_MB);
    }

    public int getCapacityMb() {
        return (int) (capacityDroplets / DROPLETS_PER_MB);
    }

    public int getSpaceMb() {
        return getCapacityMb() - getFluidAmountMb();
    }

    public boolean isEmpty() {
        return storage.getAmount() == 0;
    }

    public SingleVariantStorage<FluidVariant> getStorage() {
        return storage;
    }

    // ──────────────── NBT ────────────────

    public CompoundTag writeNBT(CompoundTag tag) {
        FluidVariant variant = storage.getResource();
        if (!variant.isBlank()) {
            tag.merge(variant.toNbt());
            tag.putLong("Amount", storage.getAmount());
        }
        if (conformedFluid != Fluids.EMPTY) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(conformedFluid);
            if (loc != null) tag.putString("ConformedFluid", loc.toString());
        }
        return tag;
    }

    public void readNBT(CompoundTag tag) {
        try (Transaction tx = Transaction.openOuter()) {
            // Перезаписываем содержимое, иначе при повторных sync/load будет накапливаться amount.
            FluidVariant cur = storage.getResource();
            long curAmt = storage.getAmount();
            if (!cur.isBlank() && curAmt > 0) {
                storage.extract(cur, curAmt, tx);
            }

            // FluidVariant в Fabric сериализуется как минимум с ключом "fluid".
            // В некоторых окружениях/старых данных мог встречаться "id" — поддержим оба варианта.
            if (tag.contains("fluid") || tag.contains("id")) {
                FluidVariant variant = FluidVariant.fromNbt(tag);
                long amount = tag.getLong("Amount");
                if (!variant.isBlank() && amount > 0) {
                    storage.insert(variant, amount, tx);
                }
            }
            tx.commit();
        }
        if (tag.contains("ConformedFluid")) {
            Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                    net.minecraft.resources.ResourceLocation.tryParse(tag.getString("ConformedFluid")));
            conformedFluid = f != null ? f : Fluids.EMPTY;
        } else {
            conformedFluid = Fluids.EMPTY;
        }
    }
}
*///?}