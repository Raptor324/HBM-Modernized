package com.hbm_m.entity.cart;

import com.hbm_m.item.tool.ItemModMinecart;
import com.hbm_m.item.tool.ItemModMinecart.EnumCartBase;
import com.hbm_m.item.tool.ItemModMinecart.EnumMinecart;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityMinecartSemtex}: Lore mit Semtexblock (rein optisch). */
public class EntityMinecartSemtex extends EntityMinecartNTM {

    public EntityMinecartSemtex(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMinecartSemtex(EntityType<?> type, Level world, double x, double y, double z, EnumCartBase base) {
        super(type, world, x, y, z, base);
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(this.getBase(), EnumMinecart.SEMTEX);
    }

    @Override
    public String specialContent() {
        return "semtex";
    }
}
