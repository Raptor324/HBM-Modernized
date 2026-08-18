package com.hbm_m.block.generic;

/**
 * Port of {@code MachineTransformer} (1.7.10 Original) - purely decorative in the original (no
 * TileEntity at all; the original's red-wire cable network reads voltage tiers from adjacent
 * cable/pylon blocks directly, not from this block). Kept as a plain decorative block to match.
 */
public class MachineTransformerBlock extends net.minecraft.world.level.block.Block {
    public MachineTransformerBlock(Properties properties) { super(properties); }
}
