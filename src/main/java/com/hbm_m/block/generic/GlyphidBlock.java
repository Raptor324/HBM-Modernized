package com.hbm_m.block.generic;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 1:1 {@code BlockGlyphid}: Nestmasse, Meta 0 normal, 1 befallen, 2 radioaktiv (je zwei Texturvarianten nach Position,
 * im Blockzustand als gewichtete Modellliste). Laesst nichts fallen.
 */
public class GlyphidBlock extends Block {

    public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 2);

    public GlyphidBlock(Properties properties) {
        this(properties, 0);
    }

    public GlyphidBlock(Properties properties, int defaultType) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, Math.max(0, Math.min(2, defaultType))));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    public BlockState withType(int type) {
        return defaultBlockState().setValue(TYPE, Math.max(0, Math.min(2, type)));
    }
}
