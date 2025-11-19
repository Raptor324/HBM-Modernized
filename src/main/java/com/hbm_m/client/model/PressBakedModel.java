package com.hbm_m.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PressBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String BASE = "Base";
    private static final String HEAD = "Head";

    private final String[] partNames;
    private final Vector3f headRestOffset;
    private final float headTravelDistance;

    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public PressBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms,
                           Vector3f headRestOffset, float headTravelDistance) {
        super(parts, transforms);
        this.partNames = parts.keySet().toArray(new String[0]);
        this.headRestOffset = headRestOffset;
        this.headTravelDistance = headTravelDistance;
    }

    public Vector3f getHeadRestOffset() {
        return new Vector3f(headRestOffset);
    }

    public float getHeadTravelDistance() {
        return headTravelDistance;
    }

    @Override
    public String[] getPartNames() {
        return partNames;
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return state != null && state.hasBlockEntity();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }

        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        return Collections.emptyList();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                         ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (!itemQuadsCached) {
            cachedItemQuads = buildItemQuads(rand, modelData, renderType);
            itemQuadsCached = true;
        }

        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }

        return cachedItemQuads;
    }

    private List<BakedQuad> buildItemQuads(RandomSource rand, ModelData modelData,
                                           @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        for (String partName : getItemRenderPartNames()) {
            BakedModel part = parts.get(partName);
            if (part == null) continue;

            for (Direction dir : Direction.values()) {
                quads.addAll(part.getQuads(null, dir, rand, modelData, renderType));
            }
            quads.addAll(part.getQuads(null, null, rand, modelData, renderType));
        }
        return quads;
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        List<String> order = new ArrayList<>(2);
        if (parts.containsKey(BASE)) {
            order.add(BASE);
        }
        if (parts.containsKey(HEAD)) {
            order.add(HEAD);
        }
        parts.keySet().stream()
            .filter(name -> !order.contains(name))
            .forEach(order::add);
        return order;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        cachedItemQuads = null;
        itemQuadsCached = false;
    }
}

