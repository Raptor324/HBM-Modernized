package com.hbm_m.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractMultipartBakedModel implements BakedModel {

    protected final Map<String, BakedModel> parts;
    protected final ItemTransforms transforms;
    private TextureAtlasSprite cachedParticleIcon;

    /**
     * Lazily-built per-cull-face quad merge for the default (non-dynamic)
     * {@code getQuadsForModelData} / {@code ...Fabric} implementations. Subclasses
     * with modelData-driven quads (e.g. {@code MachineFluidTankBakedModel}) override
     * those methods and never touch this cache. Index 0 = general {@code null} side,
     * 1..6 = {@link Direction} ordinals.
     */
    @SuppressWarnings("unchecked")
    private List<BakedQuad>[] sideQuadsCache;

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
    @Deprecated
    public TextureAtlasSprite getParticleIcon() {
        if (cachedParticleIcon == null) {
            if (!parts.isEmpty()) {
                cachedParticleIcon = parts.values().iterator().next().getParticleIcon();
            } else {
                cachedParticleIcon = Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
            }
        }
        return cachedParticleIcon;
    }

    //? if forge {
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

    protected List<BakedQuad> getQuadsForModelData(
        @Nullable BlockState state,
        @Nullable Direction side,
        RandomSource rand,
        ModelData modelData,
        @Nullable RenderType renderType
    ) {
        // Дефолтная реализация: просто собираем квады из всех частей без ModelData-логики.
        // Подклассы (например MachineFluidTankBakedModel) переопределяют для динамических текстур.
        //
        // renderType==null → item/BER hot path. Квады здесь определяются только
        // side (modelData эта дефолтная реализация игнорирует), поэтому кэшируем
        // по side, чтобы не плодить new ArrayList + addAll каждый кадр инвентаря.
        // renderType!=null → chunk-bake layer query; собираем заново, чтобы
        // корректно учитывать слой (solid/translucent).
        if (renderType == null) {
            int idx = side == null ? 0 : side.ordinal() + 1;
            List<BakedQuad>[] cache = sideQuadsCache;
            if (cache == null) {
                cache = (List<BakedQuad>[]) new List<?>[7];
                sideQuadsCache = cache;
            }
            List<BakedQuad> cached = cache[idx];
            if (cached != null) return cached;
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedModel part : parts.values()) {
                quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
            }
            List<BakedQuad> result = quads.isEmpty() ? List.of() : List.copyOf(quads);
            cache[idx] = result;
            return result;
        }
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedModel part : parts.values()) {
            quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                   RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        return getQuadsForModelData(state, side, rand, modelData, renderType);
    }


    /**
     * Vanilla BakedModel legacy methods (still required by the interface).
     * Delegate to Forge's extended overloads.
     */
    @Override
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    //?}

    //? if neoforge {
    /*// NeoForge 1.21.1: ванильный BakedModel.getQuads(BlockState, Direction, RandomSource)
    // остаётся абстрактным методом интерфейса; расширение с ModelData/RenderType — NeoForge-specific.
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        // 3-arg vanilla контракт: ModelData.EMPTY,без RenderType (см. forge-ветку выше).
        return getQuadsForModelDataNeo(state, side, rand, ModelData.EMPTY, null);
    }

    // NeoForge 1.21.1: BakedModel.getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)
    // — расширенная NeoForge-сигнатура. Реализация-делегат зеркалит forge-ветку.
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        return getQuadsForModelDataNeo(state, side, rand, modelData, renderType);
    }

    /^*
     * NeoForge-копия {@code getQuadsForModelData} (forge-ветка): дефолтная реализация
     * собирает квады из всех частей без ModelData-логики; подклассы могут переопределять.
     ^/
    protected List<BakedQuad> getQuadsForModelDataNeo(
        @Nullable BlockState state,
        @Nullable Direction side,
        RandomSource rand,
        ModelData modelData,
        @Nullable RenderType renderType
    ) {
        if (renderType == null) {
            int idx = side == null ? 0 : side.ordinal() + 1;
            List<BakedQuad>[] cache = sideQuadsCache;
            if (cache == null) {
                cache = (List<BakedQuad>[]) new List<?>[7];
                sideQuadsCache = cache;
            }
            List<BakedQuad> cached = cache[idx];
            if (cached != null) return cached;
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedModel part : parts.values()) {
                quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
            }
            List<BakedQuad> result = quads.isEmpty() ? List.of() : List.copyOf(quads);
            cache[idx] = result;
            return result;
        }
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedModel part : parts.values()) {
            quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
        }
        return quads;
    }
    *///?}

    //? if fabric {
    /*@Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuadsForModelDataFabric(state, side, rand);
    }

    /^* Fabric: без Forge ModelData — делегируем частям vanilla getQuads. ^/
    protected List<BakedQuad> getQuadsForModelDataFabric(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand
    ) {
        int idx = side == null ? 0 : side.ordinal() + 1;
        List<BakedQuad>[] cache = sideQuadsCache;
        if (cache == null) {
            cache = (List<BakedQuad>[]) new List<?>[7];
            sideQuadsCache = cache;
        }
        List<BakedQuad> cached = cache[idx];
        if (cached != null) return cached;
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedModel part : parts.values()) {
            quads.addAll(part.getQuads(state, side, rand));
        }
        List<BakedQuad> result = quads.isEmpty() ? List.of() : List.copyOf(quads);
        cache[idx] = result;
        return result;
    }
    *///?}

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

    /**
     * BEWLR items ({@link #isCustomRenderer}) apply {@code display} in {@code renderByItem}.
     * Forge {@code IForgeBakedModel#applyTransform} must stay a no-op for them.
     */

    //? if forge {
    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack,
                                     boolean applyLeftHandTransform) {
        if (isCustomRenderer()) {
            return this;
        }
        getTransforms().getTransform(transformType).apply(applyLeftHandTransform, poseStack);
        return this;
    }
    //?}

    public void clearCaches() {
        cachedParticleIcon = null;
    }
}
