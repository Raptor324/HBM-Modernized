package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.LambdaRocketEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.LaunchpadLambdaMenu;
import com.hbm_m.item.ISatChip;
import com.hbm_m.item.ModItems;

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
 * 1:1 {@code TileEntityLaunchpadLambda}: Silo-Startrampe fuer die Lambda-Rakete. Sieben Slots (Rakete, Satellit,
 * Benzin rein/raus, Peroxid rein/raus, Batterie), zwei Tanks zu je 64.000 mB (verbleites Benzin, Peroxid), 10.000 HE
 * pro Tick fuer die Mechanik. Die Rakete wird ueber eine Abfolge aus Silotueren, Aufrichter, Rotor, Klammern und
 * Kolben aufgestellt; der Start kostet je 24.000 mB und bringt den Satelliten per {@link LambdaRocketEntity} in den
 * Orbit. Fuenf Anschluesse an der Vorderkante.
 */
public class LaunchpadLambdaBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2, IControlReceiver {

    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_SATELLITE = 1;
    public static final int SLOT_FUEL_IN = 2;
    public static final int SLOT_FUEL_OUT = 3;
    public static final int SLOT_OXIDIZER_IN = 4;
    public static final int SLOT_OXIDIZER_OUT = 5;
    public static final int SLOT_BATTERY = 6;
    public static final int SLOT_COUNT = 7;

    public static final long MAX_POWER = 1_000_000;
    public static final long CONSUMPTION = 10_000;
    public static final int FUEL_PER_LAUNCH = 24_000;

