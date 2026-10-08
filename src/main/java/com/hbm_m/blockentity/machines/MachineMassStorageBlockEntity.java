package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMassStorage} (1.7.10 Original) - a single-item-type stockpile with a big
 * integer counter instead of individual item stacks. Slot 0 = input (auto-consumed into the
 * counter), slot 1 = filter (defines/locks the accepted item, can't change once stockpile &gt; 0),
 * slot 2 = output buffer (auto-refilled from the counter).
 * <p>
 * <p>Es gibt ihn wie im Original in <b>vier Groessen</b>: Holz fasst hundert, Eisen zehntausend,
 * Desh hunderttausend und Stahl eine Million. Das ist der einzige Unterschied zwischen ihnen.</p>
 *
 * <p><b>Nicht portiert:</b> die AE2-Anbindung und das Redstone-Sperrsystem - beides hat in diesem
 * Port keine Entsprechung.
 */
public class MachineMassStorageBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.tile.ILockableTile, com.hbm_m.api.tile.IControlReceiver,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider, com.hbm_m.api.redstoneoverradio.IRORInteractive {

    /** Original TileEntityMassStorage extends TileEntityCrateBase extends TileEntityLockableBase: Stiftschloss. */
    public final com.hbm_m.api.tile.LockState lockState = new com.hbm_m.api.tile.LockState();

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FILTER = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int INVENTORY_SIZE = 3;

    /** Voreinstellung, falls der Blockzustand keine Stufe hergibt. */
    private static final long DEFAULT_CAPACITY = 1_000_000L;

    private long stockpile = 0L;
    /** Original {@code output}: nur wenn an, fuellt die Kiste den Ausgabeslot nach (Schalter im GUI). */
    public boolean output = false;

    public MachineMassStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_MASS_STORAGE_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMassStorageBlockEntity be) {
        if (level.isClientSide) return;

        ItemStack filter = be.inventory.getStackInSlot(SLOT_FILTER);
        if (filter.isEmpty()) return;
        Item type = filter.getItem();

        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
        long capacity = be.getCapacity();
        if (!input.isEmpty() && input.getItem() == type && be.stockpile < capacity) {
            long room = capacity - be.stockpile;
            int toConsume = (int) Math.min(input.getCount(), room);
            if (toConsume > 0) {
                input.shrink(toConsume);
                be.stockpile += toConsume;
                be.setChanged();
            }
        }

        ItemStack output = be.inventory.getStackInSlot(SLOT_OUTPUT);
        int maxStack = com.hbm_m.platform.ItemHooks.getItemMaxStackSize(type);
        int outputSpace = output.isEmpty() ? maxStack : (output.getItem() == type ? maxStack - output.getCount() : 0);
        if (be.output && outputSpace > 0 && be.stockpile > 0) {
            int toRelease = (int) Math.min(outputSpace, be.stockpile);
            if (toRelease > 0) {
                if (output.isEmpty()) {
                    be.inventory.setStackInSlot(SLOT_OUTPUT, new ItemStack(type, toRelease));
                } else {
                    output.grow(toRelease);
                }
                be.stockpile -= toRelease;
                be.setChanged();
            }
        }
        // Original networkPackNT: Vorrat fuer GUI-Balken und Tooltip an den Client
        if (be.stockpile != be.lastSyncedStockpile) {
            be.lastSyncedStockpile = be.stockpile;
            be.sendUpdateToClient();
        }
    }

    private long lastSyncedStockpile = -1L;

    /** Original {@code type} (Filterplatz) wird fuer die Frontanzeige (RenderMassStorage) mitgeschickt. */
    @Override
    protected boolean isCriticalSlot(int slot) {
        return slot == SLOT_FILTER || super.isCriticalSlot(slot);
    }

    /** Original {@code receiveControl}: "provide" gibt ein Item (Shift: einen Stapel) aus, "toggle" schaltet die Ausgabe. */
    @Override
    public void receiveControl(CompoundTag data) {
        ItemStack filter = this.inventory.getStackInSlot(SLOT_FILTER);
        if (data.contains("provide") && !filter.isEmpty()) {
            if (this.stockpile == 0) return;
            Item type = filter.getItem();
            int max = com.hbm_m.platform.ItemHooks.getItemMaxStackSize(type);
            int amount = data.getBoolean("provide") ? max : 1;
            amount = (int) Math.min(amount, this.stockpile);
            ItemStack out = this.inventory.getStackInSlot(SLOT_OUTPUT);
            if (!out.isEmpty() && out.getItem() != type) return;
            if (out.isEmpty()) {
                this.inventory.setStackInSlot(SLOT_OUTPUT, new ItemStack(type, amount));
            } else {
                amount = Math.min(amount, max - out.getCount());
                out.grow(amount);
            }
            this.stockpile -= amount;
        }
        if (data.contains("toggle")) {
            this.output = !this.output;
        }
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128.0D;
    }

    public long getStockpile() { return stockpile; }
    /** Original {@code setStockpile}: Vorrat aus dem Item beim Setzen. */
    public void setStockpile(long stockpile) { this.stockpile = stockpile; setChanged(); }
    /** Die Groesse dieser Kiste - sie steht am Block, nicht am Blockentity. */
    public long getCapacity() {
        return getBlockState().getBlock() instanceof com.hbm_m.block.machines.MachineMassStorageBlock storage
                ? storage.getTier().capacity : DEFAULT_CAPACITY;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_FILTER) {
            ItemStack current = this.inventory.getStackInSlot(SLOT_FILTER);
            return stockpile <= 0 || current.isEmpty() || current.getItem() == stack.getItem();
        }
        if (slot == SLOT_INPUT) {
            ItemStack filter = this.inventory.getStackInSlot(SLOT_FILTER);
            return filter.isEmpty() || filter.getItem() == stack.getItem();
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.mass_storage");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineMassStorageMenu.create(id, inventory, this);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("stockpile", stockpile);
        tag.putBoolean("output", output);
        lockState.write(tag);
    }

    public boolean canAccess(Player player) {
        return lockState.canAccess(level, player);
    }

    // ---- ILockableTile ----
    @Override public com.hbm_m.api.tile.LockState getLockState() { return lockState; }
    @Override public void lock() { lockState.lock(this); setChanged(); }
    @Override public void unlock() { lockState.isLocked = false; setChanged(); }
    @Override public void setPins(int pins) { lockState.lock = pins; setChanged(); }
    @Override public void setMod(double mod) { lockState.lockMod = mod; setChanged(); }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        stockpile = tag.getLong("stockpile");
        output = tag.getBoolean("output");
        if (tag.contains("lockMod")) lockState.read(tag);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0, 2}; nur die eingestellte Sorte hinein, Ausgabe heraus - beides nur ohne Schloss. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 2 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (isLocked() || slot != 0) return false;
                    net.minecraft.world.item.ItemStack type = inventory.getStackInSlot(SLOT_FILTER);
                    return type.isEmpty() || com.hbm_m.platform.PlatformHooks.isSameItemSameTags(type, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return !isLocked() && slot == 2; }
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

    // ── Redstone-over-Radio (1:1 TileEntityMassStorage) ──

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "type",
                PREFIX_VALUE + "fill",
                PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "toggleoutput",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name))        return "" + this.stockpile;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + this.stockpile * 100 / this.getCapacity();
        if ((PREFIX_VALUE + "type").equals(name)) {
            ItemStack type = inventory.getStackInSlot(SLOT_FILTER);
            if (type.isEmpty()) return "None";
            return type.getHoverName().getString();
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "toggleoutput").equals(name)) {
            this.output = !this.output;
            this.setChanged();
            this.sendUpdateToClient();
        }
        return null;
    }
}
