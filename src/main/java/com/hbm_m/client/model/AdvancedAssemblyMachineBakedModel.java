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

public class AdvancedAssemblyMachineBakedModel implements BakedModel {

    private final Map<String, BakedModel> parts;
    private final ItemTransforms transforms; // <-- ДОБАВЛЕНО: храним трансформации для предмета

    // Конструктор обновлен
    public AdvancedAssemblyMachineBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        this.parts = parts;
        this.transforms = transforms; // <-- ДОБАВЛЕНО
    }

    public BakedModel getPart(String name) {
        return parts.get(name);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        // --- ИСПРАВЛЕНИЕ РЕНДЕРА ПРЕДМЕТА ---
        if (state == null) {
            // Собираем полигоны со ВСЕХ частей для рендера предмета
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedModel part : parts.values()) {
                quads.addAll(part.getQuads(null, side, rand));
            }
            return quads;
        }
        // Для блока в мире возвращаем пустоту (рендерит BER)
        return Collections.emptyList();
    }
    
    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return true; }
    
    @Override public TextureAtlasSprite getParticleIcon() {
        BakedModel base = parts.get("Base");
        // Получаем "missing model" из менеджера моделей и уже у нее берем иконку частицы.
        return base != null ? base.getParticleIcon() : Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
    }
    
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    
    @Override public ItemTransforms getTransforms() { 
        return this.transforms; 
    }
}