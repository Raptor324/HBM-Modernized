package com.hbm.items;

import com.hbm.items.tool.ItemDigammaDiagnostic;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registrations for legacy HBM items ported to 1.20.1.
 * Call {@link #register()} from {@link com.hbm_m.item.ModItems} to activate these items.
 */
public class ModItems {

    public static final RegistryObject<Item> DIGAMMA_DIAGNOSTIC =
        com.hbm_m.item.ModItems.ITEMS.register("digamma_diagnostic",
                    () -> new ItemDigammaDiagnostic(new Item.Properties()));

    /** Load this class so its static fields are registered. */
    public static void register() {
        // static initializer does the work
    }
}