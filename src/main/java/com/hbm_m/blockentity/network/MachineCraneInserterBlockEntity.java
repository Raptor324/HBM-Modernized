package com.hbm_m.blockentity.network;

import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.block.machines.MachineCraneInserterBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
//?}

/**
 * Crane Inserter - Port von {@code TileEntityCraneInserter} (1.7.10 Original). 21-Slot-Puffer, der
 * Items von einem Foerderband annimmt ({@link IEnterableBlock}) und jeden Tick versucht, sie in
 * das Inventar auf der Ausgabeseite zu schieben (per Forge-{@code IItemHandler}-Capability statt
 * des Original-{@code ISidedInventory}-Masquerade-Systems - funktional aequivalent, moderner Weg).
 * Fallback auf Einzelitem-Einfuegung, falls der volle Stack nicht passt - 1:1 aus dem Original.
 * <p>
 * SCOPE-Entscheidung: Die Screwdriver-Ausgabeseite-Ueberschreibung (Sneak-Klick mit Schraubenzieher
 * auf eine andere Seite) und das Copy-Tool-Einstellungssystem entfallen - kein Copy-Tool in diesem
 * Port vorhanden, Ausgabeseite ist immer die der Eingabeseite (FACING) gegenueberliegende Seite.
 */
public class MachineCraneInserterBlockEntity extends BaseMachineBlockEntity implements IEnterableBlock {

    public static final int INVENTORY_SIZE = 21;
    public static final int SLOT_DESTROYER_TOGGLE = -1; // kein echter Slot, siehe GUI-Klick-Handling

    private boolean destroyer = true;

    public MachineCraneInserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_INSERTER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneInserterBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos, state);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.hasNeighborSignal(pos)) return;

        Direction outputSide = state.getValue(MachineCraneInserterBlock.FACING).getOpposite();
        BlockPos targetPos = pos.relative(outputSide);
        BlockEntity target = level.getBlockEntity(targetPos);
        if (target == null) return;

        //? if forge {
        IItemHandler handler = target.getCapability(ForgeCapabilities.ITEM_HANDLER, outputSide.getOpposite()).orElse(null);
        if (handler == null) return;

        boolean didSomething = false;
        for (int i = 0; i < INVENTORY_SIZE && !didSomething; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack.copy(), false);
            if (remainder.getCount() != stack.getCount()) {
                inventory.setStackInSlot(i, remainder);
                didSomething = true;
            }
        }

        // Fallback: single-item insertion, falls kein voller Stack passte (1:1 aus dem Original).
        if (!didSomething) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) continue;

                ItemStack single = stack.copy();
                single.setCount(1);
                ItemStack remainder = ItemHandlerHelper.insertItem(handler, single, false);
                if (remainder.isEmpty()) {
                    stack.shrink(1);
                    if (stack.isEmpty()) inventory.setStackInSlot(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
        //?}

        setChanged();
    }

    // ── IEnterableBlock ──────────────────────────────────────────────────────

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity item) {
        if (level.isClientSide) return;

        ItemStack incoming = item.getItem().copy();
        ItemStack remainder = insertIntoBuffer(incoming);

        if (remainder.isEmpty()) {
            item.discard();
        } else if (remainder.getCount() != incoming.getCount()) {
            // teilweise aufgenommen: Rest im Item-Entity belassen
            item.discard();
            if (!destroyer) {
                ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder);
                level.addFreshEntity(drop);
            }
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

    // ── Toggle ───────────────────────────────────────────────────────────────

    public boolean isDestroyer() { return destroyer; }
    public void toggleDestroyer() { destroyer = !destroyer; setChanged(); }

    // ── NBT ─────────────────────────────────────────────────────────────────

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

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crane_inserter");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneInserterMenu.create(id, inventory, this);
    }
}
