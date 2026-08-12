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
 * World render: пустой chunk mesh — вся геометрия в BER/VBO.
 * Поворот в BER: {@link MultipartFacingTransforms#chemicalPlantBakedRotationY}.
 * <p>
 * Часть {@code Fluid} с альфой — только в
 * {@link com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer} через translucent pass.
 */
public class MachineChemicalPlantBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String[] PRIORITY = { "Base", "Frame", "Slider", "Spinner", "Fluid" };

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public MachineChemicalPlantBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);

        this.cachedPartNames = parts.keySet().stream()
            .sorted((a, b) -> {
                int aIndex = indexOf(PRIORITY, a);
                int bIndex = indexOf(PRIORITY, b);
                if (aIndex != -1 && bIndex != -1) return Integer.compare(aIndex, bIndex);
                if (aIndex != -1) return -1;
                if (bIndex != -1) return 1;
                return a.compareTo(b);
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
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }
    //?}

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        //? if forge {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
        //?}

        //? if neoforge {
        /*return super.getQuads(state, side, rand);
        *///?}

        //? if fabric {
        /*if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
        *///?}
    }

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }
        // WORLD: геометрия полностью в BER/VBO.
        return List.of();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                        ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
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

    private void buildItemQuads(RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        for (String partName : new String[] { "Base", "Slider", "Spinner" }) {
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
    //?}

    //? if fabric {
    /*private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
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
        for (String partName : new String[] { "Base", "Slider", "Spinner" }) {
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
        return List.of("Base", "Slider", "Spinner");
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
        clearItemQuadCache();
    }

    public void clearItemQuadCache() {
        this.itemQuadsCached = false;
        this.cachedItemQuads = null;
    }
}
