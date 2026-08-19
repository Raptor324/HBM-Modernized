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
public class MachineHydrotreaterBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_CATALYST = 1;
    private static final int SLOT_COUNT = 2;

    private static final long MAX_POWER = 500_000L;
    private static final long POWER_PER_CYCLE = 20_000L;
    private static final int OIL_CAPACITY_MB = 4000;
    private static final int HYDROGEN_CAPACITY_MB = 4000;
    private static final int OUTPUT_CAPACITY_MB = 2000;
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

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHydrotreaterBlockEntity be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        if (level.getGameTime() % CYCLE_INTERVAL == 0) {
            be.reform();
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
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
        super.readNbtData(tag, registries);
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
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineHydrotreaterMenu.create(id, inventory, this);
    }
}
