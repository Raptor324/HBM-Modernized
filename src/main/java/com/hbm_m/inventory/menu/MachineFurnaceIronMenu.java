package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFurnaceIronBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot-Koordinaten (176x166 Textur) 1:1 aus {@code ContainerFurnaceIron} (1.7.10 Original)
 *  uebernommen: Input (53,17), Brennstoff (53,53)+(71,53), Output (125,35), Upgrade (17,35). */
public class MachineFurnaceIronMenu extends AbstractContainerMenu {

    public final MachineFurnaceIronBlockEntity blockEntity;
    private final ContainerData data;

    private static final int SLOT_INPUT = MachineFurnaceIronBlockEntity.SLOT_INPUT;
    private static final int SLOT_FUEL_1 = MachineFurnaceIronBlockEntity.SLOT_FUEL_1;
    private static final int SLOT_FUEL_2 = MachineFurnaceIronBlockEntity.SLOT_FUEL_2;
    private static final int SLOT_OUTPUT = MachineFurnaceIronBlockEntity.SLOT_OUTPUT;
    private static final int MACHINE_SLOT_COUNT = 5;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    private static final int DATA_LIT_TIME = 0;
    private static final int DATA_LIT_DURATION = 1;
    private static final int DATA_PROGRESS = 2;

    public MachineFurnaceIronMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData), new SimpleContainerData(MachineFurnaceIronBlockEntity.DATA_COUNT));
    }

    public MachineFurnaceIronMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FURNACE_IRON_MENU.get(), id);
        checkContainerDataCount(data, MachineFurnaceIronBlockEntity.DATA_COUNT);
        this.blockEntity = (MachineFurnaceIronBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_INPUT, 53, 17));
        this.addSlot(new FuelSlot(container, SLOT_FUEL_1, 53, 53));
        this.addSlot(new FuelSlot(container, SLOT_FUEL_2, 71, 53));
        this.addSlot(new OutputSlot(container, SLOT_OUTPUT, 125, 35));
        // Original ContainerFurnaceIron: SlotUpgrade at (17, 35).
        this.addSlot(new Slot(container, MachineFurnaceIronBlockEntity.SLOT_UPGRADE, 17, 35) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    private static MachineFurnaceIronBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFurnaceIronBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineFurnaceIronBlockEntity found at " + pos);
    }

    public int getLitTime() { return data.get(DATA_LIT_TIME); }
    public int getLitDuration() { return data.get(DATA_LIT_DURATION); }
    public int getProgress() { return data.get(DATA_PROGRESS); }

    public int getBurnProgressScaled(int scale) {
        return getLitTime() * scale / Math.max(getLitDuration(), 1);
    }

    public int getProcessingTime() { return data.get(3); }
    public boolean canSmelt() { return data.get(4) != 0; }

    /** Original: {@code progress * 70 / max(processingTime, 1)}. */
    public int getCookProgressScaled(int scale) {
        return getProgress() * scale / Math.max(getProcessingTime(), 1);
    }

    public boolean isLit() {
        return getLitTime() > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, 5, false)) { // Original: mergeItemStack(0, 5)
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }

    private static class FuelSlot extends Slot {
        public FuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return MachineFurnaceIronBlockEntity.isFuel(stack);
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
