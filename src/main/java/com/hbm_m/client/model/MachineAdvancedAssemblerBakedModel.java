package com.hbm_m.client.model;

// Модель продвинутой сборочной машины, состоящая из нескольких частей.
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

public class MachineAdvancedAssemblerBakedModel implements BakedModel {

    private final Map<String, BakedModel> parts;
    private final ItemTransforms transforms; // храним трансформации для предмета

    public MachineAdvancedAssemblerBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        this.parts = parts;
        this.transforms = transforms;
    }

    public BakedModel getPart(String name) {
        return parts.get(name);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        // Рендер в мире по-прежнему обрабатывается через BlockEntityRenderer (BER),
        // но теперь нам нужен способ его "отключить" без isCustomRenderer().
        // Мы будем возвращать пустоту, ТОЛЬКО если есть BlockState, и он имеет наш BlockEntity.
        if (state != null && state.hasBlockEntity()) {
            return Collections.emptyList();
        }

        // --- РЕНДЕР ПРЕДМЕТА (state == null) или блока в инвентаре (например, в креативе) ---
        // Собираем полигоны со ВСЕХ частей для полноценного отображения.
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedModel part : parts.values()) {
            quads.addAll(part.getQuads(state, side, rand));
        }
        return quads;
    }
    
    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    
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