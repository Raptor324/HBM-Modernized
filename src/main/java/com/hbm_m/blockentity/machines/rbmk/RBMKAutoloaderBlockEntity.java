package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
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

    public static final int SLOTS = 9;
    public final ItemStack[] slots = new ItemStack[SLOTS];
    private int cooldown = 0;
    private static final int COOLDOWN_TICKS = 20;

    public RBMKAutoloaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_AUTOLOADER_BE.get(), pos, state);
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKAutoloaderBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        if (--be.cooldown > 0) return;
        be.cooldown = COOLDOWN_TICKS;

        // Try to load/unload rods from all 4 horizontal neighbors
        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof IRBMKLoadable loadable) {
                // Unload depleted rod if column has one and it is cool enough
                if (loadable.canUnload()
                        && (!(loadable instanceof RBMKRodBlockEntity rodBE) || rodBE.coldEnoughForAutoloader())) {
                    ItemStack rod = loadable.provideNext();
                    if (rod.getItem() instanceof RBMKRodItem && RBMKRodItem.getEnrichment(rod) < 0.05) {
                        // Take depleted rod if we have a slot
                        for (int i = 0; i < SLOTS; i++) {
                            if (be.slots[i].isEmpty()) {
                                be.slots[i] = rod.copy();
                                loadable.unload();
                                be.setChanged();
                                break;
                            }
                        }
                    }
                }
                // Load a fresh rod if column is empty
                if (loadable.canLoad(ItemStack.EMPTY.copy()) || !loadable.canUnload()) {
                    for (int i = 0; i < SLOTS; i++) {
                        if (!be.slots[i].isEmpty() && be.slots[i].getItem() instanceof RBMKRodItem
                                && RBMKRodItem.getEnrichment(be.slots[i]) > 0.1
                                && loadable.canLoad(be.slots[i])) {
                            loadable.load(be.slots[i]);
                            be.slots[i] = ItemStack.EMPTY;
                            be.setChanged();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_autoloader"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; } // TODO: dedicated menu
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
    }

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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

