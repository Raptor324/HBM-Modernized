package com.hbm_m.blockentity.machines.albion;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.machine.ItemPACoil;
import com.hbm_m.item.machine.ItemPACoil.CoilType;

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

/**
 * 1:1-Port von {@code TileEntityPADipole} (1.7.10): der Ablenkmagnet - die Weiche des Rings.
 *
 * <p>Er schickt das Teilchen in eine von drei einstellbaren Richtungen: unterhalb von
 * {@link #threshold} nach {@link #dirLower}, sonst nach {@link #dirUpper} - oder nach
 * {@link #dirRedstone}, wenn Redstone anliegt. Damit baut man Weichen, die den Strahl je nach
 * Impuls weiter im Ring kreisen oder zum Detektor abbiegen lassen.</p>
 *
 * <p>Faehrt das Teilchen geradeaus durch ({@code isInline}), kostet das nur den Grundverbrauch und
 * die Streckenzaehlung laeuft weiter. Wird abgelenkt, faengt die Strecke bei null an - und wer zu
 * frueh ablenkt ({@code diDistMin}) oder zu langsam ist ({@code diMin}), zahlt das Zehnfache.</p>
 * <p><b>Es ist ein Multiblock.</b> Wie im Original steht das Teilchen nie auf dem Kern, sondern
 * auf einer seiner Dummyzellen; der Kern wird von dort aus gesucht. Daraus ergeben sich die
 * Spruenge von zwei bis fuenf Feldern - und damit die tatsaechliche Groesse eines Rings.</p>
 */
public class PADipoleBlockEntity extends CooledMachineBlockEntity implements IParticleUser {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COIL = 1;
    public static final int INVENTORY_SIZE = 2;

    /** Original: {@code usage = 100_000}. */
    private static final long USAGE = 100_000L;
    private static final long MAX_POWER = 1_000_000L;
    /** Original: geradeaus zaehlen drei Bloecke Strecke. */
    private static final int DISTANCE_INLINE = 3;
    /** Kettenbetrieb: einen Block weiter (siehe Klassenkommentar). */
    /** Original: {@code offset(particle.dir, 2)} - hinter die Dummyzelle in Ausgangsrichtung. */
    private static final int EXIT_OFFSET = 2;

    /** Richtung unterhalb der Schwelle. Original: {@code dirLower}. */
    private Direction dirLower = Direction.NORTH;
    /** Richtung oberhalb der Schwelle. Original: {@code dirUpper}. */
    private Direction dirUpper = Direction.NORTH;
    /** Richtung bei anliegendem Redstone. Original: {@code dirRedstone}. */
    private Direction dirRedstone = Direction.NORTH;
    /** Impulsschwelle zwischen unterer und oberer Richtung. */
    private int threshold = 0;

    public PADipoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_DIPOLE_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PADipoleBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.chargeFromBatterySlot(SLOT_BATTERY);
        be.tickCooling(level, pos);

        be.setChanged();
        be.sendUpdateToClient();
    }

    /**
     * Original: der Dipol nimmt das Teilchen aus allen vier Himmelsrichtungen an - er ist die
     * Weiche, nicht das Rohr.
     */
    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, int x, int y, int z) {
        // 1:1-Port: er nimmt aus jeder waagerechten Richtung an - das Teilchen muss nur auf
        // gleicher Hoehe und auf einer der beiden Achsen des Kerns stehen.
        return worldPosition.getY() == y && (worldPosition.getX() == x || worldPosition.getZ() == z);
    }

    /** Original: {@code getExitDir} - Schwelle, Redstone, sonst die obere Richtung. */
    public Direction getExitDir(Particle particle) {
        if (particle.momentum < threshold) return dirLower;
        return hasRedstoneSignal() ? dirRedstone : dirUpper;
    }

    private boolean hasRedstoneSignal() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        CoilType type = ItemPACoil.typeOf(getInventory().getStackInSlot(SLOT_COIL));
        boolean isInline = dir == getExitDir(particle);

        int mult = 1;
        if (type != null) {
            if (type.diMin > particle.momentum) mult *= 10;
            if (type.diDistMin > particle.distanceTraveled) mult *= 10;
            // Geradeaus kostet immer nur den Grundverbrauch.
            if (isInline) mult = 1;
        }

        if (!isCool())                        particle.crash(PAState.CRASH_NOCOOL);
        if (getEnergyStored() < USAGE * mult) particle.crash(PAState.CRASH_NOPOWER);
        if (type == null)                     particle.crash(PAState.CRASH_NOCOIL);
        if (type != null && type.diMax < particle.momentum && !isInline) {
            particle.crash(PAState.CRASH_OVERSPEED);
        }

        if (particle.invalid) return;

        if (isInline) {
            particle.addDistance(DISTANCE_INLINE);
        } else {
            particle.resetDistance();
        }

        setEnergyStored(getEnergyStored() - USAGE * mult);
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        particle.dir = getExitDir(particle);
        return worldPosition.relative(particle.dir, EXIT_OFFSET);
    }

    // ── Einstellungen ───────────────────────────────────────────────────────

    public Direction getDirLower()    { return dirLower; }
    public Direction getDirUpper()    { return dirUpper; }
    public Direction getDirRedstone() { return dirRedstone; }
    public int getThreshold()         { return threshold; }

    public void configure(Direction lower, Direction upper, Direction redstone, int threshold) {
        this.dirLower = lower;
        this.dirUpper = upper;
        this.dirRedstone = redstone;
        this.threshold = Math.max(0, threshold);
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_COIL) return stack.getItem() instanceof ItemPACoil;
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack);
        return false;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("dirLower", dirLower.get2DDataValue());
        tag.putInt("dirUpper", dirUpper.get2DDataValue());
        tag.putInt("dirRedstone", dirRedstone.get2DDataValue());
        tag.putInt("threshold", threshold);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        dirLower = Direction.from2DDataValue(tag.getInt("dirLower"));
        dirUpper = Direction.from2DDataValue(tag.getInt("dirUpper"));
        dirRedstone = Direction.from2DDataValue(tag.getInt("dirRedstone"));
        threshold = tag.getInt("threshold");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.dipole");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.hbm_m.inventory.menu.PADipoleMenu(id, inv, this);
    }
}
