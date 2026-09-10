package com.hbm_m.blockentity.machines;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Gas Turbine - Port von {@code TileEntityMachineTurbineGas} (1.7.10 Original).
 * <p>
 * Das Zustandsmodell ist 1:1 uebernommen: Anlauframpe ueber 580 Ticks (mit dem vollen
 * Anzeigenausschlag zu Beginn), Auslauframpe ueber 225, Drehzahl- und Temperaturtraegheit,
 * geglaettete Momentanleistung und die lastabhaengige Wasserverdampfung.
 * <p>
 * Ersetzt ist nur die Bedienung: das Original hat GUI-Knoepfe fuer Start/Stop, Auto-Modus und
 * einen Leistungsregler ({@code IControlReceiver}). Dieser Port kennt projektweit kein
 * Steuerpaket, darum schaltet Redstone die Turbine (entsperrt laeuft an, gesperrt laeuft aus) und
 * der Leistungsregler folgt dauerhaft der Auto-Modus-Formel des Originals. Nicht portiert sind
 * ausserdem OpenComputers und die beiden Turbinenklaenge (ersatzweise Leuchtfeuer-Klaenge).
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

    // ── Zustandsmodell, 1:1 aus dem Original ────────────────────────────────
    /** Original: {@code rpmIdle = 10}. */
    private static final int RPM_IDLE = 10;
    /** Original: {@code tempIdle = 300}. */
    private static final int TEMP_IDLE = 300;
    /** Original: der Zaehler laeuft beim Anlauf bis 580 und beim Auslauf von 225 herunter. */
    private static final int STARTUP_END = 580;
    private static final int SHUTDOWN_START = 225;

    /** 0-100, Drehzahlanzeige. Leistung entsteht erst oberhalb von {@link #RPM_IDLE}. */
    private int rpm = 0;
    /** 0-800 Grad; ab 300 wird Wasser verdampft. */
    private int temp = 0;
    /** Original: {@code powerSliderPos}, 0 bis 60. */
    private int powerSliderPos = 0;
    /** Original: {@code throttle}, 0 bis 100 - dieselbe Groesse, andere Skala. */
    private int throttle = 0;
    /** Original: {@code state} - 0 aus, -1 im Anlauf, 1 in Betrieb. */
    private int state = 0;
    /** Original: {@code counter} - treibt Anlauf und Auslauf. */
    private int counter = 0;
    /** Original: {@code instantPowerOutput} - geglaettete Momentanleistung. */
    private int instantPowerOutput = 0;
    /** Original: {@code waterToBoil} - nur zur Anzeige zwischengehalten. */
    private double waterToBoil = 0D;
    /** Original: {@code rpmLast}/{@code tempLast} - Ausgangswerte der Auslauframpe. */
    private int rpmLast = 0;
    private int tempLast = 0;

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

        waterToBoil = 0D;
        // Original: throttle = powerSliderPos * 100 / 60.
        throttle = powerSliderPos * 100 / 60;

        // Das Original schaltet die Turbine ueber GUI-Knoepfe ein und aus. Dieser Port kennt
        // projektweit kein Steuerpaket, darum uebernimmt Redstone den Schalter: entsperrt heisst
        // anlaufen, gesperrt heisst auslaufen.
        boolean wantRun = !level.hasNeighborSignal(worldPosition);

        if (wantRun && state == 0 && hasAcceptableFuel()
                && gasTank.getFluidAmountMb() > 0 && lubeTank.getFluidAmountMb() > 0) {
            state = -1;
            counter = 0;
        } else if (!wantRun && state != 0) {
            state = 0;
        }

        updateAutoSlider();

        switch (state) {
            case 0 -> shutdown();
            case -1 -> { stopIfNotReady(); startup(); }
            case 1 -> { stopIfNotReady(); run(); }
            default -> { }
        }

        active = state == 1;

        if (wasActive != active || rpm > 0 || counter > 0) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /**
     * Original: der {@code autoMode}-Zweig aus {@code updateEntity}. Dort waehlbar, hier immer
     * aktiv - der Port hat keine GUI-Knoepfe, und das lastabhaengige Nachfuehren ist das
     * sinnvollere Standardverhalten.
     */
    private void updateAutoSlider() {
        if (state == 0) return;

        int target;
        // Original: unter 10% Tankfuellung wird der Verbrauch linear zurueckgenommen.
        if (gasTank.getFluidAmountMb() * 10 > gasTank.getCapacityMb()) {
            target = 60 - (int) (60 * getEnergyStored() / getMaxEnergyStored());
        } else {
            target = (int) (gasTank.getFluidAmountMb() * 0.0001D
                    * (60 - (int) (60 * getEnergyStored() / getMaxEnergyStored())));
        }

        // Original: der Regler gleitet, er springt nicht.
        if (target > powerSliderPos) powerSliderPos++;
        else if (target < powerSliderPos) powerSliderPos--;
    }

    /** Original: {@code stopIfNotReady} - ohne Kraftstoff oder Schmiermittel geht sie aus. */
    private void stopIfNotReady() {
        if (gasTank.getFluidAmountMb() == 0 || lubeTank.getFluidAmountMb() == 0) state = 0;
        if (!hasAcceptableFuel()) state = 0;
    }

    /**
     * Original: {@code startup} - die Anzeige schlaegt erst einmal voll aus und zurueck, dann
     * laufen Drehzahl und Temperatur ueber gut eine halbe Minute auf Leerlauf hoch.
     */
    private void startup() {
        counter++;

        if (counter <= 20) {
            rpm = 5 * counter;
        } else if (counter <= 40) {
            rpm = 100 - 5 * (counter - 20);
        } else if (counter > 50) {
            rpm = RPM_IDLE * (counter - 50) / 530;
            temp = TEMP_IDLE * (counter - 50) / 530;
        }

        if (counter == 50 && level != null) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }

        if (counter == STARTUP_END) {
            // Original: auf 225 setzen, damit ein sofortiges Abschalten sauber auslaeuft.
            counter = SHUTDOWN_START;
            state = 1;
        }
    }

    /** Original: {@code shutdown} - erst auf Leerlauf abbremsen, dann ueber 225 Ticks auslaufen. */
    private void shutdown() {
        instantPowerOutput = 0;

        if (powerSliderPos > 0) powerSliderPos--;

        if (rpm <= 10 && counter > 0) {

            if (counter == SHUTDOWN_START) {
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
                }
                rpmLast = rpm;
                tempLast = temp;
            }

            counter--;
            rpm = rpmLast * counter / SHUTDOWN_START;
            temp = tempLast * counter / SHUTDOWN_START;

        } else if (rpm > 11) {
            // Original: ein absichtlich unerreichbarer Wert, damit der Zweig oben erst danach greift.
            counter = 42069;
            rpm--;
        } else if (rpm == 11) {
            counter = SHUTDOWN_START;
            rpm--;
        }
    }

    /**
     * Original: {@code run} - Drehzahl und Temperatur laufen der Drosselstellung traege nach,
     * die Drehzahl mit einem Schritt je 5 Ticks nach oben und je 2 nach unten.
     */
    private void run() {
        int target = (int) (throttle * 0.9D);

        if (target > rpm - RPM_IDLE) {
            if (level.getGameTime() % 5 == 0) rpm++;
        } else if (target < rpm - RPM_IDLE) {
            if (level.getGameTime() % 2 == 0) rpm--;
        }

        FT_Combustible trait = FluidType.getTrait(gasTank.getStoredFluid(), FT_Combustible.class);
        int maxTemp = trait != null ? getFluidBurnTemp(trait) : TEMP_IDLE;

        int tempTarget = throttle * 5 * (maxTemp - TEMP_IDLE) / 500;
        if (tempTarget > temp - TEMP_IDLE) {
            if (level.getGameTime() % 2 == 0) temp++;
        } else if (tempTarget < temp - TEMP_IDLE) {
            if (level.getGameTime() % 2 == 0) temp--;
        }

        // Original: SOOT_PER_SECOND * 3 im Sekundentakt - ausser bei Knallgas, das sauber verbrennt.
        if (level.getGameTime() % 20 == 0
                && gasTank.getTankType() != ModFluids.OXYHYDROGEN.getSource()) {
            PollutionHandler.incrementPollution(level, worldPosition, PollutionType.SOOT,
                    PollutionHandler.SOOT_PER_SECOND * 3);
        }

        double consMax = FUEL_MAX_CONSUMPTION.getOrDefault(gasTank.getTankType(), DEFAULT_MAX_CONSUMPTION);
        makePower(consMax, throttle);
    }

    private boolean hasAcceptableFuel() {
        FT_Combustible trait = FluidType.getTrait(gasTank.getStoredFluid(), FT_Combustible.class);
        return trait != null && trait.getGrade() == FuelGrade.GAS;
    }

    /** 1:1-Port von {@code makePower(double consMax, int throttle)}. */
    private void makePower(double consMax, int throttle) {

        // Original: Leerlaufverbrauch plus lastabhaengiger Anteil.
        double idleConsumption = consMax * 0.05D;
        double consumption = idleConsumption + consMax * throttle / 100D;

        fuelToConsume += consumption;
        int toDrainFuel = (int) Math.floor(fuelToConsume);
        fuelToConsume -= toDrainFuel;

        if (toDrainFuel > 0) {
            if (toDrainFuel >= gasTank.getFluidAmountMb()) {
                gasTank.drainMb(gasTank.getFluidAmountMb());
                state = 0;
            } else {
                gasTank.drainMb(toDrainFuel);
            }
        }

        if (level.getGameTime() % 10 == 0) {
            if (lubeTank.getFluidAmountMb() <= 1) {
                lubeTank.drainMb(lubeTank.getFluidAmountMb());
                state = 0;
            } else {
                lubeTank.drainMb(1);
            }
        }

        FT_Combustible trait = FluidType.getTrait(gasTank.getStoredFluid(), FT_Combustible.class);
        long energyPerMb = trait != null ? trait.getCombustionEnergy() / 1000L : 0L;

        // Original: rpmEff ist die Drehzahl oberhalb des Leerlaufs, 0 bis 90.
        int rpmEff = rpm - RPM_IDLE;
        double targetOutput = consMax * energyPerMb * rpmEff / 90D;

        // Original: die Leistung wird herangefuehrt statt umgeschaltet, damit sie nicht springt.
        if (instantPowerOutput < targetOutput) {
            instantPowerOutput += Math.random() * 0.005D * consMax * energyPerMb;
            if (instantPowerOutput > targetOutput) instantPowerOutput = (int) targetOutput;
        } else if (instantPowerOutput > targetOutput) {
            instantPowerOutput -= Math.random() * 0.011D * consMax * energyPerMb;
            if (instantPowerOutput < targetOutput) instantPowerOutput = (int) targetOutput;
        }

        if (instantPowerOutput > 0) {
            setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + instantPowerOutput));
        }

        // Original: die verdampfte Wassermenge haengt an der Temperatur ueber Leerlauf.
        double waterPerTick = consMax * energyPerMb * (temp - TEMP_IDLE) / 220_000D;
        waterToBoil = waterPerTick;
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

    public int getRpm()      { return rpm; }
    public int getTemp()     { return temp; }
    public int getState()    { return state; }
    public int getThrottle() { return throttle; }
    public double getWaterToBoil() { return waterToBoil; }
    public int getInstantPowerOutput() { return instantPowerOutput; }

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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("active", active);
        tag.putDouble("fuel_to_consume", fuelToConsume);
        tag.putDouble("water_to_boil_acc", waterToBoilAcc);
        tag.putInt("rpm", rpm);
        tag.putInt("temp", temp);
        tag.putInt("state", state);
        tag.putInt("counter", counter);
        tag.putInt("slider", powerSliderPos);
        tag.putInt("instantPower", instantPowerOutput);
        tag.putInt("rpmLast", rpmLast);
        tag.putInt("tempLast", tempLast);
        gasTank.writeToNBT(tag, "gas");
        lubeTank.writeToNBT(tag, "lube");
        waterTank.writeToNBT(tag, "water");
        hotsteamTank.writeToNBT(tag, "hotsteam");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        active = tag.getBoolean("active");
        fuelToConsume = tag.getDouble("fuel_to_consume");
        waterToBoilAcc = tag.getDouble("water_to_boil_acc");
        rpm = tag.getInt("rpm");
        temp = tag.getInt("temp");
        state = tag.getInt("state");
        counter = tag.getInt("counter");
        powerSliderPos = tag.getInt("slider");
        instantPowerOutput = tag.getInt("instantPower");
        rpmLast = tag.getInt("rpmLast");
        tempLast = tag.getInt("tempLast");
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
