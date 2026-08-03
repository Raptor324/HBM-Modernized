package com.hbm_m.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static baked model over a flat list of block-atlas quads, produced by the
 * {@code hbm_m:dae} geometry loader. Used for the transition seal item and particle
 * icon; the in-world block is rendered by the block entity renderer instead.
 */
public class DaeBakedModel implements BakedModel {

    private final List<BakedQuad> quads;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;

    public DaeBakedModel(List<BakedQuad> quads, TextureAtlasSprite particleIcon, ItemTransforms transforms) {
        this.quads = List.copyOf(quads);
        this.particleIcon = particleIcon;
        this.transforms = transforms;
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
        return particleIcon;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData modelData, @Nullable RenderType renderType) {
        return side == null ? quads : List.of();
    }
    //?}

    @Override
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return side == null ? quads : List.of();
    }
}
