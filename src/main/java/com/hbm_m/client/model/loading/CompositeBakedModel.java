//? if fabric {
/*package com.hbm_m.client.model.loading;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
final class CompositeBakedModel implements BakedModel {

    private final List<BakedModel> children;
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final ItemOverrides overrides;
    private final Matrix4f transform;

    // Lazily-built merged quad lists per cull-face (index 0 = general null side,
    // 1..6 = Direction ordinals). Composite/loader models are built once and their
    // children are fixed, so merging once per side avoids the per-frame
    // new ArrayList + addAll churn this method previously paid on every
    // item/inventory render. renderType is not part of the key here because this
    // Fabric path has no layer argument; composite children are deterministic in
    // side alone.
    // NOTE: keep this a line comment, not a javadoc block. The whole class lives
    // inside a stonecutter conditional disable block, and a block-comment close
    // token here would prematurely terminate it. Likewise avoid typing the
    // stonecutter close marker sequence in this comment.
    @SuppressWarnings("unchecked")
    private List<BakedQuad>[] mergedBySide;

    CompositeBakedModel(List<BakedModel> children,
                        TextureAtlasSprite particle,
                        ItemTransforms transforms,
                        ItemOverrides overrides,
                        Matrix4f transform) {
        this.children = children;
        this.particle = particle;
        this.transforms = transforms;
        this.overrides = overrides;
        this.transform = transform;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        int idx = side == null ? 0 : side.ordinal() + 1;
        List<BakedQuad>[] cache = mergedBySide;
        if (cache == null) {
            cache = (List<BakedQuad>[]) new List<?>[7];
            mergedBySide = cache;
        }
        List<BakedQuad> cached = cache[idx];
        if (cached != null) return cached;

        List<BakedQuad> out = new ArrayList<>();
        for (BakedModel child : children) {
            List<BakedQuad> q = child.getQuads(state, side, rand);
            if (q == null || q.isEmpty()) continue;
            if (transform != null) {
                for (BakedQuad quad : q) out.add(QuadTransforms.transform(quad, transform));
            } else {
                out.addAll(q);
            }
        }
        List<BakedQuad> result = out.isEmpty() ? List.of() : List.copyOf(out);
        cache[idx] = result;
        return result;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
*///?}

