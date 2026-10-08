package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code ContainerHeaterHeatex}: Fluid-ID-Platz (80,72), Spielerinventar ab (8,122). */
public class MachineHeatexMenu extends AbstractContainerMenu {

    private final MachineHeatexBlockEntity blockEntity;

    public MachineHeatexMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineHeatexMenu(int id, Inventory inventory, MachineHeatexBlockEntity blockEntity) {
        super(ModMenuTypes.HEATEX_MENU.get(), id);
        this.blockEntity = blockEntity;

        this.addSlot(new Slot(new com.hbm_m.inventory.ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged), 0, 80, 72));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachineHeatexMenu create(int id, Inventory inventory, MachineHeatexBlockEntity blockEntity) {
        return new MachineHeatexMenu(id, inventory, blockEntity);
    }

    private static MachineHeatexBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineHeatexBlockEntity heatexBlockEntity) {
            return heatexBlockEntity;
        }
        throw new IllegalStateException("No MachineHeatexBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":heatex_menu");
    }

    public MachineHeatexBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(par2);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (par2 == 0) {
                if (!this.moveItemStackTo(var5, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(var5, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
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
