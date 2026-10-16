package com.hbm_m.item.rbmk;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * BlockItem for every RBMK column block, wired to render via {@code RBMKColumnItemRenderer}
 * (a throwaway block entity handed to the same renderer used in-world) instead of a static
 * baked model - see that class for why. Plain {@code new BlockItem(...)} never gets this hook,
 * which is why registering RBMK blocks through it left them stuck on flat/incorrect item icons.
 */
public class RBMKColumnBlockItem extends BlockItem {

    public RBMKColumnBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    //? if forge {
    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.RBMKColumnItemRenderer.INSTANCE;
            }
        });
    }
    //?}
}
