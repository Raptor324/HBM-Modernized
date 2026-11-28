package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.missile.SoyuzEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.LaunchpadSoyuzMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.StackNbt;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityLaunchpadSoyuz}: Startrampe mit fahrbarer Katze (Carriage), fuenf Streben,
 * Rotor und Aufnahme (Mount), Zustandsmaschine {@link SoyuzStatus} (ABSENT/LOADING/FUELING/READY/
 * LAUNCHING), 15 s Tankphase, 30 s Countdown, Startposition auf dem Supporting-Tower
 * ({@code -dir*4 -rot*4, y+4}). Zwölf Anschlusszellen ({@code makeExtra}) abonnieren Energie und
 * beide Treibstoffleitungen. Persistiert Positionen/Geschwindigkeiten/Ziele wie im Original
 * ({@code p/s/t0..7}); der Abschuss spawnt {@link SoyuzEntity} (Port von {@code EntityRocketSoyuz}).
 */
public class LaunchpadSoyuzBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider,
        com.hbm_m.api.redstoneoverradio.IRORInteractive {

    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_SATELLITE = 2;
    public static final int SLOT_LANDER = 3;
    public static final int SLOT_FUEL_IN = 4;
    public static final int SLOT_FUEL_OUT = 5;
    public static final int SLOT_OXIDIZER_IN = 6;
    public static final int SLOT_OXIDIZER_OUT = 7;
    public static final int SLOT_BATTERY = 8;
    public static final int CARGO_START = 9;
    public static final int CARGO_END = 26;
    public static final int SLOT_COUNT = 27;

    public static final int INDEX_STRUT1 = 0;
    public static final int INDEX_STRUT2 = 1;
    public static final int INDEX_STRUT3 = 2;
    public static final int INDEX_STRUT4 = 3;
    public static final int INDEX_STRUT5 = 4;
    public static final int INDEX_CARRIAGE = 5;
    public static final int INDEX_ROTOR = 6;
    public static final int INDEX_TILT = 7;
    public static final int POSITION_COUNT = 8;

    public static final long MAX_POWER = 1_000_000L;
    /** Original {@code maxPower}; Verbindung pusht bis {@code maxPower - power} (kein Eigenlimit). */
    private static final long MAX_RECEIVE = MAX_POWER;
    public static final int CONSUMPTION = 10_000;
    public static final int TANK_CAPACITY_MB = 128_000;
    /** Original {@code FUEL_DURATION}: 15 Sekunden. */
    public static final int FUEL_DURATION = 15 * 20;
    public static final int COUNTDOWN_DURATION = 600;

    public enum SoyuzStatus {
        ABSENT,     // no rocket is present, return all components to null position
        LOADING,    // rocket is moved to launch pad
        FUELING,    // rocket is on the launch pad, cooldown is active
        READY,      // rocket is ready to launch
        LAUNCHING   // countdown is active
    }

    public enum ComponentStatus {
        DEPLOY, RETRACT
    }

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.KEROSENE_REFORM.getSource(), TANK_CAPACITY_MB),
            new FluidTank(ModFluids.OXYGEN.getSource(), TANK_CAPACITY_MB)
    };

    public final float[] positions = new float[POSITION_COUNT];
    public final float[] prevPositions = new float[POSITION_COUNT];
    public final float[] speed = new float[POSITION_COUNT];
    public final float[] target = new float[POSITION_COUNT];
    /** Client-only: Werte des letzten Sync-Pakets (Original {@code syncPositions}). */
    public final float[] syncPositions = new float[POSITION_COUNT];
    protected int turnProgress;

    public SoyuzStatus soyuzStatus = SoyuzStatus.ABSENT;
    public ComponentStatus strutStatus = ComponentStatus.RETRACT;
    public ComponentStatus carriageStatus = ComponentStatus.RETRACT;
    public ComponentStatus rotorStatus = ComponentStatus.RETRACT;

    public boolean cargoMode = false;
    public int loadedType = -1;
    public int fuelCountdown = 0;
    public int countdown = 0;

    @Nullable
    private Integer targetX;
    @Nullable
    private Integer targetZ;

    /** Client-only Sound-Ticker (3 Loops, wie im Original {@code AudioWrapper[3]}). */
    @Nullable
    private com.hbm_m.interfaces.IClientTicker padClientTicker;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (energy & 0xFFFFFFFFL);
                case 1 -> (int) (energy >>> 32);
                case 2 -> cargoMode ? 1 : 0;
                case 3 -> countdown;
                case 4 -> soyuzStatus.ordinal();
                case 5 -> loadedType;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-side read-only; server owns the state.
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public LaunchpadSoyuzBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_SOYUZ_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    // ─── Ports (Original getPorts: die 12 makeExtra-Zellen) ────────────────────

    @Nullable
    private java.util.List<Object[]> cachedPorts;
    @Nullable
    private Direction cachedPortFacing;

    /** Port = {BlockPos, Direction subscribe}, Weltkoordinaten; Original {@code getPorts()}. */
    private java.util.List<Object[]> getPorts() {
        Direction facing = getBlockState().hasProperty(com.hbm_m.block.machines.LaunchpadSoyuzBlock.FACING)
                ? getBlockState().getValue(com.hbm_m.block.machines.LaunchpadSoyuzBlock.FACING)
                : Direction.NORTH;
        if (cachedPorts != null && facing == cachedPortFacing) {
            return cachedPorts;
        }

        Direction rot = facing.getClockWise(); // Original: dir.getRotation(ForgeDirection.UP)
        java.util.List<Object[]> ports = new java.util.ArrayList<>();
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        // 4 Ports an der Vorderkante (dir*2), Richtung dir
        ports.add(new Object[] { pos(x, y, z, facing, rot, 2, 1), facing });
        ports.add(new Object[] { pos(x, y, z, facing, rot, 2, -1), facing });
        ports.add(new Object[] { pos(x, y, z, facing, rot, 2, -7), facing });
        ports.add(new Object[] { pos(x, y, z, facing, rot, 2, -9), facing });
        // 4 Ports am Heck (dir*-10), Richtung dir.getOpposite()
        ports.add(new Object[] { pos(x, y, z, facing, rot, -10, 1), facing.getOpposite() });
        ports.add(new Object[] { pos(x, y, z, facing, rot, -10, -1), facing.getOpposite() });
        ports.add(new Object[] { pos(x, y, z, facing, rot, -10, -7), facing.getOpposite() });
        ports.add(new Object[] { pos(x, y, z, facing, rot, -10, -9), facing.getOpposite() });
        // 4 Ports seitlich (rot*-10), Richtung rot.getOpposite()
        ports.add(new Object[] { pos(x, y, z, facing, rot, 1, -10), rot.getOpposite() });
        ports.add(new Object[] { pos(x, y, z, facing, rot, -1, -10), rot.getOpposite() });
        ports.add(new Object[] { pos(x, y, z, facing, rot, -7, -10), rot.getOpposite() });
        ports.add(new Object[] { pos(x, y, z, facing, rot, -9, -10), rot.getOpposite() });

        cachedPorts = ports;
        cachedPortFacing = facing;
        return ports;
    }

    /** Original: {@code xCoord + dir*fwd + rot*side, yCoord + 1}. */
    private static BlockPos pos(int x, int y, int z, Direction dir, Direction rot, int fwd, int side) {
        return new BlockPos(
                x + dir.getStepX() * fwd + rot.getStepX() * side,
                y + 1,
                z + dir.getStepZ() * fwd + rot.getStepZ() * side);
    }

    // ─── Server tick (Original updateEntity, Serverzweig) ──────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, LaunchpadSoyuzBlockEntity be) {
        if (level.isClientSide) {
            return;
        }

        // Original: Port-Module jede Sekunde (Port-Konvention: %20-Throttle wie beim Legacy-Tisch)
        if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) {
            for (Object[] port : be.getPorts()) {
                BlockPos at = (BlockPos) port[0];
                Direction dir = (Direction) port[1];
                BlockPos adj = at.relative(dir);
                be.trySubscribe(server, adj.getX(), adj.getY(), adj.getZ(), dir);
                be.trySubscribe(be.tanks[0].getTankType(), server, adj, dir);
                be.trySubscribe(be.tanks[1].getTankType(), server, adj, dir);
            }
        }

        // Original: Library.chargeTEFromItems(slots, 8, power, maxPower)
        be.chargeFromBatterySlot(SLOT_BATTERY);

        // One slot snapshot per tick, shared by both tank loads and the write-back
        // (loadTank mutates the array in place for the in/out pair - like the legacy table).
        be.cachedSlots = be.slotArray();
        be.tanks[0].loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, be.cachedSlots);
        be.tanks[1].loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, be.cachedSlots);
        be.writeBackTankSlots();
        be.cachedSlots = null;

        if (!be.hasRocketLoaded()) {
            be.loadedType = -1;
            boolean isMoving = false;
            for (int i = 0; i < POSITION_COUNT; i++) {
                if (!be.finishedMoving(i)) {
                    isMoving = true;
                    break;
                }
            }
            if (!isMoving) {
                be.soyuzStatus = SoyuzStatus.ABSENT;
            }
        } else {
            be.loadedType = be.getRocketType();
        }

        if (be.getEnergyStored() >= CONSUMPTION) {
            be.updateStates();
            be.move();
            be.setEnergyStored(be.getEnergyStored() - CONSUMPTION);
        }

        for (int i = INDEX_STRUT1; i <= INDEX_STRUT5; i++) {
            if (be.finishedMoving(i) && be.wasMoving(i)) {
                Direction dir = be.getFacing();
                Direction rot = dir.getClockWise();
                double x = pos.getX() + 0.5 - dir.getStepX() * 4 + rot.getStepX() * 2;
                double z = pos.getZ() + 0.5 - dir.getStepZ() * 4 + rot.getStepZ() * 2;
                level.playSound(null, x, pos.getY() + 25, z,
                        HbmSoundsNT.get("hbm:door.sliding_seal_stop"), SoundSource.BLOCKS, 35F, 0.75F);
            }
        }

        // Sync: kompletter rendrierbarer Zustand inkl. aller 8 Positionen (Original networkPackNT(300))
        long sig = be.stateSignature();
        be.syncIfStateChanged(sig, sig2(be));
    }

    private static long sig2(LaunchpadSoyuzBlockEntity be) {
        long h = 0L;
        for (int i = 0; i < POSITION_COUNT; i++) {
            h = h * 31L + Float.floatToIntBits(be.positions[i]);
        }
        return h;
    }

    private long stateSignature() {
        long stacks = StackNbt.fingerprint(inventory.getStackInSlot(SLOT_ROCKET))
                ^ StackNbt.fingerprint(inventory.getStackInSlot(SLOT_DESIGNATOR))
                ^ StackNbt.fingerprint(inventory.getStackInSlot(SLOT_SATELLITE))
                ^ StackNbt.fingerprint(inventory.getStackInSlot(SLOT_LANDER));
        return com.hbm_m.recipe.index.CachedRecipeResolver.sign(1L,
                soyuzStatus.ordinal(), cargoMode ? 1L : 0L, countdown, fuelCountdown,
                energy, tanks[0].getFill(), tanks[1].getFill(), loadedType, stacks);
    }

    /** Cached slot snapshot for the tank load loops; rebuilt each tick it is needed. */
    @Nullable
    private ItemStack[] cachedSlots;

    private ItemStack[] slotArray() {
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }
        return slots;
    }

    /** loadTank mutiert das Snapshot-Array; die vier Tank-Item-Slots zurueckschreiben. */
    private void writeBackTankSlots() {
        // push the mutated in/out pairs of both tank loads back into the inventory
        ItemStack[] slots = this.cachedSlots != null ? this.cachedSlots : slotArray();
        inventory.setStackInSlot(SLOT_FUEL_IN, slots[SLOT_FUEL_IN]);
        inventory.setStackInSlot(SLOT_FUEL_OUT, slots[SLOT_FUEL_OUT]);
        inventory.setStackInSlot(SLOT_OXIDIZER_IN, slots[SLOT_OXIDIZER_IN]);
        inventory.setStackInSlot(SLOT_OXIDIZER_OUT, slots[SLOT_OXIDIZER_OUT]);
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(com.hbm_m.block.machines.LaunchpadSoyuzBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.LaunchpadSoyuzBlock.FACING)
                : Direction.NORTH;
    }

    // ─── Client tick (Original updateEntity, Clientzweig) ──────────────────────

    public static void clientTick(Level level, BlockPos pos, BlockState state, LaunchpadSoyuzBlockEntity be) {
        for (int i = 0; i < POSITION_COUNT; i++) {
            be.prevPositions[i] = be.positions[i];
            if (be.turnProgress > 0) {
                be.positions[i] = be.positions[i] + ((be.syncPositions[i] - be.positions[i]) / (float) be.turnProgress);
            } else {
                be.positions[i] = be.syncPositions[i];
            }
        }
        if (be.turnProgress > 0) {
            be.turnProgress--;
        }

        Direction dir = be.getFacing();
        Direction rot = dir.getClockWise();

        double x = pos.getX() + 0.5 - dir.getStepX() * 4 - rot.getStepX() * 4;
        double z = pos.getZ() + 0.5 - dir.getStepZ() * 4 - rot.getStepZ() * 4;

        // Turmdampf waehrend Tankphase/Bereitschaft/Countdown (Original: type "tower")
        if ((be.soyuzStatus == SoyuzStatus.FUELING || be.soyuzStatus == SoyuzStatus.READY
                || be.soyuzStatus == SoyuzStatus.LAUNCHING)
                && be.hasOxidizer()
                && be.nearbyPlayerWithin(level, pos, 100)) {

            for (int i = 0; i < 3; i++) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "tower");
                data.putFloat("lift", 0F);
                data.putFloat("base", 0.5F);
                data.putFloat("max", 2F);
                data.putInt("life", 70 + level.random.nextInt(30));
                data.putDouble("posX", x + level.random.nextGaussian() * 0.75);
                data.putDouble("posZ", z + level.random.nextGaussian() * 0.75);
                data.putDouble("posY", pos.getY() + 4);
                data.putBoolean("noWind", true);
                data.putFloat("alphaMod", 2F);
                data.putFloat("strafe", 0.075F);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
            }
        }

        java.util.List<SoyuzEntity> entities = level.getEntitiesOfClass(SoyuzEntity.class,
                new AABB(x - 1, pos.getY() + 4, z - 1, x + 1, pos.getY() + 14, z + 1));

        if (!entities.isEmpty() || (be.soyuzStatus == SoyuzStatus.LAUNCHING && be.countdown <= 20)) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "smoke");
            data.putString("mode", "shockRand");
            data.putInt("count", 50);
            data.putDouble("strength", level.random.nextGaussian() * 3 + 6);
            data.putDouble("posX", x);
            data.putDouble("posY", pos.getY() + 1);
            data.putDouble("posZ", z);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }

        // 3 Loop-Sounds (Original AudioWrapper[3]); Ticker client-only, NOOP auf dem Server
        if (be.padClientTicker == null || be.padClientTicker == com.hbm_m.interfaces.IClientTicker.NOOP) {
            be.padClientTicker = com.hbm_m.util.ClientTickerRegistry.create(be);
            if (be.padClientTicker == null) {
                be.padClientTicker = com.hbm_m.interfaces.IClientTicker.NOOP;
            }
        }
        com.hbm_m.interfaces.IClientTicker.tick(be.padClientTicker);
    }

    /** Original: {@code MainRegistry.proxy.me().getDistanceSq(...) <= 100*100} (Client-Spieler). */
    private boolean nearbyPlayerWithin(Level level, BlockPos pos, double range) {
        for (net.minecraft.world.entity.player.Player p : level.players()) {
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5) <= range * range) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (padClientTicker != null && padClientTicker != com.hbm_m.interfaces.IClientTicker.NOOP) {
            padClientTicker.onRemoved();
        }
    }

    // ─── Zustandsmaschine (Original updateStates) ──────────────────────────────

    public void updateStates() {

        if (this.soyuzStatus == SoyuzStatus.ABSENT) {

            /// RETURN BEHAVIOR ///

            // retract all struts
            if (this.strutStatus == ComponentStatus.DEPLOY) {
                this.strutStatus = ComponentStatus.RETRACT;

                for (int i = INDEX_STRUT1; i <= INDEX_STRUT5; i++) {
                    setTarget(i, false, 60 + level.random.nextInt(21)); // 3-4 seconds
                }
            }

            // first send away the carriage
            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                setTarget(INDEX_CARRIAGE, false, 100); // 5 seconds
            }

            // once carriage has stopped, tilt, then retract rotor
            if (this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                // if the carriage has hit the buffer stops, tilt
                if (wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);

                // after tilt has returned
                if (this.target[INDEX_TILT] == 0 && this.rotorStatus == ComponentStatus.DEPLOY) {
                    this.rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100); // 5 seconds
                }
            }

            // return tilt
            if (this.target[INDEX_TILT] > 0 && this.finishedMoving(INDEX_TILT)) {
                setTarget(INDEX_TILT, false, 3);
            }

            /// DEPLOY BEHAVIOR ///

            // start if rocket is loaded and all relevant components are fully retracted
            if (this.hasRocketLoaded()
                    && this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_ROTOR)) {

                // deploy carriage
                this.carriageStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_CARRIAGE, true, 200); // 10 seconds

                this.soyuzStatus = SoyuzStatus.LOADING;
                return; // always return on status change
            }
        }

        if (this.soyuzStatus == SoyuzStatus.LOADING) {

            // once carriage has returned, start rotor and lift the rocket onto the launch pad
            if (this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                this.rotorStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_ROTOR, true, 200); // 10 seconds
            }

            if (this.carriageStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_ROTOR)) {

                // once carriage and rotor are deployed, extend struts
                if (this.strutStatus == ComponentStatus.RETRACT) {
                    this.strutStatus = ComponentStatus.DEPLOY;

                    for (int i = INDEX_STRUT1; i <= INDEX_STRUT5; i++) {
                        setTarget(i, true, 60 + level.random.nextInt(21)); // 3-4 seconds
                    }
                } else {

                    // if struts are fully deployed, the rocket is in place, start fueling
                    boolean strutsDeployed = true;
                    for (int i = INDEX_STRUT1; i <= INDEX_STRUT5; i++) {
                        if (!this.finishedMoving(i)) strutsDeployed = false;
                    }

                    if (strutsDeployed) {
                        this.fuelCountdown = FUEL_DURATION;
                        this.soyuzStatus = SoyuzStatus.FUELING;
                        return; // always return on status change
                    }
                }
            }
        }

        if (this.soyuzStatus == SoyuzStatus.FUELING) {

            if (this.hasAllFuel()) {
                if (this.fuelCountdown > 0) {
                    this.fuelCountdown--;
                } else {
                    this.soyuzStatus = SoyuzStatus.READY;
                    return; // always return on status change
                }
            }
        }

        if (this.soyuzStatus == SoyuzStatus.READY) {

            // should the fuel somehow not be present during this phase, reset phase back to fueling
            if (!this.hasAllFuel()) {
                this.fuelCountdown = FUEL_DURATION;
                this.soyuzStatus = SoyuzStatus.FUELING;
                return; // always return on status change
            }
        }

        if (this.soyuzStatus == SoyuzStatus.LAUNCHING) {

            // return carriage
            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                this.setTarget(INDEX_CARRIAGE, false, 100); // 5 seconds
            }

            if (this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);

                if (this.target[INDEX_TILT] == 0 && this.rotorStatus == ComponentStatus.DEPLOY) {
                    this.rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100); // 5 seconds
                }
            }

            if (this.target[INDEX_TILT] > 0 && this.finishedMoving(INDEX_TILT)) {
                setTarget(INDEX_TILT, false, 3);
            }

            if (this.countdown > 0) {
                this.countdown--;

                if (countdown % 100 == 0 && countdown > 0) {
                    level.playSound(null, worldPosition, HbmSoundsNT.get("hbm:alarm.hatch"), SoundSource.BLOCKS, 100F, 1.1F);
                }

                if (countdown == 20) {
                    level.playSound(null, worldPosition, HbmSoundsNT.get("hbm:entity.soyuzTakeoff"), SoundSource.BLOCKS, 100F, 1.1F);
                }

            } else {

                if (canLaunch()) {
                    this.soyuzStatus = SoyuzStatus.ABSENT;
                    this.liftOff();
                } else {
                    this.soyuzStatus = SoyuzStatus.READY;
                }

                for (int i = INDEX_STRUT1; i <= INDEX_STRUT5; i++) {
                    setTarget(i, false, 18 + level.random.nextInt(8)); // 1 second
                }
            }
        }
    }

    // ─── Bewegung (Original move/setTarget/...) ────────────────────────────────

    public boolean hasRocketLoaded() {
        return !inventory.getStackInSlot(SLOT_ROCKET).isEmpty()
                && inventory.getStackInSlot(SLOT_ROCKET).getItem() instanceof com.hbm_m.item.special.ItemSoyuz;
    }

    /** Original {@code getType()}: Skin der Rakete oder -1. */
    public int getRocketType() {
        ItemStack rocket = inventory.getStackInSlot(SLOT_ROCKET);
        return rocket.isEmpty() ? -1 : com.hbm_m.item.special.ItemSoyuz.skinOf(rocket);
    }

    public boolean finishedMoving(int index) { return this.positions[index] == this.target[index]; }
    public boolean wasMoving(int index) { return this.positions[index] != this.prevPositions[index]; }

    public boolean hasAllFuel() {
        return hasJetFuel() && hasOxidizer();
    }

    public boolean hasJetFuel() { return this.tanks[0].getFill() >= 100_000; }
    public boolean hasOxidizer() { return this.tanks[1].getFill() >= 100_000; }

    public void setTarget(int index, boolean deploy, int duration) {
        this.target[index] = deploy ? 1F : 0F;
        this.speed[index] = 1F / duration;
    }

    public void move() {
        for (int i = 0; i < POSITION_COUNT; i++) {
            this.prevPositions[i] = this.positions[i];
            if (Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
                this.positions[i] = this.target[i];
            } else if (this.positions[i] < this.target[i]) {
                this.positions[i] += this.speed[i];
            } else {
                this.positions[i] -= this.speed[i];
            }
        }
    }

    /** Original {@code getInterpPos}: Render-Interpolation. */
    public float getInterpPos(int index, float interp) {
        return prevPositions[index] + (positions[index] - prevPositions[index]) * interp;
    }

    // ─── Start (Original canLaunch/liftOff) ────────────────────────────────────

    public boolean canLaunch() {

        // prerequisites for all modes
        if (this.loadedType < 0) return false;
        if (!this.hasAllFuel()) return false;
        if (this.getEnergyStored() < CONSUMPTION) return false;

        // at least one cargo slot must be occupied
        if (this.cargoMode) {
            if (confirmTarget()) {
                for (int i = CARGO_START; i <= CARGO_END; i++) {
                    if (!inventory.getStackInSlot(i).isEmpty()) return true;
                }
            }
            return false;

            // checks for satellite and optional orbital module
        } else {
            if (this.orbital() == 1) return false;
            if (inventory.getStackInSlot(SLOT_SATELLITE).isEmpty()) return false;
            return true;
        }
    }

    /** Original {@code launch()}-Gatter (GUI-Button): nur aus READY. */
    public void launch() {
        if (this.soyuzStatus == SoyuzStatus.READY && canLaunch()) {
            this.soyuzStatus = SoyuzStatus.LAUNCHING;
            this.countdown = COUNTDOWN_DURATION;
            setChanged();
        }
    }

    /** GUI-Modusumschaltung (Original receiveControl "cargo"). */
    public void setCargoMode(boolean cargo) {
        this.cargoMode = cargo;
        setChanged();
    }

    private boolean confirmTarget() {
        ItemStack designator = inventory.getStackInSlot(SLOT_DESIGNATOR);
        if (!designator.isEmpty()
                && designator.getItem() instanceof IDesignatorItem item
                && level != null) {
            Vec3 coords = item.getCoords(level, designator, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            this.targetX = (int) Math.floor(coords.x);
            this.targetZ = (int) Math.floor(coords.z);
            return true;
        }

        return this.targetX != null && this.targetZ != null;
    }

    public void liftOff() {

        Direction dir = getFacing();
        Direction rot = dir.getClockWise();

        double x = worldPosition.getX() + 0.5 - dir.getStepX() * 4 - rot.getStepX() * 4;
        double y = worldPosition.getY() + 4;
        double z = worldPosition.getZ() + 0.5 - dir.getStepZ() * 4 - rot.getStepZ() * 4;

        SoyuzEntity soyuz = com.hbm_m.entity.ModEntities.SOYUZ.get().create(level);
        if (soyuz == null) {
            return;
        }
        soyuz.initLaunch(x, y, z, cargoMode ? 1 : 0);
        soyuz.setSkin(loadedType);
        level.addFreshEntity(soyuz);

        tanks[0].drainMb(100_000);
        tanks[1].drainMb(100_000);

        if (cargoMode) {
            java.util.List<ItemStack> payload = new java.util.ArrayList<>();
            for (int i = CARGO_START; i <= CARGO_END; i++) {
                payload.add(inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            soyuz.setPayload(payload);

            if (rorTargetX != null && rorTargetZ != null) {
                soyuz.setTarget(rorTargetX, rorTargetZ);
            } else if (confirmTarget() && targetX != null && targetZ != null) {
                soyuz.setTarget(targetX, targetZ);
            }
            this.rorTargetX = null;
            this.rorTargetZ = null;
            this.targetX = null;
            this.targetZ = null;
        } else {
            ItemStack sat = inventory.getStackInSlot(SLOT_SATELLITE);
            java.util.List<ItemStack> satPayload = new java.util.ArrayList<>();
            satPayload.add(sat);
            soyuz.setPayload(satPayload);

            if (orbital() == 2) {
                inventory.setStackInSlot(SLOT_LANDER, ItemStack.EMPTY);
            }
            inventory.setStackInSlot(SLOT_SATELLITE, ItemStack.EMPTY);
        }

        inventory.setStackInSlot(SLOT_ROCKET, ItemStack.EMPTY);
        setChanged();
    }

    /** Returns 0 if no orbital module is required, 1 if it is and it's missing and 2 if the orbital module is required and loaded */
    public int orbital() {
        if (this.cargoMode) return 0;

        ItemStack sat = inventory.getStackInSlot(SLOT_SATELLITE);
        if (needsOrbiter(sat)) {
            ItemStack lander = inventory.getStackInSlot(SLOT_LANDER);
            if (!lander.isEmpty() && lander.is(ModItems.MISSILE_SOYUZ_LANDER.get())) return 2;
            return 1;
        }
        return 0;
    }

    public static boolean needsOrbiter(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.SAT_GERALD.get()) || stack.is(ModItems.SAT_LUNAR_MINER.get()));
    }

    // ─── Slots (Original isItemValidForSlot/getAccessibleSlotsFromSide) ─────────

    @Override
    public boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        if (slot == SLOT_ROCKET) return stack.getItem() instanceof com.hbm_m.item.special.ItemSoyuz;
        if (slot == SLOT_DESIGNATOR) return stack.getItem() instanceof IDesignatorItem;
        if (slot == SLOT_SATELLITE) return stack.getItem() instanceof com.hbm_m.item.ISatChip && !cargoMode;
        if (slot == SLOT_LANDER) return stack.is(ModItems.MISSILE_SOYUZ_LANDER.get()) && !cargoMode;
        if (slot == SLOT_FUEL_IN) return com.hbm_m.api.fluids.FluidItemAccess.isFluidContainer(stack, tanks[0].getTankType());
        if (slot == SLOT_FUEL_OUT) return false;
        if (slot == SLOT_OXIDIZER_IN) return com.hbm_m.api.fluids.FluidItemAccess.isFluidContainer(stack, tanks[1].getTankType());
        if (slot == SLOT_OXIDIZER_OUT) return false;
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack) || stack.getItem() instanceof ItemCreativeBattery;
        if (slot > SLOT_BATTERY) {
            if (!cargoMode) return false;
            // only allow items not compatible with slots 0-3
            for (int i = SLOT_ROCKET; i <= SLOT_LANDER; i++) {
                if (isItemValidForSlot(i, stack)) return false;
            }
            return true;
        }
        return true;
    }

    /** Original {@code getAccessibleSlotsFromSide}: {0, 2, 3, 9..26}. */
    private static final int[] ACCESSIBLE_SLOTS;

    static {
        ACCESSIBLE_SLOTS = new int[] { 0, 2, 3, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };
    }

    @Override
    protected ModItemStackHandler getAutomationItemHandler() {
        return new ModItemStackHandler(ACCESSIBLE_SLOTS.length) {
            private int teSlot(int slot) {
                return (slot >= 0 && slot < ACCESSIBLE_SLOTS.length) ? ACCESSIBLE_SLOTS[slot] : 0;
            }

            @Override public int getSlots() { return ACCESSIBLE_SLOTS.length; }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(teSlot(slot));
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                inventory.setStackInSlot(teSlot(slot), stack);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return inventory.isItemValid(teSlot(slot), stack);
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return inventory.insertItem(teSlot(slot), stack, simulate);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return inventory.extractItem(teSlot(slot), amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return inventory.getSlotLimit(teSlot(slot));
            }
        };
    }

    // ─── Fluids/Energie API ────────────────────────────────────────────────────

    public FluidTank[] getTanks() {
        return tanks;
    }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    // ─── Menu/NBT/Sync ─────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        // Original "container.launchpadSoyuz"
        return Component.translatable("container.hbm_m.launchpad_soyuz");
    }

    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new LaunchpadSoyuzMenu(containerId, inv, this, data);
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Original: INFINITE_EXTENT_AABB - das Modell ist ~60 Block breit und ~50 hoch
        BlockPos p = this.getBlockPos();
        return new AABB(p.getX() - 62, p.getY() - 8, p.getZ() - 62,
                p.getX() + 62, p.getY() + 56, p.getZ() + 62);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, @Nullable Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < POSITION_COUNT; i++) {
            tag.putFloat("p" + i, positions[i]);
            tag.putFloat("s" + i, speed[i]);
            tag.putFloat("t" + i, target[i]);
        }
        tag.putInt("soyuzStatus", soyuzStatus.ordinal());
        tag.putInt("strutStatus", strutStatus.ordinal());
        tag.putInt("carriageStatus", carriageStatus.ordinal());
        tag.putInt("rotorStatus", rotorStatus.ordinal());
        tag.putBoolean("cargoMode", cargoMode);
        tag.putInt("fuelCountdown", fuelCountdown);
        tag.putInt("countdown", countdown);
        tag.putInt("loadedType", loadedType);
        tanks[0].writeToNBT(tag, "tank0");
        tanks[1].writeToNBT(tag, "tank1");
    }

    @Override
    protected void readNbtData(CompoundTag tag, @Nullable Provider registries) {
        readMachineState(tag, registries, false);
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag, @Nullable Provider registries) {
        readMachineState(tag, registries, true);
    }

    /**
     * Gemeinsamer Reader: Weltnutzlast (toSync=false, Original readFromNBT) laedt Positionen direkt;
     * Sync-Paket (toSync=true, Original deserialize) fuellt {@code syncPositions} + {@code turnProgress}.
     */
    private void readMachineState(CompoundTag tag, @Nullable Provider registries, boolean toSync) {
        super.readNbtData(tag, registries);
        if (toSync) {
            for (int i = 0; i < POSITION_COUNT; i++) {
                float newSync = tag.getFloat("p" + i);
                if (this.syncPositions[i] != newSync) {
                    this.syncPositions[i] = newSync;
                    this.turnProgress = 3;
                }
            }
        } else {
            for (int i = 0; i < POSITION_COUNT; i++) {
                positions[i] = tag.getFloat("p" + i);
                speed[i] = tag.getFloat("s" + i);
                target[i] = tag.getFloat("t" + i);
                syncPositions[i] = positions[i];
            }
            this.turnProgress = 0;
        }
        soyuzStatus = grabEnumSafely(SoyuzStatus.values(), tag.getInt("soyuzStatus"), SoyuzStatus.ABSENT);
        strutStatus = grabEnumSafely(ComponentStatus.values(), tag.getInt("strutStatus"), ComponentStatus.RETRACT);
        carriageStatus = grabEnumSafely(ComponentStatus.values(), tag.getInt("carriageStatus"), ComponentStatus.RETRACT);
        rotorStatus = grabEnumSafely(ComponentStatus.values(), tag.getInt("rotorStatus"), ComponentStatus.RETRACT);
        cargoMode = tag.getBoolean("cargoMode");
        fuelCountdown = tag.getInt("fuelCountdown");
        countdown = tag.getInt("countdown");
        loadedType = tag.getInt("loadedType");
        tanks[0].readFromNBT(tag, "tank0");
        tanks[1].readFromNBT(tag, "tank1");
    }

    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        var ports = getPorts();
        BlockPos[] out = new BlockPos[ports.size()];
        for (int i = 0; i < ports.size(); i++) {
            out[i] = (BlockPos) ports.get(i)[0];
        }
        return out;
    }

    @Nullable
    private Integer rorTargetX;
    @Nullable
    private Integer rorTargetZ;

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "countdown",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "oxidizer",
                PREFIX_VALUE + "canlaunch",
                PREFIX_VALUE + "cargomode",
                PREFIX_FUNCTION + "launch",
                PREFIX_FUNCTION + "launch" + NAME_SEPARATOR + "x" + PARAM_SEPARATOR + "z",
                PREFIX_FUNCTION + "setcargomode" + NAME_SEPARATOR + "on/off"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "state").equals(name)) return soyuzStatus.name();
        if ((PREFIX_VALUE + "countdown").equals(name)) return "" + countdown;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + getEnergyStored();
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "oxidizer").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "canlaunch").equals(name)) return canLaunch() ? "1" : "0";
        if ((PREFIX_VALUE + "cargomode").equals(name)) return cargoMode ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "launch").equals(name)) {
            if (params.length == 2 && cargoMode && soyuzStatus == SoyuzStatus.READY) {
                this.rorTargetX = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0]);
                this.rorTargetZ = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[1]);
                this.soyuzStatus = SoyuzStatus.LAUNCHING;
                this.countdown = COUNTDOWN_DURATION;
                this.setChanged();
                return null;
            }
            if (canLaunch() && soyuzStatus == SoyuzStatus.READY) {
                this.soyuzStatus = SoyuzStatus.LAUNCHING;
                this.countdown = COUNTDOWN_DURATION;
                this.setChanged();
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setcargomode").equals(name) && params.length == 1) {
            this.cargoMode = "1".equals(params[0]) || Boolean.parseBoolean(params[0]) || "on".equalsIgnoreCase(params[0]);
            this.setChanged();
            return null;
        }
        return null;
    }

    /** Original {@code EnumUtil.grabEnumSafely} - kleinste Glue, da die Util-Klasse nicht mitportiert wurde. */
    private static <E extends Enum<E>> E grabEnumSafely(E[] values, int ordinal, E fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
}
