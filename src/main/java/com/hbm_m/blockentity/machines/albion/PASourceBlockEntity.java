package com.hbm_m.blockentity.machines.albion;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.PASourceMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPASource} (1.7.10): die Teilchenquelle und zugleich der Kopf des
 * ganzen Beschleunigers.
 *
 * <p>Sie erzeugt aus zwei Ausgangsstoffen ein {@link Particle} und laesst es Schritt fuer Schritt
 * durch den Ring wandern. Je schneller es ist, desto mehr Schritte macht es pro Tick - bis zu zehn
 * bei 9.000 Impuls und mehr. Bei jedem Schritt sucht sie das Bauteil an der Teilchenposition, fragt
 * es, ob der Strahl dort hinein darf, laesst es wirken und holt sich die naechste Position.</p>
 *
 * <p>Faellt der Strahl aus der Strecke, trifft er ein Bauteil von der falschen Seite oder fehlt es
 * an Kuehlung, Energie oder Spule, endet der Lauf mit dem passenden {@link PAState} - den zeigt die
 * Quelle bis zum naechsten Start an.</p>
 */
public class PASourceBlockEntity extends CooledMachineBlockEntity implements PAParticleHost {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_INPUT_A = 1;
    public static final int SLOT_INPUT_B = 2;
    public static final int INVENTORY_SIZE = 3;

    /** Original: {@code usage = 100_000} je Start. */
    private static final long USAGE = 100_000L;
    private static final long MAX_POWER = 1_000_000L;
    /**
     * Kettenbetrieb: das Teilchen startet auf dem Bauteil direkt neben der Quelle. Im Original
     * sind es fuenf Bloecke, weil die Quelle dort ein Multiblock ist - siehe die Bauteilklassen.
     */
    private static final int SPAWN_OFFSET = 1;
    /** Original: {@code 1 + clamp(momentum / 1000, 0, 9)}. */
    private static final int MOMENTUM_PER_STEP = 1_000;
    private static final int MAX_STEPS = 10;

    @Nullable
    private Particle particle;
    private PAState state = PAState.IDLE;
    /** Original: {@code lastSpeed} - zuletzt gemessener Impuls, nur zur Anzeige. */
    private int lastSpeed;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> state.ordinal();
                case 1 -> lastSpeed;
                case 2 -> particle != null ? particle.defocus : 0;
                case 3 -> (int) getTemperature();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PASourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_SOURCE_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PASourceBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.chargeFromBatterySlot(SLOT_BATTERY);
        be.tickCooling(level, pos);

        // Original: schnellere Teilchen machen mehr Schritte je Tick.
        int steps = 1;
        if (be.particle != null) {
            steps = 1 + Math.max(0, Math.min(be.particle.momentum / MOMENTUM_PER_STEP, MAX_STEPS - 1));
        }

        for (int i = 0; i < steps; i++) {
            if (be.particle != null) {
                be.state = PAState.RUNNING;
                be.step(level);
                if (be.particle != null && be.particle.invalid) be.particle = null;
            } else if (be.getEnergyStored() >= USAGE
                    && !be.getInventory().getStackInSlot(SLOT_INPUT_A).isEmpty()
                    && !be.getInventory().getStackInSlot(SLOT_INPUT_B).isEmpty()) {
                be.tryRun(level, pos);
                break;
            }
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /**
     * 1:1-Port von {@code steppy()}: ein Schritt des Teilchens durch die Strecke.
     */
    private void step(Level level) {
        if (particle == null) return;

        BlockPos at = particle.pos();

        // Original: in ungeladenen Bereichen wird angehalten, nicht abgestuerzt.
        if (!level.isLoaded(at)) {
            state = PAState.PAUSE_UNLOADED;
            return;
        }

        IParticleUser user = resolveUser(level, at);

        if (user == null) {
            particle.crash(PAState.CRASH_DERAIL);
            return;
        }

        if (!user.canParticleEnter(particle, particle.dir, particle.x, particle.y, particle.z)) {
            particle.crash(PAState.CRASH_CANNOT_ENTER);
            return;
        }

        user.onEnter(particle, particle.dir);
        if (particle.invalid) return;

        BlockPos exit = user.getExitPos(particle);
        if (exit != null) particle.move(exit);
    }

    /**
     * 1:1-Port der {@code findCore}-Zeile aus {@code steppy()}.
     *
     * <p>Das Teilchen steht nie auf dem Kern eines Bauteils, sondern immer auf einer seiner
     * <b>Dummyzellen</b> - beim Strahlrohr ein Feld davor, bei der Hochfrequenzkammer vier. Hier
     * wird von dieser Zelle aus der Kern gesucht, der die Logik traegt.</p>
     *
     * @return das Bauteil, oder {@code null} wenn dort keines steht - dann entgleist das Teilchen
     */
    @Nullable
    private static IParticleUser resolveUser(Level level, BlockPos at) {
        BlockEntity be = level.getBlockEntity(at);

        // Der Kern selbst - kommt vor, wenn zwei Bauteile unmittelbar aneinanderstossen.
        if (be instanceof IParticleUser user) return user;

        // Eine Dummyzelle: sie zeigt auf ihren Kern.
        if (be instanceof com.hbm_m.interfaces.IMultiblockPart part) {
            BlockPos core = part.getControllerPos();
            if (core == null) return null;

            BlockEntity coreBe = level.getBlockEntity(core);
            if (coreBe instanceof IParticleUser user) return user;
        }

        return null;
    }

    /** 1:1-Port von {@code tryRun()}: die Quelle schiesst ein neues Teilchen los. */
    private void tryRun(Level level, BlockPos pos) {
        if (!isCool()) return;

        setEnergyStored(getEnergyStored() - USAGE);

        Direction axis = PAOrientation.beamAxis(getBlockState());
        BlockPos spawn = pos.relative(axis, SPAWN_OFFSET);

        ItemStack a = getInventory().getStackInSlot(SLOT_INPUT_A).copy();
        ItemStack b = getInventory().getStackInSlot(SLOT_INPUT_B).copy();
        a.setCount(1);
        b.setCount(1);

        particle = new Particle(this, spawn.getX(), spawn.getY(), spawn.getZ(), axis, a, b);

        getInventory().getStackInSlot(SLOT_INPUT_A).shrink(1);
        getInventory().getStackInSlot(SLOT_INPUT_B).shrink(1);
        setChanged();
    }

    /** Bricht den laufenden Strahl ab - im Original der {@code cancel}-Funktionsaufruf. */
    public void cancel() {
        particle = null;
        state = PAState.IDLE;
        setChanged();
    }

    // ── PAParticleHost ──────────────────────────────────────────────────────

    @Override
    public void updateState(PAState state) {
        this.state = state;
    }

    @Override
    public void setLastSpeed(int momentum) {
        this.lastSpeed = momentum;
    }

    // ── Anzeige ─────────────────────────────────────────────────────────────

    public PAState getState()   { return state; }
    public int getLastSpeed()   { return lastSpeed; }
    public int getDefocus()     { return particle != null ? particle.defocus : 0; }
    public boolean isRunning()  { return particle != null; }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack);
        return slot == SLOT_INPUT_A || slot == SLOT_INPUT_B;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("state", state.ordinal());
        tag.putInt("lastSpeed", lastSpeed);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        PAState[] values = PAState.values();
        int ordinal = tag.getInt("state");
        state = ordinal >= 0 && ordinal < values.length ? values[ordinal] : PAState.IDLE;
        lastSpeed = tag.getInt("lastSpeed");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pa_source");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PASourceMenu(id, inv, this);
    }
}
