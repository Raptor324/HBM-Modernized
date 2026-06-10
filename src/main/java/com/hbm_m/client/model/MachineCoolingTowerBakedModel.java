package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}

public class MachineCoolingTowerBakedModel extends AbstractMultipartBakedModel {

    public MachineCoolingTowerBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (shouldSkipWorldRendering(state)) {
            return List.of();
        }
        List<BakedQuad> result = new ArrayList<>();
        BakedModel basePart = parts.get("Cube_Cube.001");
        if (basePart != null) {
            result.addAll(basePart.getQuads(state, side, rand));
        }
        return result;
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return false;
    }

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        if (shouldSkipWorldRendering(state)) {
            return List.of();
        }

        List<BakedQuad> result = new ArrayList<>();
        BakedModel basePart = parts.get("Cube_Cube.001");
        if (basePart != null) {
            result.addAll(basePart.getQuads(state, side, rand, modelData, renderType));
        }
        return result;
    }
    //?}

    @Override
    protected List<String> getItemRenderPartNames() {
        return List.of("Cube_Cube.001");
    }
}
