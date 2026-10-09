package com.hbm_m.blockentity.network;

import java.util.List;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneGrabberMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

//? if forge {
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * 1:1 {@code TileEntityCraneGrabber}: 9 Filter, Stapel- und Auswurf-Upgrade. Greift Teile vom Band vor dem Eingang
 * (Reichweite halb bzw. ein Drittel bei Doppel-/Dreifachband) und legt sie auf das Band am Ausgang oder setzt sie in das
 * Inventar dort ein.
 */
public class MachineCraneGrabberBlockEntity extends CraneBaseBlockEntity implements IControlReceiver, CraneBaseBlockEntity.ControlReceiverFilter {

    public static final int FILTER_START = 0;
    public static final int FILTER_END = 8;
    public static final int SLOT_UPGRADE_STACK = 9;
    public static final int SLOT_UPGRADE_EJECTOR = 10;
    public static final int INVENTORY_SIZE = 11;

    public boolean isWhitelist = false;
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(9);
    public long lastGrabbedTick = 0;

    public MachineCraneGrabberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_GRABBER_BE.get(), pos, state, INVENTORY_SIZE);
    }

    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) { return isItemValidForSlot(slot, stack); }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneGrabberBlockEntity be) {
        be.serverTick(level, pos, state);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        int delay = MachineCraneExtractorBlockEntity.delayFor(inventory.getStackInSlot(SLOT_UPGRADE_EJECTOR));

        if (level.getGameTime() >= lastGrabbedTick + delay && !level.hasNeighborSignal(pos)) {
            int amount = MachineCraneExtractorBlockEntity.amountFor(inventory.getStackInSlot(SLOT_UPGRADE_STACK));

            Direction inputSide = getInputSide();
            Direction outputSide = getOutputSide();
            IConveyorBelt belt = CraneInventoryUtil.beltAt(level, pos.relative(outputSide));

            double reach = 1D;
            if (inputSide.get3DDataValue() > 1) { // ignorieren, wenn nach oben oder unten gerichtet
                Block b = level.getBlockState(pos.relative(inputSide)).getBlock();
                if (b == ModBlocks.CONVEYOR_DOUBLE.get()) reach = 0.5D;
                if (b == ModBlocks.CONVEYOR_TRIPLE.get()) reach = 0.33D;
            }
            double x = pos.getX() + inputSide.getStepX() * reach;
            double y = pos.getY() + inputSide.getStepY() * reach;
            double z = pos.getZ() + inputSide.getStepZ() * reach;
            List<MovingConveyorItemEntity> items = level.getEntitiesOfClass(MovingConveyorItemEntity.class,
                    new AABB(x + 0.1875D, y + 0.1875D, z + 0.1875D, x + 0.8125D, y + 0.8125D, z + 0.8125D));

            if (belt != null) {
                for (MovingConveyorItemEntity item : items) {
                    if (item.isRemoved()) continue;
                    ItemStack stack = item.getItem();
                    boolean match = this.matchesFilter(stack);
                    if (this.isWhitelist && !match || !this.isWhitelist && match) continue;

                    lastGrabbedTick = level.getGameTime();
                    Vec3 snap = CraneInventoryUtil.snap(level, pos, outputSide, belt);
                    MovingConveyorItemEntity newItem = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, stack.copy());
                    item.discard();
                    level.addFreshEntity(newItem);
                    break;
                }
            } else {
                //? if forge {
                IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputSide), outputSide.getOpposite());
                if (te != null) {
                    for (MovingConveyorItemEntity item : items) {
                        ItemStack stack = item.getItem();
                        boolean match = this.matchesFilter(stack);
                        if (this.isWhitelist && !match || !this.isWhitelist && match) continue;

                        lastGrabbedTick = level.getGameTime();
                        ItemStack copy = stack.copy();
                        int toAdd = Math.min(stack.getCount(), amount);
                        copy.setCount(toAdd);
                        ItemStack ret = CraneInventoryUtil.addToInventory(te, copy);
                        int didAdd = toAdd - ret.getCount();
                        ItemStack left = stack.copy();
                        left.shrink(didAdd);
                        if (left.isEmpty()) {
                            item.discard();
                        } else {
                            item.setItem(left);
                        }
                        amount -= didAdd;
                        if (amount <= 0) break;
                    }
                }
                //?} elif neoforge {
                /*net.neoforged.neoforge.items.IItemHandler te = CraneInventoryUtil.inventoryAt(level, pos.relative(outputSide), outputSide.getOpposite());
                if (te != null) {
                    for (MovingConveyorItemEntity item : items) {
                        ItemStack stack = item.getItem();
                        boolean match = this.matchesFilter(stack);
                        if (this.isWhitelist && !match || !this.isWhitelist && match) continue;

                        lastGrabbedTick = level.getGameTime();
                        ItemStack copy = stack.copy();
                        int toAdd = Math.min(stack.getCount(), amount);
                        copy.setCount(toAdd);
                        ItemStack ret = CraneInventoryUtil.addToInventory(te, copy);
                        int didAdd = toAdd - ret.getCount();
                        ItemStack left = stack.copy();
                        left.shrink(didAdd);
                        if (left.isEmpty()) {
                            item.discard();
                        } else {
                            item.setItem(left);
                        }
                        amount -= didAdd;
                        if (amount <= 0) break;
                    }
                }
                *///?}
            }
        }

        sendUpdateToClient();
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (!filter.isEmpty() && this.matcher.isValidForFilter(filter, i, stack)) return true;
        }
        return false;
    }

    @Override
    public void nextMode(int i) {
        this.matcher.nextMode(i, inventory.getStackInSlot(i));
        setChanged();
        sendUpdateToClient();
    }

    @Override public int[] getFilterSlots() { return new int[] { 0, 9 }; }

    public ModulePatternMatcher getMatcher() { return matcher; }
    public boolean isWhitelist() { return isWhitelist; }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("whitelist")) this.isWhitelist = !this.isWhitelist;
        if (data.contains("slot")) setFilterContents(data);
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isWhitelist", isWhitelist);
        matcher.writeToNBT(tag);
        tag.putLong("lastGrabbedTick", lastGrabbedTick);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isWhitelist = tag.getBoolean("isWhitelist");
        matcher.readFromNBT(tag);
        lastGrabbedTick = tag.getLong("lastGrabbedTick");
    }

    /** Original: {@code TileEntityMachineBase}-Vorgaben - kein Automatisierungszugriff. */
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Override protected int[] getAccessibleSlots() { return new int[0]; }

    @Override protected Component getDefaultName() { return Component.translatable("container.craneGrabber"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneGrabberMenu.create(id, inventory, this);
    }
}
