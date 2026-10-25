package com.hbm_m.blockentity.machines;

import com.hbm_m.block.machines.MachineCapacitorBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code MachineCapacitor.TileEntityCapacitor} (1.7.10 Original) - a directional HE energy
 * buffer with no GUI/inventory. Charges 3x faster than it discharges (matching the original's fixed
 * {@code maxPower/200} receive vs {@code maxPower/600} provide ratio) and sits at LOW network
 * priority, i.e. a "last resort" sink/source. Only connects on the face it was placed against.
 * <p>
 * <p><b>Die Sammelschiene.</b> Wie im Original laesst sich der Ausgang verlaengern: hinter dem
 * Kondensator duerfen beliebig viele {@code capacitor_bus}-Bloecke in gerader Linie stehen, und
 * abgegeben wird am Ende dieser Kette. Alle Schienen muessen in dieselbe Richtung zeigen - knickt
 * die Reihe, gilt die ganze Kette als ungueltig und der Kondensator gibt nichts ab. So laesst sich
 * eine Batteriebank hinter einer Wand verstecken und trotzdem sauber anzapfen.</p>
 *
 * <p>Redstone-over-Radio wie im Original ({@code fill}, {@code fillpercent}); OpenComputers gibt es hier nicht.</p>
 */
public class MachineCapacitorBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.block.IPersistentNBT, com.hbm_m.api.redstoneoverradio.IRORValueProvider {

    // Port der Zaehler aus TileEntityCapacitor: Leistung pro Tick + Werte des letzten Ticks.
    public long powerReceived;
    public long powerSent;
    public long lastPowerReceived;
    public long lastPowerSent;

    @Override
    public String[] getFunctionInfo() {
        return new String[] { PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent" };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + getEnergyStored();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + getEnergyStored() * 100 / getMaxEnergyStored();
        return null;
    }

    public MachineCapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CAPACITOR_BE.get(), pos, state, 0,
                capacityFor(state), Math.max(1, capacityFor(state) / 100), Math.max(1, capacityFor(state) / 300));
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long received = super.receiveEnergy(maxReceive, simulate);
        if (!simulate && received > 0) this.powerReceived += received;
        return received;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long extracted = super.extractEnergy(maxExtract, simulate);
        if (!simulate && extracted > 0) this.powerSent += extracted;
        return extracted;
    }

    /**
     * Alle Netz-Zufuhren laufen ueber {@code transferPower} (PowerNet-Verteilung, direkte
     * Einspeisung, FE-Bruecke) - der Default zaehlt hier NICHT mit, deshalb zaehlt der
     * Override. Port des Original-Overrides (MachineCapacitor.java:232-244): angenommene
     * Leistung = power - overshoot (vollem Fit entsprechend "power", bei Overflow
     * "maxPower - power").
     */
    @Override
    public long transferPower(long power) {
        // Rumpf des Defaults von IEnergyReceiver.transferPower inline kopiert: ein
        // qualified super-call ("IEnergyReceiver.super.") ist illegal, weil diese Klasse
        // das Interface nur ueber BaseMachineBlockEntity erbt, nicht direkt implementiert.
        if (power + this.getEnergyStored() <= this.getMaxEnergyStored()) {
            this.setEnergyStored(power + this.getEnergyStored());
            this.powerReceived += power;
            return 0;
        }
        long capacity = this.getMaxEnergyStored() - this.getEnergyStored();
        long overshoot = power - capacity;
        this.powerReceived += capacity;
        this.setEnergyStored(this.getMaxEnergyStored());
        return overshoot;
    }

    /**
     * Server-Ticker: rollt die Durchflusszaehler pro Tick (Original: Ende von
     * {@code updateEntity}) und synchronisiert den Client alle 15 Ticks
     * (Original: {@code networkPackNT(15)}).
     */
    public static void tick(Level level, BlockPos pos, BlockState state, MachineCapacitorBlockEntity be) {
        if (level.isClientSide) return;

        if (level.getGameTime() % 15 == 0) {
            be.sendUpdateToClient();
        }

        be.lastPowerReceived = be.powerReceived;
        be.lastPowerSent = be.powerSent;
        be.powerReceived = 0;
        be.powerSent = 0;
    }

    private static long capacityFor(BlockState state) {
        return state.getBlock() instanceof MachineCapacitorBlock b ? b.getCapacity() : 1_000_000L;
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return IEnergyReceiver.Priority.LOW;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return getBlockState().getValue(MachineCapacitorBlock.FACING) == side;
    }

    /**
     * 1:1-Port der Schienenschleife aus {@code TileEntityCapacitor.updateEntity}: von der
     * Rueckseite aus wird der Kette gefolgt, solange dort Schienen mit gleicher Ausrichtung
     * stehen. Das Ende ist die Stelle, an der abgegeben wird.
     */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null) return new BlockPos[0];

        Direction facing = getBlockState().getValue(MachineCapacitorBlock.FACING);
        BlockPos pos = worldPosition.relative(facing.getOpposite());

        Direction last = null;
        // Ohne Deckel: eine sehr lange Kette ist zulaessig, eine endlose nicht.
        for (int step = 0; step < 64; step++) {
            // Nie Chunks nachladen: laeuft auch aus setRemoved beim Entladen/Server-Stopp.
            BlockState state = com.hbm_m.util.ChunkSafe.getBlockState(level, pos);
            if (state == null) break;
            if (state.getBlock() != com.hbm_m.block.ModBlocks.CAPACITOR_BUS.get()) break;
            if (!state.hasProperty(MachineCapacitorBlock.FACING)) break;

            Direction current = state.getValue(MachineCapacitorBlock.FACING);
            if (last == null) last = current;

            // Original: knickt die Reihe, faellt die ganze Kette weg.
            if (last != current) return new BlockPos[0];

            pos = pos.relative(current);
        }

        // Keine Schiene gefunden - der Kondensator gibt an seiner eigenen Seite ab.
        if (last == null) return new BlockPos[0];

        return new BlockPos[] { pos };
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_capacitor");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    protected void writeNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        // Original serialize(): power/maxPower/powerReceived/powerSent gehen zum Client;
        // hier läuft derselbe Kanal auch ins Datadir (Zaehler sind nach einem Tick wieder 0).
        tag.putLong("powerReceived", powerReceived);
        tag.putLong("powerSent", powerSent);
    }

    @Override
    protected void readNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        powerReceived = tag.getLong("powerReceived");
        powerSent = tag.getLong("powerSent");
    }

    /** Original {@code TileEntityCapacitor.writeNBT}: Ladung und Kapazitaet. */
    @Override
    public void writeNBT(net.minecraft.nbt.CompoundTag nbt) {
        nbt.putLong("energy", getEnergyStored());
        nbt.putLong("capacity", getMaxEnergyStored());
    }
    //? if >= 1.21.1 {
    /*public void saveToItemStack(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.nbt.CompoundTag beTag = new net.minecraft.nbt.CompoundTag();
        this.writeNBT(beTag);
        if (!beTag.isEmpty()) {
            net.minecraft.nbt.CompoundTag root = new net.minecraft.nbt.CompoundTag();
            root.put("BlockEntityTag", beTag);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(root));
        }
    }
    *///?}
}
