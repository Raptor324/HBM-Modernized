package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCatalyticReformerMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.CatalyticReformerRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Katalytischer Reformer: Portierung der Kernlogik aus {@code TileEntityMachineCatalyticReformer}
 * (1.7.10 Original). Wandelt jeden Tick 100mB eines Oel-Fluids (Tank 0) in drei Ausgangsfluide um
 * (Tanks 1-3, darunter immer etwas Wasserstoff als Nebenprodukt - anders als beim Hydrotreater
 * wird hier kein Wasserstoff verbraucht), ueber die data-driven Rezeptliste
 * {@link CatalyticReformerRecipe} (Port von {@code ReformingRecipes}). Erfordert wie im
 * Original einen katalytischen Konverter ({@link ModItems#CATALYTIC_CONVERTER}) im Katalysatorslot.
 */
public class MachineCatalyticReformerBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.block.IPersistentNBT, IFluidStandardTransceiverMK2 {

    /** Original-Inventar (11): 0 Batterie, 1/2 Kanister Eingang ein/aus, 3/4 Reformat, 5/6 Gas,
     *  7/8 Wasserstoff, 9 Fluidkennung, 10 Katalysator. */
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 9;
    public static final int SLOT_CATALYST = 10;
    public static final int SLOT_COUNT = 11;

    /** Original: {@code maxPower = 1_000_000}; Tanks Naphtha 64000, Reformat/Petroleum/Wasserstoff je 24000. */
    private static final long MAX_POWER = 1_000_000L;
    private static final long POWER_PER_CYCLE = 20_000L;
    private static final int INPUT_CAPACITY_MB = 64_000;
    private static final int OUTPUT_CAPACITY_MB = 24_000;
    private static final int INPUT_PER_CYCLE_MB = 100;

    private final FluidTank[] tanks = new FluidTank[4];

    public MachineCatalyticReformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CATALYTIC_REFORMER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER, 0L);
        tanks[0] = new FluidTank(INPUT_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                // Data-driven: рецепт ищется в RecipeManager (заменяет CatalyticReformerRecipes.has).
                return CatalyticReformerRecipe.hasRecipe(level, fluid);
            }
        };
        tanks[1] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[2] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[3] = new FluidTank(OUTPUT_CAPACITY_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCatalyticReformerBlockEntity be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        // Original: tanks[0].setType(9), tanks[0].loadTank(1, 2)
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = be.inventory.getStackInSlot(i);
        boolean changed = be.tanks[0].setType(SLOT_FLUID_ID, slots);
        changed |= be.tanks[0].loadTank(1, 2, slots);

        be.reform();

        // Original: tanks[1..3].unloadTank(3, 4), (5, 6), (7, 8)
        changed |= be.tanks[1].unloadTank(3, 4, slots);
        changed |= be.tanks[2].unloadTank(5, 6, slots);
        changed |= be.tanks[3].unloadTank(7, 8, slots);
        if (changed) for (int i = 0; i < SLOT_COUNT; i++) be.inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            be.trySubscribe(be.tanks[0].getTankType(), level, neighborPos, dir);
            be.tryProvide(be.tanks[1], level, neighborPos, dir);
            be.tryProvide(be.tanks[2], level, neighborPos, dir);
            be.tryProvide(be.tanks[3], level, neighborPos, dir);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Direktport von {@code reform()}. */
    private void reform() {
        CatalyticReformerRecipe recipe = CatalyticReformerRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) {
            tanks[1].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            tanks[2].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            tanks[3].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            return;
        }
        if (tanks[1].isEmpty()) tanks[1].conform(recipe.getOutputA());
        if (tanks[2].isEmpty()) tanks[2].conform(recipe.getOutputB());
        if (tanks[3].isEmpty()) tanks[3].conform(recipe.getOutputC());

        if (getEnergyStored() < POWER_PER_CYCLE) return;
        if (tanks[0].getFill() < INPUT_PER_CYCLE_MB) return;
        if (!hasCatalyst()) return;
        if (tanks[1].getFill() + recipe.getOutputAMb() > tanks[1].getMaxFill()) return;
        if (tanks[2].getFill() + recipe.getOutputBMb() > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + recipe.getOutputCMb() > tanks[3].getMaxFill()) return;

        setEnergyStored(getEnergyStored() - POWER_PER_CYCLE);
        tanks[0].drainMb(INPUT_PER_CYCLE_MB);
        tanks[1].fillMb(recipe.getOutputA(), recipe.getOutputAMb());
        tanks[2].fillMb(recipe.getOutputB(), recipe.getOutputBMb());
        tanks[3].fillMb(recipe.getOutputC(), recipe.getOutputCMb());
    }

    private boolean hasCatalyst() {
        ItemStack stack = inventory.getStackInSlot(SLOT_CATALYST);
        return !stack.isEmpty() && stack.is(ModItems.CATALYTIC_CONVERTER.get());
    }

    // ==================== GUI ====================

    public FluidTank[] getTanks() {
        return tanks;
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[0] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[1], tanks[2], tanks[3] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && (CatalyticReformerRecipe.hasRecipe(level, fluid)
                || tanks[1].getTankType() == fluid || tanks[2].getTankType() == fluid || tanks[3].getTankType() == fluid);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank" + i);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        int oldSize = tag.getCompound("inventory").getInt("Size");
        super.readNbtData(tag, registries);
        migrateOldInventory(oldSize);
        setEnergyCapacity(MAX_POWER); // alte Welten: frueherer Speicherwert
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank" + i);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.catalytic_reformer");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == SLOT_CATALYST) return stack.is(ModItems.CATALYTIC_CONVERTER.get());
        if (slot == SLOT_FLUID_ID) return stack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier;
        // Original: Eingaenge 1/3/5/7, Ausgaenge 2/4/6/8 nur entnehmbar
        return slot == 1 || slot == 3 || slot == 5 || slot == 7;
    }

    /** Alte Welten (2 Slots: Batterie, Katalysator auf 1) -> Katalysator auf Original-Slot 10. */
    private void migrateOldInventory(int oldSize) {
        if (oldSize != 2) return;
        ItemStack old = inventory.getStackInSlot(1);
        if (!old.isEmpty() && inventory.getStackInSlot(SLOT_CATALYST).isEmpty()) {
            inventory.setStackInSlot(SLOT_CATALYST, old);
            inventory.setStackInSlot(1, ItemStack.EMPTY);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCatalyticReformerMenu.create(id, inventory, this);
    }

    /** Original {@code writeNBT}: die vier Tanks, sofern einer etwas enthaelt. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = true;
        for (var tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return;
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "tank" + i);
    }
}
