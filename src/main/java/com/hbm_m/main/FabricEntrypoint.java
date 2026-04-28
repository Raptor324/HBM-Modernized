//? if fabric {
package com.hbm_m.main;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.event.ClientModEvents;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.network.ModPacketHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(side),
                ModBlockEntities.BLAST_FURNACE_BE.get()
        );
        FluidStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getFluidStorage(side),
                ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get()
        );
        FluidStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getFluidStorage(side),
                ModBlockEntities.INDUSTRIAL_TURBINE_BE.get()
        );

        FluidStorage.ITEM.registerForItems((stack, context) -> FluidBarrelItem.createFabricStorage(context), ModItems.FLUID_BARREL.get());
        FluidStorage.ITEM.registerForItems(
                (stack, context) -> ((InfiniteFluidItem) stack.getItem()).createFabricStorage(context),
                ModItems.FLUID_BARREL_INFINITE.get() // подставьте переменную регистрации предмета
        );
        ModPacketHandler.register();
        ClientModEvents.init();
        CreativeModeTabEventHandler.initFabric();
    }
}
//?}