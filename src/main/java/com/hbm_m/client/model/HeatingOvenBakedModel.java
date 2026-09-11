package com.hbm_m.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    /**
     * Мир-квады Main-части, предвычисленные per-state (индексы 0=null, 1..6=Direction).
     * Без кэша каждый вызов getQuads пересобирал список и копировал все вершинные массивы
     * в translateQuads — на каждое перестроение чанк-меша ×7 граней.
     */
    private final Map<BlockState, List<BakedQuad>[]> worldQuadCache = new HashMap<>();

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
        return getQuads(state, side, rand, ModelData.EMPTY, null);
        //?}

        //? if neoforge {
        /*return super.getQuads(state, side, rand);
        *///?}

        //? if fabric {
        /*// ITEM RENDER (Inventory/Hand)
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
        *///?}
    }

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // ITEM RENDER (Inventory/Hand)
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        // WORLD RENDER: Main is baked into chunk (Embeddium/Sodium compatible)
        BakedModel mainPart = parts.get(MAIN);
        if (mainPart == null) {
            return Collections.emptyList();
        }
        if (renderType == null && modelData == ModelData.EMPTY) {
            return cachedWorldQuads(state, rand, mainPart, side);
        }
        return buildWorldQuads(state, rand, mainPart, side, modelData, renderType);
    }

    private List<BakedQuad> cachedWorldQuads(@Nullable BlockState state, RandomSource rand,
                                             BakedModel mainPart, @Nullable Direction side) {
        List<BakedQuad>[] perSide = worldQuadCache.get(state);
        if (perSide == null) {
            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(mainPart.getQuads(state, d, rand, ModelData.EMPTY, null));
            }
            partQuads.addAll(mainPart.getQuads(state, null, rand, ModelData.EMPTY, null));
            if (partQuads.isEmpty()) {
                return Collections.emptyList();
            }
            List<BakedQuad> translated = ModelHelper.translateQuads(partQuads, 0.5f, 0f, 0.5f);
            //noinspection unchecked
            perSide = (List<BakedQuad>[]) new List<?>[7];
            for (int i = 0; i < 7; i++) {
                Direction s = i == 0 ? null : Direction.values()[i - 1];
                List<BakedQuad> filtered = new ArrayList<>();
                for (BakedQuad q : translated) {
                    if (q.getDirection() == s) {
                        filtered.add(q);
                    }
                }
                perSide[i] = List.copyOf(filtered);
            }
            worldQuadCache.put(state, perSide);
        }
        return perSide[side == null ? 0 : side.ordinal() + 1];
    }

    private List<BakedQuad> buildWorldQuads(@Nullable BlockState state, RandomSource rand, BakedModel mainPart,
                                            @Nullable Direction side, ModelData modelData,
                                            @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> partQuads = new ArrayList<>();
        for (Direction d : Direction.values()) {
            partQuads.addAll(mainPart.getQuads(state, d, rand, modelData, renderType));
        }
        partQuads.addAll(mainPart.getQuads(state, null, rand, modelData, renderType));
        if (partQuads.isEmpty()) {
            return Collections.emptyList();
        }
        List<BakedQuad> translated = ModelHelper.translateQuads(partQuads, 0.5f, 0f, 0.5f);
        if (side != null) {
            List<BakedQuad> filtered = new ArrayList<>();
            for (BakedQuad q : translated) {
                if (q.getDirection() == side) {
                    filtered.add(q);
                }
            }
            return List.copyOf(filtered);
        }
        return translated;
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        this.itemQuadsCached = false;
        this.cachedItemQuads = null;
        this.worldQuadCache.clear();
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
    //?}

    //? if fabric {
    /*private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
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
    *///?}

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
