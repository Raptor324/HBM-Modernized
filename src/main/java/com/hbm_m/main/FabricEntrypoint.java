//? if fabric {
package com.hbm_m.main;

import java.util.concurrent.atomic.AtomicBoolean;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.api.fluids.bootstrap.ModFluidTraitsBootstrap;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.recipe.ChemicalPlantRecipes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class FabricEntrypoint implements ModInitializer {

    /** Источник «water» из {@link ModFluids}; когда есть в {@link BuiltInRegistries#FLUID}, {@link dev.architectury.registry.registries.RegistrySupplier#get()} безопасен. */
    private static final ResourceLocation HBM_WATER_SOURCE_ID = RefStrings.resourceLocation("water");

    /**
     * На Fabric {@link dev.architectury.event.events.common.LifecycleEvent#SETUP} и даже
     * {@link net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents#CLIENT_STARTED}
     * бывают раньше фактической записи жидкостей в {@link BuiltInRegistries#FLUID}.
     * Ждём появления ключа и один раз регистрируем рецепты/трейты на тике сервера или клиента.
     */
    private static final AtomicBoolean FLUID_DEPENDENT_SETUP_DONE = new AtomicBoolean(false);

    @Override
    public void onInitialize() {
        // Сначала подписка FLUIDS на реестр — до MainRegistry.init(), который вешает SETUP.
        ModFluids.register();
        MainRegistry.init();
        registerFluidDependentSetupWhenReady();

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
        FluidStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getFluidStorage(side),
                ModBlockEntities.FLUID_TANK_BE.get()
        );
        FluidStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getFluidStorage(side),
                ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get()
        );
        FluidStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getFluidStorage(side),
                ModBlockEntities.CHEMICAL_PLANT_BE.get()
        );

        EnergyStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getEnergyStorageSided(side),
                ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get()
        );

        FluidStorage.ITEM.registerForItems((stack, context) -> FluidBarrelItem.createFabricStorage(context), ModItems.FLUID_BARREL.get());
        FluidStorage.ITEM.registerForItems(
                (stack, context) -> ((InfiniteFluidItem) stack.getItem()).createFabricStorage(stack, context),
                ModItems.FLUID_BARREL_INFINITE.get() // подставьте переменную регистрации предмета
        );

        // TeamReborn Energy: делаем батарейки "видимыми" через EnergyStorage.ITEM.find(...)
        EnergyStorage.ITEM.registerForItems(
                (stack, ctx) -> new BatteryEnergyStorage(stack),
                ModItems.ARMOR_BATTERY.get(),
                ModItems.ARMOR_BATTERY_MK2.get(),
                ModItems.ARMOR_BATTERY_MK3.get(),
                ModItems.CREATIVE_BATTERY.get(),
                ModItems.BATTERY_SCHRABIDIUM.get(),
                ModItems.BATTERY_POTATO.get(),
                ModItems.BATTERY.get(),
                ModItems.BATTERY_RED_CELL.get(),
                ModItems.BATTERY_RED_CELL_6.get(),
                ModItems.BATTERY_RED_CELL_24.get(),
                ModItems.BATTERY_ADVANCED.get(),
                ModItems.BATTERY_ADVANCED_CELL.get(),
                ModItems.BATTERY_ADVANCED_CELL_4.get(),
                ModItems.BATTERY_ADVANCED_CELL_12.get(),
                ModItems.BATTERY_LITHIUM.get(),
                ModItems.BATTERY_LITHIUM_CELL.get(),
                ModItems.BATTERY_LITHIUM_CELL_3.get(),
                ModItems.BATTERY_LITHIUM_CELL_6.get(),
                ModItems.BATTERY_SCHRABIDIUM_CELL.get(),
                ModItems.BATTERY_SCHRABIDIUM_CELL_2.get(),
                ModItems.BATTERY_SCHRABIDIUM_CELL_4.get(),
                ModItems.BATTERY_SPARK.get(),
                ModItems.BATTERY_TRIXITE.get(),
                ModItems.BATTERY_SPARK_CELL_6.get(),
                ModItems.BATTERY_SPARK_CELL_25.get(),
                ModItems.BATTERY_SPARK_CELL_100.get(),
                ModItems.BATTERY_SPARK_CELL_1000.get(),
                ModItems.BATTERY_SPARK_CELL_2500.get(),
                ModItems.BATTERY_SPARK_CELL_10000.get(),
                ModItems.BATTERY_SPARK_CELL_POWER.get()
        );

        CreativeModeTabEventHandler.initFabric();
        ChunkRadiationManager.initFabric();
    }

    private static void registerFluidDependentSetupWhenReady() {
        Runnable tryRunFluidDependentSetup = () -> {
            synchronized (FabricEntrypoint.class) {
                if (FLUID_DEPENDENT_SETUP_DONE.get()) {
                    return;
                }
                if (!BuiltInRegistries.FLUID.containsKey(HBM_WATER_SOURCE_ID)) {
                    return;
                }
                try {
                    ChemicalPlantRecipes.registerRecipes();
                    ModFluidTraitsBootstrap.registerAll();
                    FLUID_DEPENDENT_SETUP_DONE.set(true);
                } catch (RuntimeException ex) {
                    // На всякий случай: если ключ уже есть, но DeferredRegister ещё не связан.
                    if (!isDeferredFluidNotYetBound(ex)) {
                        throw ex;
                    }
                }
            }
        };
        ServerTickEvents.END_SERVER_TICK.register(server -> tryRunFluidDependentSetup.run());
        ClientTickEvents.END_CLIENT_TICK.register(client -> tryRunFluidDependentSetup.run());
    }

    private static boolean isDeferredFluidNotYetBound(RuntimeException ex) {
        if (!(ex instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = ex.getStackTrace();
        return st.length > 0
                && "get".equals(st[0].getMethodName())
                && st[0].getClassName().contains("DeferredRegister");
    }

    private static final class BatteryEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
        private final net.minecraft.world.item.ItemStack stack;
        private final ModBatteryItem battery;

        private BatteryEnergyStorage(net.minecraft.world.item.ItemStack stack) {
            this.stack = stack;
            this.battery = (ModBatteryItem) stack.getItem();
        }

        @Override
        protected Long createSnapshot() {
            return ModBatteryItem.getEnergy(stack);
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            ModBatteryItem.setEnergy(stack, snapshot);
        }

        @Override
        protected void onFinalCommit() {
            // NBT уже обновлён; дополнительных side-effects не нужно
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (!supportsInsertion() || maxAmount <= 0) return 0;
            long stored = ModBatteryItem.getEnergy(stack);
            long space = battery.getCapacity() - stored;
            long amount = Math.min(maxAmount, Math.min(space, battery.getMaxReceive()));
            if (amount <= 0) return 0;
            updateSnapshots(transaction);
            ModBatteryItem.setEnergy(stack, stored + amount);
            return amount;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            if (!supportsExtraction() || maxAmount <= 0) return 0;
            long stored = ModBatteryItem.getEnergy(stack);
            long amount = Math.min(maxAmount, Math.min(stored, battery.getMaxExtract()));
            if (amount <= 0) return 0;
            updateSnapshots(transaction);
            ModBatteryItem.setEnergy(stack, stored - amount);
            return amount;
        }

        @Override
        public long getAmount() {
            return ModBatteryItem.getEnergy(stack);
        }

        @Override
        public long getCapacity() {
            return battery.getCapacity();
        }

        @Override
        public boolean supportsInsertion() {
            return battery.getMaxReceive() > 0;
        }

        @Override
        public boolean supportsExtraction() {
            return battery.getMaxExtract() > 0 && getAmount() > 0;
        }
    }
}
//?}