package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.SoyuzEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.SoyuzLauncherMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
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
 * Port of legacy {@code TileEntitySoyuzLauncher}: two fuel tanks (kerosene/oxygen),
 * energy storage, 4 dedicated item slots (rocket/designator/satellite/lander) + 18
 * cargo slots, a Satellite/Cargo mode switch, and a launch countdown that spawns a
 * {@link SoyuzEntity}.
 * <p>
 * Satellite Mode intentionally does not hook into a real orbital/frequency
 * simulation - that legacy subsystem ({@code com.hbm.saveddata.satellites.Satellite})
 * doesn't exist anywhere in this port. Launching just consumes the satellite chip.
 */
public class SoyuzLauncherBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

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

    public static final int MODE_SATELLITE = 0;
    public static final int MODE_CARGO = 1;

    private static final long MAX_POWER = 1_000_000L;
    private static final long MAX_RECEIVE = 10_000L;
    private static final int TANK_CAPACITY_MB = 128_000;
    public static final int MAX_COUNTDOWN = 600;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.KEROSENE.getSource(), TANK_CAPACITY_MB),
            new FluidTank(ModFluids.OXYGEN.getSource(), TANK_CAPACITY_MB)
    };

    private int mode = MODE_SATELLITE;
    private boolean starting = false;
    private int countdown = MAX_COUNTDOWN;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (energy & 0xFFFFFFFFL);
                case 1 -> (int) (energy >>> 32);
                case 2 -> mode;
                case 3 -> countdown;
                case 4 -> starting ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-side read-only; server owns the state.
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public SoyuzLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_LAUNCHER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SoyuzLauncherBlockEntity be) {
        if (level.isClientSide) {
            return;
        }

        // Original: alle 20 Ticks Strom und beide Treibstoffe an den Feldern rund um den Tisch abonnieren
        if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) {
            for (Object[] con : be.getConPos()) {
                BlockPos at = (BlockPos) con[0];
                net.minecraft.core.Direction dir = (net.minecraft.core.Direction) con[1];
                be.trySubscribe(server, at.getX(), at.getY(), at.getZ(), dir);
                be.trySubscribe(be.tanks[0].getTankType(), server, at, dir);
                be.trySubscribe(be.tanks[1].getTankType(), server, at, dir);
            }
        }

        boolean fuelChanged = be.tanks[0].loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, be.slotArray());
        boolean oxyChanged = be.tanks[1].loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, be.slotArray());
        be.writeBackSlots();

        be.chargeBattery();

        if (!be.starting || !be.canLaunch()) {
            be.countdown = MAX_COUNTDOWN;
            be.starting = false;
        } else if (be.countdown > 0) {
            be.countdown--;
            if (be.countdown % 100 == 0 && be.countdown > 0) {
                level.playSound(null, pos, com.hbm_m.sound.HbmSoundsNT.get("hbm:alarm.hatch"), SoundSource.BLOCKS, 100F, 1.1F);
            }
        } else {
            be.liftOff();
        }

        if (fuelChanged || oxyChanged) {
            be.setChanged();
        }
        be.sendUpdateToClient();
    }

    /** Original updateEntity (Client): Bereitschafts-Schleifenton waehrend des Countdowns, Rauch beim Abheben. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SoyuzLauncherBlockEntity be) {
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(be, be.starting && be.countdown > 0, be::createAudioLoop);

        java.util.List<SoyuzEntity> entities = level.getEntitiesOfClass(SoyuzEntity.class,
                new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 10, pos.getZ() + 1.5));

        if (!entities.isEmpty()) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "smoke");
            data.putString("mode", "shockRand");
            data.putInt("count", 50);
            data.putDouble("strength", level.random.nextGaussian() * 3 + 6);
            data.putDouble("posX", pos.getX() + 0.5);
            data.putDouble("posY", pos.getY() - 3);
            data.putDouble("posZ", pos.getZ() + 0.5);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.SoyuzReadyLoopSoundFactory").getMethod("create", SoyuzLauncherBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private ItemStack[] slotArray() {
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }
        return slots;
    }

    private void writeBackSlots() {
        // loadTank() mutates the array in place for the in/out slot pair; push both back.
        ItemStack[] slots = slotArray();
        inventory.setStackInSlot(SLOT_FUEL_IN, slots[SLOT_FUEL_IN]);
        inventory.setStackInSlot(SLOT_FUEL_OUT, slots[SLOT_FUEL_OUT]);
        inventory.setStackInSlot(SLOT_OXIDIZER_IN, slots[SLOT_OXIDIZER_IN]);
        inventory.setStackInSlot(SLOT_OXIDIZER_OUT, slots[SLOT_OXIDIZER_OUT]);
    }

    private void chargeBattery() {
        ItemStack battery = inventory.getStackInSlot(SLOT_BATTERY);
        if (!battery.isEmpty() && battery.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    // ─── Legacy TileEntitySoyuzLauncher API ────────────────────────────────────

    public void startCountdown() {
        if (canLaunch()) {
            starting = true;
        }
    }

    public void setMode(int mode) {
        this.mode = mode == MODE_CARGO ? MODE_CARGO : MODE_SATELLITE;
        setChanged();
        sendUpdateToClient();
    }

    public int getMode() {
        return mode;
    }

    public boolean isStarting() {
        return starting;
    }

    public int getCountdown() {
        return countdown;
    }

    private void liftOff() {
        starting = false;

        if (level == null || level.isClientSide) {
            return;
        }

        int fuelReq = getFuelRequired();
        long powerReq = getPowerRequired();

        SoyuzEntity soyuz = ModEntities.SOYUZ.get().create(level);
        if (soyuz == null) {
            return;
        }
        soyuz.initLaunch(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, mode);
        soyuz.setSkin(getRocketType());
        level.addFreshEntity(soyuz);
        level.playSound(null, worldPosition, com.hbm_m.sound.HbmSoundsNT.get("hbm:entity.soyuzTakeoff"), SoundSource.BLOCKS, 100F, 1.1F);

        tanks[0].drainMb(fuelReq);
        tanks[1].drainMb(fuelReq);
        setEnergyStored(getEnergyStored() - powerReq);

        if (mode == MODE_SATELLITE) {
            java.util.List<ItemStack> satPayload = new java.util.ArrayList<>();
            satPayload.add(inventory.getStackInSlot(SLOT_SATELLITE));
            soyuz.setPayload(satPayload);

            if (orbital() == 2) {
                inventory.setStackInSlot(SLOT_LANDER, ItemStack.EMPTY);
            }
            inventory.setStackInSlot(SLOT_SATELLITE, ItemStack.EMPTY);
        } else {
            java.util.List<ItemStack> cargo = new java.util.ArrayList<>();
            for (int i = CARGO_START; i <= CARGO_END; i++) {
                cargo.add(inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            soyuz.setPayload(cargo);

            ItemStack designatorStack = inventory.getStackInSlot(SLOT_DESIGNATOR);
            int targetX = worldPosition.getX();
            int targetZ = worldPosition.getZ();
            if (designatorStack.getItem() instanceof IDesignatorItem designator
                    && designator.isReady(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) {
                Vec3 coords = designator.getCoords(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
                targetX = (int) Math.floor(coords.x);
                targetZ = (int) Math.floor(coords.z);
            }
            soyuz.setTarget(targetX, targetZ);
        }

        inventory.setStackInSlot(SLOT_ROCKET, ItemStack.EMPTY);
        setChanged();
    }

    public boolean canLaunch() {
        return hasRocket() && hasFuel() && hasPower() && designator() != 1 && orbital() != 1 && satellite() != 1;
    }

    public boolean hasFuel() {
        return tanks[0].getFill() >= getFuelRequired();
    }

    public boolean hasOxy() {
        return tanks[1].getFill() >= getFuelRequired();
    }

    public int getFuelRequired() {
        if (mode == MODE_CARGO) {
            return Math.min(5000 + getDist(), TANK_CAPACITY_MB);
        }
        return TANK_CAPACITY_MB;
    }

    private int getDist() {
        if (designator() != 2 || level == null) {
            return 0;
        }
        ItemStack designatorStack = inventory.getStackInSlot(SLOT_DESIGNATOR);
        if (!(designatorStack.getItem() instanceof IDesignatorItem designator)) {
            return 0;
        }
        Vec3 coords = designator.getCoords(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        double dx = worldPosition.getX() - coords.x;
        double dz = worldPosition.getZ() - coords.z;
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    public boolean hasPower() {
        return getEnergyStored() >= getPowerRequired();
    }

    public long getPowerRequired() {
        return (long) (MAX_POWER * 0.75);
    }

    public long getPowerScaled(long scale) {
        return (getEnergyStored() * scale) / MAX_POWER;
    }

    /** Original {@code hasRocket}: eine missile_soyuz (jeder Skin) im Raketenplatz. */
    public boolean hasRocket() {
        return inventory.getStackInSlot(SLOT_ROCKET).getItem() instanceof com.hbm_m.item.special.ItemSoyuz;
    }

    /** Original {@code getType()}: Skin der Rakete oder -1. */
    public int getRocketType() {
        return com.hbm_m.item.special.ItemSoyuz.skinOf(inventory.getStackInSlot(SLOT_ROCKET));
    }

    /** 0 = not required (satellite mode), 1 = required but missing/not-ready, 2 = present & ready. */
    public int designator() {
        if (mode == MODE_SATELLITE) {
            return 0;
        }
        ItemStack stack = inventory.getStackInSlot(SLOT_DESIGNATOR);
        if (level != null && stack.getItem() instanceof IDesignatorItem designator
                && designator.isReady(level, stack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) {
            return 2;
        }
        return 1;
    }

    /** 0 = not required (cargo mode), 1 = required but missing, 2 = present. */
    public int satellite() {
        if (mode == MODE_CARGO) {
            return 0;
        }
        return inventory.getStackInSlot(SLOT_SATELLITE).isEmpty() ? 1 : 2;
    }

    /** 0 = no lander needed, 1 = needed but missing, 2 = present. */
    public int orbital() {
        if (mode == MODE_CARGO) {
            return 0;
        }
        ItemStack sat = inventory.getStackInSlot(SLOT_SATELLITE);
        if (!sat.isEmpty() && (sat.is(ModItems.SAT_GERALD.get()) || sat.is(ModItems.SAT_LUNAR_MINER.get()))) {
            ItemStack lander = inventory.getStackInSlot(SLOT_LANDER);
            return lander.is(ModItems.MISSILE_SOYUZ_LANDER.get()) ? 2 : 1;
        }
        return 0;
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    // ─── BaseMachineBlockEntity ─────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.soyuz_launcher");
    }

    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        if (slot == SLOT_ROCKET) return stack.getItem() instanceof com.hbm_m.item.special.ItemSoyuz;
        if (slot == SLOT_DESIGNATOR) return stack.getItem() instanceof IDesignatorItem;
        if (slot == SLOT_LANDER) return stack.is(ModItems.MISSILE_SOYUZ_LANDER.get());
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack) || stack.getItem() instanceof ItemCreativeBattery;
        return slot >= 0 && slot < SLOT_COUNT;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new SoyuzLauncherMenu(containerId, inv, this, data);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos p = this.getBlockPos();
        return new AABB(p.getX() - 8, p.getY() - 5, p.getZ() - 8,
                         p.getX() + 9, p.getY() + 66, p.getZ() + 9);
    }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("soyuz_mode", mode);
        tag.putBoolean("soyuz_starting", starting);
        tag.putInt("soyuz_countdown", countdown);
        tanks[0].writeToNBT(tag, "fuel");
        tanks[1].writeToNBT(tag, "oxidizer");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        mode = tag.getInt("soyuz_mode");
        starting = tag.getBoolean("soyuz_starting");
        countdown = tag.getInt("soyuz_countdown");
        tanks[0].readFromNBT(tag, "fuel");
        tanks[1].readFromNBT(tag, "oxidizer");
    }

    private java.util.List<Object[]> conPos;

    /** Original {@code getConPos}: je Seite 13 Felder in Abstand 7, auf Kernhoehe und eins darunter. */
    private java.util.List<Object[]> getConPos() {
        if (conPos != null) return conPos;
        conPos = new java.util.ArrayList<>();
        for (net.minecraft.core.Direction dir : new net.minecraft.core.Direction[] {
                net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.NORTH }) {
            net.minecraft.core.Direction rot = dir.getClockWise();
            for (int i = -6; i <= 6; i++) {
                BlockPos base = worldPosition.relative(dir, 7).relative(rot, i);
                conPos.add(new Object[] { base, dir });
                conPos.add(new Object[] { base.below(), dir });
            }
        }
        return conPos;
    }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
}
