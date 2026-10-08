package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
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
 * 1:1 {@code ContainerSILEX}: Eingang (80,12), Fluidkennung (8,24), Kanister ein/aus (26/44,24), Ausgang (116,90),
 * Warteschlange 2x3 ab (134,72), Spielerinventar ab y 140.
 */
public class MachineSilexMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 11;

    private final MachineSilexBlockEntity blockEntity;

    public MachineSilexMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineSilexMenu(int id, Inventory inventory, MachineSilexBlockEntity blockEntity) {
        super(ModMenuTypes.SILEX_MENU.get(), id);
        this.blockEntity = blockEntity;

        var handler = blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOTS);
        var container = new ModItemStackHandlerContainer(handler, blockEntity != null ? blockEntity::setChanged : () -> { });

        // Eingang
        this.addSlot(new Slot(container, 0, 80, 12));
        // Fluidkennung
        this.addSlot(new Slot(container, 1, 8, 24));
        // Kanister
        this.addSlot(new Slot(container, 2, 8 + 18, 24));
        this.addSlot(new Slot(container, 3, 8 + 18 * 2, 24) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        // Ausgang
        this.addSlot(new Slot(container, 4, 116, 90) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        // Warteschlange
        int[][] q = { {134, 72}, {152, 72}, {134, 90}, {152, 90}, {134, 108}, {152, 108} };
        for (int i = 0; i < 6; i++) {
            this.addSlot(new Slot(container, 5 + i, q[i][0], q[i][1]) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + (18 * 3) + 2));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142 + (18 * 3) + 2));
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
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No MachineSilexBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":silex_menu");
    }

    public MachineSilexBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // audit13: Original isUseableByPlayer (<= 128 zur Kernmitte) oder Huelle <= 64; Vanilla 64 schloss die GUI an grossen Maschinen
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    /** Original {@code transferStackInSlot}: Maschine -> Spieler, sonst in den Eingang. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
