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
 *       {@code Optional<net.minecraftforge.fluids.capability.IFluidHandlerItem>}</li>
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

    /**
     * Fluessigkeitsbehaelter fuer GUI-Slots: Plattform-Capability ODER Eintrag in der
     * {@link com.hbm_m.inventory.FluidContainerRegistry} (voll oder leer: Kanister, Gasflaschen, Tanks, Faesser,
     * Zellen ... - die HBM-Behaelter tragen keine Capability) ODER unendliches Fass. Frueher pruefte der Slot nur die
     * Capability, damit liessen sich die Registry-Behaelter gar nicht mehr in Fass/Tank/Chemiewerk legen.
     */
    public static boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof com.hbm_m.item.liquids.InfiniteFluidItem) return true;
        if (com.hbm_m.inventory.FluidContainerRegistry.isContainer(stack)) return true;
        // Original FluidLoaderFillableItem: IFillableItem (Jetpack, PLSS, Loetlampe ...) und Ruestung mit solchem Mod
        if (stack.getItem() instanceof IFillableItem) return true;
        if (com.hbm_m.armormod.util.ArmorModificationHelper.hasMods(stack)) {
            for (ItemStack mod : com.hbm_m.armormod.util.ArmorModificationHelper.pryMods(stack)) {
                if (mod != null && (mod.getItem() instanceof IFillableItem || hasFluidHandler(mod))) return true;
            }
        }
        return hasFluidHandler(stack);
    }

    public static boolean hasFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return false;
        //? if forge {
        return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        //?} elif neoforge {
        /*return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
        *///?} else {
        /*return false;
        *///?}
    }

    /**
     * Доступ к Forge/NeoForge {@code IFluidHandlerItem} предмета.
     *
     * <p>На forge возвращает {@code net.minecraftforge.fluids.capability.IFluidHandlerItem},
     * на neoforge — {@code net.minecraftforge.fluids.capability.IFluidHandlerItem}.
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
    /*public static Optional<net.minecraftforge.fluids.capability.IFluidHandlerItem> getFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return Optional.ofNullable(stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM));
    }
    *///?}
}
