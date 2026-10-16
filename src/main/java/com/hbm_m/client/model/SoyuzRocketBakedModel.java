package com.hbm_m.client.model;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

/**
 * Baked model for the decorative Soyuz rocket (soyuz.obj, ~52 blocks tall,
 * single multi-material mesh). Same issue/fix as {@link SoyuzLauncherBakedModel}:
 * world rendering of baked quads is skipped (16-bit chunk-mesh vertex overflow
 * corrupts geometry this tall); real rendering happens in
 * {@link com.hbm_m.client.render.implementations.SoyuzRocketRenderer} via VBO.
 */
public class SoyuzRocketBakedModel extends AbstractMultipartBakedModel {

    public static final String ROCKET = "Rocket";
    public static final List<String> ALL_PARTS = List.of(ROCKET);

    public SoyuzRocketBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return state != null;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (shouldSkipWorldRendering(state)) {
            return List.of();
        }
        BakedModel part = parts.get(ROCKET);
        return part != null ? part.getQuads(state, side, rand) : List.of();
    }

    //? if forge || neoforge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (shouldSkipWorldRendering(state)) {
            return List.of();
        }
        BakedModel part = parts.get(ROCKET);
        return part != null ? part.getQuads(state, side, rand, modelData, renderType) : List.of();
    }
    //?}

    @Override
    protected List<String> getItemRenderPartNames() {
        return ALL_PARTS;
    }
}