    public static final int INDEX_DOORS = 0;
    public static final int INDEX_ERECTOR = 1;
    public static final int INDEX_ROTOR = 2;
    public static final int INDEX_CLAMPS = 3;
    public static final int INDEX_PISTONS = 4;

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.GASOLINE_LEADED.getSource(), 64_000),
            new FluidTank(ModFluids.PEROXIDE.getSource(), 64_000)
    };

    public float[] positions = new float[5];
    public float[] prevPositions = new float[5];
    public float[] speed = new float[5];
    public float[] target = new float[5];
    public float[] syncPositions = new float[5];

    protected int turnProgress;

    /** Rakete steht auf der Silotuer, wird nicht mehr mit dem Aufrichter gezeichnet. */
    public boolean erected = false;
    /** Aufrichter faehrt hoch und stellt die Rakete auf. */
    public boolean erecting = false;

    public int animationProgress = 0;
    public int animationDelay = 0;

    public boolean autolaunch = false;

    public static final int COUNTDOWN_DURATION = 200;
    public int countdown;

    /** Client: laufende Schleifentoene (Tueren, Aufrichter, Rotor, Sirene), siehe {@code LaunchpadLambdaSounds}. */
    public final Object[] audios = new Object[4];

    public LaunchpadLambdaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_LAMBDA_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    private Direction facing() {
        return getBlockState().hasProperty(DummyableMachineBlock.FACING) ? getBlockState().getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    /** Original {@code getPorts}: {@code dir * 7 - rot * (2..6)} auf Hoehe +1; verbunden wird mit dem Block davor. */
    private BlockPos[] getConPos() {
        Direction dir = facing();
        Direction rot = dir.getClockWise();
        BlockPos[] out = new BlockPos[5];
        for (int k = 2; k <= 6; k++) out[k - 2] = worldPosition.relative(dir, 8).relative(rot, -k).above();
        return out;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchpadLambdaBlockEntity be) {
        if (level instanceof ServerLevel server) be.serverTick(server);
        else be.clientTick(level);
    }

    private void serverTick(ServerLevel world) {

        Direction dir = facing();
        for (BlockPos con : getConPos()) {
            this.trySubscribe(world, con.getX(), con.getY(), con.getZ(), dir);
            this.trySubscribe(tanks[0].getTankType(), world, con, dir);
            this.trySubscribe(tanks[1].getTankType(), world, con, dir);
        }

        chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = inventory.getStackInSlot(i);
        tanks[0].loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, slots);
        tanks[1].loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, slots);
        for (int i = SLOT_FUEL_IN; i <= SLOT_OXIDIZER_OUT; i++) inventory.setStackInSlot(i, slots[i]);

        if (!this.hasRocketLoaded()) {
            this.erected = false;
            this.erecting = false;
            this.countdown = 0;
        }

        if (this.energy >= CONSUMPTION) {
            this.updateStates(world);
            this.move();
            this.energy -= CONSUMPTION;

            if (this.autolaunch && this.erected && this.canLaunch() && this.countdown <= 0) {
                this.countdown = COUNTDOWN_DURATION;
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level world) {

        for (int i = 0; i < this.positions.length; i++) {
            this.prevPositions[i] = this.positions[i];
            if (this.turnProgress > 0) {
                this.positions[i] = this.positions[i] + ((this.syncPositions[i] - this.positions[i]) / (float) this.turnProgress);
            } else {
                this.positions[i] = this.syncPositions[i];
            }
        }
        if (this.turnProgress > 0) --this.turnProgress;

        try {
            Class.forName("com.hbm_m.client.sound.LaunchpadLambdaClient").getMethod("tick", LaunchpadLambdaBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException ignored) { }
    }

    public void updateStates(ServerLevel world) {

        // Rakete wird aufgerichtet
        if (finishedAllMoving() && this.erecting && !this.erected) {

            if (this.erectorDown() && this.doorsClosed()) {
                this.setTarget(INDEX_DOORS, 3F, 3F, 60);
                this.setTarget(INDEX_PISTONS, 0F, 1F, 1);
                this.setTarget(INDEX_CLAMPS, 0F, 1F, 1);
                this.setTarget(INDEX_ROTOR, 180F, 180F, 1);
            } else if (this.erectorDown() && this.doorsOpen()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ERECTOR, 27F, 27F, 100);
            } else if (this.erectorUp() && this.rotorDown()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ROTOR, 0F, 180F, 60);
                this.setTarget(INDEX_DOORS, 0F, 3F, 60);
            } else if (this.erectorUp() && this.rotorUp() && this.doorsClosed()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ERECTOR, 25F, 2F, 60);
            } else if (this.erectorCenter() && this.rotorUp() && this.doorsClosed()) {
                this.erected = true;
            } else {
                // vom Ablauf abgekommen: alles zurueckfahren
                this.erecting = false;
            }
        }

        // Aufrichter bei T-1 zurueckfahren
        if (this.erected && this.countdown <= 20 && this.countdown > 0) {
            this.erecting = false;
        }

        // Aufrichter nach dem Start zurueckfahren
        if (finishedAllMoving() && !this.erecting) {

            this.setTarget(INDEX_DOORS, 0F, 3F, 60);

            if (this.clampsOn()) {
                this.setTarget(INDEX_PISTONS, 0.75F, 0.75F, 20);
            } else if (this.clampsOff() && this.clampsDown()) {
                this.animationDelay = 10;
                this.setTarget(INDEX_CLAMPS, 90F, 90F, 40);
            } else if (this.clampsUp() && !this.erectorDown()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ERECTOR, 0F, 25F, 100);
            } else if (this.erectorDown() && this.doorsClosed()) {
                if (this.hasRocketLoaded()) this.erecting = true;
            }
        }

        if (this.erected && this.countdown > 0) {
            this.countdown--;

            if (this.countdown <= 0) {
                world.playSound(null, worldPosition, com.hbm_m.sound.HbmSoundsNT.get("hbm:entity.soyuzTakeoff"), SoundSource.BLOCKS, 100F, 1.1F);
                this.liftOff(world);
            }
        }
    }

    public void liftOff(ServerLevel world) {
        LambdaRocketEntity rocket = ModEntities.ROCKET_LAMBDA.get().create(world);
        if (rocket == null) return;
        rocket.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5, 0, 0);
        rocket.setSat(inventory.getStackInSlot(SLOT_SATELLITE));
        world.addFreshEntity(rocket);

        tanks[0].setFill(tanks[0].getFill() - FUEL_PER_LAUNCH);
        tanks[1].setFill(tanks[1].getFill() - FUEL_PER_LAUNCH);

        inventory.setStackInSlot(SLOT_ROCKET, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_SATELLITE, ItemStack.EMPTY);

        this.erected = false;
    }

    public boolean doorsClosed() { return this.positions[INDEX_DOORS] == 0F; }
    public boolean doorsOpen() { return this.positions[INDEX_DOORS] == 3F; }
    public boolean erectorDown() { return this.positions[INDEX_ERECTOR] == 0F; }
    public boolean erectorCenter() { return this.positions[INDEX_ERECTOR] == 25F; }
    public boolean erectorUp() { return this.positions[INDEX_ERECTOR] == 27F; }
    public boolean rotorUp() { return this.positions[INDEX_ROTOR] == 0F; }
    public boolean rotorDown() { return this.positions[INDEX_ROTOR] == 180F; }
    public boolean clampsOn() { return this.positions[INDEX_PISTONS] == 0F; }
    public boolean clampsOff() { return this.positions[INDEX_PISTONS] == 0.75F; }
    public boolean clampsUp() { return this.positions[INDEX_CLAMPS] == 90F; }
    public boolean clampsDown() { return this.positions[INDEX_CLAMPS] == 0F; }

    public void setTarget(int index, float target, float span, int duration) {
        if (span <= 0) span = 1F;
        this.target[index] = target;
        this.speed[index] = span / duration;
    }

    public void move() {

        if (this.animationDelay > 0) this.animationDelay--;

        for (int i = 0; i < this.positions.length; i++) {

            this.prevPositions[i] = this.positions[i];
            if (this.animationDelay > 0) continue;

            if (Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
                this.positions[i] = this.target[i];
            } else if (this.positions[i] < this.target[i]) {
                this.positions[i] += this.speed[i];
            } else {
                this.positions[i] -= this.speed[i];
            }
        }
    }

    /** Original {@code TileEntityLaunchpadSoyuz.needsOrbiter}: Gerald und der Mondbergbau brauchen die Soyuz. */
    public static boolean needsOrbiter(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.SAT_GERALD.get()) || stack.is(ModItems.SAT_LUNAR_MINER.get()));
    }

    public boolean canLaunch() {
        if (!this.hasRocketLoaded()) return false;
        if (!this.hasAllFuel()) return false;
        if (this.energy < CONSUMPTION) return false;
        ItemStack sat = inventory.getStackInSlot(SLOT_SATELLITE);
        if (sat.isEmpty()) return false;
        if (!(sat.getItem() instanceof ISatChip)) return false;
        return !needsOrbiter(sat);
    }

    public boolean hasRocketLoaded() { return inventory.getStackInSlot(SLOT_ROCKET).is(ModItems.MISSILE_LAMBDA.get()); }
    public boolean finishedMoving(int index) { return this.positions[index] == this.target[index]; }
    public boolean wasMoving(int index) { return this.positions[index] != this.prevPositions[index]; }

    public boolean finishedAllMoving() {
        for (int i = 0; i < this.positions.length; i++) if (!finishedMoving(i)) return false;
        return true;
    }

    public float getInterpPos(int index, float interp) {
        return prevPositions[index] + (positions[index] - prevPositions[index]) * interp;
    }

    public boolean hasAllFuel() { return hasJetFuel() && hasOxidizer(); }
    public boolean hasJetFuel() { return this.tanks[0].getFill() >= FUEL_PER_LAUNCH; }
    public boolean hasOxidizer() { return this.tanks[1].getFill() >= FUEL_PER_LAUNCH; }

    // ==================== Steuerung ====================

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 24 * 24;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("auto")) {
            this.autolaunch = data.getBoolean("auto");
            setChanged();
        }
        if (data.contains("launch") && canLaunch()) {
            this.countdown = COUNTDOWN_DURATION;
        }
    }

    // ==================== Slots / Fluide ====================

    @Override
    protected boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        if (slot == SLOT_ROCKET) return stack.is(ModItems.MISSILE_LAMBDA.get());
        if (slot == SLOT_SATELLITE) return stack.getItem() instanceof ISatChip && !needsOrbiter(stack);
        if (slot == SLOT_FUEL_OUT || slot == SLOT_OXIDIZER_OUT) return false;
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        return true;
    }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("power", energy);
        tanks[0].writeToNBT(tag, "tank0");
        tanks[1].writeToNBT(tag, "tank1");
        for (int i = 0; i < 5; i++) {
            tag.putFloat("p" + i, positions[i]);
            tag.putFloat("s" + i, speed[i]);
            tag.putFloat("t" + i, target[i]);
        }
        tag.putBoolean("erected", erected);
        tag.putBoolean("erecting", erecting);
        tag.putInt("animationProgress", animationProgress);
        tag.putInt("animationDelay", animationDelay);
        tag.putBoolean("autolaunch", autolaunch);
        tag.putInt("countdown", countdown);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (!tag.contains("power")) return;
        energy = tag.getLong("power");
        tanks[0].readFromNBT(tag, "tank0");
        tanks[1].readFromNBT(tag, "tank1");
        for (int i = 0; i < 5; i++) {
            positions[i] = tag.getFloat("p" + i);
            speed[i] = tag.getFloat("s" + i);
            target[i] = tag.getFloat("t" + i);
        }
        erected = tag.getBoolean("erected");
        erecting = tag.getBoolean("erecting");
        animationProgress = tag.getInt("animationProgress");
        animationDelay = tag.getInt("animationDelay");
        autolaunch = tag.getBoolean("autolaunch");
        countdown = tag.getInt("countdown");
    }

    /** Original {@code deserialize}: neue Positionen werden ueber drei Ticks angefahren statt gesprungen. */
    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        float[] current = positions.clone();
        super.applyClientUpdate(tag);
        for (int i = 0; i < positions.length; i++) {
            float newSync = positions[i];
            positions[i] = current[i];
            if (syncPositions[i] != newSync) {
                syncPositions[i] = newSync;
                turnProgress = 3;
            }
        }
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.launchpad_lambda");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LaunchpadLambdaMenu(containerId, playerInventory, this);
    }

    /** Original: 15x30x15 um den Kern. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 7, worldPosition.getY(), worldPosition.getZ() - 7,
                worldPosition.getX() + 8, worldPosition.getY() + 30, worldPosition.getZ() + 8);
    }

    //? if forge {
    /** Original {@code getAccessibleSlotsFromSide}: nur Rakete und Satellit. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(Direction side) { return new int[] { SLOT_ROCKET, SLOT_SATELLITE }; }
                @Override public boolean canInsert(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, ItemStack stack, Direction side) { return false; }
            });

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
