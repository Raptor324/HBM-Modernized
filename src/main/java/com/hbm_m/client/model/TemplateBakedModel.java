package com.hbm_m.client.model;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Обёртка над запечённой моделью. Кастомный ItemOverrides подклассить нельзя (приватный ctor с 1.20+).

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class TemplateBakedModel implements BakedModel {

    private final BakedModel originalModel;

    public TemplateBakedModel(BakedModel originalModel) {
        this.originalModel = originalModel;
    }

    @NotNull
    @Override
    public ItemOverrides getOverrides() {
        return this.originalModel.getOverrides();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return originalModel.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return originalModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return originalModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return originalModel.isCustomRenderer();
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
        return originalModel.getParticleIcon();
    }

    @Override
    public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
        return originalModel.getTransforms();
    }
}
