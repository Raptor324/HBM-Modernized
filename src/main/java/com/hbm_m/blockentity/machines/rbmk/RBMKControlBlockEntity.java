package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.inventory.menu.RBMKControlMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RBMKControlBlockEntity extends RBMKColumnBlockEntity implements MenuProvider, IEnergyReceiver {

    // 1:1 with the original's TileEntityRBMKControl (level defaults to 0, i.e. fully inserted/
    // SCRAMMED): a freshly placed reactor should start subcritical-safe, not with every rod
    // already withdrawn. Also fixes the control-rod cap renderer, which floated a full block
    // above the column by default when level started at 1.
    public double level       = 0.0;
    public double targetLevel = 0.0;
    public double lastLevel   = 0.0;
    public static final double SPEED = 0.00277;

    /** Color group for console control (-1 = ungrouped, 0-4 = groups). */
    public short color = -1;

    /**
     * ReaSim control rods ({@code rbmk_control_reasim} / {@code rbmk_control_reasim_auto}) are the
     * only two variants that need electricity: CE's {@code TileEntityRBMKControl.isPowered()}
     * checks exactly those two blocks. They draw {@link #CONSUMPTION} from the block <b>below</b>
     * them for every tick in which the rod actually moves, and simply refuse to move at all while
     * the buffer is short. The port had no power requirement whatsoever, so both ReaSim variants
     * behaved as free-moving manual rods. Set from the block variant - see
     * {@link com.hbm_m.block.machines.rbmk.RBMKControlManualBlock}.
     */
    public boolean powered = false;

    public static final long CONSUMPTION = 5_000L;
    /** Enough buffer for half a second of continuous movement, as in CE. */
    public static final long MAX_POWER = CONSUMPTION * 10;

    public long power = 0;
    public boolean hasPower = false;
    private boolean energyNodeRegistered = false;

    public boolean isPowered() { return powered; }

    protected RBMKControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 1:1 with the original's RBMKBase.hasOwnLid(): control rod variants never get the generic
    // cover/glass lid textures at all (no rbmk_control_cover_top/_glass_top assets exist) - the
    // animated rod cap itself is their "lid". Without this override, RBMKColumnRenderer's lid-
    // aware top-texture selection (added for the other column types) would try to load those
    // missing textures for every control rod and render a missing-texture checkerboard.
    @Override public boolean   hasLid()          { return false; }
    @Override public boolean   isLidRemovable()  { return false; }
    @Override public RBMKType  getRBMKType()      { return RBMKType.CONTROL_ROD; }
    @Override public ColumnType getConsoleType()  { return ColumnType.CONTROL; }

    public double getMult() { return level; }

    public void setTarget(double target) {
        this.targetLevel = Math.max(0, Math.min(1, target));
    }

    /**
     * Refreshes {@link #hasPower} and hooks the rod into the energy network, 1:1 with the first
     * half of CE's {@code TileEntityRBMKControl.update}. Unpowered variants are always "powered".
     */
    protected void updatePower(Level level) {
        this.hasPower = true;
        if (!isPowered()) return;

        if (!energyNodeRegistered && level instanceof net.minecraft.server.level.ServerLevel server) {
            com.hbm_m.api.energy.EnergyNetworkManager.get(server).addNode(getBlockPos());
            energyNodeRegistered = true;
        }
        if (this.power < CONSUMPTION) this.hasPower = false;
    }

    protected void moveLevelToTarget(Level level) {
        if (!hasPower) return;

        double speed = SPEED * RBMKDials.getControlSpeed(level);
        if (this.level < targetLevel)      this.level = Math.min(this.level + speed, targetLevel);
        else if (this.level > targetLevel) this.level = Math.max(this.level - speed, targetLevel);

        // Only movement costs power - a rod parked at its target draws nothing.
        if (isPowered() && this.level != this.lastLevel) this.power -= CONSUMPTION;
    }

    // ─── IEnergyReceiver ───────────────────────────────────────────────────────

    @Override public long getEnergyStored()    { return power; }
    @Override public long getMaxEnergyStored() { return isPowered() ? MAX_POWER : 0; }
    @Override public void setEnergyStored(long energy) {
        this.power = Math.max(0, Math.min(getMaxEnergyStored(), energy));
        setChanged();
    }
    @Override public long getReceiveSpeed() { return CONSUMPTION; }
    /** CE deliberately puts control rods on LOW priority, behind ordinary machines. */
    @Override public IEnergyReceiver.Priority getPriority() { return IEnergyReceiver.Priority.LOW; }
    @Override public boolean canReceive() { return isPowered() && power < getMaxEnergyStored(); }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long received = Math.min(getMaxEnergyStored() - power, Math.min(getReceiveSpeed(), maxReceive));
        if (!simulate && received > 0) setEnergyStored(power + received);
        return received;
    }

    /** CE's {@code canConnect}: the cable has to come up from underneath the column. */
    @Override
    public boolean canConnectEnergy(Direction side) {
        return isPowered() && (side == null || side == Direction.DOWN);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable Direction side) {
        if (isPowered()) {
            if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER)
                return net.minecraftforge.common.util.LazyOptional.of(() -> (IEnergyReceiver) this).cast();
            if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR)
                return net.minecraftforge.common.util.LazyOptional.of(() -> (com.hbm_m.interfaces.IEnergyConnector) this).cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    @Override
    public void setRemoved() {
        // NB: `level` is shadowed in this class by the rod's extraction level, so the world has to
        // be fetched through getLevel().
        if (energyNodeRegistered && getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
            com.hbm_m.api.energy.EnergyNetworkManager.get(server).removeNode(getBlockPos());
            energyNodeRegistered = false;
        }
        super.setRemoved();
    }

    /**
     * 1:1 with the original's {@code TileEntityRBMKControl.onMelt}: 2-3 GRAPHITE debris if
     * moderated, plus 2-3 ROD debris always, before the standard melt. Was entirely missing -
     * control rods melted down without dropping any debris at all.
     */
    @Override
    public void onMelt(Level level, int reduce) {
        if (isModerated()) {
            int graphiteCount = 2 + level.random.nextInt(2);
            for (int i = 0; i < graphiteCount; i++) spawnDebris(level, "graphite");
        }
        int rodCount = 2 + level.random.nextInt(2);
        for (int i = 0; i < rodCount; i++) spawnDebris(level, "rod");
        standardMelt(level, reduce);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_control"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKControlMenu(id, inv, this); }

    @Override
    public CompoundTag getNBTForConsole() {
        CompoundTag d = new CompoundTag();
        d.putDouble("level", level);
        d.putShort("color", color);
        return d;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("level", level);
        tag.putDouble("lastLevel", lastLevel);
        tag.putDouble("targetLevel", targetLevel);
        tag.putShort("color", color);
        tag.putLong("power", power);
        tag.putBoolean("hasPower", hasPower);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        level       = tag.getDouble("level");
        // lastLevel was never sent to the client before (this same saveAdditional/load pair
        // backs both disk NBT and the per-tick network sync packet) - the renderer's
        // level/lastLevel partial-tick interpolation was lerping toward a client-side value
        // that never updated, then snapping back to it at every tick boundary. That snap-back,
        // repeated 20x/second, is the "twerking" bounce.
        lastLevel   = tag.contains("lastLevel") ? tag.getDouble("lastLevel") : level;
        targetLevel = tag.getDouble("targetLevel");
        color       = tag.getShort("color");
        power       = tag.getLong("power");
        hasPower    = !tag.contains("hasPower") || tag.getBoolean("hasPower");
    }
}

