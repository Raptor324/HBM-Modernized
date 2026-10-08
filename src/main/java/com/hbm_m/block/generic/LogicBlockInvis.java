package com.hbm_m.block.generic;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code LogicBlockInvis} (logic_block_invis): unsichtbarer Logikblock (Renderart -1, nicht undurchsichtig). */
public class LogicBlockInvis extends LogicBlock {

    public LogicBlockInvis(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
