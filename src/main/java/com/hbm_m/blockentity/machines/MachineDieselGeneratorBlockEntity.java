package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.menu.MachineDieselGeneratorMenu;

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
 * Diesel Generator - Port von {@code TileEntityMachineDiesel} (1.7.10 Original). Verbrennt einen
 * beliebigen {@code FT_Combustible}-Treibstoff der Grade MEDIUM/HIGH/AERO (LOW wird abgelehnt,
 * genau wie im Original) mit konstant 1mB/Tick, Energie-Output {@code (combustionEnergy/1000) *
 * fuelEfficiency[grade]} - 1:1 aus dem Original ({@code fuelEfficiency}: MEDIUM=0.5, HIGH=0.75,
 * AERO=0.1). Analog zu {@link MachineTurbofanBlockEntity} (gleiches FT_Combustible-Lookup-Muster),
 * aber fuer die dieseltypischen Grades statt nur AERO.
 * <p>
 * SCOPE-Entscheidungen:
 * <ul>
 *   <li>Kein Bucket-/Kanister-Item-Slot-Paar fuer die Betankung (Original: Slot 0 Container-Input,
 *   Slot 1 leerer-Container-Output) - wie bei allen anderen Fluid-Maschinen dieser Session wird
 *   der Tank stattdessen direkt ueber das MK2-Fluid-Netz befuellt ({@link IFluidStandardReceiverMK2}).</li>
 *   <li>Der <b>An/Aus-Knopf</b> in der Oberflaeche ist wie im Original vorhanden: der Generator
 *   laeuft erst, wenn man ihn anwirft, und laesst sich damit auch ohne Redstone stilllegen.</li>
 *   <li>Abgas wie im Original: die Maschine erbt von
 *   {@link com.hbm_m.blockentity.MachinePollutingBlockEntity}, fuellt ihre drei Rauchtanks und
 *   schiebt sie ins Rohrnetz. Nur der Ueberlauf landet unmittelbar im Verschmutzungsraster.</li>
 * </ul>
 */
public class MachineDieselGeneratorBlockEntity extends com.hbm_m.blockentity.MachinePollutingBlockEntity
        implements IFluidStandardReceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    private static final int SLOT_COUNT = 1;

    private static final int TANK_CAPACITY_MB = 16_000;
    /** Original: {@code super(4, 100)} in {@code TileEntityMachineDiesel}. */
    private static final int SMOKE_BUFFER_MB = 100;
    private static final long MAX_POWER = 50_000L;
    private static final int BURN_MB_PER_TICK = 1;

    private final FluidTank tank = new FluidTank(ModFluids.DIESEL.getSource(), TANK_CAPACITY_MB);

    /** Original: {@code isOn} - der Knopf in der Oberflaeche. */
    private boolean isOn = false;
    /** Original: {@code wasOn} - ob im letzten Tick wirklich verbrannt wurde (Laufanzeige und Ton). */
    private boolean wasOn = false;

    public MachineDieselGeneratorBlockEntity(BlockPos pos, BlockState state) {
        // Original: super(4, 100) - vier Slots, 100 mB Rauchpuffer je Sorte.
        super(ModBlockEntities.DIESEL_GENERATOR_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, 0L, MAX_POWER, SMOKE_BUFFER_MB);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDieselGeneratorBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        chargeItemInSlot(SLOT_BATTERY);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(tank.getTankType(), level, pos.relative(dir), dir);
                // Original: sendSmoke in derselben Schleife - das Abgas geht ins Rohrnetz,
                // idealerweise zu einem Schornstein.
                sendSmoke(level, pos.relative(dir), dir);
            }
        }

        boolean dirty = false;
        boolean burned = false;

        // 1:1: ohne Zuendung passiert nichts, egal wieviel Diesel im Tank steht.
        if (isOn && !level.hasNeighborSignal(pos)) {
            Fluid fuel = tank.getStoredFluid();
            FT_Combustible combustible = FluidType.getTrait(fuel, FT_Combustible.class);
            double efficiency = fuelEfficiency(combustible);

            if (efficiency > 0 && tank.getFluidAmountMb() >= BURN_MB_PER_TICK && getEnergyStored() < getMaxEnergyStored()) {
                long burnValue = combustible.getCombustionEnergy() / 1_000L;
                long output = (long) (burnValue * BURN_MB_PER_TICK * efficiency);

                tank.drainMb(BURN_MB_PER_TICK);

                // Original: alle fuenf Ticks BURN mit Faktor 5 - der Rauch geht in die Rauchtanks,
                // erst deren Ueberlauf verschmutzt die Welt.
                if (level.getGameTime() % 5 == 0) {
                    pollute(tank.getTankType(), FluidReleaseType.BURN, 5F);
                }
                setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + output));
                burned = true;
                dirty = true;
            }
        }

        if (burned != wasOn) {
            wasOn = burned;
            dirty = true;
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /** 1:1 aus dem Original: LOW wird abgelehnt, alles andere hat eine feste Effizienz. */
    private static double fuelEfficiency(@Nullable FT_Combustible combustible) {
        if (combustible == null) return 0.0D;
        return switch (combustible.getGrade()) {
            case MEDIUM -> 0.5D;
            case HIGH -> 0.75D;
            case AERO -> 0.1D;
            default -> 0.0D;
        };
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank, smoke, smokeLeaded, smokePoison };
    }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("wasOn", wasOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "tank");
        isOn = tag.getBoolean("isOn");
        wasOn = tag.getBoolean("wasOn");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.dieselgen");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && isEnergyProviderItem(stack);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineDieselGeneratorMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    public boolean isOn() {
        return isOn;
    }

    /** Original: {@code wasOn} - laeuft der Motor gerade wirklich? */
    public boolean wasOn() {
        return wasOn;
    }

    /** Original: {@code receiveControl} mit dem Schluessel {@code turnOn}. */
    public void toggleIgnition() {
        isOn = !isOn;
        setChanged();
        sendUpdateToClient();
    }

    public boolean isActive() {
        Fluid fuel = tank.getStoredFluid();
        return fuelEfficiency(FluidType.getTrait(fuel, FT_Combustible.class)) > 0 && tank.getFluidAmountMb() >= BURN_MB_PER_TICK;
    }
}
