package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;

//? if fabric {
/*import com.hbm_m.item.liquids.FluidBarrelItem;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.SimpleContainer;
*///?}

import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
//?}

/**
 * Стандартный загрузчик жидкости (ведро/бочка → бак и бак → ведро/бочка).
 * Порт FluidLoaderStandard из 1.7.10.
 *
 * <p><b>Семантика «всё или ничего»:</b> каждая бочка/ведро полностью опустошается
 * (или полностью заполняется) за один тик, либо остаётся в верхнем слоте до тех пор,
 * пока в танке не накопится достаточно свободного места / жидкости.  Частичный перенос
 * не производится — это предотвращает застревание полупустых бочек в выходном слоте.</p>
 *
 * <p><b>Важно:</b> на Forge {@code FluidUtil.tryFillContainer/tryEmptyContainer}
 * при {@code doFill/doDrain=true} выполняют drain+fill в два приёма без rollback;
 * совместно с {@link FluidTank}-override {@code drain()} (нормализация типа)
 * это приводит к потере жидкости.  Поэтому здесь используется ручной
 * drain→fill с жёсткой проверкой amount.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class FluidLoaderStandard implements FluidTank.LoadingHandler {

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack inputStack = slots[in];
        if (inputStack == null || inputStack.isEmpty()) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        if (inputStack.getItem() instanceof com.hbm_m.item.liquids.InfiniteFluidItem) {
            return false;
        }

        //? if forge {
        if (tank.getPressure() != 0) return false;

        ItemStack one = inputStack.copy();
        one.setCount(1);

        IFluidHandlerItem itemHandler = one.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (itemHandler == null) return false;

        // 1. Симуляция: сколько жидкости в контейнере
        FluidStack available = itemHandler.drain(Integer.MAX_VALUE, IFluidHandlerItem.FluidAction.SIMULATE);
        if (available.isEmpty()) return false;

        int barrelFill = available.getAmount();

        // 2. Совместимость с баком
        boolean tankEmpty = !FluidTank.isFluidTypeExplicitlySet(tank.getTankType()) || tank.getFill() <= 0;
        if (!tankEmpty && !VanillaFluidEquivalence.sameSubstance(tank.getTankType(), available.getFluid())) return false;

        int space = tank.getMaxFill() - tank.getFill();

        // 3. All-or-nothing: сливаем только если бак полностью вмещает содержимое бочки.
        //    Иначе (особенно если стак = 1 бочка) ждём, пока в танке не освободится место.
        if (space < barrelFill) return false;

        // 4. Симуляция fill в бак
        IFluidHandler tankHandler = tank.getCapability().orElse(null);
        if (tankHandler == null) return false;
        FluidStack toDrain = new FluidStack(available.getFluid(), barrelFill);
        int filledSim = tankHandler.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
        if (filledSim != barrelFill) return false;

        // 5. Симуляция результата на копии (IFluidHandlerItem.getContainer при SIMULATE не меняется)
        ItemStack simCopy = inputStack.copy();
        simCopy.setCount(1);
        IFluidHandlerItem simHandler = simCopy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (simHandler == null) return false;
        FluidStack simDrained = simHandler.drain(toDrain, IFluidHandlerItem.FluidAction.EXECUTE);
        if (simDrained.isEmpty() || simDrained.getAmount() != barrelFill) return false;
        ItemStack simResult = simHandler.getContainer();
        if (!FluidTank.canPlaceItemInSlot(slots, out, simResult)) {
            simHandler.fill(simDrained, IFluidHandlerItem.FluidAction.EXECUTE); // rollback
            return false;
        }

        // 6. Выполнение на оригинале
        FluidStack drainedReal = itemHandler.drain(toDrain, IFluidHandlerItem.FluidAction.EXECUTE);
        if (drainedReal.isEmpty() || drainedReal.getAmount() != barrelFill) return false;
        int filledReal = tankHandler.fill(drainedReal, IFluidHandler.FluidAction.EXECUTE);
        if (filledReal != barrelFill) {
            itemHandler.fill(drainedReal, IFluidHandlerItem.FluidAction.EXECUTE); // rollback
            return false;
        }

        FluidTank.placeItemInSlot(slots, out, itemHandler.getContainer());
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        //?}

        //? if fabric {
        /*ItemStack simCopy = inputStack.copy();
        simCopy.setCount(1);
        SimpleContainer simInv = new SimpleContainer(simCopy);
        ContainerItemContext simCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(simInv, null).getSlot(0));
        Storage<FluidVariant> simStorage = FluidStorage.ITEM.find(simCopy, simCtx);
        if (simStorage == null) return false;

        FluidVariant storedVariant = FluidVariant.blank();
        long barrelDroplets = 0L;
        for (StorageView<FluidVariant> view : simStorage) {
            if (!view.isResourceBlank() && view.getAmount() > 0) {
                storedVariant = view.getResource();
                barrelDroplets = view.getAmount();
                break;
            }
        }

        if (storedVariant.isBlank()) return false;
        if (!VanillaFluidEquivalence.sameSubstance(tank.getTankType(), storedVariant.getFluid())) {
            return false;
        }

        int space = tank.getMaxFill() - tank.getFill();
        long spaceDroplets = (long) space * 81L;

        // All-or-nothing
        if (spaceDroplets < barrelDroplets) return false;

        try (Transaction tx = Transaction.openOuter()) {
            long drainableSim = simStorage.extract(storedVariant, barrelDroplets, tx);
            if (drainableSim != barrelDroplets) return false;
            if (!FluidTank.canPlaceItemInSlot(slots, out, simInv.getItem(0))) return false;
        }

        ItemStack execCopy = inputStack.copy();
        execCopy.setCount(1);
        SimpleContainer execInv = new SimpleContainer(execCopy);
        ContainerItemContext execCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(execInv, null).getSlot(0));
        Storage<FluidVariant> execStorage = FluidStorage.ITEM.find(execCopy, execCtx);
        if (execStorage == null) return false;

        long drained;
        try (Transaction tx = Transaction.openOuter()) {
            drained = execStorage.extract(storedVariant, barrelDroplets, tx);
            if (drained > 0) tx.commit();
        }
        if (drained <= 0) return false;

        // На Fabric нельзя использовать tank.fill/setFill когда бак пустой:
        // setFill опирается на getStoredFluid() (variant), и если variant blank — жидкость не "коммитится".
        int mb = (int) (drained / 81L);
        if (mb <= 0) return false;
        int filled = tank.fillMb(storedVariant.getFluid(), mb);
        if (filled != mb) return false;
        FluidTank.placeItemInSlot(slots, out, execInv.getItem(0));
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        *///?}
    }

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack inputStack = slots[in];
        if (inputStack == null || inputStack.isEmpty() || tank.getFill() <= 0) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        if (inputStack.getItem() instanceof com.hbm_m.item.liquids.InfiniteFluidItem) {
            return false;
        }

        //? if forge {
        if (tank.getPressure() != 0) return false;

        ItemStack one = inputStack.copy();
        one.setCount(1);

        IFluidHandlerItem itemHandler = one.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (itemHandler == null) return false;

        int itemCapacity = itemHandler.getTankCapacity(0);
        if (itemCapacity <= 0) return false;

        IFluidHandler tankHandler = tank.getCapability().orElse(null);
        if (tankHandler == null) return false;

        int tankFill = tank.getFill();

        // 1. All-or-nothing: наполняем только если бак может отдать всю ёмкость контейнера.
        //    Иначе (стак = 1 пустая бочка) ждём, пока накопится жидкость.
        if (tankFill < itemCapacity) return false;

        // 2. Симуляция: сколько жидкости готов отдать бак
        FluidStack available = tankHandler.drain(itemCapacity, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty() || available.getAmount() != itemCapacity) return false;

        // 3. Симуляция результата на копии (EXECUTE + rollback)
        ItemStack simCopy = inputStack.copy();
        simCopy.setCount(1);
        IFluidHandlerItem simHandler = simCopy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (simHandler == null) return false;
        FluidStack simToFill = new FluidStack(available.getFluid(), itemCapacity);
        int simFilled = simHandler.fill(simToFill, IFluidHandlerItem.FluidAction.EXECUTE);
        if (simFilled != itemCapacity) return false;
        ItemStack simResult = simHandler.getContainer();
        if (!FluidTank.canPlaceItemInSlot(slots, out, simResult)) {
            simHandler.drain(simToFill, IFluidHandlerItem.FluidAction.EXECUTE); // rollback
            return false;
        }
        simHandler.drain(simToFill, IFluidHandlerItem.FluidAction.EXECUTE); // rollback

        // 4. Выполнение drain из бака
        FluidStack drainedReal = tankHandler.drain(itemCapacity, IFluidHandler.FluidAction.EXECUTE);
        if (drainedReal.isEmpty() || drainedReal.getAmount() != itemCapacity) return false;

        // 5. Выполнение fill в контейнер
        int filledReal = itemHandler.fill(drainedReal, IFluidHandlerItem.FluidAction.EXECUTE);
        if (filledReal != itemCapacity) {
            tankHandler.fill(drainedReal, IFluidHandler.FluidAction.EXECUTE); // rollback
            return false;
        }

        FluidTank.placeItemInSlot(slots, out, itemHandler.getContainer());
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        //?}

        //? if fabric {
        /*ItemStack simCopy = inputStack.copy();
        simCopy.setCount(1);
        SimpleContainer simInv = new SimpleContainer(simCopy);
        ContainerItemContext simCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(simInv, null).getSlot(0));
        Storage<FluidVariant> simStorage = FluidStorage.ITEM.find(simCopy, simCtx);
        if (simStorage == null) return false;

        FluidVariant variant = FluidVariant.of(
                VanillaFluidEquivalence.forVanillaContainerFill(tank.getTankType()));
        long fullBarrelDroplets = (long) FluidBarrelItem.CAPACITY * 81L;
        long droplets = (long) tank.getFill() * 81L;

        // All-or-nothing
        if (droplets < fullBarrelDroplets) return false;

        try (Transaction tx = Transaction.openOuter()) {
            long fillableSim = simStorage.insert(variant, fullBarrelDroplets, tx);
            if (fillableSim != fullBarrelDroplets) return false;
            if (!FluidTank.canPlaceItemInSlot(slots, out, simInv.getItem(0))) return false;
        }

        ItemStack execCopy = inputStack.copy();
        execCopy.setCount(1);
        SimpleContainer execInv = new SimpleContainer(execCopy);
        ContainerItemContext execCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(execInv, null).getSlot(0));
        Storage<FluidVariant> execStorage = FluidStorage.ITEM.find(execCopy, execCtx);
        if (execStorage == null) return false;

        long filled;
        try (Transaction tx = Transaction.openOuter()) {
            filled = execStorage.insert(variant, fullBarrelDroplets, tx);
            if (filled > 0) tx.commit();
        }
        if (filled <= 0) return false;

        // На Fabric бак может быть пустым по variant, поэтому используем drainMb.
        int mb = (int) (filled / 81L);
        if (mb <= 0) return false;
        tank.drainMb(mb);
        FluidTank.placeItemInSlot(slots, out, execInv.getItem(0));
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        *///?}
    }
}