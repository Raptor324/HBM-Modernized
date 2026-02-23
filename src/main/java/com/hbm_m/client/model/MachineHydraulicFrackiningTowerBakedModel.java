package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.custom.machines.MachineHydraulicFrackiningTowerBlock;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class MachineHydraulicFrackiningTowerBakedModel extends AbstractMultipartBakedModel {

    public MachineHydraulicFrackiningTowerBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // ВСЕГДА ПРОПУСКАЕМ ЗАПЕКАНИЕ В ЧАНК ДЛЯ МИРА!
        // Наша модель 24 блока в высоту. Sodium вывернет её наизнанку из-за 16-битного лимита.
        // Мы будем рендерить её ИСКЛЮЧИТЕЛЬНО через VBO в BER, там используются 32-битные float.
        if (state == null) return false; // Предметы в инвентаре рендерим как обычно
        return true; 
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        
        if (shouldSkipWorldRendering(state)) {
            return List.of();
        }

        List<BakedQuad> result = new ArrayList<>();
        int rotationY = 0;
        
        if (state != null && state.hasProperty(MachineHydraulicFrackiningTowerBlock.FACING)) {
            rotationY = getRotationYForFacing(state);
        }
        
        BakedModel basePart = parts.get("Cube_Cube.001");
        if (basePart != null) {
            result.addAll(ModelHelper.transformQuadsByFacing(
                basePart.getQuads(state, side, rand, modelData, renderType), rotationY));
        }
        
        return result;
    }

    private int getRotationYForFacing(BlockState state) {
        return switch (state.getValue(MachineHydraulicFrackiningTowerBlock.FACING)) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        return List.of("Cube_Cube.001");
    }
}