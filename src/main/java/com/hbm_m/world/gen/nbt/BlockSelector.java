package com.hbm_m.world.gen.nbt;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code StructureComponent.BlockSelector} (1.7.10): waehlt pro Aufruf einen Block (fruher Block + Meta, hier
 * ein Blockzustand). Wird von NBT-Jigsaw-Teilen ({@code blockTable}/{@code platform}) und den Bauteil-Strukturen
 * benutzt.
 */
public abstract class BlockSelector {

    /** {@code field_151562_a} + {@code field_151563_b}. */
    protected BlockState selected = Blocks.AIR.defaultBlockState();

    public abstract void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior);

    /** {@code func_151561_a}/{@code getSelectedBlockMetaData} zusammengefasst. */
    public BlockState getSelected() {
        return selected;
    }
}
