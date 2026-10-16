package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineElectricFurnaceBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot-Koordinaten (176x186 Textur) 1:1 aus {@code ContainerElectricFurnace} (1.7.10 Original)
 *  uebernommen: Batterie (152,54), Input (20,35), Output (80,35). Der Upgrade-Slot (152,20) des
 *  Originals entfaellt - siehe {@link MachineElectricFurnaceBlockEntity}. */
public class MachineElectricFurnaceMenu extends AbstractContainerMenu {

    public final MachineElectricFurnaceBlockEntity blockEntity;
    private final ContainerData data;

    private static final int SLOT_BATTERY = MachineElectricFurnaceBlockEntity.SLOT_BATTERY;
    private static final int SLOT_INPUT = MachineElectricFurnaceBlockEntity.SLOT_INPUT;
    private static final int SLOT_OUTPUT = MachineElectricFurnaceBlockEntity.SLOT_OUTPUT;
    private static final int SLOT_UPGRADE = MachineElectricFurnaceBlockEntity.SLOT_UPGRADE;
    private static final int MACHINE_SLOT_COUNT = 4;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_HAS_POWER = 2;
    private static final int DATA_COUNT = 3;

    public MachineElectricFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData), new SimpleContainerData(DATA_COUNT));
    }

    public MachineElectricFurnaceMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), id);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = (MachineElectricFurnaceBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_BATTERY, 152, 54));
        this.addSlot(new Slot(container, SLOT_INPUT, 20, 35));
        this.addSlot(new OutputSlot(container, SLOT_OUTPUT, 80, 35));
        // ContainerElectricFurnace: SlotUpgrade at (111, 34).
        this.addSlot(new Slot(container, SLOT_UPGRADE, 111, 34) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }

        addDataSlots(data);
    }

    private static MachineElectricFurnaceBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineElectricFurnaceBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineElectricFurnaceBlockEntity found at " + pos);
    }

    public int getProgress() { return data.get(DATA_PROGRESS); }
    public int getMaxProgress() { return Math.max(1, data.get(DATA_MAX_PROGRESS)); }
    public boolean hasPower() { return data.get(DATA_HAS_POWER) != 0; }

    public int getCookProgressScaled(int scale) {
        return getProgress() * scale / getMaxProgress();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.ELECTRIC_FURNACE.get());
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
            } else {
                if (!this.moveItemStackTo(slotStack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
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

    private static class OutputSlot extends Slot {
        public OutputSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
