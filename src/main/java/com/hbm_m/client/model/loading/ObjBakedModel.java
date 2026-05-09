//? if fabric {
package com.hbm_m.client.model.loading;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

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
public class ObjBakedModel implements BakedModel {

    private final Map<Direction, List<BakedQuad>> quads;
    private final TextureAtlasSprite particle;
    private final boolean shadeQuads;
    private final boolean automaticCulling;
    private final ItemTransforms itemTransforms;
    private final ItemOverrides overrides;

    public ObjBakedModel(Map<Direction, List<BakedQuad>> quads,
                         TextureAtlasSprite particle,
                         boolean shadeQuads,
                         boolean automaticCulling,
                         ItemTransforms itemTransforms,
                         ItemOverrides overrides) {
        this.quads = quads;
        this.particle = particle;
        this.shadeQuads = shadeQuads;
        this.automaticCulling = automaticCulling;
        this.itemTransforms = itemTransforms;
        this.overrides = overrides;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return quads.getOrDefault(side, Collections.emptyList());
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
        return itemTransforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
//?}