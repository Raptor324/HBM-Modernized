package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.machines.MachineCraneBoxerBlock;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;
import com.hbm_m.inventory.menu.MachineCraneBoxerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crane Boxer - Port von {@code TileEntityCraneBoxer} (1.7.10 Original). 21-Slot-Puffer, der
 * Items von jeder Seite annimmt ({@link IEnterableBlock}) und sie je nach Modus zu einem
 * {@link MovingConveyorPackageEntity} buendelt: MODE_4/8/16 buendeln N volle Stacks, sobald genug
 * vorhanden sind; MODE_REDSTONE buendelt ALLE belegten Slots bei steigender Redstone-Flanke.
 */
public class MachineCraneBoxerBlockEntity extends BaseMachineBlockEntity implements IEnterableBlock {

    public static final int INVENTORY_SIZE = 21;

    public static final byte MODE_4 = 0;
    public static final byte MODE_8 = 1;
    public static final byte MODE_16 = 2;
    public static final byte MODE_REDSTONE = 3;

    private byte mode = MODE_4;
    private boolean lastRedstone = false;

    public MachineCraneBoxerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_BOXER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneBoxerBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos, state);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean redstone = level.hasNeighborSignal(pos);

        if (mode == MODE_REDSTONE) {
            if (redstone && !lastRedstone) {
                packAll(level, pos, state);
            }
        } else if (level.getGameTime() % 2 == 0) {
            int packSize = switch (mode) {
                case MODE_8 -> 8;
                case MODE_16 -> 16;
                default -> 4;
            };
            packFullStacks(level, pos, state, packSize);
        }

        lastRedstone = redstone;
        setChanged();
    }

    private void packAll(Level level, BlockPos pos, BlockState state) {
        List<ItemStack> collected = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                collected.add(stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        if (!collected.isEmpty()) {
            sendPackage(level, pos, state, collected.toArray(new ItemStack[0]));
        }
    }

    private void packFullStacks(Level level, BlockPos pos, BlockState state, int packSize) {
        int fullStacks = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) fullStacks++;
        }
        if (fullStacks < packSize) return;

        List<ItemStack> collected = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE && collected.size() < packSize; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) {
                collected.add(stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        sendPackage(level, pos, state, collected.toArray(new ItemStack[0]));
    }

    private void sendPackage(Level level, BlockPos pos, BlockState state, ItemStack[] box) {
        Direction facing = state.getValue(MachineCraneBoxerBlock.FACING);
        BlockPos outPos = pos.relative(facing);
        var outBlock = level.getBlockState(outPos).getBlock();

        if (outBlock instanceof IConveyorBelt belt) {
            var snap = belt.snapNewItem(level, outPos, new net.minecraft.world.phys.Vec3(
                    outPos.getX() + 0.5, outPos.getY() + 0.5, outPos.getZ() + 0.5));
            MovingConveyorPackageEntity moving = MovingConveyorPackageEntity.create(level, snap.x, snap.y, snap.z, box);
            level.addFreshEntity(moving);
            return;
        }

        for (ItemStack stack : box) {
            ItemEntity drop = new ItemEntity(level, outPos.getX() + 0.5, outPos.getY() + 0.5, outPos.getZ() + 0.5, stack);
            level.addFreshEntity(drop);
        }
    }

    // ── IEnterableBlock (item input from any side) ──────────────────────────

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity item) {
        if (level.isClientSide) return;

        ItemStack incoming = item.getItem().copy();
        ItemStack remainder = insertIntoBuffer(incoming);

        if (remainder.isEmpty()) {
            item.discard();
        } else if (remainder.getCount() != incoming.getCount()) {
            item.discard();
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder);
            level.addFreshEntity(drop);
        }
        setChanged();
    }

    private ItemStack insertIntoBuffer(ItemStack stack) {
        for (int i = 0; i < INVENTORY_SIZE && !stack.isEmpty(); i++) {
            ItemStack current = inventory.getStackInSlot(i);
            if (current.isEmpty()) {
                inventory.setStackInSlot(i, stack);
                return ItemStack.EMPTY;
            } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, stack)) {
                int space = current.getMaxStackSize() - current.getCount();
                if (space > 0) {
                    int toMove = Math.min(space, stack.getCount());
                    current.grow(toMove);
                    stack.shrink(toMove);
                }
            }
        }
        return stack;
    }

    // ── Mode toggle ──────────────────────────────────────────────────────────

    public byte getMode() { return mode; }
    public void nextMode() { mode = (byte) ((mode + 1) % 4); setChanged(); }

    // ── NBT ─────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putByte("mode", mode);
        tag.putBoolean("lastRedstone", lastRedstone);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putByte("mode", mode);
    tag.putBoolean("lastRedstone", lastRedstone);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        mode = tag.getByte("mode");
        lastRedstone = tag.getBoolean("lastRedstone");
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crane_boxer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneBoxerMenu.create(id, inventory, this);
    }
}
