package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.nuclear.PWRFuelItem;
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

/** 1:1 {@code ContainerPWR}: Brennstoff (0), heisser Brennstoff (1, nur Entnahme), Fluessigkeitskennung (2). */
public class PWRControllerMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 3;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final PWRControllerBlockEntity blockEntity;
    private final ModItemStackHandlerContainer machineContainer;

    public PWRControllerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public PWRControllerMenu(int id, Inventory inventory, PWRControllerBlockEntity blockEntity) {
        super(ModMenuTypes.PWR_CONTROLLER_MENU.get(), id);
        this.blockEntity = blockEntity;
        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        this.machineContainer = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : () -> {});

        this.addSlot(new Slot(machineContainer, 0, 53, 5));
        this.addSlot(new Slot(machineContainer, 1, 89, 32) {
            // Original: SlotCraftingOutput - nur Entnahme
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(machineContainer, 2, 8, 59));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 164));
        }
    }

    public static PWRControllerMenu create(int id, Inventory inventory, PWRControllerBlockEntity blockEntity) {
        return new PWRControllerMenu(id, inventory, blockEntity);
    }

    private static PWRControllerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PWRControllerBlockEntity pwr) {
            return pwr;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No PWRControllerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":pwr_controller_menu");
    }

    public PWRControllerBlockEntity getBlockEntity() {
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

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
                if (!this.moveItemStackTo(stack, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return result;
    }
}
