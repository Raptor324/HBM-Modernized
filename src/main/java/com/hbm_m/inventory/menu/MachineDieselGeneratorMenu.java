package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;
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

/**
 * 1:1 {@code ContainerMachineDiesel}: Kanister rein (17,17), leer raus (17,53, nur entnehmen), Batterie (141,71),
 * Fluid-Identifier (35,71), Spielerinventar bei y=121.
 */
public class MachineDieselGeneratorMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 4;

    private final MachineDieselGeneratorBlockEntity blockEntity;

    public MachineDieselGeneratorMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineDieselGeneratorMenu(int id, Inventory inventory, MachineDieselGeneratorBlockEntity blockEntity) {
        super(ModMenuTypes.DIESEL_GENERATOR_MENU.get(), id);
        this.blockEntity = blockEntity;

        var tedf = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : null);

        this.addSlot(new Slot(tedf, 0, 17, 17));
        // Original SlotTakeOnly
        this.addSlot(new Slot(tedf, 1, 17, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(tedf, 2, 141, 71));
        this.addSlot(new Slot(tedf, 3, 35, 71));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 121 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 179));
        }
    }

    private static MachineDieselGeneratorBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineDieselGeneratorBlockEntity generator) {
            return generator;
        }
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineDieselGeneratorBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":dieselgen_menu");
    }

    public MachineDieselGeneratorBlockEntity getBlockEntity() {
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

    /** 1:1 {@code transferStackInSlot} (einschliesslich der Original-Grenzen {@code par2 <= 4} und Ersatzziel 4..5). */
    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(par2);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (par2 <= 4) {
                if (!this.moveItemStackTo(var5, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(var5, 0, 1, false)) {
                if (!this.moveItemStackTo(var5, 2, 3, false))
                    if (!this.moveItemStackTo(var5, 4, 5, false))
                        return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
