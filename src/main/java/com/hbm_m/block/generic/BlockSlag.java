package com.hbm_m.block.generic;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Блок шлака (порт {@link com.hbm.blocks.generic.BlockSlag} 1.7.10).
 * {@code broken=true} — текстура «сломанного» варианта (meta 1 в оригинале).
 */
public class BlockSlag extends Block {

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    public BlockSlag(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(BROKEN);
    }
}
