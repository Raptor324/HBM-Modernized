package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.custom.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class MachineAdvancedAssemblerBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public MachineAdvancedAssemblerBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
        
        this.cachedPartNames = parts.keySet().stream()
            .sorted((a, b) -> {
                String[] priority = {"Base", "Frame", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
        "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
                int aIndex = indexOf(priority, a);
                int bIndex = indexOf(priority, b);
                
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
                                     RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // ITEM RENDER (Инвентарь/Рука)
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        // WORLD RENDER: переключение по стороннему шейдеру (Iris/Oculus)
        // Нет шейдера → модель пустая, всё рендерит BER (VBO).
        // Шейдер активен: Base+Frame всегда; анимированные — только когда машина спит (особый рендер при работе).
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            return List.of();
        }

        List<BakedQuad> result = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);

        // 1. Base
        BakedModel basePart = parts.get("Base");
        if (basePart != null) {
            result.addAll(ModelHelper.transformQuadsByFacing(
                    basePart.getQuads(state, side, rand, modelData, renderType), rotationY));
        }

        // 2. Frame (зависит от свойства FRAME)
        if (state.hasProperty(MachineAdvancedAssemblerBlock.FRAME) 
                && state.getValue(MachineAdvancedAssemblerBlock.FRAME)) {
            BakedModel framePart = parts.get("Frame");
            if (framePart != null) {
                result.addAll(ModelHelper.transformQuadsByFacing(
                        framePart.getQuads(state, side, rand, modelData, renderType), rotationY));
            }
        }
        
        // 3. Анимированные части — только когда машина спит. При работе их рендерит BER (putBulkData).
        boolean renderActive = state.hasProperty(MachineAdvancedAssemblerBlock.RENDER_ACTIVE) 
                             && state.getValue(MachineAdvancedAssemblerBlock.RENDER_ACTIVE);
        if (!renderActive) {
            String[] animatedParts = {"Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
                                      "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
            for (String partName : animatedParts) {
                BakedModel part = parts.get(partName);
                if (part != null) {
                    result.addAll(ModelHelper.transformQuadsByFacing(
                            part.getQuads(state, side, rand, modelData, renderType), rotationY));
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
        List<String> itemRenderParts = getItemRenderPartNames();
        
        for (String partName : itemRenderParts) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                for (Direction dir : Direction.values()) {
                    List<BakedQuad> partQuads = part.getQuads(null, dir, rand, modelData, renderType);
                    allQuads.addAll(partQuads);
                }
                List<BakedQuad> generalQuads = part.getQuads(null, null, rand, modelData, renderType);
                allQuads.addAll(generalQuads);
            }
        }
        this.cachedItemQuads = allQuads;
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        List<String> result = new ArrayList<>();
        String[] priorityParts = {"Base", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
        "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
        
        for (String priorityPart : priorityParts) {
            if (parts.containsKey(priorityPart)) {
                result.add(priorityPart);
            }
        }
        return result;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
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

    private static int getRotationYForFacing(BlockState state) {
        if (!state.hasProperty(MachineAdvancedAssemblerBlock.FACING)) return 90;
        return (switch (state.getValue(MachineAdvancedAssemblerBlock.FACING)) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        } + 270) % 360;
    }
}