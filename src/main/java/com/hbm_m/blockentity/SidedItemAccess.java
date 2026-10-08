package com.hbm_m.blockentity;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Original {@code ISidedInventory} ({@code getAccessibleSlotsFromSide} / {@code canInsertItem} / {@code canExtractItem}):
 * Automatisierungszugriff je Seite. Die GUI arbeitet weiter direkt auf dem Inventar, nur Trichter/Rohre sehen diese Regeln.
 */
public final class SidedItemAccess {

    /** Regeln wie im Original; {@code side} ist die angesprochene Seite des Blocks. */
    public interface Rules {
        int[] accessibleSlots(Direction side);
        boolean canInsert(int slot, ItemStack stack, Direction side);
        boolean canExtract(int slot, ItemStack stack, Direction side);
    }

    //? if forge {
    private final Supplier<net.minecraftforge.items.IItemHandlerModifiable> inventory;
    private final Rules rules;
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> cache = new EnumMap<>(Direction.class);

    public SidedItemAccess(Supplier<net.minecraftforge.items.IItemHandlerModifiable> inventory, Rules rules) {
        this.inventory = inventory;
        this.rules = rules;
    }

    public net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> get(Direction side) {
        return cache.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new Handler(d)));
    }

    /** Feste Slotliste ohne Seitenabhaengigkeit, z.B. fuer eine {@code IConditionalInvAccess}-Anschlusszelle. */
    public static net.minecraftforge.items.IItemHandler fixed(Supplier<net.minecraftforge.items.IItemHandlerModifiable> inventory, int[] slots,
                                                              java.util.function.BiPredicate<Integer, ItemStack> canInsert,
                                                              java.util.function.BiPredicate<Integer, ItemStack> canExtract) {
        SidedItemAccess access = new SidedItemAccess(inventory, new Rules() {
            @Override public int[] accessibleSlots(Direction side) { return slots; }
            @Override public boolean canInsert(int slot, ItemStack stack, Direction side) { return canInsert.test(slot, stack); }
            @Override public boolean canExtract(int slot, ItemStack stack, Direction side) { return canExtract.test(slot, stack); }
        });
        return access.new Handler(Direction.NORTH);
    }

    public void invalidate() {
        cache.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        cache.clear();
    }

    /** Bildet die freigegebenen Slots dieser Seite auf 0..n-1 ab. */
    private final class Handler implements net.minecraftforge.items.IItemHandler {
        private final Direction side;

        Handler(Direction side) { this.side = side; }

        private int[] slots() { return rules.accessibleSlots(side); }

        private int real(int slot) {
            int[] s = slots();
            return slot >= 0 && slot < s.length ? s[slot] : -1;
        }

        @Override public int getSlots() { return slots().length; }

        @Override public @NotNull ItemStack getStackInSlot(int slot) {
            int r = real(slot);
            return r < 0 ? ItemStack.EMPTY : inventory.get().getStackInSlot(r);
        }

        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int r = real(slot);
            if (r < 0 || stack.isEmpty() || !rules.canInsert(r, stack, side)) return stack;
            return inventory.get().insertItem(r, stack, simulate);
        }

        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            int r = real(slot);
            if (r < 0) return ItemStack.EMPTY;
            ItemStack present = inventory.get().getStackInSlot(r);
            if (present.isEmpty() || !rules.canExtract(r, present, side)) return ItemStack.EMPTY;
            return inventory.get().extractItem(r, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) {
            int r = real(slot);
            return r < 0 ? 0 : inventory.get().getSlotLimit(r);
        }

        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            int r = real(slot);
            return r >= 0 && rules.canInsert(r, stack, side);
        }
    }
    //?}

    /** Original {@code IBatteryItem.getCharge}: Ladung eines Akkus, -1 wenn es keiner ist. */
    public static long charge(ItemStack stack) {
        var hbm = com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack);
        if (hbm.isPresent()) return hbm.get().getEnergyStored();
        //? if forge {
        var fe = com.hbm_m.api.energy.ItemEnergyAccess.getForgeEnergy(stack);
        if (fe.isPresent()) return fe.get().getEnergyStored();
        //?}
        return -1;
    }

    /** Original {@code IBatteryItem.getMaxCharge}; -1 wenn kein Akku. */
    public static long maxCharge(ItemStack stack) {
        var hbm = com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack);
        if (hbm.isPresent()) return hbm.get().getMaxEnergyStored();
        //? if forge {
        var fe = com.hbm_m.api.energy.ItemEnergyAccess.getForgeEnergy(stack);
        if (fe.isPresent()) return fe.get().getMaxEnergyStored();
        //?}
        return -1;
    }

    /** Akku leer (Original-Entnahmeregel der Verbraucher). */
    public static boolean isEmptyBattery(ItemStack stack) {
        return charge(stack) == 0;
    }

    /** Akku voll (Original-Entnahmeregel der Erzeuger). */
    public static boolean isFullBattery(ItemStack stack) {
        long c = charge(stack);
        return c >= 0 && c == maxCharge(stack);
    }

    /** Hilfe fuer {@code new int[] {a, ..., b}}. */
    public static int[] range(int fromInclusive, int toInclusive) {
        int[] r = new int[toInclusive - fromInclusive + 1];
        for (int i = 0; i < r.length; i++) r[i] = fromInclusive + i;
        return r;
    }
}
