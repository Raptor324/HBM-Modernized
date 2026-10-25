package com.hbm_m.block.decorations;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code fence_metal} (Original {@code BlockMetalFence}): Vanilla-Zaun, der sich zusaetzlich mit Bloecken im Tag
 * {@code hbm_m:fence_metal_connects} verbindet (W-R9B2-1, Original {@code canConnectFenceTo: block == fence_gate};
 * ohne Addon leer -> Vanilla-Verhalten).
 */
public class MetalFenceBlock extends FenceBlock {

    public static final TagKey<Block> CONNECTS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("hbm_m", "fence_metal_connects"));

    public MetalFenceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean connectsTo(BlockState state, boolean isSideSolid, Direction direction) {
        return super.connectsTo(state, isSideSolid, direction) || state.is(CONNECTS);
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<FenceBlock> CODEC = simpleCodec(MetalFenceBlock::new);
    @Override public com.mojang.serialization.MapCodec<FenceBlock> codec() { return CODEC; }
    *///?}
}
