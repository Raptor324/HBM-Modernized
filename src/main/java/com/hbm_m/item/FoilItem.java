package com.hbm_m.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Предмет с зачарованным блеском (порт {@code ItemCustomLore.setEffect()} из 1.7.10).
 * {@code isFoil} имеет одинаковую сигнатуру на 1.20.1 и 1.21.1.
 */
public class FoilItem extends Item {

    public FoilItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
