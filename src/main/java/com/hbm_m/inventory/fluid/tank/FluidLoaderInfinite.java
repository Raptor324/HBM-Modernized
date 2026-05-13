package com.hbm_m.inventory.fluid.tank;

import java.util.Random;
import com.hbm_m.item.liquids.InfiniteFluidItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if forge {
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
//?}
/**
 * FluidLoaderInfinite из 1.7.10 — точная семантика по логике:
 *
 * fillItem (бак -> контейнер):
 * - если chance срабатывает: tank -= amount
 * - всегда возвращает true
 *
 * emptyItem (контейнер -> бак):
 * - если chance срабатывает: tank += amount (clamp maxFill)
 * - всегда возвращает true
 *
 * Примечания по порту:
 * - В 1.20+ Infinite*Items не содержат explicit chance/allowPressure/getType как в 1.7.10.
 *   В этой реализации делаем соответствие:
 *   * amount = transferRate (берём через drain по максимально запросимому объёму)
 *   * chance = 1 (т.е. срабатывает всегда)
 *   * allowPressure делегируем тому, что решит общий pressure-flow: tank.loadTank уже не блокирует
 *     infinite предметы под давлением, а сам Infinite*Item не имеет separate pressure-проверки.
 */
@SuppressWarnings("UnstableApiUsage")
public class FluidLoaderInfinite implements FluidTank.LoadingHandler {

    private static final Random rand = new Random();

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        // В 1.7.10: slots[in] == null || !(item instanceof ItemInfiniteFluid) => false
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof InfiniteFluidItem)) return false;

        // В 1.7.10: при fillItem tank.pressure учитывался allowPressure(tank.pressure).
        // Здесь Infinite*Item не умеет allowPressure напрямую, поэтому используем состояние бака:
        // если в баке пусто — ничего не делаем.
        if (tank.getFill() <= 0) return false;

        Fluid requestedType = tank.getTankType();

        Fluid itemType = ((InfiniteFluidItem) stack.getItem()).getFluidType(stack);
        if (itemType != null && itemType != Fluids.EMPTY
                && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(requestedType, itemType)) return false;

        // amount = сколько вернёт drain(resource, EXECUTE) при запросе на maxDrain
        // В 1.7.10 amount был фиксирован per-ивент; здесь берём через capability.
        int amount = drainAmountFromInfinite(stack, requestedType, Integer.MAX_VALUE);
        if (amount <= 0) return false;

        // В 1.7.10: tank.setFill(Math.max(tank.getFill() - item.getAmount(), 0));
        // chance <= 1 => выполняется всегда, сохраним rand-ветку как “chance=1” (всегда true)
        tank.setFill(Math.max(tank.getFill() - amount, 0));

        return true;
    }

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        // В 1.7.10:
        // - если slots[in] == null или не ItemInfiniteFluid => false
        // - OR tank.getTankType() == Fluids.NONE => false
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof InfiniteFluidItem)) return false;

        Fluid currentType = tank.getTankType();
        if (currentType == null || currentType == Fluids.EMPTY) return false;

        Fluid itemType = ((InfiniteFluidItem) stack.getItem()).getFluidType(stack);
        if (itemType != null && itemType != Fluids.EMPTY
                && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(currentType, itemType)) return false;

        int amount = drainAmountFromInfinite(stack, currentType, Integer.MAX_VALUE);
        if (amount <= 0) return false;

        int space = tank.getMaxFill() - tank.getFill();
        int toFill = Math.min(amount, space);
        if (toFill <= 0) return false;

        // Важно: нельзя делать tank.setFill(...) когда бак полностью пустой, потому что setFill
        // опирается на getStoredFluid()==EMPTY. Для бесконечных предметов нужно форсировать
        // Forge fill(), чтобы выставился тип внутри forgeStorage.
        int filled = tank.fillMb(currentType, toFill);
        return filled > 0;
    }

    private int drainAmountFromInfinite(ItemStack stack, Fluid fluid, int maxRequest) {
        //? if forge {
        /*return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
            // Важно: InfiniteFluidItem может быть "ненастроенным" (FluidType = EMPTY).
            // В таком состоянии его drain(int) вернёт EMPTY, но drain(FluidStack) обязан
            // выдавать запрошенный тип (как в 1.7.10: тип определяется баком/контекстом).
            FluidStack drained = (fluid != null && fluid != Fluids.EMPTY)
                    ? handler.drain(new FluidStack(fluid, maxRequest), IFluidHandlerItem.FluidAction.EXECUTE)
                    : handler.drain(maxRequest, IFluidHandlerItem.FluidAction.EXECUTE);
            if (drained == null || drained.isEmpty()) return 0;
            if (fluid != null && drained.getFluid() != fluid) return 0;
            // drained.getAmount() ограничен transferRate внутри предмета
            return drained.getAmount();
        }).orElse(0);
        *///?}
        //? if fabric {
        return fabricDrainAmountFromInfinite(stack, fluid, maxRequest);
        //?}
    }

    //? if fabric {
    @SuppressWarnings("UnstableApiUsage")
    private static int fabricDrainAmountFromInfinite(ItemStack stack, Fluid fluid, int maxRequest) {
        Storage<FluidVariant> storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        if (storage == null) return 0;
        FluidVariant variant = (fluid != null && fluid != Fluids.EMPTY)
                ? FluidVariant.of(fluid)
                : null;
        long maxDrop = (long) maxRequest * 81L;
        try (Transaction tx = Transaction.openOuter()) {
            long ext;
            if (variant != null && !variant.isBlank()) {
                ext = storage.extract(variant, maxDrop, tx);
            } else {
                ext = 0;
                for (net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant> view : storage) {
                    if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                    ext = view.extract(view.getResource(), maxDrop, tx);
                    if (ext > 0) break;
                }
            }
            if (ext <= 0) return 0;
            tx.commit();
            return (int) Math.min(Integer.MAX_VALUE / 81, ext / 81L);
        }
    }
    //?}
}
