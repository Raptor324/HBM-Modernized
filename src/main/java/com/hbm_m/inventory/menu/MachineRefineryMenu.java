package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineRefineryBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class MachineRefineryMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 13;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineRefineryBlockEntity blockEntity;

    public MachineRefineryMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRefineryMenu(int id, Inventory inventory, MachineRefineryBlockEntity blockEntity) {
        super(ModMenuTypes.REFINERY_MENU.get(), id);
        this.blockEntity = blockEntity;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку,
        // чтобы конструктор дошёл до конца и пакет открытия меню не уронил клиент
        var machineContainer = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : null);

        // Original ContainerMachineRefinery: Positionen 1:1
        // Battery
        this.addSlot(new Slot(machineContainer, 0, 158, 108));
        // Canister input/output
        this.addSlot(new Slot(machineContainer, 1, 12, 90));
        this.addSlot(new TakeOnlySlot(machineContainer, 2, 12, 108));
        // Heavy oil input/output
        this.addSlot(new Slot(machineContainer, 3, 64, 90));
        this.addSlot(new TakeOnlySlot(machineContainer, 4, 64, 108));
        // Naphtha input/output
        this.addSlot(new Slot(machineContainer, 5, 82, 90));
        this.addSlot(new TakeOnlySlot(machineContainer, 6, 82, 108));
        // Light oil input/output
        this.addSlot(new Slot(machineContainer, 7, 100, 90));
        this.addSlot(new TakeOnlySlot(machineContainer, 8, 100, 108));
        // Petroleum/Gas input/output
        this.addSlot(new Slot(machineContainer, 9, 118, 90));
        this.addSlot(new TakeOnlySlot(machineContainer, 10, 118, 108));
        // Sulfur output
        this.addSlot(new TakeOnlySlot(machineContainer, 11, 38, 90));
        // Fluid ID slot
        this.addSlot(new Slot(machineContainer, 12, 38, 108));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 11 + col * 18, 158 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 11 + col * 18, 216));
        }
    }

    public static MachineRefineryMenu create(int id, Inventory inventory, MachineRefineryBlockEntity blockEntity) {
        return new MachineRefineryMenu(id, inventory, blockEntity);
    }

    private static MachineRefineryBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineRefineryBlockEntity refineryBlockEntity) {
            return refineryBlockEntity;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No MachineRefineryBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":refinery_menu");
    }

    public MachineRefineryBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // audit13: Original isUseableByPlayer (<= 128 zur Kernmitte) oder Huelle <= 64; Vanilla 64 schloss die GUI an grossen Maschinen
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 1, false)
                    && !this.moveItemStackTo(stack, 1, 2, false)
                    && !this.moveItemStackTo(stack, 3, 4, false)
                    && !this.moveItemStackTo(stack, 5, 6, false)
                    && !this.moveItemStackTo(stack, 7, 8, false)
                    && !this.moveItemStackTo(stack, 9, 10, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }
}