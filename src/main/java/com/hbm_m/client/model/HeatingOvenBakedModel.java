package com.hbm_m.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
/*import net.minecraftforge.client.model.data.ModelData;
*///?}
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Baked model for HeatingOven with animated door and inner burning state.
 * Parts: Main (static), Door (animated), Inner (when not burning), InnerBurning (when burning)
 */
public class HeatingOvenBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    public static final String MAIN = "Main";
    public static final String DOOR = "Door";
    public static final String INNER = "Inner";
    public static final String INNER_BURNING = "InnerBurning";

    private final String[] partNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public HeatingOvenBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
        this.partNames = parts.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getPartNames() {
        return partNames;
    }

    public BakedModel getMainPart() {
        return parts.get(MAIN);
    }

    public BakedModel getDoorPart() {
        return parts.get(DOOR);
    }

    public BakedModel getInnerPart() {
        return parts.get(INNER);
    }

    public BakedModel getInnerBurningPart() {
        return parts.get(INNER_BURNING);
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Main is baked into chunk, BER renders Door and Inner parts
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        //? if forge {
        /*return getQuads(state, side, rand, ModelData.EMPTY, null);
        *///?}

        //? if fabric {
        // ITEM RENDER (Inventory/Hand)
        if (state == null) {
            return getItemQuads(side, rand);
        }

        // WORLD RENDER: Main is baked into chunk
        BakedModel mainPart = parts.get(MAIN);
        if (mainPart != null) {
            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(mainPart.getQuads(state, d, rand));
            }
            partQuads.addAll(mainPart.getQuads(state, null, rand));
            if (!partQuads.isEmpty()) {
                List<BakedQuad> translated = ModelHelper.translateQuads(partQuads, 0.5f, 0f, 0.5f);
                if (side != null) {
                    return translated.stream().filter(q -> q.getDirection() == side).toList();
                }
                return translated;
            }
        }
        return Collections.emptyList();
        //?}
    }

    //? if forge {
    /*@Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // ITEM RENDER (Inventory/Hand)
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        // WORLD RENDER: Main is baked into chunk (Embeddium/Sodium compatible)
        BakedModel mainPart = parts.get(MAIN);
        if (mainPart != null) {
            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(mainPart.getQuads(state, d, rand, modelData, renderType));
            }
            partQuads.addAll(mainPart.getQuads(state, null, rand, modelData, renderType));
            if (!partQuads.isEmpty()) {
                List<BakedQuad> translated = ModelHelper.translateQuads(partQuads, 0.5f, 0f, 0.5f);
                if (side != null) {
                    return translated.stream().filter(q -> q.getDirection() == side).toList();
                }
                return translated;
            }
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

        // Render Main and Door for item display
        BakedModel mainPart = parts.get(MAIN);
        BakedModel doorPart = parts.get(DOOR);

        if (mainPart != null) {
            for (Direction d : Direction.values()) {
                quads.addAll(mainPart.getQuads(null, d, rand, modelData, renderType));
            }
            quads.addAll(mainPart.getQuads(null, null, rand, modelData, renderType));
        }

        if (doorPart != null) {
            for (Direction d : Direction.values()) {
                quads.addAll(doorPart.getQuads(null, d, rand, modelData, renderType));
            }
            quads.addAll(doorPart.getQuads(null, null, rand, modelData, renderType));
        }

        return quads;
    }
    *///?}

    //? if fabric {
    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
        if (!itemQuadsCached) {
            cachedItemQuads = buildItemQuads(rand);
            itemQuadsCached = true;
        }

        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }
        return cachedItemQuads;
    }

    private List<BakedQuad> buildItemQuads(RandomSource rand) {
        List<BakedQuad> quads = new ArrayList<>();

        // Render Main and Door for item display
        BakedModel mainPart = parts.get(MAIN);
        BakedModel doorPart = parts.get(DOOR);

        if (mainPart != null) {
            for (Direction d : Direction.values()) {
                quads.addAll(mainPart.getQuads(null, d, rand));
            }
            quads.addAll(mainPart.getQuads(null, null, rand));
        }

        if (doorPart != null) {
            for (Direction d : Direction.values()) {
                quads.addAll(doorPart.getQuads(null, d, rand));
            }
            quads.addAll(doorPart.getQuads(null, null, rand));
        }

        return quads;
    }
    //?}

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
        BakedModel mainPart = parts.get(MAIN);
        if (mainPart != null) {
            return mainPart.getParticleIcon();
        }
        return super.getParticleIcon();
    }
}
