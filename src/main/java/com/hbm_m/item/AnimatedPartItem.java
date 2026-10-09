package com.hbm_m.item;

import net.minecraft.world.item.Item;

/**
 * vI: Einzelteil, dessen Original-Itemrenderer ({@code ItemRenderLibrary}) im Inventar zeitabhaengig dreht
 * (grosses Zahnrad, Saegeblatt). Darstellung ueber builtin/entity + AnimatedPartItemRenderer.
 */
public class AnimatedPartItem extends Item {

    public AnimatedPartItem(Properties properties) {
        super(properties);
    }

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.AnimatedPartItemRenderer.instance();
            }
        });
    }
    //?} elif neoforge {
    /*@SuppressWarnings("removal")
    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.AnimatedPartItemRenderer.instance();
            }
        });
    }
    *///?}
}
