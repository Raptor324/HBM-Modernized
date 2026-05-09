package com.hbm_m.api.fluids;

import net.minecraft.world.item.ItemStack;

public final class FluidItemAccess {
    private FluidItemAccess() {}

    public static boolean hasFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return false;
        //? if forge {
        /*return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        *///?} else if fabric {
        return net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.find(
                stack, net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack)
        ) != null;
        //?} else {
        /*return false;
        *///?}
    }
}

