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

        // Original UpgradeManagerNT: nur gueltige Typen zaehlen, Mutex-Typen verdraengen sich (hoeherer Ordinal gewinnt)
        if (caps == null) return;
        ItemMachineUpgrade.UpgradeType mutexType = null;

        for (int slot = slotStartInclusive; slot <= slotEndInclusive; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ItemMachineUpgrade up)) continue;

            ItemMachineUpgrade.UpgradeType type = up.getUpgradeType();
            if (!caps.containsKey(type)) continue;
            int idx = type.ordinal();

            if (type.mutex) {
                if (mutexType == null) {
                    levels[idx] = 1;
                    mutexType = type;
                } else if (type.ordinal() > mutexType.ordinal()) {
                    levels[mutexType.ordinal()] = 0;
                    levels[idx] = 1;
                    mutexType = type;
                }
            } else {
                levels[idx] = Math.min(levels[idx] + up.getTier(), caps.get(type));
            }
        }
    }

    public int getLevel(ItemMachineUpgrade.UpgradeType type) {
        return levels[type.ordinal()];
    }
}
