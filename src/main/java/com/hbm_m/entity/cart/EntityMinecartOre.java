package com.hbm_m.entity.cart;

import com.hbm_m.item.tool.ItemModMinecart;
import com.hbm_m.item.tool.ItemModMinecart.EnumCartBase;
import com.hbm_m.item.tool.ItemModMinecart.EnumMinecart;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityMinecartOre}: die leere NTM-Lore. */
public class EntityMinecartOre extends EntityMinecartNTM {

    public EntityMinecartOre(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMinecartOre(EntityType<?> type, Level world, double x, double y, double z, EnumCartBase base) {
        super(type, world, x, y, z, base);
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(this.getBase(), EnumMinecart.EMPTY);
    }
}
