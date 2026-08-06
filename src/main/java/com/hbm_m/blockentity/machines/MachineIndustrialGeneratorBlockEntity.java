package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineIndustrialGeneratorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Industrial Generator - Port von {@code TileEntityMachineIGenerator} (1.7.10 Original). Im
 * Original ist die komplette {@code updateEntity()} auskommentiert und der Tooltip ein Insider-
 * Scherz ("In memory of all that we have lost") - der Block war im Original faktisch tot/ohne
 * Funktion. Auf ausdruecklichen Wunsch wird hier stattdessen die AUSKOMMENTIERTE Original-Logik
 * (fester Brennstoff + fluessiger Brennstoff + Wasser-/Schmiermittel-Multiplikatoren) tatsaechlich
 * funktionsfaehig umgesetzt, 1:1 nach der im Quelltext hinterlegten (nie aktiven) Formel:
 * {@code output = baseRate * genMult}, {@code genMult = 0.5 + (Wasser vorhanden ? 0.5 : 0) +
 * (Schmiermittel vorhanden ? 0.25 : 0)}.
 * <p>
 * SCOPE-Entscheidung: Die 10 RTG-Slots des Originals (passive Zusatz-Heizleistung) werden NICHT
 * uebernommen - dieser Port hat keine RTG-Pellet-Items mit eindeutiger Heizwert-Zuordnung, und die
 * RTG-Beitrag war im Original ohnehin nur ein kleiner Bonus, kein Kernbestandteil.
 */
public class MachineIndustrialGeneratorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    private static final int FUEL_SLOT_START = 1;
    private static final int FUEL_SLOT_COUNT = 4;
    private static final int SLOT_COUNT = 5;

    private static final long MAX_POWER = 1_000_000L;
    private static final long BASE_RATE = 100L;
    private static final int LIQUID_FUEL_PER_TICK_MB = 10;
    private static final int TANK_CAPACITY_WATER = 16_000;
    private static final int TANK_CAPACITY_LUBRICANT = 4_000;
    private static final int TANK_CAPACITY_FUEL = 16_000;

    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY_WATER);
    private final FluidTank lubricantTank = new FluidTank(TANK_CAPACITY_LUBRICANT);
    private final FluidTank fuelTank = new FluidTank(TANK_CAPACITY_FUEL);

    private int burnTime = 0;
    private int maxBurnTime = 1;

    public MachineIndustrialGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_GENERATOR_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, 0L, MAX_POWER);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return fuelTank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineIndustrialGeneratorBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        chargeItemInSlot(SLOT_BATTERY);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(waterTank.getTankType(), level, pos.relative(dir), dir);
                trySubscribe(lubricantTank.getTankType(), level, pos.relative(dir), dir);
                trySubscribe(fuelTank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        boolean dirty = false;

        if (burnTime <= 0) {
            tryConsumeSolidFuel();
        }

        double genMult = 0.5D
                + (waterTank.getFill() > 0 ? 0.5D : 0.0D)
                + (lubricantTank.getFill() > 0 ? 0.25D : 0.0D);

        boolean burningSolid = burnTime > 0;
        boolean burningLiquid = !burningSolid && fuelTank.getFill() >= LIQUID_FUEL_PER_TICK_MB;

        if (burningSolid || burningLiquid) {
            long output = (long) (BASE_RATE * genMult);
            if (getEnergyStored() < getMaxEnergyStored()) {
                setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + output));
                dirty = true;
            }

            if (burningSolid) {
                burnTime--;
            } else {
                fuelTank.drainMb(LIQUID_FUEL_PER_TICK_MB);
            }
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private void tryConsumeSolidFuel() {
        for (int i = 0; i < FUEL_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(FUEL_SLOT_START + i);
            if (stack.isEmpty()) continue;
            int burnValue = AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
            if (burnValue <= 0) continue;

            stack.shrink(1);
            maxBurnTime = burnValue;
            burnTime = burnValue;
            return;
        }
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { waterTank, lubricantTank, fuelTank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { waterTank, lubricantTank, fuelTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("burn_time", burnTime);
        tag.putInt("max_burn_time", maxBurnTime);
        waterTank.writeToNBT(tag, "tank_water");
        lubricantTank.writeToNBT(tag, "tank_lubricant");
        fuelTank.writeToNBT(tag, "tank_fuel");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("burn_time");
        maxBurnTime = tag.contains("max_burn_time") ? Math.max(1, tag.getInt("max_burn_time")) : 1;
        waterTank.readFromNBT(tag, "tank_water");
        lubricantTank.readFromNBT(tag, "tank_lubricant");
        fuelTank.readFromNBT(tag, "tank_fuel");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.industrial_generator");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot >= FUEL_SLOT_START && slot < FUEL_SLOT_START + FUEL_SLOT_COUNT) {
            return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0;
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineIndustrialGeneratorMenu(containerId, playerInventory, this);
    }

    public FluidTank getWaterTank() { return waterTank; }
    public FluidTank getLubricantTank() { return lubricantTank; }
    public FluidTank getFuelTank() { return fuelTank; }

    public int getBurnTimeScaled(int scale) {
        return maxBurnTime <= 0 ? 0 : (burnTime * scale) / maxBurnTime;
    }

    public boolean isActive() {
        return burnTime > 0 || fuelTank.getFill() >= LIQUID_FUEL_PER_TICK_MB;
    }
}
