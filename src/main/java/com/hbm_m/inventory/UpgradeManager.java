package com.hbm_m.inventory;

import java.util.Arrays;
import java.util.Map;

import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.world.item.ItemStack;

/**
 * Минимальная реализация менеджера апгрейдов.
 *
 * Используется машинами, где апгрейды лежат в диапазоне слотов, а уровень считается
 * по количеству предметов {@link ItemMachineUpgrade} нужного типа.
 */
public final class UpgradeManager {

    private final int[] levels = new int[ItemMachineUpgrade.UpgradeType.values().length];

    public void checkSlots(ModItemStackHandler inv, int slotStartInclusive, int slotEndInclusive,
                           Map<ItemMachineUpgrade.UpgradeType, Integer> caps) {
        Arrays.fill(levels, 0);
        if (inv == null) return;

        for (int slot = slotStartInclusive; slot <= slotEndInclusive; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ItemMachineUpgrade up)) continue;

            ItemMachineUpgrade.UpgradeType type = up.getUpgradeType();
            int idx = type.ordinal();
            int add = Math.max(up.getTier(), 1);
            int cap = caps != null ? caps.getOrDefault(type, Integer.MAX_VALUE) : Integer.MAX_VALUE;
            levels[idx] = Math.min(cap, levels[idx] + add);
        }
    }

    public int getLevel(ItemMachineUpgrade.UpgradeType type) {
        return levels[type.ordinal()];
    }
}
