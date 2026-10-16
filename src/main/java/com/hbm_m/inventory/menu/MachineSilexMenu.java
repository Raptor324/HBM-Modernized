package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
//? if forge {
import net.minecraftforge.items.SlotItemHandler;
//?} elif neoforge {
/*import net.neoforged.neoforge.items.SlotItemHandler;
*///?}
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineSilexMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 3;

    private final MachineSilexBlockEntity blockEntity;

    public MachineSilexMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineSilexMenu(int id, Inventory inventory, MachineSilexBlockEntity blockEntity) {
        super(ModMenuTypes.SILEX_MENU.get(), id);
        this.blockEntity = blockEntity;

        // Upstream ContainerSILEX draws the lasered item at (80,12), its output at (116,90) and
        // a column of aux slots at y=24; this port keeps input, output and a battery, so they go
        // on those frames. Without them the machine could only be filled by automation.
        var handler = blockEntity.getInventory();
        addSlot(new SlotItemHandler(handler, MachineSilexBlockEntity.SLOT_INPUT, 80, 12));
        addSlot(new SlotItemHandler(handler, MachineSilexBlockEntity.SLOT_OUTPUT, 116, 90));
        addSlot(new SlotItemHandler(handler, MachineSilexBlockEntity.SLOT_BATTERY, 8, 24));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }
    }

    public static MachineSilexMenu create(int id, Inventory inventory, MachineSilexBlockEntity blockEntity) {
        return new MachineSilexMenu(id, inventory, blockEntity);
    }

    private static MachineSilexBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineSilexBlockEntity silexBlockEntity) {
            return silexBlockEntity;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new MenuBlockEntityMissingException("No MachineSilexBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":silex_menu");
    }

    public MachineSilexBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, blockEntity);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        net.minecraft.world.item.ItemStack result = net.minecraft.world.item.ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        net.minecraft.world.item.ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return net.minecraft.world.item.ItemStack.EMPTY;
        } else {
            // Never into the output slot.
            if (!moveItemStackTo(stack, MachineSilexBlockEntity.SLOT_BATTERY, MachineSilexBlockEntity.SLOT_BATTERY + 1, false)
             && !moveItemStackTo(stack, MachineSilexBlockEntity.SLOT_INPUT, MachineSilexBlockEntity.SLOT_INPUT + 1, false))
                return net.minecraft.world.item.ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(net.minecraft.world.item.ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}