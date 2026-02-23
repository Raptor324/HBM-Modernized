package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineHydraulicFrackiningTowerBlockEntity extends BlockEntity {

    public MachineHydraulicFrackiningTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Контроллер находится на полу (y=0) в центре структуры 7х7.
        // Вышка имеет ширину 7 блоков (от -3 до +3 относительно контроллера).
        // Высота башни — 24 блока (от 0 до +24).
        // Это создаст огромный BoundingBox, который спасёт от пропаданий куллинга.
        return new AABB(this.worldPosition).inflate(3.0, 0.0, 3.0).expandTowards(0.0, 24.0, 0.0);
    }
}
