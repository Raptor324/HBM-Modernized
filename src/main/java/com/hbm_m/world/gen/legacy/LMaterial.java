package com.hbm_m.world.gen.legacy;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port-Hilfe: die 1.7.10-{@code Material}-Klassen, die die alten Weltgen-Klassen abfragen, aus einem
 * 1.20-Blockzustand abgeleitet.
 */
public enum LMaterial {

    air(false, false, true),
    water(false, true, true),
    lava(false, true, true),
    plants(false, false, true),
    vine(false, false, true),
    leaves(true, false, false),
    snow(false, false, true),
    wood(true, false, false),
    ground(true, false, false),
    grass(true, false, false),
    sand(true, false, false),
    rock(true, false, false),
    iron(true, false, false);

    private final boolean solid;
    private final boolean liquid;
    private final boolean replaceable;

    LMaterial(boolean solid, boolean liquid, boolean replaceable) {
        this.solid = solid;
        this.liquid = liquid;
        this.replaceable = replaceable;
    }

    public boolean isSolid() { return solid; }
    public boolean isLiquid() { return liquid; }
    public boolean isReplaceable() { return replaceable; }
    public boolean blocksMovement() { return solid; }

    public static LMaterial of(BlockState s) {
        if (s.isAir()) return air;
        if (s.getFluidState().is(FluidTags.WATER) && !s.blocksMotion()) return water;
        if (s.getFluidState().is(FluidTags.LAVA)) return lava;
        if (s.is(BlockTags.LEAVES)) return leaves;
        if (s.is(net.minecraft.world.level.block.Blocks.VINE)) return vine;
        if (s.is(net.minecraft.world.level.block.Blocks.SNOW)) return snow;
        if (s.getBlock() instanceof BushBlock || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.FLOWERS)) return plants;
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS)) return wood;
        if (s.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) return grass;
        if (s.is(BlockTags.SAND)) return sand;
        if (s.is(BlockTags.DIRT)) return ground;
        if (!s.blocksMotion()) return plants;
        return rock;
    }
}
