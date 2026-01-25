package com.hbm_m.item.custom.industrial;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemBlades extends Item {
    private final int maxUses;
    private final boolean unbreakable;

    public ItemBlades(Properties properties, int maxUses) {
        super(properties.defaultDurability(maxUses));
        this.maxUses = maxUses;
        this.unbreakable = false;
    }

    public ItemBlades(Properties properties) {
        super(properties);
        this.maxUses = 0;
        this.unbreakable = true;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return !unbreakable;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return unbreakable ? 0 : maxUses;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }
}
