package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.platform.FluidHooks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class FluidLoaderStandard implements FluidTank.LoadingHandler {

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack inputStack = slots[in];
        if (inputStack == null || inputStack.isEmpty()) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        if (inputStack.getItem() instanceof InfiniteFluidItem) return false;
        if (tank.getPressure() != 0) return false;

        int space = tank.getMaxFill() - tank.getFill();
        if (space <= 0) return false;

        // 1. Симулируем извлечение, ограниченную свободным местом бака (паритет NEXT
        // FluidTankNTM.loadTank: container.drain(stack, type, capacity - fill, pressure) —
        // переносится столько, сколько влезло, а не весь контейнер разом).
        FluidHooks.FluidExtraction simEx = FluidHooks.extractFluidFromItem(inputStack, space, true);
        if (simEx.amount() <= 0) return false;

        // Паритет NEXT FluidTankNTM.emptyContainer: вещество контейнера сверяется с баком
        // ВСЕГДА (accepts(): пустой, но типизированный бак принимает только свой pin) — иначе
        // чужая жидкость реально извлекалась из контейнера, а fillMb её отбрасывал.
        if (!VanillaFluidEquivalence.sameSubstance(tank.getTankType(), simEx.fluid())) return false;

        // 2. Паритет NEXT emptyContainer: принятие проверяется ДО любой реальной мутации —
        // если бак откажет (isFluidValid), содержимое контейнера не должно пропасть.
        if (tank.fillMb(simEx.fluid(), simEx.amount(), true) <= 0) return false;

        // 3. Реальное извлечение на копии контейнера (место уже гарантировано: amount <= space).
        FluidHooks.FluidExtraction execEx = FluidHooks.extractFluidFromItem(inputStack, simEx.amount(), false);
        if (execEx.amount() <= 0) return false;

        // 4. Паритет NEXT emptyContainer (:413-422): ПОЛНОСТЬЮ опустошённый контейнер
        // выбрасывается в выходной слот (abort, если не влезает); частично опустошённый —
        // только из стека размером 1, и ОСТАЁТСЯ во входном слоте.
        boolean fullyEmptied = FluidHooks.extractFluidFromItem(execEx.remainder(), Integer.MAX_VALUE, true).amount() <= 0;
        if (fullyEmptied) {
            if (!FluidTank.canPlaceItemInSlot(slots, out, execEx.remainder())) return false;
        } else {
            if (inputStack.getCount() != 1) return false;
        }

        // 5. Выполняем перемещение.
        tank.fillMb(execEx.fluid(), execEx.amount(), false);
        if (fullyEmptied) {
            FluidTank.placeItemInSlot(slots, out, execEx.remainder());
            slots[in].shrink(1);
            if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        } else {
            slots[in] = execEx.remainder();
        }

        return true;
    }

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack inputStack = slots[in];
        if (inputStack == null || inputStack.isEmpty() || tank.getFill() <= 0) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        if (inputStack.getItem() instanceof InfiniteFluidItem) return false;
        if (tank.getPressure() != 0) return false;

        int itemCapacity = FluidHooks.getItemFluidCapacity(inputStack);
        if (itemCapacity <= 0) return false;

        // Паритет NEXT FluidTankNTM.unloadTank: container.fill(stack, getFluid(), fill, pressure) —
        // вливаем сколько есть в баке (clamp на fill), остаток контейнера уходит в выходной слот.
        int amount = Math.min(itemCapacity, tank.getFill());

        Fluid stored = tank.getStoredFluid();
        Fluid fluidToInsert = VanillaFluidEquivalence.forVanillaContainerFill(stored);

        // 1. Симулируем вливание
        FluidHooks.FluidInsertion simIns = FluidHooks.insertFluidIntoItem(inputStack, fluidToInsert, amount, true);
        if (simIns.amountInserted() <= 0) return false;

        // 2. Реальное вливание на копии контейнера.
        FluidHooks.FluidInsertion execIns = FluidHooks.insertFluidIntoItem(inputStack, fluidToInsert, amount, false);
        if (execIns.amountInserted() <= 0) return false;

        // 3. Паритет NEXT fillContainer (:452-460): ПОЛНОСТЬЮ заполненный контейнер
        // выбрасывается в выходной слот (abort, если не влезает); частично заполненный —
        // только из стека размером 1, и ОСТАЁТСЯ во входном слоте.
        boolean completelyFilled = FluidHooks.insertFluidIntoItem(execIns.remainder(), fluidToInsert, Integer.MAX_VALUE, true).amountInserted() <= 0;
        if (completelyFilled) {
            if (!FluidTank.canPlaceItemInSlot(slots, out, execIns.remainder())) return false;
        } else {
            if (inputStack.getCount() != 1) return false;
        }

        // 4. Выполняем перемещение.
        tank.drainMb(execIns.amountInserted(), false);
        if (completelyFilled) {
            FluidTank.placeItemInSlot(slots, out, execIns.remainder());
            slots[in].shrink(1);
            if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        } else {
            slots[in] = execIns.remainder();
        }

        return true;
    }
}