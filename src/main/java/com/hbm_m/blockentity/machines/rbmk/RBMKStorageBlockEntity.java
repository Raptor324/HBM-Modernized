package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.menu.RBMKStorageMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
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

        // CE's compaction (TileEntityRBMKStorage.update): a single full pass every tick that walks
        // the slots and slides each occupied one down onto the lowest free index. The port used a
        // bubble-one-step-every-10-ticks variant instead, so a rack unloaded from the middle took
        // over a hundred ticks to close the gap that CE closes immediately - long enough for the
        // crane to read slot 0 as empty and skip the column entirely.
        int freeSlot = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (slots_empty(be.slots[i])) continue;
            if (slots_empty(be.slots[freeSlot])) {
                be.slots[freeSlot] = be.slots[i].copy();
                be.slots[i] = ItemStack.EMPTY;
            }
            freeSlot++;
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
                s.put("item", com.hbm_m.platform.PlatformHooks.safeItemSave(slots[i], registries));
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
                slots[idx] = com.hbm_m.platform.PlatformHooks.itemStackOf(s.getCompound("item"), registries);
        }
    }
}
