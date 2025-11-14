package com.hbm_m.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractMultipartBakedModel implements BakedModel {

    protected final Map<String, BakedModel> parts;
    protected final ItemTransforms transforms;
    private TextureAtlasSprite cachedParticleIcon;
    
    // УДАЛЕНО: Опасная рефлексия, которая вызывает краш JVM
    // Теперь используем только безопасные методы

    protected AbstractMultipartBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        this.parts = parts;
        this.transforms = transforms;
    }

    public BakedModel getPart(String name) {
        return parts.get(name);
    }

    public Map<String, BakedModel> getPartModels() {
        return Collections.unmodifiableMap(parts);
    }
    
    
    /**
     * БЕЗОПАСНАЯ ВЕРСИЯ: Получает список названий частей только из подклассов
     * БЕЗ рефлексии для избежания краша JVM
     */
    protected String[] getPartNamesInternal() {
        // Прямая проверка типа без рефлексии
        if (this instanceof PartNamesProvider provider) {
            return provider.getPartNames();
        }
        
        // Fallback: используем ключи Map в алфавитном порядке
        return parts.keySet().stream()
            .sorted()
            .toArray(String[]::new);
    }
    
    /**
     * Интерфейс для безопасного получения названий частей
     */
    public interface PartNamesProvider {
        String[] getPartNames();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        if (cachedParticleIcon == null) {
            // Используем getPartNamesInternal() для получения приоритетного порядка
            String[] partNames = getPartNamesInternal();
            
            // Сначала пробуем стандартные имена частей для иконки
            String[] iconParts = {"Base", "base", "frame", "Frame", "main", "Main"};
            
            for (String partName : iconParts) {
                BakedModel part = parts.get(partName);
                if (part != null) {
                    cachedParticleIcon = part.getParticleIcon(data);
                    break;
                }
            }
            
            // Если не нашли стандартные, используем первую часть из getPartNames()
            if (cachedParticleIcon == null && partNames.length > 0) {
                BakedModel part = parts.get(partNames[0]);
                if (part != null) {
                    cachedParticleIcon = part.getParticleIcon(data);
                }
            }
            
            // Если не нашли, берём первую доступную часть
            if (cachedParticleIcon == null && !parts.isEmpty()) {
                cachedParticleIcon = parts.values().iterator().next().getParticleIcon(data);
            }
            
            // Последний fallback - missing model
            if (cachedParticleIcon == null) {
                cachedParticleIcon = Minecraft.getInstance()
                    .getModelManager()
                    .getMissingModel()
                    .getParticleIcon(data);
            }
        }
        return cachedParticleIcon;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                   RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        
        // WORLD RENDER: state != null означает запрос от chunk mesh
        if (shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }
        
        // ITEM RENDER: state == null означает запрос для item/GUI рендера
        return Collections.emptyList();
    }

    protected abstract boolean shouldSkipWorldRendering(@Nullable BlockState state);

    protected boolean shouldSkipSideRendering(@Nullable BlockState state, @Nullable Direction side) {
        return state == null && side != null;
    }

    protected BlockState getStateForPart(@Nullable BlockState state) {
        return state;
    }
    
    protected List<String> getItemRenderPartNames() {
        String[] allPartNames = getPartNamesInternal();
        
        // Стандартный приоритетный порядок для item рендера
        String[] priorityParts = {"frame", "Frame", "doorLeft", "doorRight", "Base", "base", "main", "Main"};
        
        List<String> result = new java.util.ArrayList<>();
        
        // Сначала добавляем приоритетные части в нужном порядке
        for (String priorityPart : priorityParts) {
            if (parts.containsKey(priorityPart)) {
                result.add(priorityPart);
            }
        }
        
        // Затем добавляем остальные части
        for (String partName : allPartNames) {
            if (!result.contains(partName) && parts.containsKey(partName)) {
                result.add(partName);
            }
        }
        
        return result;
    }

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
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.transforms;
    }
    
    public void clearCaches() {
        cachedParticleIcon = null;
    }
}
