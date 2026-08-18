package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemPlateFuel;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineReactorResearchMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineReactorResearchBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START  = MACHINE_SLOT_COUNT;

    private static final int[][] SLOT_POS = {
            {95, 22}, {131, 22}, {77, 40}, {113, 40}, {149, 40},
            {95, 58}, {131, 58}, {77, 76}, {113, 76}, {149, 76},
            {95, 94}, {131, 94}
    };

    private final MachineReactorResearchBlockEntity blockEntity;

    public MachineReactorResearchMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineReactorResearchMenu(int id, Inventory inventory, MachineReactorResearchBlockEntity blockEntity) {
        super(ModMenuTypes.REACTOR_RESEARCH_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int i = 0; i < MACHINE_SLOT_COUNT; i++) {
            this.addSlot(new Slot(container, i, SLOT_POS[i][0], SLOT_POS[i][1]));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }
    }

    public static MachineReactorResearchMenu create(int id, Inventory inventory, MachineReactorResearchBlockEntity blockEntity) {
        return new MachineReactorResearchMenu(id, inventory, blockEntity);
    }

    private static MachineReactorResearchBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineReactorResearchBlockEntity reactorBlockEntity) {
            return reactorBlockEntity;
        }
        throw new IllegalStateException("No MachineReactorResearchBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":reactor_research_menu");
    }

    public MachineReactorResearchBlockEntity getBlockEntity() {
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
        } else if (stack.getItem() instanceof ItemPlateFuel) {
            if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
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
