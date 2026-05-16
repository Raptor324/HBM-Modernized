package com.hbm.items.special;

import com.hbm_m.radiation.PlayerHandler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * RBMK fuel rod item with Digamma radiation tick behaviour.
 * Ported from 1.7.10 HBM-NT to 1.20.1.
 */
public class RbmkFuelDrxItem extends Item {

    public RbmkFuelDrxItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);

        if (!level.isClientSide() && entity instanceof Player player) {
            // Add 1 mDRX/s Digamma radiation as accumulated radiation (placeholder until
            // a dedicated Digamma contamination system is implemented)
            PlayerHandler.incrementPlayerRads(player, 1.0F);
        }
    }
}