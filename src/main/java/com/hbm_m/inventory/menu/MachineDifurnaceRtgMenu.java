package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineDifurnaceRtgBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Port of {@code ContainerMachineDiFurnaceRTG} (1.7.10 Original). */
public class MachineDifurnaceRtgMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineDifurnaceRtgBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineDifurnaceRtgBlockEntity blockEntity;

    public MachineDifurnaceRtgMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineDifurnaceRtgMenu(int id, Inventory inventory, MachineDifurnaceRtgBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_DIFURNACE_RTG_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, MachineDifurnaceRtgBlockEntity.SLOT_INPUT_TOP, 56, 17));
        this.addSlot(new Slot(container, MachineDifurnaceRtgBlockEntity.SLOT_INPUT_BOTTOM, 56, 53));
        this.addSlot(new Slot(container, MachineDifurnaceRtgBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int i = 0; i < MachineDifurnaceRtgBlockEntity.PELLET_SLOT_COUNT; i++) {
            this.addSlot(new Slot(container, MachineDifurnaceRtgBlockEntity.PELLET_SLOT_START + i, 8 + i * 18, 71));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 161));
        }
    }

    public static MachineDifurnaceRtgMenu create(int id, Inventory inventory, MachineDifurnaceRtgBlockEntity blockEntity) {
        return new MachineDifurnaceRtgMenu(id, inventory, blockEntity);
    }

    private static MachineDifurnaceRtgBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineDifurnaceRtgBlockEntity difurnace) {
            return difurnace;
        }
        throw new IllegalStateException("No MachineDifurnaceRtgBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":machine_difurnace_rtg_menu");
    }

    public MachineDifurnaceRtgBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return result;
    }
}
