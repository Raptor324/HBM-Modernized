package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.SimpleContainer;
//?}

import net.minecraft.world.item.ItemStack;
//? if forge {
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.capability.IFluidHandler;
*///?}

/**
 * Стандартный загрузчик жидкости (ведро/бочка → бак и бак → ведро/бочка).
 * Порт FluidLoaderStandard из 1.7.10.
 *
 * Ключевая логика: использует тип бака ({@code tank.getTankType()}) как ключ
 * для определения, подходит ли предмет. Если тип бака не задан (NONE/EMPTY),
 * заполнение блокируется.
 */
public class FluidLoaderStandard implements FluidTank.LoadingHandler {

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack inputStack = slots[in];
        if (inputStack == null || inputStack.isEmpty()) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        // Бесконечные бочки обрабатываются только {@link FluidLoaderInfinite}, иначе Standard перенесёт их в output слот.
        if (inputStack.getItem() instanceof com.hbm_m.item.liquids.InfiniteFluidItem) {
            return false;
        }

        //? if forge {
        /*if (tank.getPressure() != 0) return false;
        int space = tank.getMaxFill() - tank.getFill();
        if (space <= 0) return false;

        ItemStack one = inputStack.copy();
        one.setCount(1);

        IFluidHandler tankHandler = tank.getCapability().orElse(null);
        if (tankHandler == null) return false;

        // (SIMULATE) проверяем: действительно ли в контейнере есть нужная жидкость и есть куда перелить,
        // а также влезет ли результат в output слот.
        FluidActionResult sim = FluidUtil.tryEmptyContainer(one.copy(), tankHandler, space, null, false);
        if (!sim.isSuccess()) return false;
        if (!FluidTank.canPlaceItemInSlot(slots, out, sim.getResult())) return false;

        FluidActionResult res = FluidUtil.tryEmptyContainer(one, tankHandler, space, null, true);
        if (!res.isSuccess()) return false;

        FluidTank.placeItemInSlot(slots, out, res.getResult());
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        *///?}

        //? if fabric {
        ItemStack simCopy = inputStack.copy();
        simCopy.setCount(1);
        SimpleContainer simInv = new SimpleContainer(simCopy);
        ContainerItemContext simCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(simInv, null).getSlot(0));
        Storage<FluidVariant> simStorage = FluidStorage.ITEM.find(simCopy, simCtx);
        if (simStorage == null) return false;

        FluidVariant storedVariant = FluidVariant.blank();
        for (StorageView<FluidVariant> view : simStorage) {
            if (!view.isResourceBlank() && view.getAmount() > 0) {
                storedVariant = view.getResource();
                break;
            }
        }

        if (storedVariant.isBlank()) return false;
        if (!VanillaFluidEquivalence.sameSubstance(tank.getTankType(), storedVariant.getFluid())) {
            return false;
        }

        int space = tank.getMaxFill() - tank.getFill();
        long maxExtract = (long) space * 81L;

        try (Transaction tx = Transaction.openOuter()) {
            long drainableSim = simStorage.extract(storedVariant, maxExtract, tx);
            if (drainableSim <= 0) return false;
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
            drained = execStorage.extract(storedVariant, maxExtract, tx);
            if (drained > 0) tx.commit();
        }
        if (drained <= 0) return false;

        tank.fill(tank.getFill() + (int) (drained / 81L));
        FluidTank.placeItemInSlot(slots, out, execInv.getItem(0));
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        //?}
    }

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack inputStack = slots[in];
        if (inputStack == null || inputStack.isEmpty() || tank.getFill() <= 0) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        // Бесконечные бочки обрабатываются только {@link FluidLoaderInfinite}, иначе Standard перенесёт их в output слот.
        if (inputStack.getItem() instanceof com.hbm_m.item.liquids.InfiniteFluidItem) {
            return false;
        }

        //? if forge {
        /*if (tank.getPressure() != 0) return false;

        ItemStack one = inputStack.copy();
        one.setCount(1);

        IFluidHandler tankHandler = tank.getCapability().orElse(null);
        if (tankHandler == null) return false;

        // В tryFillContainer целевой handler — это источник жидкости, поэтому используем обёртку на базе tankHandler:
        // сперва симуляция, затем выполнение.
        FluidActionResult sim = FluidUtil.tryFillContainer(one.copy(), tankHandler, tank.getFill(), null, false);
        if (!sim.isSuccess()) return false;
        if (!FluidTank.canPlaceItemInSlot(slots, out, sim.getResult())) return false;

        FluidActionResult res = FluidUtil.tryFillContainer(one, tankHandler, tank.getFill(), null, true);
        if (!res.isSuccess()) return false;

        FluidTank.placeItemInSlot(slots, out, res.getResult());
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        *///?}

        //? if fabric {
        ItemStack simCopy = inputStack.copy();
        simCopy.setCount(1);
        SimpleContainer simInv = new SimpleContainer(simCopy);
        ContainerItemContext simCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(simInv, null).getSlot(0));
        Storage<FluidVariant> simStorage = FluidStorage.ITEM.find(simCopy, simCtx);
        if (simStorage == null) return false;

        FluidVariant variant = FluidVariant.of(
                VanillaFluidEquivalence.forVanillaContainerFill(tank.getTankType()));
        long droplets = (long) tank.getFill() * 81L;

        try (Transaction tx = Transaction.openOuter()) {
            long fillableSim = simStorage.insert(variant, droplets, tx);
            if (fillableSim <= 0) return false;
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
            filled = execStorage.insert(variant, droplets, tx);
            if (filled > 0) tx.commit();
        }
        if (filled <= 0) return false;

        tank.fill(tank.getFill() - (int) (filled / 81L));
        FluidTank.placeItemInSlot(slots, out, execInv.getItem(0));
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        return true;
        //?}
    }
}
