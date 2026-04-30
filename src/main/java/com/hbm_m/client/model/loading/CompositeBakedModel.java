//? if fabric {
package com.hbm_m.client.model.loading;

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
        return out;
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
//?}

