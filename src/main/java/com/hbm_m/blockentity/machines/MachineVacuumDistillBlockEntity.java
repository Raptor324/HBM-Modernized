package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineVacuumDistillMenu;
import com.hbm_m.recipe.VacuumDistillRecipes;
import com.hbm_m.recipe.VacuumDistillRecipes.Output;

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
 * Vakuumdestillator: Portierung der Kernlogik aus {@code TileEntityMachineVacuumDistill} (1.7.10
 * Original). Spaltet 100mB Rohoel (Tank 0) pro Tick in vier leichtere Fraktionen (Tanks 1-4) auf,
 * ueber die feste Rezeptliste {@link VacuumDistillRecipes} (Direktport von
 * {@code VacuumRefineryRecipes}). Energie kommt wie im Original aus einer Batterie im Slot statt
 * aus dem HBM-Energienetz.
 * <p>
 * Vereinfachung ggue. Original: Einzelblock-Kern (Anschluss ans MK2-Rohrnetz an allen 6 Seiten)
 * innerhalb eines echten Mehrblock-Footprints (siehe {@link com.hbm_m.block.machines.MachineVacuumDistillBlock}),
 * statt der festen Multiblock-Anschlusspunkte des 1.7.10-Originals - analog zu
 * {@link MachineFractionTowerBlockEntity}.
 */
public class MachineVacuumDistillBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    private static final int SLOT_COUNT = 1;

    private static final long MAX_POWER = 1_000_000L;
    private static final long POWER_PER_CYCLE = 10_000L;
    private static final int OIL_CAPACITY_MB = 4000;
    private static final int OUTPUT_CAPACITY_MB = 2000;
    private static final int OIL_PER_CYCLE_MB = 100;

    private final FluidTank[] tanks = new FluidTank[5];
    private boolean isOn = false;

    public MachineVacuumDistillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VACUUM_DISTILL_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER, 0L);
        tanks[0] = new FluidTank(OIL_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return VacuumDistillRecipes.has(fluid);
            }
        };
        tanks[1] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[2] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[3] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[4] = new FluidTank(OUTPUT_CAPACITY_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineVacuumDistillBlockEntity be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);
        be.setupTanks();
        be.isOn = be.refine();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            be.trySubscribe(be.tanks[0].getTankType(), level, neighborPos, dir);
            be.tryProvide(be.tanks[1], level, neighborPos, dir);
            be.tryProvide(be.tanks[2], level, neighborPos, dir);
            be.tryProvide(be.tanks[3], level, neighborPos, dir);
            be.tryProvide(be.tanks[4], level, neighborPos, dir);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private void setupTanks() {
        Output out = VacuumDistillRecipes.get(tanks[0].getTankType());
        if (out == null) return;
        if (tanks[1].isEmpty()) tanks[1].conform(out.heavy());
        if (tanks[2].isEmpty()) tanks[2].conform(out.reformate());
        if (tanks[3].isEmpty()) tanks[3].conform(out.light());
        if (tanks[4].isEmpty()) tanks[4].conform(out.sour());
    }

    /** Direktport von {@code refine()}. */
    private boolean refine() {
        Output out = VacuumDistillRecipes.get(tanks[0].getTankType());
        if (out == null) return false;
        if (getEnergyStored() < POWER_PER_CYCLE) return false;
        if (tanks[0].getFill() < OIL_PER_CYCLE_MB) return false;
        if (tanks[1].getFill() + VacuumDistillRecipes.HEAVY_MB > tanks[1].getMaxFill()) return false;
        if (tanks[2].getFill() + VacuumDistillRecipes.REFORMATE_MB > tanks[2].getMaxFill()) return false;
        if (tanks[3].getFill() + VacuumDistillRecipes.LIGHT_MB > tanks[3].getMaxFill()) return false;
        if (tanks[4].getFill() + VacuumDistillRecipes.SOUR_MB > tanks[4].getMaxFill()) return false;

        setEnergyStored(getEnergyStored() - POWER_PER_CYCLE);
        tanks[0].drainMb(OIL_PER_CYCLE_MB);
        tanks[1].fillMb(out.heavy(), VacuumDistillRecipes.HEAVY_MB);
        tanks[2].fillMb(out.reformate(), VacuumDistillRecipes.REFORMATE_MB);
        tanks[3].fillMb(out.light(), VacuumDistillRecipes.LIGHT_MB);
        tanks[4].fillMb(out.sour(), VacuumDistillRecipes.SOUR_MB);
        return true;
    }

    // ==================== GUI ====================

    public boolean isOn() {
        return isOn;
    }

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
        return new FluidTank[] { tanks[1], tanks[2], tanks[3], tanks[4] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && (VacuumDistillRecipes.has(fluid)
                || tanks[1].getTankType() == fluid || tanks[2].getTankType() == fluid
                || tanks[3].getTankType() == fluid || tanks[4].getTankType() == fluid);
    }

    // ==================== NBT ====================

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank" + i);
        }
        tag.putBoolean("isOn", isOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank" + i);
        }
        isOn = tag.getBoolean("isOn");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.vacuum_distill");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && isEnergyProviderItem(stack);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineVacuumDistillMenu.create(id, inventory, this);
    }
}
