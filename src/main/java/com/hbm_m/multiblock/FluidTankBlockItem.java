package com.hbm_m.multiblock;

import java.util.function.Consumer;

import net.minecraft.world.level.block.Block;

/**
 * Item цистерны (fluid_tank): MultiblockBlockItem + BEWLR-рендер предмета по типу
 * залитой жидкости ({@link com.hbm_m.client.render.item.FluidTankItemRenderer}) —
 * порт 1.7.10 {@code RenderFluidTank.getRenderer()} (IItemRenderer через ClientProxy).
 */
public class FluidTankBlockItem extends MultiblockBlockItem {

    public FluidTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    //? if forge {
    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.FluidTankItemRenderer.INSTANCE;
            }
        });
    }
    //?} elif neoforge {
    /*@Override
    public void initializeClient(Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.FluidTankItemRenderer.INSTANCE;
            }
        });
    }
    *///?}
}
