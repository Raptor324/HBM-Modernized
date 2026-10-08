package com.hbm_m.blockentity.machines;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.annihilator.AnnihilatorPoolManager;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.menu.MachineAnnihilatorMenu;
import com.hbm_m.item.liquids.FluidIdentifierItem;

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
 * 1:1 {@code TileEntityMachineAnnihilator}: vernichtet alles in Slot 0 und den Inhalt des 2,5-Mio.-mB-Tanks und
 * zaehlt es im gewaehlten Pool ({@link AnnihilatorPoolManager}). Erreichte Meilensteine werden in die Ausgabeslots
 * 2-7 ausgezahlt; ein Gegenstand in Slot 9 wird einzeln vernichtet und zahlt dabei immer den hoechsten erreichten
 * Meilenstein nach Slot 10 aus. Slot 8 zeigt den Zaehlerstand (Item oder per Fluidkennung ein Fluid). Beim Vernichten
 * brennt oben eine Flamme, strahlende Gegenstaende verstrahlen den Schlot.
 */
public class MachineAnnihilatorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2,
        com.hbm_m.api.tile.IControlReceiver {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_MONITOR = 8;
    public static final int SLOT_REQUEST = 9;
    public static final int SLOT_PAYOUT = 10;
    private static final int SLOT_COUNT = 11;

    public String pool = "Recycling";
    public int timer;

    public final FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), 2_500_000);
    public BigInteger monitorBigInt = BigInteger.ZERO;

    public MachineAnnihilatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANNIHILATOR_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    /** Original {@code ForgeDirection.getOrientation(meta - 10)} - im Port die FACING-Richtung. */
    private Direction dir() {
        BlockState state = getBlockState();
        return state.hasProperty(com.hbm_m.block.machines.MachineAnnihilatorBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachineAnnihilatorBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAnnihilatorBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
    }

    private void serverTick(ServerLevel world) {

        ItemStack[] slots = slotsArray();
        if (this.tank.setType(SLOT_FLUID_ID, slots)) applySlots(slots);

        if (this.pool != null && !this.pool.isEmpty()) {

            for (DirPos con : getConPos()) {
                if (tank.getTankType() != ModFluids.NONE.getSource()) this.trySubscribe(tank.getTankType(), world, con.pos, con.dir);
            }

            AnnihilatorPoolManager data = AnnihilatorPoolManager.get(world);
            boolean didSomething = false;

            ItemStack trash = inventory.getStackInSlot(SLOT_INPUT);
            if (!trash.isEmpty()) {
                onDestroy(trash);
                tryAddPayout(data.pushToPool(pool, trash, false));
                inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
                this.setChanged();
                didSomething = true;
            }
            if (tank.getFill() > 0) {
                FT_Polluting.pollute(world, worldPosition, tank.getTankType(), FluidReleaseType.BURN, tank.getFill() * 2);
                tryAddPayout(data.pushToPool(pool, tank.getTankType(), tank.getFill(), false));
                tank.setFill(0);
                this.setChanged();
                didSomething = true;
            }

            if (didSomething) {
                Direction dir = dir();
                com.hbm_m.util.ParticleUtil.spawnGasFlame(world, worldPosition.getX() + 0.5 - dir.getStepX() * 3, worldPosition.getY() + 8.75,
                        worldPosition.getZ() + 0.5 - dir.getStepZ() * 3, world.random.nextGaussian() * 0.05, 0.1, world.random.nextGaussian() * 0.05);

                if (world.getGameTime() % 3 == 0)
                    world.playSound(null, worldPosition.getX() + 0.5 - dir.getStepX() * 3, worldPosition.getY() + 8.75, worldPosition.getZ() + 0.5 - dir.getStepZ() * 3,
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.flamethrowerShoot"), SoundSource.BLOCKS, 1F, 0.5F + world.random.nextFloat() * 0.25F);
            }

            ItemStack monitor = inventory.getStackInSlot(SLOT_MONITOR);
            if (!monitor.isEmpty()) {
                if (monitor.getItem() instanceof FluidIdentifierItem) {
                    Fluid type = FluidIdentifierItem.resolvePrimaryForTank(monitor);
                    monitor(data, type == null ? null : AnnihilatorPoolManager.fluidKey(type));
                } else {
                    monitor(data, AnnihilatorPoolManager.compKey(monitor));
                }
            }

            ItemStack request = inventory.getStackInSlot(SLOT_REQUEST);
            if (!request.isEmpty()) {
                ItemStack single = request.copy();
                single.setCount(1);
                onDestroy(single);
                ItemStack payout = data.pushToPool(pool, single, true);
                inventory.extractItem(SLOT_REQUEST, 1, false);
                if (payout != null) {
                    ItemStack out = inventory.getStackInSlot(SLOT_PAYOUT);
                    if (out.isEmpty()) {
                        inventory.setStackInSlot(SLOT_PAYOUT, payout);
                    } else if (ItemStack.isSameItemSameTags(out, payout) && out.getMaxStackSize() >= out.getCount() + payout.getCount()) {
                        out.grow(payout.getCount());
                        inventory.setStackInSlot(SLOT_PAYOUT, out);
                    }
                }
            }
        }

        sendUpdateToClient();
    }

    public void onDestroy(ItemStack stack) {
        float radiation = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
        if (radiation > 0) {
            Direction dir = dir();
            com.hbm_m.radiation.ChunkRadiationManager.incrementRad(level, worldPosition.getX() - dir.getStepX() * 3, worldPosition.getY() + 9,
                    worldPosition.getZ() - dir.getStepZ() * 3, Math.min(radiation * 5F, 1_000F));
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    public DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)
        BlockPos p = worldPosition;

        return new DirPos[] {
                new DirPos(p.relative(dir, 5), dir),
                new DirPos(p.relative(dir, 3).relative(rot, 2), rot),
                new DirPos(p.relative(dir, 3).relative(rot, -2), rot.getOpposite())
        };
    }

    public void monitor(AnnihilatorPoolManager data, @Nullable String key) {
        if (data.hasPool(this.pool) && key != null) {
            this.monitorBigInt = data.getOrNull(this.pool, key);
            if (this.monitorBigInt == null) this.monitorBigInt = BigInteger.ZERO;
        } else {
            this.monitorBigInt = BigInteger.ZERO;
        }
    }

    public void tryAddPayout(@Nullable ItemStack payout) {
        if (payout == null) return;

        for (int i = 2; i <= 7; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, payout) && slot.getMaxStackSize() >= slot.getCount() + payout.getCount()) {
                slot.grow(payout.getCount());
                inventory.setStackInSlot(i, slot);
                this.setChanged();
                return;
            }
        }

        for (int i = 2; i <= 7; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, payout);
                this.setChanged();
                return;
            }
        }
    }

    private ItemStack[] slotsArray() {
        ItemStack[] arr = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlots(ItemStack[] arr) {
        for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    /** Original: Muell (0), Fluidkennung (1), Monitor (8) und Auszahlungsanfrage (9) nehmen Gegenstaende an. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) return true;
        if (slot == SLOT_FLUID_ID && stack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) return true;
        if (slot == SLOT_MONITOR) return true;
        if (slot == SLOT_REQUEST) return true;
        return false;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public com.hbm_m.api.fluids.ConnectionPriority getFluidPriority() {
        return com.hbm_m.api.fluids.ConnectionPriority.LOW;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== Steuerung ====================

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("pool")) setPoolName(data.getString("pool"));
    }

    /** Original {@code receiveControl}: nur nicht leere Namen werden uebernommen. */
    public void setPoolName(String pool) {
        if (pool != null && !pool.isEmpty()) {
            this.pool = pool;
            this.setChanged();
            sendUpdateToClient();
        }
    }

    public String getPoolName() { return pool; }
    public FluidTank getTank() { return tank; }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "t");
        tag.putString("pool", pool == null ? "" : pool);
        tag.putByteArray("monitor", monitorBigInt.toByteArray());
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "t");
        pool = tag.getString("pool");
        byte[] mon = tag.getByteArray("monitor");
        monitorBigInt = mon.length == 0 ? BigInteger.ZERO : new BigInteger(mon);
    }

    // ==================== Menue ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.annihilator");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAnnihilatorMenu(containerId, playerInventory, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 5, worldPosition.getY(), worldPosition.getZ() - 5,
                worldPosition.getX() + 6, worldPosition.getY() + 8, worldPosition.getZ() + 6);
        return bb;
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0, 2-7}; Eingang hinein, 2-7 heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 2, 3, 4, 5, 6, 7 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 0 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot >= 2 && slot <= 7; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
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
