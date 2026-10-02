package com.hbm_m.item.tool;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;

/** 1:1 {@code com.hbm.items.tool.HoeSchrabidium}: {@link ModHoe} mit Seltenheit "rare". */
public class HoeSchrabidium extends ModHoe {

    public HoeSchrabidium(Tier material) {
        super(material);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }
}
