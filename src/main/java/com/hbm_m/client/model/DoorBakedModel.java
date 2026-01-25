package com.hbm_m.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.custom.doors.DoorDecl;
import com.hbm_m.block.entity.custom.doors.DoorDeclRegistry;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.DoorPartAABBRegistry;
import com.hbm_m.physics.PartAABBExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DoorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {
    
    private final String[] cachedPartNames;
    private final ResourceLocation doorId;
    
    // Кэш квадов для item рендера
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;
    
    public DoorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, ResourceLocation doorId) {
        super(parts, transforms);
        this.doorId = doorId;
        
        // Кешируем имена частей из JSON
        this.cachedPartNames = parts.keySet().toArray(new String[0]);
        
        // Инициализируем DoorDecl сразу при создании модели
        initializeDoorDecl();
        extractAndRegisterAABBs();
    }

    private void extractAndRegisterAABBs() {
        String doorIdStr = extractDoorId(doorId.getPath());
        
        // Извлекаем AABB БЕЗ масштабирования (в нормализованных координатах 0..1)
        Map<String, AABB> partAABBs = PartAABBExtractor.extractAll(parts);
        
        if (!partAABBs.isEmpty()) {
            DoorPartAABBRegistry.register(doorIdStr, partAABBs);
            
            if (DoorDeclRegistry.contains(doorIdStr)) {
                DoorDecl decl = DoorDeclRegistry.getById(doorIdStr);
                if (decl != null) {
                    decl.setPartAABBs(partAABBs);
                }
            }
            
            MainRegistry.LOGGER.info("DoorBakedModel: Extracted {} part AABBs for door {}",
                    partAABBs.size(), doorIdStr);
        } else {
            MainRegistry.LOGGER.warn("DoorBakedModel: No AABBs extracted for door {}", doorIdStr);
        }
    }

    private String extractDoorId(String path) {
        String pathWithoutDir = path.contains("/") 
            ? path.substring(path.lastIndexOf('/') + 1) 
            : path;
        return pathWithoutDir.endsWith(".obj") 
            ? pathWithoutDir.substring(0, pathWithoutDir.length() - 4) 
            : pathWithoutDir;
    }
    
    /**
     * Инициализирует DoorDecl с информацией о частях
     */
    private void initializeDoorDecl() {
        // Извлекаем ID двери из ResourceLocation
        // ИСПРАВЛЕНО: Убираем путь и расширение .obj
        String fullPath = doorId.getPath();
        
        // Убираем "models/block/" если есть
        String pathWithoutDir = fullPath.contains("/") 
            ? fullPath.substring(fullPath.lastIndexOf('/') + 1) 
            : fullPath;
        
        // Убираем расширение .obj
        String id = pathWithoutDir.endsWith(".obj") 
            ? pathWithoutDir.substring(0, pathWithoutDir.length() - 4) 
            : pathWithoutDir;
        
        // Проверяем, зарегистрирована ли дверь
        if (DoorDeclRegistry.contains(id)) {
            DoorDecl decl = DoorDeclRegistry.getById(id);
            if (decl != null) {
                // Устанавливаем части из модели
                decl.setPartsFromModel(cachedPartNames, parts);
                MainRegistry.LOGGER.info("Initialized DoorDecl for {} with {} parts: {}", 
                    id, cachedPartNames.length, String.join(", ", cachedPartNames));
            }
        } else {
            // Отладочное логирование
            MainRegistry.LOGGER.warn("Door ID '{}' (from path '{}') not found in DoorDeclRegistry. Available doors: {}", 
                id, fullPath, String.join(", ", DoorDeclRegistry.getAll().keySet()));
        }
    }
    
    @Override
    public String[] getPartNames() {
        return cachedPartNames;
    }
    
    public Map<String, BakedModel> getParts() {
        return parts;
    }
    
    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Пропускаем ТОЛЬКО world рендер (когда state НЕ null)
        return state != null;
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, 
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
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
                                          ModelData modelData, 
                                          @Nullable net.minecraft.client.renderer.RenderType renderType) {
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
    
    private void buildItemQuads(RandomSource rand, ModelData modelData, 
                                 @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        
        // Используем правильный порядок частей из базового класса
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
