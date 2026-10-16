package com.hbm_m.block.generic;

import net.minecraft.world.level.block.FallingBlock;

/**
 * Падающий нефтяной песок ({@code ore_oil_sand}, 1.7.10 BlockFalling).
 * В 1.20.1 FallingBlock конкретный, в 1.21.1 — абстрактный с
 * абстрактным {@code codec()}, поэтому гейт.
 */
public class OilSandBlock extends FallingBlock {

    public OilSandBlock(Properties properties) {
        super(properties);
    }

    //? if >= 1.21.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() {
        return simpleCodec(OilSandBlock::new);
    }
    *///?}
}
