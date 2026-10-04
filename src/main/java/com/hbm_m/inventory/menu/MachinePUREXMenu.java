package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachinePUREXBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemBlueprints;
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

/**
 * 1:1 {@code ContainerMachinePUREX}: Batterie (152,81), Ordner (35,126), Upgrades (152,108/126), drei Eingaenge ab
 * (8,90), sechs Ausgaenge 2x3 ab (80,36), Spielerinventar ab y 174.
 */
public class MachinePUREXMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachinePUREXBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    private final MachinePUREXBlockEntity blockEntity;

    public MachinePUREXMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachinePUREXMenu(int id, Inventory inventory, MachinePUREXBlockEntity blockEntity) {
        super(ModMenuTypes.PUREX_MENU.get(), id);
        this.blockEntity = blockEntity;

        var handler = blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, blockEntity::setChanged);

        // Batterie
        this.addSlot(new Slot(container, 0, 152, 81));
        // Blueprint-Ordner
        this.addSlot(new Slot(container, 1, 35, 126) {
            @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(1, stack); }
        });
        // Upgrades
        for (int i = 0; i < 2; i++) {
            final int s = 2 + i;
            this.addSlot(new Slot(container, s, 152, 108 + i * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(s, stack); }
            });
        }
        // Eingaenge
        for (int i = 0; i < 3; i++) {
            final int s = 4 + i;
            this.addSlot(new Slot(container, s, 8 + i * 18, 90) {
                @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(s, stack); }
            });
        }
        // Ausgaenge
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                this.addSlot(new Slot(container, 7 + col + row * 2, 80 + col * 18, 36 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 174 + 58));
        }
    }

    private static MachinePUREXBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachinePUREXBlockEntity purex) {
            return purex;
        }
        throw new IllegalStateException("No MachinePUREXBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":purex_menu");
    }

    public MachinePUREXBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    /** Original {@code transferStackInSlot}: Batterie, Ordner, Upgrades, sonst Eingaenge. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
            } else {
                boolean battery = com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(slotStack).isPresent()
                        || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(slotStack).isPresent();
                if (battery) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (slotStack.getItem() instanceof ItemBlueprints) {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (slotStack.getItem() instanceof ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(slotStack, 2, 4, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotStack, 4, 7, false)) return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);
        }
        return result;
    }
}
