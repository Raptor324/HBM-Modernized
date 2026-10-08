package com.hbm_m.blockentity.machines;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineBatteryREDDBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.BatteryREDDMenu;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityBatteryREDD} (+ {@code TileEntityBatteryBase}): die FEnSU-Reddendit-Batterie. Speichert
 * beliebig viel Strom als BigInteger; dem Netz gegenueber zeigt sie hoechstens {@code maxPower / 2}
 * ({@code Long.MAX_VALUE / 100}). Redstone an einem der sechs Ports schaltet zwischen {@code redLow} und
 * {@code redHigh} (0 Eingang, 1 Puffer, 2 Ausgang, 3 aus). Clientseitig dreht sich das Rad je nach Ladung.
 *
 * <p>Die Basisklasse rechnet mit dem {@code energy}-Feld; es spiegelt hier immer die gedeckelte Sicht auf
 * {@link #power}, und {@link #setEnergyStored} uebertraegt jede Aenderung als Differenz auf die BigInteger.</p>
 */
public class BatteryREDDBlockEntity extends BaseMachineBlockEntity
        implements com.hbm_m.interfaces.IEnergyModeHolder, com.hbm_m.api.energy.PowerBuffer, com.hbm_m.api.block.IPersistentNBT {

    public static final int mode_input = 0;
    public static final int mode_buffer = 1;
    public static final int mode_output = 2;
    public static final int mode_none = 3;

    private static final long MAX_POWER = Long.MAX_VALUE / 100L;

    public float prevRotation = 0F;
    public float rotation = 0F;

    public BigInteger[] log = new BigInteger[20];
    public BigInteger delta = BigInteger.ZERO;
    public BigInteger power = BigInteger.ZERO;

    public short redLow = 0;
    public short redHigh = 2;
    public Priority priority = Priority.LOW;
    public byte lastRedstone = 0;

    public BatteryREDDBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_REDD_BE.get(), pos, state, 2, MAX_POWER, MAX_POWER, MAX_POWER);
    }

    // ─── BigInteger-Speicher hinter der long-API der Basisklasse ────────────────────────────

    /** {@code getPower()}: fuer die Abgabe hoechstens die Haelfte des Verbindungsmaximums. */
    private long view() {
        return this.power.min(BigInteger.valueOf(MAX_POWER / 2)).max(BigInteger.ZERO).longValue();
    }

    private void sync() {
        this.energy = view();
    }

    @Override
    public long getEnergyStored() {
        return view();
    }

    @Override
    public long getMaxEnergyStored() {
        return MAX_POWER;
    }

    @Override
    public void setEnergyStored(long newEnergy) {
        long diff = newEnergy - this.energy;
        if (diff != 0) {
            this.power = this.power.add(BigInteger.valueOf(diff));
            if (this.power.signum() < 0) this.power = BigInteger.ZERO;
        }
        sync();
        setChanged();
    }

    // ─── Betriebsart ─────────────────────────────────────────────────────────────────────────

    /** {@code getRelevantMode}: Redstone an irgendeinem Port waehlt {@code redHigh}. */
    public short getRelevantMode() {
        if (level == null) return redLow;
        for (BlockPos p : getPortPos()) if (level.hasNeighborSignal(p)) return redHigh;
        return redLow;
    }

    /** Port-Netz: 0 = Puffer, 1 = Eingang, 2 = Ausgang, 3 = aus. */
    @Override
    public int getCurrentMode() {
        return switch (getRelevantMode()) {
            case mode_input -> 1;
            case mode_buffer -> 0;
            case mode_output -> 2;
            default -> 3;
        };
    }

    @Override
    public long getProvideSpeed() {
        short m = getRelevantMode();
        return (m == mode_buffer || m == mode_output) ? MAX_POWER : 0;
    }

    @Override
    public long getReceiveSpeed() {
        short m = getRelevantMode();
        return (m == mode_buffer || m == mode_input) ? MAX_POWER : 0;
    }

    @Override public boolean canExtract() { return getProvideSpeed() > 0 && this.energy > 0; }
    @Override public boolean canReceive() { return getReceiveSpeed() > 0; }
    @Override public Priority getPriority() { return this.priority; }

    // ─── Tick ────────────────────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryREDDBlockEntity be) {
        be.update(level);
    }

    private void update(Level world) {
        BigInteger prevPower = this.power;

        if (!world.isClientSide) {
            sync();
            if (priority == null || priority.ordinal() == 0 || priority.ordinal() == 4) priority = Priority.LOW;

            ensureNetworkInitialized();

            chargeFromBatterySlot(0);
            chargeItemInSlot(1);

            // same implementation as for batteries, however retooled to use bigints because fuck
            BigInteger avg = this.power.add(prevPower).divide(BigInteger.valueOf(2));
            this.delta = avg.subtract(this.log[0] == null ? BigInteger.ZERO : this.log[0]);
            System.arraycopy(this.log, 1, this.log, 0, this.log.length - 1);
            this.log[19] = avg;

            byte comp = getComparatorPower();
            if (comp != this.lastRedstone) world.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            this.lastRedstone = comp;

            // networkPackNT(100)
            setChanged();
            sendUpdateToClient();
        } else {
            this.prevRotation = this.rotation;
            this.rotation += this.getSpeed();

            if (rotation >= 360) {
                rotation -= 360;
                prevRotation -= 360;
            }

            com.hbm_m.client.sound.ReddSoundClient.tick(this);
        }
    }

    public float getSpeed() {
        return (float) Math.min(Math.pow(Math.log(this.power.doubleValue() * 0.05 + 1) * 0.05F, 5), 15F);
    }

    public byte getComparatorPower() {
        double frac = (double) getEnergyStored() / (double) Math.max(getMaxEnergyStored(), 1) * 15D;
        return (byte) Mth.clamp((int) Math.round(frac), 0, 15);
    }

    /** {@code receiveControl}: Knopf 0 = redLow, 1 = redHigh, 2 = Prioritaet (LOW..HIGH). */
    public void handleButtonPress(int id) {
        if (id == 0) { redLow++; if (redLow > 3) redLow = 0; }
        if (id == 1) { redHigh++; if (redHigh > 3) redHigh = 0; }
        if (id == 2) {
            int ordinal = this.priority.ordinal() + 1;
            if (ordinal > Priority.HIGH.ordinal()) ordinal = Priority.LOW.ordinal();
            this.priority = Priority.values()[ordinal];
        }
        setChanged();
        sendUpdateToClient();
    }

    // ─── Ports ───────────────────────────────────────────────────────────────────────────────

    /** {@code getPortPos}: die sechs Anschlussfelder am Sockel. */
    public BlockPos[] getPortPos() {
        Direction dir = getBlockState().getValue(MachineBatteryREDDBlock.FACING);
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new BlockPos[] {
                p.relative(dir, 2).relative(rot, 2),
                p.relative(dir, 2).relative(rot, -2),
                p.relative(dir, -2).relative(rot, 2),
                p.relative(dir, -2).relative(rot, -2),
                p.relative(rot, 4),
                p.relative(rot, -4),
        };
    }

    /** {@code getConPos}: die Kabelfelder vor den sechs Ports (Puffermodus-Knoten). */
    @Override
    public com.hbm_m.api.energy.Nodespace.PowerNode createNode(BlockPos pos) {
        Direction dir = getBlockState().getValue(MachineBatteryREDDBlock.FACING);
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new com.hbm_m.api.energy.Nodespace.PowerNode(com.hbm_m.api.energy.Nodespace.THE_POWER_PROVIDER, pos).setConnections(
                con(p.relative(dir, 3).relative(rot, 2), dir),
                con(p.relative(dir, 3).relative(rot, -2), dir),
                con(p.relative(dir, -3).relative(rot, 2), dir.getOpposite()),
                con(p.relative(dir, -3).relative(rot, -2), dir.getOpposite()),
                con(p.relative(rot, 5), rot),
                con(p.relative(rot, -5), rot.getOpposite()));
    }

    private static com.hbm_m.api.network.NodeDirPos con(BlockPos p, Direction d) {
        return new com.hbm_m.api.network.NodeDirPos(p.getX(), p.getY(), p.getZ(), d);
    }

    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof MachineBatteryREDDBlock controller)) return new BlockPos[0];
        Direction facing = getBlockState().getValue(MachineBatteryREDDBlock.FACING);
        var helper = controller.getStructureHelper();
        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos local : helper.getStructureMap().keySet()) {
            if (helper.resolvePartRole(local, controller) == PartRole.ENERGY_CONNECTOR) ports.add(helper.getRotatedPos(worldPosition, local, facing));
        }
        return ports.toArray(new BlockPos[0]);
    }

    // ─── Inventar / GUI ──────────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.battery_redd");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return isEnergyProviderItem(stack) || isEnergyReceiverItem(stack)
                || stack.getItem() instanceof com.hbm_m.item.fekal_electric.ItemCreativeBattery;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BatteryREDDMenu(id, inv, this);
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putByteArray("power", this.power.toByteArray());
        nbt.putByteArray("delta", this.delta.toByteArray());
        nbt.putShort("redLow", redLow);
        nbt.putShort("redHigh", redHigh);
        nbt.putByte("lastRedstone", lastRedstone);
        nbt.putByte("priority", (byte) this.priority.ordinal());
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        byte[] p = nbt.getByteArray("power");
        this.power = p.length > 0 ? new BigInteger(p) : BigInteger.ZERO;
        byte[] d = nbt.getByteArray("delta");
        this.delta = d.length > 0 ? new BigInteger(d) : BigInteger.ZERO;
        if (nbt.contains("redLow")) this.redLow = nbt.getShort("redLow");
        if (nbt.contains("redHigh")) this.redHigh = nbt.getShort("redHigh");
        this.lastRedstone = nbt.getByte("lastRedstone");
        if (nbt.contains("priority")) this.priority = Priority.values()[Mth.clamp(nbt.getByte("priority"), 0, Priority.values().length - 1)];
        sync();
    }

    /** Original {@code writeNBT}: nur die Ladung wandert in den Drop. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        nbt.putByteArray("power", this.power.toByteArray());
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 4, worldPosition.getY(), worldPosition.getZ() - 4,
                worldPosition.getX() + 5, worldPosition.getY() + 10, worldPosition.getZ() + 5);
    }
}
