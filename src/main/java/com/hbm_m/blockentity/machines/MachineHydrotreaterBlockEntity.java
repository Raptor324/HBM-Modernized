package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineHydrotreaterMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.HydrotreaterRecipe;

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
 * Hydrotreater: Portierung der Kernlogik aus {@code TileEntityMachineHydrotreater} (1.7.10
 * Original). Entschwefelt alle 2 Ticks 100mB eines Oel-Fluids (Tank 0) unter Verbrauch von
 * Wasserstoff (Tank 1, druckbeaufschlagt) zu entschwefeltem Oel (Tank 2) + Sauergas (Tank 3),
 * ueber die data-driven Rezeptliste {@link HydrotreaterRecipe} (Port von
 * {@code HydrotreatingRecipes}). Erfordert wie im Original einen katalytischen Konverter
 * ({@link ModItems#CATALYTIC_CONVERTER}) im Katalysatorslot.
 */
public class MachineHydrotreaterBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.block.IPersistentNBT, IFluidStandardTransceiverMK2 {

    /** Original-Inventar (11): 0 Batterie, 1/2 Kanister Oel ein/aus, 3/4 Wasserstoff (stillgelegt, braucht Druck),
     *  5/6 entschwefeltes Oel, 7/8 Sauergas, 9 Fluidkennung, 10 Katalysator. */
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 9;
    public static final int SLOT_CATALYST = 10;
    public static final int SLOT_COUNT = 11;

    /** Original: {@code maxPower = 1_000_000}; Tanks Oel 64000, Wasserstoff 64000, Ausgaenge je 24000. */
    private static final long MAX_POWER = 1_000_000L;
    private static final long POWER_PER_CYCLE = 20_000L;
    private static final int OIL_CAPACITY_MB = 64_000;
    private static final int HYDROGEN_CAPACITY_MB = 64_000;
    private static final int OUTPUT_CAPACITY_MB = 24_000;
    private static final int OIL_PER_CYCLE_MB = 100;
    private static final int CYCLE_INTERVAL = 2;

    private final FluidTank[] tanks = new FluidTank[4];

    public MachineHydrotreaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDROTREATER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER, 0L);
        tanks[0] = new FluidTank(OIL_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                // Data-driven: рецепт ищется в RecipeManager (заменяет HydrotreaterRecipes.has).
                return HydrotreaterRecipe.hasRecipe(level, fluid);
            }
        };
        tanks[1] = new FluidTank(HYDROGEN_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return fluid == com.hbm_m.inventory.fluid.ModFluids.HYDROGEN.getSource();
            }
        };
        tanks[1].withPressure(1);
        tanks[2] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[3] = new FluidTank(OUTPUT_CAPACITY_MB);
    }

    /** Original {@code getConPos}: {x, z} relativ zum Kern und Library.POS_X/NEG_X/POS_Z/NEG_Z. */
    private static final int[][] CON_POS = { {2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {-1, 2}, {1, -2}, {-1, -2} };
    private static final Direction[] CON_DIRS = { Direction.EAST, Direction.EAST, Direction.WEST, Direction.WEST,
            Direction.SOUTH, Direction.SOUTH, Direction.NORTH, Direction.NORTH };

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHydrotreaterBlockEntity be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        // Original: tanks[0].setType(9), tanks[0].loadTank(1, 2), tanks[1].loadTank(3, 4)
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = be.inventory.getStackInSlot(i);
        boolean changed = be.tanks[0].setType(SLOT_FLUID_ID, slots);
        changed |= be.tanks[0].loadTank(1, 2, slots);
        changed |= be.tanks[1].loadTank(3, 4, slots);

        if (level.getGameTime() % CYCLE_INTERVAL == 0) {
            be.reform();
        }

        // Original: tanks[2].unloadTank(5, 6), tanks[3].unloadTank(7, 8)
        changed |= be.tanks[2].unloadTank(5, 6, slots);
        changed |= be.tanks[3].unloadTank(7, 8, slots);
        if (changed) for (int i = 0; i < SLOT_COUNT; i++) be.inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);

        // w16b: Original getConPos() - acht Stellen vor den vier Eck-Anschlusszellen (jetzt echter 3x3x7-Mehrblock)
        for (int i = 0; i < 8; i++) {
            Direction dir = CON_DIRS[i];
            BlockPos neighborPos = pos.offset(CON_POS[i][0], 0, CON_POS[i][1]);
            be.trySubscribe(be.tanks[0].getTankType(), level, neighborPos, dir);
            be.trySubscribe(be.tanks[1].getTankType(), level, neighborPos, dir);
            be.tryProvide(be.tanks[2], level, neighborPos, dir);
            be.tryProvide(be.tanks[3], level, neighborPos, dir);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Direktport von {@code reform()}. */
    private void reform() {
        HydrotreaterRecipe recipe = HydrotreaterRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) {
            tanks[2].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            tanks[3].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            return;
        }
        if (tanks[2].isEmpty()) tanks[2].conform(recipe.getOutput());
        if (tanks[3].isEmpty()) tanks[3].conform(recipe.getSourGas());

        if (getEnergyStored() < POWER_PER_CYCLE) return;
        if (tanks[0].getFill() < OIL_PER_CYCLE_MB) return;
        if (tanks[1].getFill() < recipe.getHydrogenMb()) return;
        if (!hasCatalyst()) return;
        if (tanks[2].getFill() + recipe.getOutputMb() > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + recipe.getSourGasMb() > tanks[3].getMaxFill()) return;

        setEnergyStored(getEnergyStored() - POWER_PER_CYCLE);
        tanks[0].drainMb(OIL_PER_CYCLE_MB);
        tanks[1].drainMb(recipe.getHydrogenMb());
        tanks[2].fillMb(recipe.getOutput(), recipe.getOutputMb());
        tanks[3].fillMb(recipe.getSourGas(), recipe.getSourGasMb());
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
        return new FluidTank[] { tanks[0], tanks[1] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[2], tanks[3] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && (HydrotreaterRecipe.hasRecipe(level, fluid)
                || fluid == com.hbm_m.inventory.fluid.ModFluids.HYDROGEN.getSource()
                || tanks[2].getTankType() == fluid || tanks[3].getTankType() == fluid);
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
        return Component.translatable("container.hbm_m.hydrotreater");
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
        // Original: Eingaenge 1/5/7; 3/4 stillgelegt (SlotDeprecated), 2/4/6/8 nur entnehmbar
        return slot == 1 || slot == 5 || slot == 7;
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
        return MachineHydrotreaterMenu.create(id, inventory, this);
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
