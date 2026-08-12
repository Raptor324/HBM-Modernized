package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * Multipart baked model для CargoElevator.
 * Части: "Base", "Platform", "Piston", "Guides" (из elevator.obj).
 * В мире геометрия рендерится через BER+VBO, getQuads возвращает пустой список.
 * Для предмета собираются все квады.
 */
public class CargoElevatorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String[] PART_PRIORITY = {"Base", "Guides", "Piston", "Platform"};

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public CargoElevatorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
        this.cachedPartNames = parts.keySet().stream()
                .sorted((a, b) -> {
                    int aIndex = indexOf(PART_PRIORITY, a);
                    int bIndex = indexOf(PART_PRIORITY, b);
                    if (aIndex != -1 && bIndex != -1) return Integer.compare(aIndex, bIndex);
                    else if (aIndex != -1) return -1;
                    else if (bIndex != -1) return 1;
                    else return a.compareTo(b);
                })
                .toArray(String[]::new);
    }

    private static int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) return i;
        }
        return -1;
    }

    @Override
    public String[] getPartNames() {
        return cachedPartNames;
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData,
                                     @Nullable RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }
        // WORLD: геометрия полностью в BER/VBO
        return List.of();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                           ModelData modelData, @Nullable RenderType renderType) {
        if (!itemQuadsCached) {
            buildItemQuads(rand, modelData, renderType);
            itemQuadsCached = true;
        }
        if (side != null) {
            return cachedItemQuads.stream()
                    .filter(quad -> quad.getDirection() == side)
                    .toList();
        }
        return cachedItemQuads;
    }

    private void buildItemQuads(RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        for (String partName : getItemRenderPartNames()) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                for (Direction dir : Direction.values()) {
                    allQuads.addAll(part.getQuads(null, dir, rand, modelData, renderType));
                }
                allQuads.addAll(part.getQuads(null, null, rand, modelData, renderType));
            }
        }
        this.cachedItemQuads = allQuads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }
    //?}

    //? if fabric {
    /*@Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
        if (!itemQuadsCached) {
            buildItemQuads(rand);
            itemQuadsCached = true;
        }
        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }
        return cachedItemQuads;
    }

    private void buildItemQuads(RandomSource rand) {
        List<BakedQuad> allQuads = new ArrayList<>();
        for (String partName : getItemRenderPartNames()) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                for (Direction dir : Direction.values()) {
                    allQuads.addAll(part.getQuads(null, dir, rand));
                }
                allQuads.addAll(part.getQuads(null, null, rand));
            }
        }
        this.cachedItemQuads = allQuads;
    }
    *///?}

    @Override
    protected List<String> getItemRenderPartNames() {
        List<String> result = new ArrayList<>();
        for (String p : PART_PRIORITY) {
            if (parts.containsKey(p)) result.add(p);
        }
        return result;
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
        this.itemQuadsCached = false;
        this.cachedItemQuads = null;
    }
}