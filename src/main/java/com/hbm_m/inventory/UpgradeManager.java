package com.hbm_m.inventory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.world.item.ItemStack;

/**
 * Порт UpgradeManagerNT из 1.7.10.
 *
 * Сканирует указанный диапазон слотов инвентаря, суммирует уровни
 * {@link ItemMachineUpgrade} по типам и ограничивает по cap'у
 * из {@link IUpgradeInfoProvider#getValidUpgrades()}.
 *
 * Использует кэш: пересчёт происходит только при изменении содержимого слотов.
 */
public class UpgradeManager {

    private final Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);
    private ItemStack[] cachedSlots;

    public void checkSlots(ModItemStackHandler inventory, int startSlot, int endSlot,
                           Map<UpgradeType, Integer> validUpgrades) {
        if (validUpgrades == null || validUpgrades.isEmpty()) return;

        int count = endSlot - startSlot + 1;
        ItemStack[] current = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            current[i] = inventory.getStackInSlot(startSlot + i).copy();
        }

        if (cachedSlots != null && Arrays.equals(current, cachedSlots)) return;
        cachedSlots = current;

        upgrades.clear();

        for (ItemStack stack : current) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ItemMachineUpgrade upgrade)) continue;

            UpgradeType type = upgrade.getUpgradeType();
            Integer cap = validUpgrades.get(type);
            if (cap == null) continue;

            int currentLevel = upgrades.getOrDefault(type, 0);
            int add = Math.max(upgrade.getTier(), 1);
            upgrades.put(type, Math.min(currentLevel + add, cap));
        }
    }

    public int getLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void invalidateCache() {
        cachedSlots = null;
    }
}
