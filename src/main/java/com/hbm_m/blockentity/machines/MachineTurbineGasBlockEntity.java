package com.hbm_m.blockentity.machines;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.menu.MachineTurbineGasMenu;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Gas Turbine - Port von {@code TileEntityMachineTurbineGas} (1.7.10 Original). Das Original
 * simuliert Startup/Shutdown-Rampen, ein RPM-/Temperatur-Traegheitsmodell, einen manuellen
 * Leistungsregler samt Auto-Modus sowie Redstone-over-Radio-/OpenComputers-Integration - all das
 * entfaellt hier (dokumentierte Vereinfachung, analog zu {@code MachineCombustionEngineBlockEntity}:
 * Redstone-Sperre statt manuellem Start/Stop-Knopf, immer volle Drosselstellung wenn aktiv, kein
 * RPM-/Temperatur-Traegheitsmodell - die Wasser/Dampf-Umwandlung nimmt direkt die volle
 * Betriebstemperatur an).
 * <p>
 * WICHTIGER FUND: GAS/SYNGAS/REFORMGAS/OXYHYDROGEN besassen im Original selbst NIE eine {@code
 * FT_Combustible}-Eigenschaft (siehe Kommentar in {@code ContainerMachineTurbineGas}: "redundant
 * restriction that does nothing at best and at worst breaks shit") - {@code hasAcceptableFuel()}
 * war dort permanent {@code false}, der Gas Turbine also nie tatsaechlich lauffaehig. Die
 * entsprechenden Traits wurden in {@code ModFluidTraitsBootstrap} nachtraeglich (mit erfundenen,
 * nicht 1:1 belegten Energiewerten) ergaenzt, damit dieser Port tatsaechlich funktioniert - analog
 * zur Industrial-Generator-Entscheidung dieser Session.
 */
public class MachineTurbineGasBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IEnergyModeHolder {

    public static final int SLOT_BATTERY  = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int INVENTORY_SIZE = 2;

    private static final int GAS_TANK_CAPACITY      = 100_000;
    private static final int LUBE_TANK_CAPACITY      = 16_000;
    private static final int WATER_TANK_CAPACITY     = 16_000;
    private static final int HOTSTEAM_TANK_CAPACITY  = 160_000;
    private static final long MAX_POWER              = 1_000_000L;
    private static final long ENERGY_EXTRACT_RATE    = 50_000L;

    /** Kraftstoffverbrauch pro Tick bei Volllast, mb - 1:1 aus dem Original ({@code fuelMaxCons}). */
    private static final Map<Fluid, Double> FUEL_MAX_CONSUMPTION = new HashMap<>();
    static {
        FUEL_MAX_CONSUMPTION.put(ModFluids.GAS.getSource(), 50D);
        FUEL_MAX_CONSUMPTION.put(ModFluids.SYNGAS.getSource(), 10D);
        FUEL_MAX_CONSUMPTION.put(ModFluids.OXYHYDROGEN.getSource(), 100D);
        FUEL_MAX_CONSUMPTION.put(ModFluids.REFORMGAS.getSource(), 5D);
    }
    private static final double DEFAULT_MAX_CONSUMPTION = 5D;

    private final FluidTank gasTank      = new FluidTank(ModFluids.GAS.getSource(), GAS_TANK_CAPACITY);
    private final FluidTank lubeTank     = new FluidTank(ModFluids.LUBRICANT.getSource(), LUBE_TANK_CAPACITY);
    private final FluidTank waterTank    = new FluidTank(ModFluids.WATER.getSource(), WATER_TANK_CAPACITY);
    private final FluidTank hotsteamTank = new FluidTank(ModFluids.HOTSTEAM.getSource(), HOTSTEAM_TANK_CAPACITY);

    private double fuelToConsume  = 0D;
    private double waterToBoilAcc = 0D;
    private boolean active = false;

    public MachineTurbineGasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINEGAS_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, ENERGY_EXTRACT_RATE);
    }

    @Override
    public int getCurrentMode() {
        return 2; // OUTPUT only, so the energy network treats this as a generator.
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbineGasBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();

        ItemStack idStack = inventory.getStackInSlot(SLOT_FLUID_ID);
        if (idStack.getItem() instanceof FluidIdentifierItem) {
            Fluid candidate = FluidIdentifierItem.resolvePrimaryForTank(idStack);
            FT_Combustible trait = candidate != null ? FluidType.getTrait(candidate, FT_Combustible.class) : null;
            if (trait != null && trait.getGrade() == FuelGrade.GAS) {
                ItemStack[] slots = new ItemStack[]{ idStack };
                if (gasTank.setType(0, slots)) {
                    setChanged();
                }
            }
        }

        chargeItemInSlot(SLOT_BATTERY);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof IFluidConnectorMK2)) continue;

                trySubscribe(gasTank.getTankType(), level, neighborPos, dir);
                trySubscribe(lubeTank.getTankType(), level, neighborPos, dir);
                trySubscribe(waterTank.getTankType(), level, neighborPos, dir);
                if (hotsteamTank.getFill() > 0) {
                    tryProvide(hotsteamTank, level, neighborPos, dir);
                }
            }
        }

        boolean wasActive = active;
        active = !level.hasNeighborSignal(worldPosition) && hasAcceptableFuel()
                && gasTank.getFluidAmountMb() > 0 && lubeTank.getFluidAmountMb() > 0;

        if (active) {
            makePower();
        }

        if (wasActive != active || active) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean hasAcceptableFuel() {
        FT_Combustible trait = FluidType.getTrait(gasTank.getStoredFluid(), FT_Combustible.class);
        return trait != null && trait.getGrade() == FuelGrade.GAS;
    }

    private void makePower() {
        FT_Combustible trait = FluidType.getTrait(gasTank.getStoredFluid(), FT_Combustible.class);
        if (trait == null) return;

        double consMax = FUEL_MAX_CONSUMPTION.getOrDefault(gasTank.getTankType(), DEFAULT_MAX_CONSUMPTION);
        double consumption = consMax * 0.05D + consMax; // idle overhead + full throttle (immer Volllast, siehe Klassenkommentar)

        fuelToConsume += consumption;
        int toDrainFuel = (int) Math.floor(fuelToConsume);
        fuelToConsume -= toDrainFuel;
        gasTank.drainMb(Math.min(toDrainFuel, gasTank.getFluidAmountMb()));

        if (level.getGameTime() % 10 == 0) {
            lubeTank.drainMb(Math.min(1, lubeTank.getFluidAmountMb()));
        }

        long energyPerMb = trait.getCombustionEnergy() / 1000L;
        long output = (long) (consumption * energyPerMb);
        setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + output));

        int maxTemp = getFluidBurnTemp(trait);
        double waterPerTick = consMax * energyPerMb * (maxTemp - 300D) / 220_000D;
        waterToBoilAcc += Math.max(0D, waterPerTick);

        int heatCycles = (int) Math.floor(waterToBoilAcc);
        int waterCycles = waterTank.getFluidAmountMb();
        int steamCycles = (hotsteamTank.getCapacityMb() - hotsteamTank.getFluidAmountMb()) / 10;
        int cycles = Math.min(heatCycles, Math.min(waterCycles, steamCycles));
        if (cycles > 0) {
            waterToBoilAcc -= cycles;
            waterTank.drainMb(cycles);
            hotsteamTank.fillMb(ModFluids.HOTSTEAM.getSource(), cycles * 10);
        }
    }

    /** 1:1 aus dem Original: skaliert von 300°C-800°C anhand der Verbrennungsenergie. */
    private static int getFluidBurnTemp(FT_Combustible trait) {
        double dFuel = trait.getCombustionEnergy();
        return (int) Math.floor(800D - Math.pow(Math.E, -dFuel / 100_000D) * 300D);
    }

    // ── IFluidStandardTransceiverMK2 ─────────────────────────────────────────

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { gasTank, lubeTank, waterTank, hotsteamTank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { gasTank, lubeTank, waterTank }; }

    @Override
    public FluidTank[] getSendingTanks() {
        return hotsteamTank.getFill() > 0 ? new FluidTank[]{ hotsteamTank } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null || fluid == null || fluid == Fluids.EMPTY) return false;
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.HOTSTEAM.getSource())) return true;
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.WATER.getSource())) return true;
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.LUBRICANT.getSource())) return true;
        FT_Combustible trait = FluidType.getTrait(fluid, FT_Combustible.class);
        return trait != null && trait.getGrade() == FuelGrade.GAS;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public FluidTank getGasTank()      { return gasTank; }
    public FluidTank getLubeTank()     { return lubeTank; }
    public FluidTank getWaterTank()    { return waterTank; }
    public FluidTank getHotsteamTank() { return hotsteamTank; }

    public boolean isActive() { return active; }

    public int getPowerScaled(int scale) {
        long max = Math.max(getMaxEnergyStored(), 1L);
        return (int) Math.min(scale, getEnergyStored() * scale / max);
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("active", active);
        tag.putDouble("fuel_to_consume", fuelToConsume);
        tag.putDouble("water_to_boil_acc", waterToBoilAcc);
        gasTank.writeToNBT(tag, "gas");
        lubeTank.writeToNBT(tag, "lube");
        waterTank.writeToNBT(tag, "water");
        hotsteamTank.writeToNBT(tag, "hotsteam");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean("active");
        fuelToConsume = tag.getDouble("fuel_to_consume");
        waterToBoilAcc = tag.getDouble("water_to_boil_acc");
        gasTank.readFromNBT(tag, "gas");
        lubeTank.readFromNBT(tag, "lube");
        waterTank.readFromNBT(tag, "water");
        hotsteamTank.readFromNBT(tag, "hotsteam");
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> stack.getItem() instanceof ItemCreativeBattery
                                  || isEnergyProviderItem(stack)
                                  || isEnergyReceiverItem(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.turbinegas");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineTurbineGasMenu.create(id, inventory, this);
    }
}
