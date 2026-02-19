package com.hbm_m.block.custom.machines.crates;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Утилита валидации предметов для ящиков.
 * Запрещает класть ящики внутрь других ящиков (бесконечный сундук).
 */
public final class CrateValidation {

    private CrateValidation() {}

    /**
     * Проверяет, является ли предмет ящиком HBM (наследником BaseCrateBlock).
     * Такие предметы запрещено класть внутрь любого ящика.
     */
    public static boolean isCrateItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            return block instanceof BaseCrateBlock;
        }
        return false;
    }

    /**
     * Проверяет, допустим ли предмет для размещения в слоте ящика.
     */
    public static boolean isValidForCrate(ItemStack stack) {
        return !isCrateItem(stack);
    }
}
