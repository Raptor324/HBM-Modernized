package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}
import org.jetbrains.annotations.Nullable;

/**
 * Baked missile mesh for inventory / BER / entity VBO paths. World chunk mesh is skipped (VBO only).
 */
public class MissileBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private final String[] partNames;
    private final ResourceLocation modelId;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached;

    public MissileBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, ResourceLocation modelId) {
        super(parts, transforms);
        this.partNames = parts.keySet().toArray(new String[0]);
        this.modelId = modelId;
    }

    public ResourceLocation getModelId() {
        return modelId;
    }

    /**
     * Items whose BEWLR applies JSON {@code display} (e.g. range detonator). Forge / Embeddium may call
     * {@link #getTransforms()} before BEWLR; those callers must see identity transforms so display is not doubled.
     */
    public boolean usesJsonDisplayInBewlr() {
        String path = modelId.getPath();
        return path.contains("detonator") || path.contains("range_detonator");
    }

    /** Baked JSON {@code display} for BEWLR — not exposed via {@link #getTransforms()} when {@link #usesJsonDisplayInBewlr()}. */
    public ItemTransforms getBewlrDisplayTransforms() {
        return transforms;
    }

    /** OBJ parts from the baked item model (excludes empty fallback {@code Base}). */
    public List<String> getRenderablePartNames() {
        return Arrays.stream(partNames)
                .filter(name -> !"Base".equals(name))
                .filter(name -> parts.get(name) != null)
                .toList();
    }

    @Override
    public String[] getPartNames() {
        return partNames;
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        return getRenderablePartNames();
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return state != null;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        //? if forge {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
        //?}

        //? if neoforge {
        /*/// neoforge 3-arg — ITEM/BER hot path. WORLD (state != null) → List.of() (VBO owns geometry),
        /// ITEM → приоритетные части через getItemQuads.
        if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
        *///?}

        //? if fabric {
        /*if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
        *///?}
    }

    //? if forge || neoforge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
    }
    //?}

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
        if (!itemQuadsCached || cachedItemQuads == null) {
            // Кэш собираем один раз для ВСЕХ направлений + unsided, иначе первый вызов
            // с конкретным side затравит кэш только одной стороной и getQuads(null) вернёт
            // плоский «блин». В item-проходах vanilla перебирает все Direction.values()+null.
            List<BakedQuad> all = new ArrayList<>();
            //? if forge {
            ModelData modelData = ModelData.EMPTY;
            RenderType renderType = RenderType.solid();
            //?}
            for (String partName : getItemRenderPartNames()) {
                BakedModel part = parts.get(partName);
                if (part == null) {
                    continue;
                }
                for (Direction d : Direction.values()) {
                    //? if forge {
                    all.addAll(part.getQuads(null, d, rand, modelData, renderType));
                    //?}
                    //? if fabric {
                    /*all.addAll(part.getQuads(null, d, rand));
                    *///?}
                }
                //? if forge {
                all.addAll(part.getQuads(null, null, rand, modelData, renderType));
                //?}
                //? if fabric {
                /*all.addAll(part.getQuads(null, null, rand));
                *///?}
            }
            cachedItemQuads = all;
            itemQuadsCached = true;
        }
        if (side != null) {
            return cachedItemQuads.stream()
                    .filter(quad -> quad.getDirection() == side)
                    .toList();
        }
        return cachedItemQuads;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    /** Item icons (GUI) and in-world item forms route through {@link com.hbm_m.client.render.item.ItemRenderMissileGeneric}. */
    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public ItemTransforms getTransforms() {
        if (usesJsonDisplayInBewlr()) {
            return ItemTransforms.NO_TRANSFORMS;
        }
        return super.getTransforms();
    }

}
