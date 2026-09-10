package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.pneumatic.ISlotMonitorProvider;
import com.hbm_m.api.pneumatic.IPneumaticConnector;
import com.hbm_m.api.pneumatic.PneumaticNet;
import com.hbm_m.api.pneumatic.PneumaticNetProvider;
import com.hbm_m.api.pneumatic.SlotMonitor;
import com.hbm_m.api.pneumatic.StackCache;
import com.hbm_m.api.pneumatic.StackCache.CacheSlot;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPneumaticStorageBase} (1.7.10): die Grundlage aller Lager im
 * Druckluft-Lagernetz.
 *
 * <p>Ein Lager ist nur <b>erreichbar, solange Druckluft anliegt</b> - laeuft der Tank leer,
 * verschwindet sein ganzer Inhalt aus allen Terminals. Der Verbrauch steigt mit dem Fuellstand,
 * das Lager saugt also von selbst so viel wie es bekommen kann. Die eingestellte Druckstufe
 * bestimmt, wie weit ein Terminal entfernt stehen darf: dieselbe Staffel wie beim Rohr, von zehn
 * Bloecken bei Stufe eins bis tausend bei Stufe fuenf.</p>
 *
 * <p>Jeder Platz bekommt einen {@link SlotMonitor Waechter}, der Aenderungen an die Terminals
 * meldet. Aendert sich die Erreichbarkeit - Luft alle, Druckstufe umgestellt -, werden alle
 * Waechter angestossen, statt jeden Tick alle Entfernungen nachzurechnen.</p>
 */
public abstract class PneumaticStorageBlockEntity extends BaseMachineBlockEntity
        implements IPneumaticConnector, IFluidStandardReceiverMK2, ISlotMonitorProvider {

    private static final int TANK_CAPACITY = 4_000;

    protected final FluidTank compair;
    protected final SlotMonitor[] monitors;

    @Nullable
    protected GenNode<PneumaticNet> node;
    protected boolean wasAvailable = false;

    protected PneumaticStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots, 0L, 0L, 0L);
        this.compair = new FluidTank(ModFluids.AIR.getSource(), TANK_CAPACITY).withPressure(1);

        this.monitors = new SlotMonitor[slots];
        for (int i = 0; i < slots; i++) this.monitors[i] = new SlotMonitor(i, this);
    }

    public FluidTank getTank() { return compair; }

    /** Original: die Druckstufe laeuft im Kreis von 1 bis 5 und aendert die Reichweite. */
    public void nextPressure() {
        int pressure = compair.getPressure() + 1;
        if (pressure > 5) pressure = 1;
        compair.setTankType(ModFluids.AIR.getSource());
        compair.withPressure(pressure);
        for (SlotMonitor monitor : monitors) monitor.availabilityHasChanged();
        setChanged();
    }

    /** 1:1-Port des Rumpfs von {@code updateEntity}. */
    protected void tickStorage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        boolean isAvailable = isAvailable();
        if (isAvailable != wasAvailable) {
            wasAvailable = isAvailable;
            for (SlotMonitor monitor : monitors) monitor.availabilityHasChanged();
        }

        if (node == null || node.expired) {
            node = UniNodespace.getNode(serverLevel, pos, PneumaticNetProvider.THE_PROVIDER);

            if (node == null || node.expired) {
                GenNode<PneumaticNet> fresh = new GenNode<>(PneumaticNetProvider.THE_PROVIDER, pos);
                NodeDirPos[] conns = new NodeDirPos[6];
                int i = 0;
                for (Direction dir : Direction.values()) conns[i++] = new NodeDirPos(pos.relative(dir), dir);
                fresh.setConnections(conns);
                UniNodespace.createNode(serverLevel, fresh);
                node = fresh;
            }
        }

        if (node != null && !node.expired && node.hasValidNet()) {
            node.net.storages.add(this);
        }

        if (level.getGameTime() % 10 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(compair.getTankType(), level, pos.relative(dir), dir);
            }
        }

        // Original: der Verbrauch steigt mit dem Fuellstand - ein volles Lager zieht am meisten.
        if (compair.getFill() > 0) {
            int consumption = (int) Math.ceil(compair.getFill() * 9D / compair.getMaxFill()) + 1;
            compair.setFill(Math.max(compair.getFill() - consumption, 0));
        }

        updateMonitors();
    }

    /** Original: {@code isAvailable} - ohne Druckluft ist das Lager fuer das Netz nicht da. */
    public boolean isAvailable() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition) && compair.getFill() > 0;
    }

    // ── ISlotMonitorProvider ────────────────────────────────────────────────

    @Override public SlotMonitor[] getMonitors() { return monitors; }
    @Override public ItemStack getSlotAt(int index) { return getInventory().getStackInSlot(index); }

    @Nullable
    @Override
    public PneumaticNet getRelevantNetwork() {
        if (node == null || node.expired || !node.hasValidNet()) return null;
        return node.net;
    }

    /** Original: {@code isAvailableToCache} - die Druckstufe entscheidet ueber die Reichweite. */
    @Override
    public boolean isAvailableToCache(StackCache cache) {
        if (!isAvailable()) return false;
        int range = PneumoTubeBlockEntity.getRangeFromPressure(compair.getPressure());
        return worldPosition.distSqr(cache.pos) <= (long) range * range;
    }

    // ── IFluidStandardReceiverMK2 ───────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return new FluidTank[] { compair }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { compair }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── Abbau ───────────────────────────────────────────────────────────────

    private void detach() {
        for (SlotMonitor monitor : monitors) {
            for (CacheSlot cache : new java.util.ArrayList<>(monitor.viewedBy)) cache.removeMonitor(monitor);
            monitor.viewedBy.clear();
        }
        if (node != null && node.hasValidNet()) node.net.storages.remove(this);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            detach();
            if (level instanceof ServerLevel serverLevel && node != null) {
                UniNodespace.destroyNode(serverLevel, worldPosition, PneumaticNetProvider.THE_PROVIDER);
            }
        }
        super.setRemoved();
    }

    //? if forge {
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        detach();
    }
    //?}

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        compair.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        compair.readFromNBT(tag, "tank");
    }
}
