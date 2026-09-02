package com.hbm_m.api.fluids;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Кросс-платформенный доступ к fluid-handler-item capability предмета.
 *
 * <p>Параллель с {@link com.hbm_m.api.energy.ItemEnergyAccess}:
 * <ul>
 *   <li><b>Forge</b>: {@code stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)} →
 *       {@code Optional<net.minecraftforge.fluids.capability.IFluidHandlerItem>}</li>
 *   <li><b>NeoForge</b>: {@code stack.getCapability(Capabilities.FluidHandler.ITEM)} →
 *       {@code Optional<net.neoforged.neoforge.fluids.capability.IFluidHandlerItem>}</li>
 *   <li><b>Fabric</b>: {@code FluidStorage.ITEM.find(...)} (Transfer API)</li>
 * </ul>
 *
 * <p><b>ВНИМАНИЕ:</b> возвращаемый тип {@code IFluidHandlerItem} отличается пакетом
 * (forge vs neoforge). Метод {@link #getFluidHandler} поэтому имеет платформенно-специфичную
 * сигнатуру через {@code //? if}. Call-site внутри {@code .ifPresent(...)} работает одинаково —
 * у обоих интерфейсов идентичные методы ({@code fill}/{@code drain}/{@code getTanks}).
 */
public final class FluidItemAccess {
    private FluidItemAccess() {}

    public static boolean hasFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return false;
        //? if forge {
        return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        //?} else if neoforge {
        /*return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
        *///?} else if fabric {
        /*return net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.find(
                stack, net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack)
        ) != null;
        *///?} else {
        /*return false;
        *///?}
    }

    /**
     * Доступ к Forge/NeoForge {@code IFluidHandlerItem} предмета.
     *
     * <p>На forge возвращает {@code net.minecraftforge.fluids.capability.IFluidHandlerItem},
     * на neoforge — {@code net.neoforged.neoforge.fluids.capability.IFluidHandlerItem}.
     * У обоих интерфейсов идентичная сигнатура методов, call-сайт внутри
     * {@code .ifPresent(...)} работает одинаково.
     */
    //? if forge {
    public static Optional<net.minecraftforge.fluids.capability.IFluidHandlerItem> getFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
    }
    //?}
    //? if neoforge {
    /*public static Optional<net.neoforged.neoforge.fluids.capability.IFluidHandlerItem> getFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return Optional.ofNullable(stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM));
    }
    *///?}
}
