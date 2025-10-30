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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MachineAdvancedAssemblerBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    // КРИТИЧНАЯ ОПТИМИЗАЦИЯ: Кэш массива имён частей
    private final String[] cachedPartNames;
    
    // Кэш квадов для item рендера (инвентарь, рука, земля)
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public MachineAdvancedAssemblerBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
        
        // Определяем приоритетный порядок частей для сборочной машины
        this.cachedPartNames = parts.keySet().stream()
            .sorted((a, b) -> {
                // Приоритетный порядок: Base -> ring -> arms
                String[] priority = {"Base", "Frame", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
        "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
                int aIndex = indexOf(priority, a);
                int bIndex = indexOf(priority, b);
                
                if (aIndex != -1 && bIndex != -1) {
                    return Integer.compare(aIndex, bIndex);
                } else if (aIndex != -1) {
                    return -1;
                } else if (bIndex != -1) {
                    return 1;
                } else {
                    return a.compareTo(b);
                }
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

    /**
     * БЕЗОПАСНАЯ РЕАЛИЗАЦИЯ: Через интерфейс, а не рефлексию
     */
    @Override
    public String[] getPartNames() {
        return cachedPartNames;
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Пропускаем world рендер только если есть BlockEntity
        return state != null && state.hasBlockEntity();
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
        // WORLD RENDER: Пропускаем (рендерится через BlockEntityRenderer)
        if (shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }

        // ITEM RENDER: Рендерим для GUI, руки, земли и тд
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        return Collections.emptyList();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                          ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // Кэшируем квады для item рендера
        if (!itemQuadsCached) {
            buildItemQuads(rand, modelData, renderType);
            itemQuadsCached = true;
        }

        // Фильтруем по стороне если нужно
        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }

        return cachedItemQuads;
    }

    private void buildItemQuads(RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        
        // Используем правильный порядок частей: Base + ring + arms
        List<String> itemRenderParts = getItemRenderPartNames();
        
        for (String partName : itemRenderParts) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                // Добавляем все квады этой части
                for (Direction dir : Direction.values()) {
                    List<BakedQuad> partQuads = part.getQuads(null, dir, rand, modelData, renderType);
                    allQuads.addAll(partQuads);
                }
                
                // Добавляем квады без направления (cullface == null)
                List<BakedQuad> generalQuads = part.getQuads(null, null, rand, modelData, renderType);
                allQuads.addAll(generalQuads);
            }
        }
        
        this.cachedItemQuads = allQuads;
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        // Для сборочной машины в GUI рендерим: Base + ring + arms
        List<String> result = new ArrayList<>();
        
        // Приоритетный порядок для item рендера
        String[] priorityParts = {"Base", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
        "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
        
        for (String priorityPart : priorityParts) {
            if (parts.containsKey(priorityPart)) {
                result.add(priorityPart);
            }
        }
        
        // Добавляем остальные части если есть
        // for (String partName : cachedPartNames) {
        //     if (!result.contains(partName) && parts.containsKey(partName)) {
        //         result.add(partName);
        //     }
        // }
        
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
}
