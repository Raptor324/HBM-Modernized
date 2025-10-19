package com.hbm_m.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DoorBakedModel implements BakedModel {
    private final Map<String, BakedModel> parts;
    private final ItemTransforms transforms;
    
    public DoorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        this.parts = parts;
        this.transforms = transforms;
    }
    
    public BakedModel getPart(String name) {
        return parts.get(name);
    }
    
    public String[] getPartNames() {
        return parts.keySet().toArray(new String[0]);
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand) {
        // state == null означает рендер в инвентаре/GUI
        // state != null означает рендер в мире - используем BER
        if (state != null) {
            // Блок в мире - отключаем стандартный рендер, используем BlockEntityRenderer
            return Collections.emptyList();
        }
        
        // Рендер в инвентаре/GUI - показываем все части
        // ИСПРАВЛЕНО: Собираем квады только для side == null (общая геометрия)
        if (side != null) {
            return Collections.emptyList();
        }
        
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedModel part : parts.values()) {
            // Запрашиваем квады с side = null для получения всей геометрии части
            quads.addAll(part.getQuads(null, null, rand));
        }
        
        return quads;
    }
    
    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return false; }
    @Override public boolean isCustomRenderer() { return false; }
    
    @Override
    public TextureAtlasSprite getParticleIcon() {
        // Приоритет: Base -> frame -> первая доступная часть
        BakedModel base = parts.get("Base");
        if (base != null) {
            return base.getParticleIcon();
        }
        
        BakedModel frame = parts.get("frame");
        if (frame != null) {
            return frame.getParticleIcon();
        }
        
        if (!parts.isEmpty()) {
            return parts.values().iterator().next().getParticleIcon();
        }
        
        return Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
    }
    
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    @Override public ItemTransforms getTransforms() { return this.transforms; }
}
