package com.hbm_m.creativetabs;

import java.util.function.Consumer;

import com.hbm_m.item.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Порт {@link com.hbm.creativetabs.MissileTab} — иконка вкладки и доп. предметы (кастомные ракеты).
 */
public final class MissileTab {

    private MissileTab() {
    }

    public static Item getTabIconItem() {
        if (ModItems.MISSILE_NUCLEAR.isPresent()) {
            return ModItems.MISSILE_NUCLEAR.get();
        }
        return Items.IRON_PICKAXE;
    }

  /**
   * Дополнительные стеки вкладки (оригинал: {@code displayAllReleventItems} + {@code ItemCustomMissile}).
   * Кастомные ракеты будут добавлены после порта {@code ItemCustomMissile}.
   */
    public static void appendExtraItems(Consumer<ItemStack> output) {
        // ItemCustomMissile + mp_* parts not ported yet
    }
}
