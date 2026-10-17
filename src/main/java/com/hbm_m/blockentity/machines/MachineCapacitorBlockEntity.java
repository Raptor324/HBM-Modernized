package com.hbm_m.blockentity.machines;

import com.hbm_m.api.redstoneoverradio.IRORInfo;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineCapacitorBlock;
import com.hbm_m.block.machines.MachineCapacitorBusBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Port of {@code MachineCapacitor.TileEntityCapacitor} (1.7.10 Original) - a directional HE energy
 * buffer with no GUI/inventory. Charges 3x faster than it discharges: the original's CODE is
 * {@code getReceiverSpeed() = maxPower/100} and {@code getProviderSpeed() = maxPower/300}
 * (MachineCapacitor.java:262-268); the old tooltip claimed /200 and /600, but original code and
 * NTM NEXT agree on /100 + /300, which is ported here. Sits at LOW network priority, i.e. a
 * "last resort" sink/source. Only connects on the face it was placed against.
 * <p>
 * <p><b>Die Sammelschiene.</b> Wie im Original laesst sich der Ausgang verlaengern: hinter dem
 * Kondensator duerfen beliebig viele {@code capacitor_bus}-Bloecke in gerader Linie stehen, und
 * abgegeben wird am Ende dieser Kette. Alle Schienen muessen in dieselbe Richtung zeigen - knickt
 * die Reihe, gilt die ganze Kette als ungueltig und der Kondensator gibt nichts ab. So laesst sich
 * eine Batteriebank hinter einer Wand verstecken und trotzdem sauber anzapfen.</p>
 *
 * <p><b>Paritaet:</b> ohne Schienenkette gibt der Kondensator NICHTS ab (die fruehere
 * Port-Abweichung "gibt und nimmt an allen 6 Seiten" ist zurueckgebaut) und nimmt nur an seiner
 * Anschlussseite ({@code FACING}) an. Redstone-over-Radio ({@code VAL:fill}/{@code VAL:fillpercent})
 * ist wie im Original portiert; OpenComputers bleibt unportiert (hier existiert keine
 * OC/CC-Compat-Infrastruktur).</p>
 *
 * <p><b>Bekannte Abweichung:</b> die Schienenschleife bricht nach 64 Schienen bzw. an der Grenze
 * ungeladener Chunks ab (Schutz vor synchronem Chunk-Load im Stop-Server-Pfad); das Original
 * lief endlos weit.</p>
 */
public class MachineCapacitorBlockEntity extends BaseMachineBlockEntity implements IRORValueProvider {

    // Port der Zaehler aus TileEntityCapacitor: Leistung pro Tick + Werte des letzten Ticks.
    public long powerReceived;
    public long powerSent;
    public long lastPowerReceived;
    public long lastPowerSent;

    /** Port of {@code TileEntityCapacitor.getFunctionInfo}: VAL:fill / VAL:fillpercent. */
    private static final String[] ROR = new String[] {
            IRORInfo.PREFIX_VALUE + "fill",
            IRORInfo.PREFIX_VALUE + "fillpercent",
    };

