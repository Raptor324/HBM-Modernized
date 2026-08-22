package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.menu.RBMKAutoloaderMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
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
 * Autoloader: stores fuel rods and automatically loads/unloads adjacent loadable RBMK columns.
 */
public class RBMKAutoloaderBlockEntity extends RBMKColumnBlockEntity implements MenuProvider {

    /**
     * 1:1 with {@code TileEntityRBMKAutoloader}: eighteen slots, split in two halves. Slots 0-8
     * hold fresh rods waiting to go in, slots 9-17 collect the spent ones pulled back out. This
     * port previously had a single nine-slot buffer used for both directions, so recovered rods
     * were dropped back into the same pool the loader feeds from and could be re-inserted.
     */
    public static final int SLOTS = 18;
    public static final int INPUT_SLOTS = 9;
    public final ItemStack[] slots = new ItemStack[SLOTS];

    /**
     * Minimum enrichment, in percent, a rod must still have to be worth loading - and equally the
     * point below which a rod in the reactor counts as spent. The original exposes this as a
     * per-machine setting; the default is 50.
     */
    public int cycle = 50;

    private int cooldown = 0;
    private static final int COOLDOWN_TICKS = 20;

    /** Original hasFuel(): is there anything in the input half still rich enough to load? */
    public boolean hasFuel() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = slots[i];
            if (!stack.isEmpty() && stack.getItem() instanceof RBMKRodItem
                    && RBMKRodItem.getEnrichment(stack) * 100 >= cycle) return true;
        }
        return false;
    }

    /** Original hasSpace(): is there room left in the output half for a spent rod? */
    public boolean hasSpace() {
        for (int i = INPUT_SLOTS; i < SLOTS; i++) if (slots[i].isEmpty()) return true;
        return false;
    }

    /** Original isItemValidForSlot: only rich-enough rods, and only into the input half. */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < INPUT_SLOTS && stack.getItem() instanceof RBMKRodItem
                && RBMKRodItem.getEnrichment(stack) * 100 >= cycle;
    }

    public RBMKAutoloaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_AUTOLOADER_BE.get(), pos, state);
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKAutoloaderBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        if (--be.cooldown > 0) return;
        be.cooldown = COOLDOWN_TICKS;

        // The original drives the column directly BELOW the loader, not its horizontal
        // neighbours - the autoloader sits on top of a fuel channel like a cap.
        if (!(level.getBlockEntity(pos.below()) instanceof IRBMKLoadable loadable)) return;

        boolean coldEnough = !(loadable instanceof RBMKRodBlockEntity rodBE) || rodBE.coldEnoughForAutoloader();
        if (!coldEnough) return;

        // Pull the spent rod out into the output half first, so the freed channel can be refilled
        // in the same pass - same order as the original.
        if (loadable.canUnload() && be.hasSpace()) {
            ItemStack spent = loadable.provideNext();
            if (!spent.isEmpty()) {
                for (int i = INPUT_SLOTS; i < SLOTS; i++) {
                    if (be.slots[i].isEmpty()) {
                        be.slots[i] = spent.copy();
                        loadable.unload();
                        be.setChanged();
                        break;
                    }
                }
            }
        }

        // Then feed a fresh rod in, but only one still above the cycle threshold.
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = be.slots[i];
            if (stack.isEmpty() || !(stack.getItem() instanceof RBMKRodItem)) continue;
            if (RBMKRodItem.getEnrichment(stack) * 100 < be.cycle) continue;
            if (!loadable.canLoad(stack)) continue;

            loadable.load(stack);
            be.slots[i] = ItemStack.EMPTY;
            be.setChanged();
            break;
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_autoloader"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKAutoloaderMenu(id, inv, this); }
    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.STORAGE; }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots[i].isEmpty()) {
                CompoundTag s = new CompoundTag();
                s.putByte("s", (byte) i);
                s.put("item", safeItemSave(slots[i]));
                list.add(s);
            }
        }
        tag.put("slots", list);
        tag.putInt("cycle", cycle);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("cycle")) cycle = tag.getInt("cycle");
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
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots[i].isEmpty()) {
                CompoundTag s = new CompoundTag();
                s.putByte("s", (byte) i);
                s.put("item", safeItemSave(slots[i], registries));
                list.add(s);
            }
        }
        tag.put("slots", list);
        tag.putInt("cycle", cycle);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("cycle")) cycle = tag.getInt("cycle");
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
        ListTag list = tag.getList("slots", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            int idx = s.getByte("s") & 0xFF;
            if (idx < SLOTS && s.contains("item"))
                slots[idx] = ItemStack.parseOptional(registries, s.getCompound("item"));
        }
    }
    *///?}
}

