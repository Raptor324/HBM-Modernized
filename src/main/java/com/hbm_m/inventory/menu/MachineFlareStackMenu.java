package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineFlareStackBlockEntity;
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

/** 1:1 {@code ContainerMachineGasFlare}: Batterie, Fluid rein/raus, Fluidkennung, zwei Upgrades. */
public class MachineFlareStackMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 6;

    private final MachineFlareStackBlockEntity blockEntity;

    public MachineFlareStackMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineFlareStackMenu(int id, Inventory inventory, MachineFlareStackBlockEntity blockEntity) {
        super(ModMenuTypes.FLARE_STACK_MENU.get(), id);
        this.blockEntity = blockEntity;
        ModItemStackHandlerContainer tedf = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOTS),
                blockEntity != null ? blockEntity::setChanged : null);

        // Batterie
        this.addSlot(new Slot(tedf, 0, 143, 71));
        // Fluid rein
        this.addSlot(new Slot(tedf, 1, 17, 17));
        // Fluid raus
        this.addSlot(new Slot(tedf, 2, 17, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // Fluidkennung
        this.addSlot(new Slot(tedf, 3, 35, 71));
        // Upgrades
        this.addSlot(new Slot(tedf, 4, 80, 71));
        this.addSlot(new Slot(tedf, 5, 98, 71));

        int offset = 37;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + offset));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142 + offset));
        }
    }

    public static MachineFlareStackMenu create(int id, Inventory inventory, MachineFlareStackBlockEntity blockEntity) {
        return new MachineFlareStackMenu(id, inventory, blockEntity);
    }

    private static MachineFlareStackBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFlareStackBlockEntity flareStackBlockEntity) {
            return flareStackBlockEntity;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No MachineFlareStackBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":flare_stack_menu");
    }

    public MachineFlareStackBlockEntity getBlockEntity() {
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

            if (index <= 5) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (result.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(stack, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (result.getItem() instanceof com.hbm_m.api.item.IBatteryItem) {
                    if (!this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (result.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(stack, 4, 6, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }
}
