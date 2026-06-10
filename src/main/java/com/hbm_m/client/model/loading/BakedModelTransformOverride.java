//? if fabric {
/*package com.hbm_m.client.model.loading;

import org.jetbrains.annotations.Nullable;

import java.util.List;

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
final class BakedModelTransformOverride implements BakedModel {

    private final BakedModel delegate;
    private final ItemTransforms transforms;

    BakedModelTransformOverride(BakedModel delegate, ItemTransforms transforms) {
        this.delegate = delegate;
        this.transforms = transforms == null ? ItemTransforms.NO_TRANSFORMS : transforms;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return delegate.getQuads(state, side, rand);
    }

    @Override public boolean useAmbientOcclusion() { return delegate.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return delegate.isGui3d(); }
    @Override public boolean usesBlockLight() { return delegate.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return delegate.isCustomRenderer(); }
    @Override public TextureAtlasSprite getParticleIcon() { return delegate.getParticleIcon(); }
    @Override public ItemOverrides getOverrides() { return delegate.getOverrides(); }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }
}
*///?}

