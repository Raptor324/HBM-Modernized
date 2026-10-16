package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineSolderingStationBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineSolderingStationMenu extends AbstractContainerMenu {

    private final MachineSolderingStationBlockEntity blockEntity;

    /** First player-inventory slot index (after all machine slots). */
    private static final int MACHINE_SLOTS = MachineSolderingStationBlockEntity.SLOTS; // 11

    public MachineSolderingStationMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineSolderingStationMenu(int id, Inventory inv, MachineSolderingStationBlockEntity be) {
        super(ModMenuTypes.SOLDERING_STATION_MENU.get(), id);
        this.blockEntity = be;

        // Every machine slot used to be inside a forge-only block, so on NeoForge the menu was just
        // the 36 player slots while quickMoveStack still moved to indices 0-10 - the player's own
        // inventory. ModItemStackHandlerContainer is the platform-neutral adapter used elsewhere.
        ModItemStackHandlerContainer h =
                new ModItemStackHandlerContainer(be.getItemHandler(), be::setChanged);
        // ── Machine slots (0-10) ─────────────────────────────────────────────
        // Inputs row 1 — toppings (slots 0-2)
        for (int j = 0; j < 3; j++)
            addSlot(new Slot(h, j, 17 + j * 18, 18));
        // Inputs row 2 — PCB (slots 3-5)
        for (int j = 0; j < 3; j++)
            addSlot(new Slot(h, 3 + j, 17 + j * 18, 36));
        // Output (slot 6) — extraction only
        addSlot(new Slot(h, 6, 107, 27) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        addSlot(new Slot(h, 7, 152, 72)); // Battery
        addSlot(new Slot(h, 8,  17, 63)); // Fluid-ID
        addSlot(new Slot(h, 9,  89, 63)); // Upgrade 1
        addSlot(new Slot(h, 10, 107, 63)); // Upgrade 2

        // ── Player inventory (11-37) ─────────────────────────────────────────
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));

        // ── Hotbar (38-46) ───────────────────────────────────────────────────
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
    }

    public static MachineSolderingStationMenu create(int id, Inventory inv, MachineSolderingStationBlockEntity be) {
        return new MachineSolderingStationMenu(id, inv, be);
    }

    private static MachineSolderingStationBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineSolderingStationBlockEntity s) return s;
        throw new MenuBlockEntityMissingException("No MachineSolderingStationBlockEntity at " + pos);
    }

    public MachineSolderingStationBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, blockEntity);
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
            // From player → machine
            if (!moveItemStackTo(stack, 7, 8, false)    // battery
             && !moveItemStackTo(stack, 8, 9, false)    // fluid-ID
             && !moveItemStackTo(stack, 9, 11, false)   // upgrades
             && !moveItemStackTo(stack, 0, 3, false)    // toppings
             && !moveItemStackTo(stack, 3, 6, false))   // PCB/secondary
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
