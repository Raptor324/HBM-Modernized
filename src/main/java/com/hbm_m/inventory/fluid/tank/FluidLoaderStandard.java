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

        // 1. Симулируем извлечение для проверки количества жидкости
        FluidHooks.FluidExtraction simEx = FluidHooks.extractFluidFromItem(inputStack, Integer.MAX_VALUE, true);
        if (simEx.amount() <= 0) return false;

        boolean tankEmpty = !FluidTank.isFluidTypeExplicitlySet(tank.getTankType()) || tank.getFill() <= 0;
        if (!tankEmpty && !VanillaFluidEquivalence.sameSubstance(tank.getTankType(), simEx.fluid())) return false;

        int space = tank.getMaxFill() - tank.getFill();
        if (space < simEx.amount()) return false; // Бак должен принять всё (All-or-nothing)

        // 2. Получаем ТОЧНЫЙ остаток. 
        // Если выходной слот занят, симулированный остаток не подойдет (он имеет старый NBT).
        // Поэтому для стакающихся предметов (исключаем Ender Tanks) делаем реальное извлечение на копии.
        ItemStack exactRemainder = simEx.remainder();
        if (slots[out] != null && !slots[out].isEmpty()) {
            if (inputStack.getMaxStackSize() > 1) {
                exactRemainder = FluidHooks.extractFluidFromItem(inputStack, Integer.MAX_VALUE, false).remainder();
            }
        }

        // 3. Проверяем, влезет ли точный остаток в выходной слот
        if (!FluidTank.canPlaceItemInSlot(slots, out, exactRemainder)) return false;

        // 4. Проверяем цистерну (симуляция)
        int filledSim = tank.fillMb(simEx.fluid(), simEx.amount(), true);
        if (filledSim != simEx.amount()) return false;

        // 5. Выполняем реальное перемещение
        FluidHooks.FluidExtraction execEx = FluidHooks.extractFluidFromItem(inputStack, simEx.amount(), false);
        if (execEx.amount() <= 0) return false;

        tank.fillMb(execEx.fluid(), execEx.amount(), false);
        FluidTank.placeItemInSlot(slots, out, execEx.remainder());
        
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        
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

        if (tank.getFill() < itemCapacity) return false; // All-or-nothing

        Fluid stored = tank.getStoredFluid();
        Fluid fluidToInsert = VanillaFluidEquivalence.forVanillaContainerFill(stored);
        
        // 1. Симулируем вливание
        FluidHooks.FluidInsertion simIns = FluidHooks.insertFluidIntoItem(inputStack, fluidToInsert, itemCapacity, true);
        if (simIns.amountInserted() != itemCapacity) return false;

        // 2. Получаем ТОЧНЫЙ остаток (с правильным NBT) для проверки выходного слота
        ItemStack exactRemainder = simIns.remainder();
        if (slots[out] != null && !slots[out].isEmpty()) {
            if (inputStack.getMaxStackSize() > 1) {
                exactRemainder = FluidHooks.insertFluidIntoItem(inputStack, fluidToInsert, itemCapacity, false).remainder();
            }
        }

        // 3. Проверяем, влезет ли точный остаток в выходной слот
        if (!FluidTank.canPlaceItemInSlot(slots, out, exactRemainder)) return false;

        // 4. Выполняем реальное перемещение
        FluidHooks.FluidInsertion execIns = FluidHooks.insertFluidIntoItem(inputStack, fluidToInsert, itemCapacity, false);
        if (execIns.amountInserted() != itemCapacity) return false;

        tank.drainMb(itemCapacity, false);
        FluidTank.placeItemInSlot(slots, out, execIns.remainder());
        
        slots[in].shrink(1);
        if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
        
        return true;
    }
}