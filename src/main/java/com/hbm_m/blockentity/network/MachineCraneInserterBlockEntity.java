package com.hbm_m.blockentity.network;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineCraneInserterMenu;

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

//? if forge {
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * 1:1 {@code TileEntityCraneInserter}: 21 Pufferplaetze. Jeden Tick (ohne Redstone) wird der erste Stapel, von dem etwas in
 * das Zielinventar passt, eingesetzt; klappt das mit keinem ganzen Stapel, wird es einzeln versucht.
 */
public class MachineCraneInserterBlockEntity extends CraneBaseBlockEntity implements IControlReceiver {

    public static final int INVENTORY_SIZE = 21;

    public boolean destroyer = true;

    public MachineCraneInserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_INSERTER_BE.get(), pos, state, INVENTORY_SIZE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneInserterBlockEntity be) {
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        if (!level.hasNeighborSignal(pos)) {
            //? if forge {
            Direction outputSide = getOutputSide();
            IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputSide), outputSide.getOpposite());

            boolean didSomething = false;

            if (te != null) {
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStack ret = CraneInventoryUtil.addToInventory(te, stack.copy());
                        if (ret.isEmpty() || ret.getCount() != stack.getCount()) {
                            inventory.setStackInSlot(i, ret);
                            setChanged();
                            didSomething = true;
                            break;
                        }
                    }
                }

                // klappt es mit keinem ganzen Stapel, einzeln versuchen (Ziele mit Stapelgrenze)
                if (!didSomething) for (int i = 0; i < INVENTORY_SIZE; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStack single = stack.copy();
                        single.setCount(1);
                        ItemStack ret = CraneInventoryUtil.addToInventory(te, single.copy());
                        if (ret.isEmpty() || ret.getCount() != single.getCount()) {
                            inventory.extractItem(i, 1, false);
                            setChanged();
                            break;
                        }
                    }
                }
            }
            //?} elif neoforge {
            /*Direction outputSide = getOutputSide();
            net.neoforged.neoforge.items.IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputSide), outputSide.getOpposite());

            boolean didSomething = false;

            if (te != null) {
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStack ret = CraneInventoryUtil.addToInventory(te, stack.copy());
                        if (ret.isEmpty() || ret.getCount() != stack.getCount()) {
                            inventory.setStackInSlot(i, ret);
                            setChanged();
                            didSomething = true;
                            break;
                        }
                    }
                }

                // klappt es mit keinem ganzen Stapel, einzeln versuchen (Ziele mit Stapelgrenze)
                if (!didSomething) for (int i = 0; i < INVENTORY_SIZE; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStack single = stack.copy();
                        single.setCount(1);
                        ItemStack ret = CraneInventoryUtil.addToInventory(te, single.copy());
                        if (ret.isEmpty() || ret.getCount() != single.getCount()) {
                            inventory.extractItem(i, 1, false);
                            setChanged();
                            break;
                        }
                    }
                }
            }
            *///?}
        }

        sendUpdateToClient();
    }

    /** Original {@code CraneInserter.onItemEnter}. */
    public void accept(Level level, BlockPos pos, ItemStack toAdd) {
        //? if forge {
        Direction outputDirection = getOutputSide();
        if (!level.hasNeighborSignal(pos)) {
            IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputDirection), outputDirection.getOpposite());
            if (te != null) CraneInventoryUtil.addToInventory(te, toAdd);
        }
        if (!toAdd.isEmpty()) CraneInventoryUtil.addToInventory(inventory, toAdd);
        //?} elif neoforge {
        /*Direction outputDirection = getOutputSide();
        if (!level.hasNeighborSignal(pos)) {
            net.neoforged.neoforge.items.IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputDirection), outputDirection.getOpposite());
            if (te != null) CraneInventoryUtil.addToInventory(te, toAdd);
        }
        if (!toAdd.isEmpty()) CraneInventoryUtil.addToInventory(inventory, toAdd);
        *///?}
        if (!toAdd.isEmpty() && !destroyer) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toAdd.copy()));
        }
        setChanged();
    }

    /** Original {@code CraneInserter.onPackageEnter}. */
    public void acceptAll(Level level, BlockPos pos, ItemStack[] toAdd) {
        //? if forge {
        Direction outputDirection = getOutputSide();
        if (!level.hasNeighborSignal(pos)) {
            IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputDirection), outputDirection.getOpposite());
            if (te != null) for (ItemStack stack : toAdd) CraneInventoryUtil.addToInventory(te, stack);
        }
        for (ItemStack stack : toAdd) {
            if (!stack.isEmpty()) CraneInventoryUtil.addToInventory(inventory, stack);
            if (!stack.isEmpty() && !destroyer) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy()));
            }
        }
        //?} elif neoforge {
        /*Direction outputDirection = getOutputSide();
        if (!level.hasNeighborSignal(pos)) {
            net.neoforged.neoforge.items.IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputDirection), outputDirection.getOpposite());
            if (te != null) for (ItemStack stack : toAdd) CraneInventoryUtil.addToInventory(te, stack);
        }
        for (ItemStack stack : toAdd) {
            if (!stack.isEmpty()) CraneInventoryUtil.addToInventory(inventory, stack);
            if (!stack.isEmpty() && !destroyer) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy()));
            }
        }
        *///?}
        setChanged();
    }

    public boolean isDestroyer() { return destroyer; }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("destroyer")) this.destroyer = !this.destroyer;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("destroyer", destroyer);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        destroyer = tag.getBoolean("destroyer");
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    @Override protected boolean canExtractItem(int slot, ItemStack stack) { return true; }

    @Override protected Component getDefaultName() { return Component.translatable("container.craneInserter"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneInserterMenu.create(id, inventory, this);
    }
}
