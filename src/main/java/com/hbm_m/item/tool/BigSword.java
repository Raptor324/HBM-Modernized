package com.hbm_m.item.tool;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 1:1 {@code com.hbm.items.tool.BigSword} ("Great Sword"): ein Schwert, dessen Rechtsklick nichts tut
 * (die Plasma-Strahl-Nutzung ist im Original auskommentiert).
 */
public class BigSword extends SwordItem {

    public BigSword(Tier material) {
        //? if < 1.21.1 {
        super(material, 4, -2.4F, new Properties());
        //?} else {
        /*super(material, new Properties().attributes(SwordItem.createAttributes(material, 4, -2.4F)));
        *///?}
    }

    /** BEWLR fuer die 1.7-IItemRenderer (ItemRenderBigSword); greift nur bei builtin/entity-Itemmodellen. */
    //? if forge {
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    //?} elif neoforge {
    /*@Override
    @SuppressWarnings("removal")
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    *///?}
}
