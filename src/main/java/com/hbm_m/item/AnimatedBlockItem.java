package com.hbm_m.item;

import java.util.Set;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * Audit 7: einfacher Block-Gegenstand, dessen Original-Itemrenderer (ItemRenderBase) zeitabhaengig animiert
 * (Stirling-Zahnrad, Saege, Dreschwerk ...). Darstellung ueber builtin/entity + AnimatedMachineItemRenderer.
 * Mehrblock-Maschinen bekommen dasselbe ueber {@link com.hbm_m.multiblock.MultiblockBlockItem}.
 */
public class AnimatedBlockItem extends BlockItem {

    /** Registrierungsnamen der einfachen Bloecke mit animiertem Gegenstand. */
    public static final Set<String> NAMES = Set.of("autosaw", "thresher"); // Stirling ist jetzt Mehrblock (MultiblockBlockItem)

    public AnimatedBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.AnimatedMachineItemRenderer.instance();
            }
        });
    }
    //?} elif neoforge {
    /*@Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.AnimatedMachineItemRenderer.instance();
            }
        });
    }
    *///?}
}
