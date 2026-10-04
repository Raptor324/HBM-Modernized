package com.hbm_m.blockentity.machines;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.MachineTurbineGasMenu;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineTurbineGas}: Gasturbine mit Start/Stop-Knopf, Leistungsschieber (0-60), Automatik (folgt
 * Speicherstand und Kraftstoff), Anlauf ueber 580 Ticks mit Anzeigenausschlag, Auslauf ueber 225, Drehzahl- und
 * Temperaturtraegheit, geglaetteter Momentanleistung und lastabhaengiger Heissdampferzeugung. Kraftstoff (Gas-Klasse)
 * und Schmiermittel links/rechts vorn, Wasser hinten, Heissdampf rechts oben, Strom links oben. Bedienung ueber
 * {@code IControlReceiver}, Klaenge Anlauf/Lauf/Auslauf, ROR-Werte und -Funktionen.
 */
public class MachineTurbineGasBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2,
        com.hbm_m.api.tile.IControlReceiver, IRORValueProvider, IRORInteractive {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int INVENTORY_SIZE = 2;

    public static final long maxPower = 1000000L;

    public int rpm; // 0-100
    public int temp; // 0-800
    public int rpmIdle = 10;
    public int tempIdle = 300;

    public int powerSliderPos; // 0-60
    public int throttle; // 0-100

    public boolean autoMode;
    public int state = 0; // 0 aus, -1 Anlauf, 1 Betrieb

    public int counter = 0;
    public int instantPowerOutput;
    public double waterToBoil;

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.GAS.getSource(), 100000),
            new FluidTank(ModFluids.LUBRICANT.getSource(), 16000),
            new FluidTank(ModFluids.WATER.getSource(), 16000),
            new FluidTank(ModFluids.HOTSTEAM.getSource(), 160000)
    };

    public static final Map<Fluid, Double> fuelMaxCons = new HashMap<>();
    static {
        fuelMaxCons.put(ModFluids.GAS.getSource(), 50D);
        fuelMaxCons.put(ModFluids.SYNGAS.getSource(), 10D);
        fuelMaxCons.put(ModFluids.OXYHYDROGEN.getSource(), 100D);
        fuelMaxCons.put(ModFluids.REFORMGAS.getSource(), 5D);
    }

    /** Fuer die GUI: Speicherstand vor Abgabe ans Netz (Original {@code powerBeforeNet}). */
    public long powerBeforeNet;

    int rpmLast;
    int tempLast;
    double fuelToConsume;

    public MachineTurbineGasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINEGAS_BE.get(), pos, state, INVENTORY_SIZE, maxPower, 0L, maxPower);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbineGasBlockEntity be) {
        if (level instanceof ServerLevel world) be.serverTick(world, pos);
        else be.clientTick();
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        waterToBoil = 0;
        throttle = powerSliderPos * 100 / 60;

        ItemStack id = inventory.getStackInSlot(SLOT_FLUID_ID);
        if (id.getItem() instanceof FluidIdentifierItem) {
            Fluid fluid = FluidIdentifierItem.getType(id, true);
            if (fluid != null) {
                FT_Combustible trait = FluidType.getTrait(fluid, FT_Combustible.class);
                if (trait != null && trait.getGrade() == FuelGrade.GAS) tanks[0].setTankType(fluid);
            }
        }

        if (autoMode) {
            int powerSliderTarget;
            if (tanks[0].getFill() * 10 > tanks[0].getMaxFill()) {
                powerSliderTarget = 60 - (int) (60 * energy / maxPower);
            } else {
                powerSliderTarget = (int) (tanks[0].getFill() * 0.0001 * (60 - (int) (60 * energy / maxPower)));
            }

            if (powerSliderTarget > powerSliderPos) powerSliderPos++;
            else if (powerSliderTarget < powerSliderPos) powerSliderPos--;
        }

        switch (state) {
            case 0 -> shutdown(world);
            case -1 -> { stopIfNotReady(); startup(world); }
            case 1 -> { stopIfNotReady(); run(world); }
            default -> { }
        }

        Direction dir = getBlockState().getValue(DummyableMachineBlock.FACING);
        Direction rot = dir.getClockWise();

        powerBeforeNet = Math.min(this.energy, maxPower);

        // Original: zuerst Akku und Netz bedienen, dann auf die Kapazitaet begrenzen
        chargeItemInSlot(SLOT_BATTERY);
        BlockPos powerPos = pos.relative(rot, 5).above();
        this.tryProvide(world, powerPos.getX(), powerPos.getY(), powerPos.getZ(), rot);

        if (this.energy > maxPower) this.energy = maxPower;

        for (int i = 0; i < 2; i++) { // Kraftstoff und Schmiermittel
            this.trySubscribe(tanks[i].getTankType(), world, pos.relative(dir, -2).relative(rot), dir.getOpposite());
            this.trySubscribe(tanks[i].getTankType(), world, pos.relative(dir, 2).relative(rot), dir);
        }
        // Wasser
        this.trySubscribe(tanks[2].getTankType(), world, pos.relative(dir, -2).relative(rot, -4), dir.getOpposite());
        this.trySubscribe(tanks[2].getTankType(), world, pos.relative(dir, 2).relative(rot, -4), dir);
        // Heissdampf
        this.tryProvide(tanks[3], world, pos.relative(rot, -6).above(), rot.getOpposite());

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, rpm >= 10 && state != -1, this::createAudioLoop);
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.TurbineGasLoopSoundFactory").getMethod("create", MachineTurbineGasBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, false, null);
    }

    private void stopIfNotReady() {
        if (tanks[0].getFill() == 0 || tanks[1].getFill() == 0) state = 0;
        if (!hasAcceptableFuel()) state = 0;
    }

    public boolean hasAcceptableFuel() {
        FT_Combustible trait = FluidType.getTrait(tanks[0].getTankType(), FT_Combustible.class);
        return trait != null && trait.getGrade() == FuelGrade.GAS;
    }

    private void startup(Level world) {
        counter++;

        if (counter <= 20) rpm = 5 * counter;
        else if (counter > 20 && counter <= 40) rpm = 100 - 5 * (counter - 20);
        else if (counter > 50) {
            rpm = rpmIdle * (counter - 50) / 530;
            temp = tempIdle * (counter - 50) / 530;
        }

        if (counter == 50) {
            world.playSound(null, worldPosition.getX(), worldPosition.getY() + 2, worldPosition.getZ(),
                    com.hbm_m.sound.HbmSoundsNT.get("hbm:block.turbinegasStartup"), SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (counter == 580) {
            counter = 225; // damit ein sofortiges Abschalten sauber auslaeuft
            state = 1;
        }
    }

    private void shutdown(Level world) {
        autoMode = false;
        instantPowerOutput = 0;

        if (powerSliderPos > 0) powerSliderPos--;

        if (rpm <= 10 && counter > 0) {

            if (counter == 225) {
                world.playSound(null, worldPosition.getX(), worldPosition.getY() + 2, worldPosition.getZ(),
                        com.hbm_m.sound.HbmSoundsNT.get("hbm:block.turbinegasShutdown"), SoundSource.BLOCKS, 1.0F, 1.0F);
                rpmLast = rpm;
                tempLast = temp;
            }

            counter--;

            rpm = rpmLast * counter / 225;
            temp = tempLast * counter / 225;

        } else if (rpm > 11) {
            counter = 42069;
            rpm--;
        } else if (rpm == 11) {
            counter = 225;
            rpm--;
        }
    }

    /** Original: Brenntemperatur aus der Verbrennungsenergie, 300 bis 800 Grad. */
    protected int getFluidBurnTemp(Fluid type) {
        FT_Combustible trait = FluidType.getTrait(type, FT_Combustible.class);
        double dFuel = trait != null ? trait.getCombustionEnergy() : 0;
        return (int) Math.floor(800D - (Math.pow(Math.E, -dFuel / 100_000D)) * 300D);
    }

    private void run(Level world) {

        if ((int) (throttle * 0.9) > rpm - rpmIdle) {
            if (world.getGameTime() % 5 == 0) rpm++;
        } else if ((int) (throttle * 0.9) < rpm - rpmIdle) {
            if (world.getGameTime() % 2 == 0) rpm--;
        }

        int maxTemp = getFluidBurnTemp(tanks[0].getTankType());

        if (throttle * 5 * (maxTemp - tempIdle) / 500 > temp - tempIdle) {
            if (world.getGameTime() % 2 == 0) temp++;
        } else if (throttle * 5 * (maxTemp - tempIdle) / 500 < temp - tempIdle) {
            if (world.getGameTime() % 2 == 0) temp--;
        }

        double consumption = fuelMaxCons.getOrDefault(tanks[0].getTankType(), 5D);
        if (world.getGameTime() % 20 == 0 && tanks[0].getTankType() != ModFluids.OXYHYDROGEN.getSource())
            PollutionHandler.incrementPollution(world, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
        makePower(world, consumption, throttle);
    }

    private void makePower(Level world, double consMax, int throttle) {

        double idleConsumption = consMax * 0.05D;
        double consumption = idleConsumption + consMax * throttle / 100;

        fuelToConsume += consumption;

        tanks[0].setFill(tanks[0].getFill() - (int) Math.floor(fuelToConsume));
        fuelToConsume -= (int) Math.floor(fuelToConsume);

        if (world.getGameTime() % 10 == 0) tanks[1].setFill(tanks[1].getFill() - 1);

        if (tanks[0].getFill() < 0) {
            tanks[0].setFill(0);
            state = 0;
        }
        if (tanks[1].getFill() < 0) {
            tanks[1].setFill(0);
            state = 0;
        }

        long energyPerMb = 0;
        FT_Combustible trait = FluidType.getTrait(tanks[0].getTankType(), FT_Combustible.class);
        if (trait != null) energyPerMb = trait.getCombustionEnergy() / 1000L;

        int rpmEff = rpm - rpmIdle;

        if (instantPowerOutput < (consMax * energyPerMb * rpmEff / 90)) {
            instantPowerOutput += Math.random() * 0.005 * consMax * energyPerMb;
            if (instantPowerOutput > (consMax * energyPerMb * rpmEff / 90))
                instantPowerOutput = (int) (consMax * energyPerMb * rpmEff / 90);
        } else if (instantPowerOutput > (consMax * energyPerMb * rpmEff / 90)) {
            instantPowerOutput -= Math.random() * 0.011 * consMax * energyPerMb;
            if (instantPowerOutput < (consMax * energyPerMb * rpmEff / 90))
                instantPowerOutput = (int) (consMax * energyPerMb * rpmEff / 90);
        }
        this.energy += instantPowerOutput;

        double waterPerTick = (consMax * energyPerMb * (temp - tempIdle) / 220000);

        this.waterToBoil = waterPerTick;

        int heatCycles = (int) Math.floor(waterToBoil);
        int waterCycles = tanks[2].getFill();
        int steamCycles = (tanks[3].getMaxFill() - tanks[3].getFill()) / 10;
        int cycles = Math.min(heatCycles, Math.min(waterCycles, steamCycles));

        tanks[2].setFill(tanks[2].getFill() - cycles);
        tanks[3].setFill(tanks[3].getFill() + cycles * 10);
    }

    // ==================== Steuerung ====================

    @Override
    public boolean hasPermission(Player player) {
        return Math.sqrt(player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) < 25;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("slidPos")) powerSliderPos = (int) data.getDouble("slidPos");
        if (data.contains("autoMode")) autoMode = data.getBoolean("autoMode");
        if (data.contains("state")) state = data.getInt("state");
        this.setChanged();
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != Direction.DOWN;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0], tanks[1], tanks[2] }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[3] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fromDir != Direction.DOWN;
    }

    // ==================== Accessors ====================

    public FluidTank getGasTank() { return tanks[0]; }
    public FluidTank getLubeTank() { return tanks[1]; }
    public FluidTank getWaterTank() { return tanks[2]; }
    public FluidTank getHotsteamTank() { return tanks[3]; }
    public int getRpm() { return rpm; }
    public int getTemp() { return temp; }
    public int getState() { return state; }
    public int getThrottle() { return throttle; }
    public int getInstantPowerOutput() { return instantPowerOutput; }
    public double getWaterToBoil() { return waterToBoil; }
    public boolean isActive() { return state == 1; }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "gas");
        tanks[1].writeToNBT(tag, "lube");
        tanks[2].writeToNBT(tag, "water");
        tanks[3].writeToNBT(tag, "densesteam");
        tag.putBoolean("automode", autoMode);
        tag.putLong("power", energy);
        // Original: nur der laufende Zustand wird gespeichert
        if (state == 1) {
            tag.putInt("state", this.state);
            tag.putInt("rpm", this.rpm);
            tag.putInt("temperature", this.temp);
            tag.putInt("slidPos", this.powerSliderPos);
            tag.putInt("instPwr", instantPowerOutput);
            tag.putInt("counter", 225);
        } else {
            tag.putInt("state", 0);
            tag.putInt("rpm", 0);
            tag.putInt("temperature", 20);
            tag.putInt("slidPos", 0);
            tag.putInt("instpwr", 0);
            tag.putInt("counter", 0);
        }
        // Client-Sync (Original serialize)
        tag.putLong("powerBeforeNet", powerBeforeNet);
        tag.putInt("syncState", state);
        tag.putInt("syncRpm", rpm);
        tag.putInt("syncTemp", temp);
        tag.putInt("syncSlid", powerSliderPos);
        tag.putInt("syncThrottle", throttle);
        tag.putInt("syncCounter", counter);
        tag.putInt("syncInst", instantPowerOutput);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "gas");
        tanks[1].readFromNBT(tag, "lube");
        tanks[2].readFromNBT(tag, "water");
        tanks[3].readFromNBT(tag, "densesteam");
        this.autoMode = tag.getBoolean("automode");
        this.energy = tag.getLong("power");
        this.state = tag.getInt("state");
        this.rpm = tag.getInt("rpm");
        this.temp = tag.getInt("temperature");
        this.powerSliderPos = tag.getInt("slidPos");
        this.instantPowerOutput = tag.getInt("instPwr");
        this.counter = tag.getInt("counter");
    }

    @Override
    protected void applyClientUpdate(CompoundTag tag) {
        super.applyClientUpdate(tag);
        this.energy = tag.getLong("powerBeforeNet");
        this.state = tag.getInt("syncState");
        this.rpm = tag.getInt("syncRpm");
        this.temp = tag.getInt("syncTemp");
        this.powerSliderPos = tag.getInt("syncSlid");
        this.throttle = tag.getInt("syncThrottle");
        // Original serialize: im Betrieb kommt die Momentanleistung, sonst der Zaehler
        if (this.state != 1) this.counter = tag.getInt("syncCounter");
        else this.instantPowerOutput = tag.getInt("syncInst");
    }

    // ==================== ROR ====================

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "turbinepercent",
                PREFIX_VALUE + "turbinespeed",
                PREFIX_VALUE + "output",
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "automode",
                PREFIX_VALUE + "temp",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "lubricant",
                PREFIX_VALUE + "water",
                PREFIX_VALUE + "steam",
                PREFIX_FUNCTION + "setauto" + NAME_SEPARATOR + "auto",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "turbinepercent").equals(name)) return "" + (int) (this.powerSliderPos * 100D / 60D);
        if ((PREFIX_VALUE + "turbinespeed").equals(name)) return "" + this.rpm;
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (int) (this.instantPowerOutput * 20);
        if ((PREFIX_VALUE + "state").equals(name)) return "" + this.state;
        if ((PREFIX_VALUE + "automode").equals(name)) return "" + (this.autoMode ? 1 : 0);
        if ((PREFIX_VALUE + "temp").equals(name)) return "" + this.temp;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + this.energy;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "lubricant").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "water").equals(name)) return "" + tanks[2].getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + tanks[3].getFill();
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setauto").equals(name) && params.length > 0) {
            try {
                this.autoMode = Integer.parseInt(params[0]) == 1;
                this.setChanged();
            } catch (NumberFormatException e) { }
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            try {
                int percent = Math.max(0, Math.min(100, Integer.parseInt(params[0])));
                this.powerSliderPos = percent * 60 / 100;
                this.setChanged();
            } catch (NumberFormatException e) { }
            return null;
        }
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            try {
                int newState = Integer.parseInt(params[0]);
                if (newState == 1) {
                    if (this.state == 0) this.state = -1;
                } else if (newState == 0) {
                    if (this.state == 1) this.state = 0;
                }
                this.setChanged();
            } catch (NumberFormatException e) { }
            return null;
        }
        return null;
    }

    // ==================== Sonstiges ====================

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyProviderItem(stack) || isEnergyReceiverItem(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

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

    /** Original: 11x3x11 um den Kern. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 5, worldPosition.getY(), worldPosition.getZ() - 5,
                worldPosition.getX() + 6, worldPosition.getY() + 3, worldPosition.getZ() + 6);
    }
}
