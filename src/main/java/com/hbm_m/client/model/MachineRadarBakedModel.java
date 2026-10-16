package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * Малый радар: {@code Base} + {@code Dish}; большой — {@code Radar} + {@code Dish}.
 * При {@link ShaderCompatibilityDetector#useVboGeometry()} chunk mesh пуст — геометрия в BER/VBO.
 */
public class MachineRadarBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String[] PRIORITY = { "Base", "Radar", "Dish" };

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public MachineRadarBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);

        this.cachedPartNames = parts.keySet().stream()
                .sorted((a, b) -> {
                    int aIndex = indexOf(PRIORITY, a);
                    int bIndex = indexOf(PRIORITY, b);
                    if (aIndex != -1 && bIndex != -1) {
                        return Integer.compare(aIndex, bIndex);
                    }
                    if (aIndex != -1) {
                        return -1;
                    }
                    if (bIndex != -1) {
                        return 1;
                    }
                    return a.compareTo(b);
                })
                .toArray(String[]::new);
    }

    private static int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    /** Статическая часть (основание): {@code Base} или {@code Radar}. */
    public String getStaticPartName() {
        // Большой радар: loader добавляет пустой fallback Base — игнорируем, берём Radar.
        if (parts.containsKey("Radar")) {
            return "Radar";
        }
        if (parts.containsKey("Base")) {
            return "Base";
        }
        return cachedPartNames.length > 0 ? cachedPartNames[0] : "Base";
    }

    public boolean isLargeRadar() {
        return parts.containsKey("Radar");
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
        /*// 1.21.1 neoforge: чанк-бэкер вызывает 5-arg overload (см. ниже). 3-arg — item/BER hot path.
        // Зеркалируем forge-логику: ITEM — приоритетные части, WORLD — staticPart или List.of() (VBO).
        if (state == null) {
            return buildItemQuadsFromRenderParts(side, rand);
        }
        if (ShaderCompatibilityDetector.useVboGeometry()) {
            return List.of();
        }
        BakedModel staticPart = parts.get(getStaticPartName());
        if (staticPart == null) {
            return List.of();
        }
        return staticPart.getQuads(state, side, rand);
        *///?}

        //? if fabric {
        /*if (state == null) {
            return getItemQuads(side, rand);
        }
        if (ShaderCompatibilityDetector.useVboGeometry()) {
            return List.of();
        }
        BakedModel staticPart = parts.get(getStaticPartName());
        if (staticPart == null) {
            return List.of();
        }
        return staticPart.getQuads(state, side, rand);
        *///?}
    }

    //? if neoforge {
    /*// NeoForge 1.21.1: 5-arg overload — вызывается чанк-бэкером. Зеркалируем forge-логику:
    // ITEM (state == null) — приоритетные части. WORLD — staticPart или List.of() если useVboGeometry.
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, net.neoforged.neoforge.client.model.data.ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return buildItemQuadsFromRenderParts(side, rand);
        }
        if (ShaderCompatibilityDetector.useVboGeometry()) {
            return List.of();
        }
        BakedModel staticPart = parts.get(getStaticPartName());
        if (staticPart == null) {
            return List.of();
        }
        return staticPart.getQuads(state, side, rand, modelData, renderType);
    }
    *///?}

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }
        if (ShaderCompatibilityDetector.useVboGeometry()) {
            return List.of();
        }

        BakedModel staticPart = parts.get(getStaticPartName());
        if (staticPart == null) {
            return List.of();
        }
        return staticPart.getQuads(state, side, rand, modelData, renderType);
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
        for (String partName : getItemRenderPartNames()) {
            BakedModel part = parts.get(partName);
            if (part == null) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                allQuads.addAll(part.getQuads(null, dir, rand, modelData, renderType));
            }
            allQuads.addAll(part.getQuads(null, null, rand, modelData, renderType));
        }
        this.cachedItemQuads = allQuads;
    }
    //?}

    @Override
    protected List<String> getItemRenderPartNames() {
        List<String> names = new ArrayList<>();
        for (String name : cachedPartNames) {
            if (isLargeRadar() && "Base".equals(name)) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        this.itemQuadsCached = false;
        this.cachedItemQuads = null;
    }
}
