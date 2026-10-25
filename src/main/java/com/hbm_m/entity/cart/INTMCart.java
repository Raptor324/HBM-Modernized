package com.hbm_m.entity.cart;

import com.hbm_m.item.tool.ItemModMinecart.EnumCartBase;

import net.minecraft.world.item.ItemStack;

/** Gemeinsame Sicht auf NTM-Loren (mit und ohne Inventar) fuer den Renderer. */
public interface INTMCart {

    EnumCartBase getBase();

    ItemStack getCartItem();

    /** Original {@code renderSpecialContent}: Art des Aufbaus, den der Renderer zeichnet. */
    default String specialContent() {
        return null;
    }
}
