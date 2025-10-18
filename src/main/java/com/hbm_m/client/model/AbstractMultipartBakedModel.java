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

    protected AbstractMultipartBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        this.parts = parts;
        this.transforms = transforms;
    }

    public BakedModel getPart(String name) {
        return parts.get(name);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        if (cachedParticleIcon == null) {
            BakedModel base = parts.get("Base");
            if (base != null) {
                cachedParticleIcon = base.getParticleIcon(data);
            } else {
                BakedModel frame = parts.get("frame");
                if (frame != null) {
                    cachedParticleIcon = frame.getParticleIcon(data);
                } else if (!parts.isEmpty()) {
                    cachedParticleIcon = parts.values().iterator().next().getParticleIcon(data);
                } else {
                    cachedParticleIcon = Minecraft.getInstance()
                            .getModelManager()
                            .getMissingModel()
                            .getParticleIcon(data);
                }
            }
        }
        return cachedParticleIcon;
    }

    /**
     * КРИТИЧНО: Возвращаем ПУСТОЙ список для мира!
     * Рендер происходит через BlockEntityRenderer, НЕ через chunk mesh.
     */
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        // Если state != null - это запрос для мира (chunk mesh)
        // BlockEntity рендерится через TESR, поэтому НЕ добавляем квады в chunk
        if (shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }
        
        // Если state == null - это запрос для Item/GUI рендера
        // Возвращаем только Base часть для иконки в инвентаре
        if (state == null) {
            BakedModel base = parts.get("Base");
            if (base != null) {
                return base.getQuads(null, side, rand, modelData, renderType);
            }
        }
        
        return Collections.emptyList();
    }

    protected abstract boolean shouldSkipWorldRendering(@Nullable BlockState state);

    protected boolean shouldSkipSideRendering(@Nullable BlockState state, @Nullable Direction side) {
        return state == null && side != null;
    }

    protected BlockState getStateForPart(@Nullable BlockState state) {
        return state;
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
        return false;
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
}
