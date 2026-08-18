package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineArcWelderBlockEntity;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.items.SlotItemHandler;
//?} elif neoforge {
/*import net.neoforged.neoforge.items.SlotItemHandler;
*///?}

public class MachineArcWelderMenu extends AbstractContainerMenu {

    private final MachineArcWelderBlockEntity blockEntity;

    /** First player-inventory slot index (after all machine slots). */
    private static final int MACHINE_SLOTS = MachineArcWelderBlockEntity.SLOTS; // 8

    public MachineArcWelderMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineArcWelderMenu(int id, Inventory inv, MachineArcWelderBlockEntity be) {
        super(ModMenuTypes.ARC_WELDER_MENU.get(), id);
        this.blockEntity = be;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку
        var handler = be != null ? be.getItemHandler() : new DummyItemStackHandler(MACHINE_SLOTS);
        // ── Machine slots (0-7) ──────────────────────────────────────────────
        addSlot(new SlotItemHandler(handler, 0,  17, 36)); // Input 1
        addSlot(new SlotItemHandler(handler, 1,  35, 36)); // Input 2
        addSlot(new SlotItemHandler(handler, 2,  53, 36)); // Input 3
        addSlot(new SlotItemHandler(handler, 3, 107, 36) { // Output — extraction only
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        addSlot(new SlotItemHandler(handler, 4, 152, 72)); // Battery
        addSlot(new SlotItemHandler(handler, 5,  17, 63)); // Fluid-ID item
        addSlot(new SlotItemHandler(handler, 6,  89, 63)); // Upgrade 1
        addSlot(new SlotItemHandler(handler, 7, 107, 63)); // Upgrade 2


        // ── Player inventory (8-34) ──────────────────────────────────────────
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));

        // ── Hotbar (35-43) ───────────────────────────────────────────────────
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
    }

    public static MachineArcWelderMenu create(int id, Inventory inv, MachineArcWelderBlockEntity be) {
        return new MachineArcWelderMenu(id, inv, be);
    }

    private static MachineArcWelderBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineArcWelderBlockEntity w) return w;
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineArcWelderBlockEntity at " + pos);
    }

    public MachineArcWelderBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return false; // тайл может отсутствовать на клиенте (реплей Flashback)
        }
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    // ── Shift-click routing ──────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOTS) {
            // From machine → player inventory
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // From player inventory → machine
            // Try battery slot first, then fluid-ID, then upgrades, then inputs
            if (!moveItemStackTo(stack, 4, 5, false)   // battery
             && !moveItemStackTo(stack, 5, 6, false)   // fluid-ID
             && !moveItemStackTo(stack, 6, 8, false)   // upgrades
             && !moveItemStackTo(stack, 0, 3, false))  // inputs
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
