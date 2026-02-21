package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.custom.machines.MachineAssemblerBlock;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

public class MachineAssemblerBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String[] PART_PRIORITY = {"Body", "Slider", "Arm", "Cog"};
    private static final String[] ANIMATED_PARTS = {"Slider", "Arm", "Cog"};

    // 4 cog positions (base + BAKED_COG_OFFSET 0.5, 1.5) minus cog center (1, 0, 1) after bakedOffsetZ.
    private static final float[][] COG_IDLE_OFFSETS = {
            {-0.6f, 0.75f, 1.0625f},   // (-0.6, 0.75, 1.0625) + offset - center
            {0.6f, 0.75f, 1.0625f},    // (0.6, 0.75, 1.0625) + offset - center
            {-0.6f, 0.75f, -1.0625f},  // (-0.6, 0.75, -1.0625) + offset - center
            {0.6f, 0.75f, -1.0625f}   // (0.6, 0.75, -1.0625) + offset - center
    };

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public MachineAssemblerBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
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

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData,
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            // No shaders: full model is rendered by BER/VBO path.
            return List.of();
        }

        List<BakedQuad> result = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        // Compensate baked pipeline's extra offset (2 blocks in +Z) so baked matches VBO.
        float bakedOffsetZ = -1.0f;

        // Base is always baked-rendered.
        BakedModel bodyPart = parts.get("Body");
        if (bodyPart != null) {
            var bodyQuads = ModelHelper.translateQuads(
                    bodyPart.getQuads(state, side, rand, modelData, renderType), 0, 0, bakedOffsetZ);
            result.addAll(ModelHelper.transformQuadsByFacing(bodyQuads, rotationY));
        }

        // Animated parts are baked only while idle.
        boolean renderActive = state.hasProperty(MachineAssemblerBlock.RENDER_ACTIVE)
                && state.getValue(MachineAssemblerBlock.RENDER_ACTIVE);
        if (!renderActive) {
            for (String partName : ANIMATED_PARTS) {
                BakedModel part = parts.get(partName);
                if (part == null) continue;

                var rawQuads = part.getQuads(state, side, rand, modelData, renderType);
                var partQuads = ModelHelper.translateQuads(rawQuads, 0, 0, bakedOffsetZ);

                if ("Cog".equals(partName)) {
                    // Cog: render 4 copies at 4 slot positions (BER renders 4 cogs when active).
                    for (float[] offset : COG_IDLE_OFFSETS) {
                        var translated = ModelHelper.translateQuads(partQuads, offset[0], offset[1], offset[2]);
                        result.addAll(ModelHelper.transformQuadsByFacing(translated, rotationY));
                    }
                } else {
                    result.addAll(ModelHelper.transformQuadsByFacing(partQuads, rotationY));
                }
            }
        }

        return result;
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
    protected List<String> getItemRenderPartNames() {
        List<String> result = new ArrayList<>();
        for (String p : PART_PRIORITY) {
            if (parts.containsKey(p)) result.add(p);
        }
        return result;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        // cutoutMipped для прозрачных текстур (стекло, решётки и т.д.)
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        this.itemQuadsCached = false;
        this.cachedItemQuads = null;
    }

    private static int getRotationYForFacing(BlockState state) {
        if (!state.hasProperty(MachineAssemblerBlock.FACING)) return 90;
        return (switch (state.getValue(MachineAssemblerBlock.FACING)) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        }) % 360;
    }
}
