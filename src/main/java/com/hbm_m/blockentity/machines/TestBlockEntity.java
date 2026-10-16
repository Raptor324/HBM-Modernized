package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Тривиальный BlockEntity для тестового блока: нужен только как якорь BER (VBO-рендер).
 * Никакой логики и тика.
 */
public class TestBlockEntity extends BlockEntity {

    public TestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_BE.get(), pos, state);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        // Модель сильно больше одного блока — раздуваем AABB, чтобы BER не кулился ванилью.
        BlockPos pos = getBlockPos();
        return new AABB(pos).inflate(16.0);
    }
}
