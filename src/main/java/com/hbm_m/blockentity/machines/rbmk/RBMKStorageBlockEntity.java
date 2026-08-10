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

        // Compact towards slot 0 every 10 ticks
        if (level.getGameTime() % 10 == 0) {
            for (int i = 0; i < SLOTS - 1; i++) {
                if (slots_empty(be.slots[i]) && !slots_empty(be.slots[i + 1])) {
                    be.slots[i]     = be.slots[i + 1];
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
        return !s.isEmpty() && s.getItem() instanceof RBMKRodItem && slots_empty(slots[11]);
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

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots_empty(slots[i])) {
                CompoundTag s = new CompoundTag();
                s.putByte("s", (byte) i);
                s.put("item", safeItemSave(slots[i]));
                list.add(s);
            }
        }
        tag.put("slots", list);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots_empty(slots[i])) {
                CompoundTag s = new CompoundTag();
                s.putByte("s", (byte) i);
                s.put("item", safeItemSave(slots[i]));
                list.add(s);
            }
        }
        tag.put("slots", list);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
        ListTag list = tag.getList("slots", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            int idx = s.getByte("s") & 0xFF;
            if (idx < SLOTS && s.contains("item"))
                slots[idx] = ItemStack.of(s.getCompound("item"));
        }
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
        ListTag list = tag.getList("slots", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            int idx = s.getByte("s") & 0xFF;
            if (idx < SLOTS && s.contains("item"))
                slots[idx] = ItemStack.of(s.getCompound("item"));
        }
    
    }
    *///?}
}
