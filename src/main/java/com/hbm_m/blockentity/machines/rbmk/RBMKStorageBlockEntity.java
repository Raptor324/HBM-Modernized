package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.menu.RBMKStorageMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.hbm_m.platform.ModItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of TileEntityRBMKStorage.
 * 12 inventory slots, compacted towards slot 0 every 10 ticks.
 * IRBMKLoadable: load goes into slot 11, unload takes from slot 0.
 */
public class RBMKStorageBlockEntity extends RBMKColumnBlockEntity
        implements IRBMKLoadable, MenuProvider {

    public static final int SLOTS = 12;
    public final ItemStack[] slots = new ItemStack[SLOTS];

    public RBMKStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STORAGE_BE.get(), pos, state);
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKStorageBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        // 1:1 TileEntityRBMKStorage.updateEntity: compaction only runs every 10 ticks, and each
        // pass is a single bubble shift - an occupied slot slides down exactly one position per
        // pass, so a mid-rack gap closes gradually over several passes and the crane can observe
        // the half-closed gap during that window.
        if (level.getGameTime() % 10 == 0) {
            for (int i = 0; i < SLOTS - 1; i++) {
                if (slots_empty(be.slots[i]) && !slots_empty(be.slots[i + 1])) {
                    be.slots[i] = be.slots[i + 1];
                    be.slots[i + 1] = ItemStack.EMPTY;
                }
            }
        }
    }

    private static boolean slots_empty(ItemStack s) {
        return s == null || s.isEmpty();
    }

    // ─── IRBMKLoadable ────────────────────────────────────────────────────────

    @Override
    public boolean canLoad(ItemStack s) {
        // CE only checks that the incoming stack exists and slot 11 is free - the storage column is
        // a generic rack, not a fuel-only one.
        return !s.isEmpty() && slots_empty(slots[11]);
    }

    @Override
    public void load(ItemStack s) {
        slots[11] = s.copy();
        setChanged();
    }

    @Override
    public boolean canUnload() {
        return !slots_empty(slots[0]);
    }

    @Override
    public ItemStack provideNext() {
        return slots_empty(slots[0]) ? ItemStack.EMPTY : slots[0];
    }

    @Override
    public void unload() {
        slots[0] = ItemStack.EMPTY;
        setChanged();
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_storage"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKStorageMenu(id, inv, this); }
    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.STORAGE; }

    // ─── NBT ─────────────────────────────────────────────────────────────────

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots_empty(slots[i])) {
                CompoundTag s = new CompoundTag();
                s.putByte("s", (byte) i);
                s.put("item", com.hbm_m.platform.StackNbt.saveNew(slots[i], registries));
                list.add(s);
            }
        }
        tag.put("slots", list);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
        ListTag list = tag.getList("slots", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            int idx = s.getByte("s") & 0xFF;
            if (idx < SLOTS && s.contains("item"))
                slots[idx] = com.hbm_m.platform.StackNbt.parse(s.getCompound("item"), registries);
        }
    }

    // ─── Automation (hoppers/pipes) ──────────────────────────────────────────

    /** 1:1 {@code TileEntityRBMKStorage.isItemValidForSlot}: automation only takes RBMK rods. */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof RBMKRodItem;
    }

    /**
     * 1:1 {@code TileEntityRBMKStorage} automation surface. {@code getAccessibleSlotsFromSide}
     * exposes all 12 slots to pipes/hoppers from every side, {@code isItemValidForSlot} restricts
     * insertion to rods, {@code canExtractItem} allows extraction from every slot. Live view of the
     * raw {@link #slots} array - the tick, the menu and the NBT keep working on the same storage.
     */
    @Override
    public @org.jetbrains.annotations.Nullable Object getItemHandler(@org.jetbrains.annotations.Nullable Direction side) {
        return automationHandler;
    }

    private final ModItemStackHandler automationHandler = new ModItemStackHandler(SLOTS) {

        @Override
        public int getSlots() { return SLOTS; }

        @Override
        public ItemStack getStackInSlot(int slot) { return slots[slot]; }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            slots[slot] = stack;
            onContentsChanged(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) { return isItemValidForSlot(slot, stack); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValidForSlot(slot, stack)) return stack;

            ItemStack existing = slots[slot];
            int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItem(existing, stack) || existing.getCount() >= limit) return stack;
                int accepted = Math.min(stack.getCount(), limit - existing.getCount());
                if (!simulate) {
                    existing.grow(accepted);
                    onContentsChanged(slot);
                }
                return accepted >= stack.getCount() ? ItemStack.EMPTY
                                                    : stack.copyWithCount(stack.getCount() - accepted);
            }
            int accepted = Math.min(stack.getCount(), limit);
            if (accepted <= 0) return stack;
            if (!simulate) {
                slots[slot] = stack.copyWithCount(accepted);
                onContentsChanged(slot);
            }
            return accepted >= stack.getCount() ? ItemStack.EMPTY
                                                : stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // canExtractItem: every slot can be drained
            if (amount <= 0) return ItemStack.EMPTY;
            ItemStack existing = slots[slot];
            if (existing.isEmpty()) return ItemStack.EMPTY;
            int toExtract = Math.min(amount, existing.getCount());
            ItemStack extracted = existing.copyWithCount(toExtract);
            if (!simulate) {
                if (toExtract >= existing.getCount()) slots[slot] = ItemStack.EMPTY;
                else existing.shrink(toExtract);
                onContentsChanged(slot);
            }
            return extracted;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    //? if forge {
    // Forge exposes items only through the BE's own getCapability (ModCapabilities' generic
    // ItemHandler registration exists on NeoForge alone); same split as RBMKAutoloaderBlockEntity.
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional.of(() -> automationHandler).cast();
        }
        return super.getCapability(cap, side);
    }
    //?}
}
