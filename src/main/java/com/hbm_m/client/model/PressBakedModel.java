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
        // Статика (Base) рендерится движком через инстансинг (MachineRenderers);
        // в чанк-меш модель не отдаём — иначе двойной рендер.
        return true;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        //? if forge {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
        //?}

        //? if neoforge {
        /*// 1.21.1 neoforge: чанк-бэкер вызывает 5-arg overload (см. ниже). 3-arg — item/BER hot path.
        // Зеркалируем forge-логику: ITEM — приоритетные части (Base+Head), WORLD — только Base (translate+filter).
        if (state == null) {
            return buildItemQuadsFromRenderParts(side, rand);
        }
        BakedModel basePart = parts.get(BASE);
        if (basePart != null) {
            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(basePart.getQuads(state, d, rand));
            }
            partQuads.addAll(basePart.getQuads(state, null, rand));
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

        //? if fabric {
        /*// ITEM RENDER
        if (state == null) {
            return getItemQuads(side, rand);
        }

        // WORLD RENDER: Base baked into chunk mesh
        BakedModel basePart = parts.get(BASE);
        if (basePart != null) {
            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(basePart.getQuads(state, d, rand));
            }
            partQuads.addAll(basePart.getQuads(state, null, rand));
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

    //? if neoforge {
    /*// NeoForge 1.21.1: 5-arg overload — вызывается чанк-бэкером. Зеркалируем forge-логику:
    // ITEM (state == null) — приоритетные части (Base+Head). WORLD — только Base (translate + filter по side); Head — BER.
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, net.neoforged.neoforge.client.model.data.ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return buildItemQuadsFromRenderParts(side, rand);
        }
        BakedModel basePart = parts.get(BASE);
        if (basePart != null) {
            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(basePart.getQuads(state, d, rand, modelData, renderType));
            }
            partQuads.addAll(basePart.getQuads(state, null, rand, modelData, renderType));
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
    *///?}

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // ITEM RENDER (Инвентарь/Рука)
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        // WORLD RENDER: статика (Base) рендерится движком (инстансинг) — в чанк не отдаём.
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
        for (String partName : getItemRenderPartNames()) {
            BakedModel part = parts.get(partName);
            if (part == null) continue;

            for (Direction dir : Direction.values()) {
                quads.addAll(part.getQuads(null, dir, rand));
            }
            quads.addAll(part.getQuads(null, null, rand));
        }
        return quads;
    }
    *///?}

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
        //? if forge {
        return getParticleIcon(ModelData.EMPTY);
        //?}

        //? if fabric {
        /*return super.getParticleIcon();
        *///?}

        //? if neoforge {
        /*return super.getParticleIcon();
        *///?}
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        cachedItemQuads = null;
        itemQuadsCached = false;
    }
}