    public MachineCapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CAPACITOR_BE.get(), pos, state, 0,
                capacityFor(state), Math.max(1, capacityFor(state) / 100), Math.max(1, capacityFor(state) / 300));
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

    /**
     * 1:1-Port des Netzwerkabschnitts von {@code TileEntityCapacitor.updateEntity}: subscribed
     * wird NUR an der FACING-Seite (Original: ein einziges {@code trySubscribe} an der
     * Metadaten-Seite), abgegeben wird NUR am Ende einer geraden Schienenkette (Original:
     * {@code pos != null && last != null}). Ohne Kette gibt der Kondensator nichts ab.
     * Wird vom zentralen Abonnement-Treiber ({@code EnergySubscriptions.tickAll}) jeden Tick
     * aufgerufen; trySubscribe/tryProvide aktualisieren nur Timestamps bestehender
     * Netzeinträge und sind daher pro Tick unkritisch.
     */
    @Override
    public void ensureNetworkInitialized() {
        if (!(level instanceof ServerLevel serverLevel) || isRemoved()) return;

        BusChain chain = walkBusChain();
        if (chain != null) {
            tryUnsubscribe(serverLevel, chain.end().getX(), chain.end().getY(), chain.end().getZ());
            tryProvide(serverLevel, chain.end().getX(), chain.end().getY(), chain.end().getZ(), chain.dir());
        }

        Direction facing = getBlockState().getValue(MachineCapacitorBlock.FACING);
        BlockPos input = worldPosition.relative(facing);
        if (serverLevel.hasChunkAt(input)) {
            trySubscribe(serverLevel, input.getX(), input.getY(), input.getZ(), facing);
        }
    }

    /**
     * 1:1-Port der Schienenschleife aus {@code TileEntityCapacitor.updateEntity}: von der
     * Rueckseite aus wird der Kette gefolgt, solange dort Schienen mit gleicher Ausrichtung
     * stehen. Das Ende ist die Stelle, an der abgegeben wird.
     */
    @Nullable
    private BusChain walkBusChain() {
        if (level == null) return null;

        Direction facing = getBlockState().getValue(MachineCapacitorBlock.FACING);
        BlockPos pos = worldPosition.relative(facing.getOpposite());

        Direction last = null;
        // Original lief endlos weit. Cap bei 64 Schienen (bekannte Abweichung, siehe Klassen-
        // javadoc): eine sehr lange Kette ist zulaessig, eine endlose nicht.
        for (int step = 0; step < 64; step++) {
            // Просмотр цепочки идёт по СОСЕДНИМ позициям: если чанк соседа уже
            // выгружен (типично — при выгрузке мира/остановке сервера), запрос
            // getBlockState синхронно загрузит чанк с диска, и он снова попадёт
            // в очередь выгрузки — лавина повторных загрузок висит stopServer
            // на минуты. Цепочка просто обрывается на границе загруженности.
            if (!level.hasChunkAt(pos)) break;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != ModBlocks.CAPACITOR_BUS.get()) break;
            if (!state.hasProperty(MachineCapacitorBusBlock.FACING)) break;

            Direction current = state.getValue(MachineCapacitorBusBlock.FACING);
            if (last == null) last = current;

            // Original: knickt die Reihe, faellt die ganze Kette weg.
            if (last != current) return null;

            pos = pos.relative(current);
        }

        // Keine Schiene gefunden - der Kondensator gibt NICHTS ab (wie im Original).
        if (last == null) return null;

        return new BusChain(pos, last);
    }

    /**
     * Zusätzliche Ports nur für die Aufräum-Pfade ({@code unsubscribeAll} in setRemoved):
     * das Kettenende. Die Abwicklung selbst macht {@link #ensureNetworkInitialized}.
     */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        BusChain chain = walkBusChain();
        return chain == null ? new BlockPos[0] : new BlockPos[] { chain.end() };
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        // Original serialize(): power/maxPower/powerReceived/powerSent gehen zum Client;
        // hier läuft derselbe Kanal auch ins Datadir (Zaehler sind nach einem Tick wieder 0).
        tag.putLong("powerReceived", powerReceived);
        tag.putLong("powerSent", powerSent);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        powerReceived = tag.getLong("powerReceived");
        powerSent = tag.getLong("powerSent");
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String provideRORValue(String name) {
        if ((IRORInfo.PREFIX_VALUE + "fill").equals(name)) return "" + getEnergyStored();
        if ((IRORInfo.PREFIX_VALUE + "fillpercent").equals(name)) return "" + (getEnergyStored() * 100 / getMaxEnergyStored());
        return null;
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

    /**
     * 1.21.1: copy_nbt в лут-таблицах удалён (1.20.5+), поэтому перенос заряда в дропнутый
     * предмет делается кодом — по паттерну батареи
     * ({@code MachineBatteryBlock#playerWillDestroy} → {@code saveToItemStack}).
     * Формат совпадает с чтением в {@code MachineCapacitorBlock#setPlacedBy}:
     * CUSTOM_DATA → {"BlockEntityTag": {...}}.
     */
    //? if >= 1.21.1 {
    /*public void saveToItemStack(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.core.HolderLookup.Provider registries = this.level.registryAccess();
        CompoundTag beTag = this.saveWithoutMetadata(registries);
        if (!beTag.isEmpty()) {
            CompoundTag root = new CompoundTag();
            root.put("BlockEntityTag", beTag);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(root));
        }
    }
    *///?}

    /**
     * Kleiner Wert-Halter fürs Ergebnis der Schienenschleife (Entsprechung der lokalen
     * {@code pos}/{@code last} im Original-Loop) - Endposition plus Kettenrichtung.
     */
    private record BusChain(BlockPos end, Direction dir) {}
}
