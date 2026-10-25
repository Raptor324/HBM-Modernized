package com.hbm_m.world.gen.legacy;

/** 1:1 {@code RecipesCommon.MetaBlock}: Block + Metadatum (fuer die alten Weltgen-Klassen). */
public class MetaBlock {

    public final LB block;
    public final int meta;

    public MetaBlock(LB block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    public MetaBlock(LB block) {
        this(block, 0);
    }
}
