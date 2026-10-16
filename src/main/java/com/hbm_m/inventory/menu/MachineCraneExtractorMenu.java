package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.network.MachineCraneExtractorBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineCraneExtractorMenu extends AbstractContainerMenu {

    private static final int FILTER_START = MachineCraneExtractorBlockEntity.FILTER_START;
    private static final int FILTER_END = MachineCraneExtractorBlockEntity.FILTER_END;
    private static final int BUFFER_START = MachineCraneExtractorBlockEntity.BUFFER_START;
    private static final int BUFFER_END = MachineCraneExtractorBlockEntity.BUFFER_END;
    private static final int SLOT_UPGRADE_STACK = MachineCraneExtractorBlockEntity.SLOT_UPGRADE_STACK;
    private static final int SLOT_UPGRADE_EJECTOR = MachineCraneExtractorBlockEntity.SLOT_UPGRADE_EJECTOR;
    private static final int MACHINE_SLOT_COUNT = MachineCraneExtractorBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineCraneExtractorBlockEntity blockEntity;

    public MachineCraneExtractorMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCraneExtractorMenu(int id, Inventory inventory, MachineCraneExtractorBlockEntity blockEntity) {
        super(ModMenuTypes.CRANE_EXTRACTOR_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // filter (3x3) - placing/removing an item here (re)initializes its match mode, like the original.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int filterIndex = col + row * 3 + FILTER_START;
                this.addSlot(new Slot(container, filterIndex, 71 + col * 18, 17 + row * 18) {
                    @Override
                    public void set(ItemStack stack) {
                        super.set(stack);
                        blockEntity.getMatcher().initPattern(filterIndex, stack);
                    }
                });
            }
        }
        // buffer (3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, col + row * 3 + BUFFER_START, 8 + col * 18, 17 + row * 18));
            }
        }
        // upgrades
        this.addSlot(new Slot(container, SLOT_UPGRADE_STACK, 152, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.STACK;
            }
        });
        this.addSlot(new Slot(container, SLOT_UPGRADE_EJECTOR, 152, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.EJECTOR;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 26 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 26 + col * 18, 161));
        }
    }

    public static MachineCraneExtractorMenu create(int id, Inventory inventory, MachineCraneExtractorBlockEntity blockEntity) {
        return new MachineCraneExtractorMenu(id, inventory, blockEntity);
    }

    private static MachineCraneExtractorBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCraneExtractorBlockEntity extractorBlockEntity) {
            return extractorBlockEntity;
        }
        throw new IllegalStateException("No MachineCraneExtractorBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":crane_extractor_menu");
    }

    public MachineCraneExtractorBlockEntity getBlockEntity() {
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
        // filter slots (0-8) are never shift-click transferable, matching the original.
        if (index < FILTER_START || index > FILTER_END) {
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
                if (stack.getItem() instanceof ItemMachineUpgrade up) {
                    boolean moved = up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.STACK
                            ? this.moveItemStackTo(stack, SLOT_UPGRADE_STACK, SLOT_UPGRADE_STACK + 1, false)
                            : up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.EJECTOR
                                    ? this.moveItemStackTo(stack, SLOT_UPGRADE_EJECTOR, SLOT_UPGRADE_EJECTOR + 1, false)
                                    : this.moveItemStackTo(stack, BUFFER_START, BUFFER_END + 1, false);
                    if (!moved) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, BUFFER_START, BUFFER_END + 1, false)) {
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
        return ItemStack.EMPTY;
    }
}
