package com.hbm_m.blockentity.generic;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code TileEntityObjTester}: keine Logik, nur Traeger fuer den Renderer (unendliche Render-Box). */
public class ObjTesterBlockEntity extends BlockEntity {

    public ObjTesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OBJ_TESTER.get(), pos, state);
    }
}
