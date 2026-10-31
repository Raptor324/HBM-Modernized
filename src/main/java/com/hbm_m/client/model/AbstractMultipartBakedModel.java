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
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractMultipartBakedModel implements BakedModel {

    protected final Map<String, BakedModel> parts;
    protected final ItemTransforms transforms;
    private TextureAtlasSprite cachedParticleIcon;
    /**
     * Спрайт "particle" из JSON модели (инжектируется загрузчиком семейства
     * AbstractObjPartModelLoader при запекании). Приоритетнее авто-выбора части,
     * потому что авто-выбор может взять парт без привязанных спрайтов (пустые
     * алиасы/"Base"-фолбэки) и выдать missing-спрайт (фиолетовые частицы при
     * ломании блока).
     */
    @Nullable private TextureAtlasSprite jsonParticleIcon;

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

    /** Инжектирует спрайт "particle" из JSON модели (вызывается загрузчиком после запекания). */
    public void setParticleIcon(@Nullable TextureAtlasSprite sprite) {
        this.jsonParticleIcon = sprite;
        this.cachedParticleIcon = null;
    }

    /**
     * Спрайт НЕ считается missing-текстурой (фиолетовый/чёрный checkerboard).
     * contents() бывает null у тестовых моков — тогда считаем спрайт пригодным.
     */
    private static boolean isUsableIcon(@Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            return false;
        }
        try {
            return sprite.contents() == null
                || !net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()
                    .equals(sprite.contents().name());
        } catch (Throwable t) {
            // Мок-спрайты без внутренностей: не блокируем выбор.
            return true;
        }
    }

    /**
     * Общий выбор спрайта частиц: инжектированный "particle" из JSON, затем части
     * по приоритетным именам, затем остальные части. Кандидаты с null/missing
     * спрайтом ПРОПУСКАЮТСЯ (раньше первый же кандидат кэшировался даже если это
     * был missing-спрайт пустой части — источник фиолетовых частиц при ломании).
     */
    @Nullable
    private TextureAtlasSprite resolveParticleIcon(Function<BakedModel, TextureAtlasSprite> partIcon) {
        if (isUsableIcon(jsonParticleIcon)) {
            return jsonParticleIcon;
        }
        Set<BakedModel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        // Сначала стандартные имена частей для иконки.
        String[] iconParts = {"Base", "base", "frame", "Frame", "main", "Main"};
        for (String partName : iconParts) {
            BakedModel part = parts.get(partName);
            if (part != null && seen.add(part)) {
                TextureAtlasSprite sprite = partIcon.apply(part);
                if (isUsableIcon(sprite)) {
                    return sprite;
                }
            }
        }
        // Затем приоритетный порядок частей из getPartNames().
        for (String partName : getPartNamesInternal()) {
            BakedModel part = parts.get(partName);
            if (part != null && seen.add(part)) {
                TextureAtlasSprite sprite = partIcon.apply(part);
                if (isUsableIcon(sprite)) {
                    return sprite;
                }
            }
        }
        // Наконец — любая оставшаяся часть.
        for (BakedModel part : parts.values()) {
            if (part != null && seen.add(part)) {
                TextureAtlasSprite sprite = partIcon.apply(part);
                if (isUsableIcon(sprite)) {
                    return sprite;
                }
            }
        }
        return null;
    }

    @Nullable
    private TextureAtlasSprite missingModelIcon(@Nullable ModelData data) {
        // getMissingModel() ещё null во время запекания (ModifyBakingResult) — тогда
        // возвращаем null и попробуем снова при следующем запросе после загрузки.
        try {
            return Minecraft.getInstance()
                .getModelManager()
                .getMissingModel()
                .getParticleIcon(data);
        } catch (NullPointerException | IllegalStateException e) {
            return null;
        }
    }

    @Override
    @Deprecated
    public TextureAtlasSprite getParticleIcon() {
        if (cachedParticleIcon == null) {
            cachedParticleIcon = resolveParticleIcon(BakedModel::getParticleIcon);
            if (cachedParticleIcon == null && !parts.isEmpty()) {
                cachedParticleIcon = missingModelIcon(null);
            }
        }
        return cachedParticleIcon;
    }

    //? if forge {
    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        if (cachedParticleIcon == null) {
            cachedParticleIcon = resolveParticleIcon(part -> part.getParticleIcon(data));
            if (cachedParticleIcon == null && !parts.isEmpty()) {
                cachedParticleIcon = missingModelIcon(data);
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
        // ОПТИМИЗАЦИЯ: Если блок в мире рисуется через BER/VBO, немедленно возвращаем пустой список,
        // предотвращая аллокации ArrayList во время запекания чанков.
        if (state != null && shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }

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
            Set<BakedModel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (BakedModel part : parts.values()) {
                if (part != null && seen.add(part)) {
                    quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
                }
            }
            List<BakedQuad> result = quads.isEmpty() ? List.of() : List.copyOf(quads);
            cache[idx] = result;
            return result;
        }
        List<BakedQuad> quads = new ArrayList<>();
        Set<BakedModel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BakedModel part : parts.values()) {
            if (part != null && seen.add(part)) {
                quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
            }
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
        if (state != null && shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }

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
            Set<BakedModel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (BakedModel part : parts.values()) {
                if (part != null && seen.add(part)) {
                    quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
                }
            }
            List<BakedQuad> result = quads.isEmpty() ? List.of() : List.copyOf(quads);
            cache[idx] = result;
            return result;
        }
        List<BakedQuad> quads = new ArrayList<>();
        Set<BakedModel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BakedModel part : parts.values()) {
            if (part != null && seen.add(part)) {
                quads.addAll(part.getQuads(state, side, rand, modelData, renderType));
            }
        }
        return quads;
    }
    *///?}

    /**
     * NeoForge 1.21.1 ITEM-render helper: строит квады только из приоритетных
     * частей ({@link #getItemRenderPartNames()}), а не из всех parts (как
     * {@code super.getQuads}). Зеркалирует forge/fabric {@code buildItemQuads}.
     * Возвращает {@code List.of()} если частей нет.
     */
    //? if neoforge {
    /*protected List<BakedQuad> buildItemQuadsFromRenderParts(@Nullable Direction side, RandomSource rand) {
        List<BakedQuad> allQuads = new ArrayList<>();
        for (String partName : getItemRenderPartNames()) {
            BakedModel part = parts.get(partName);
            if (part == null) continue;
            for (Direction dir : Direction.values()) {
                allQuads.addAll(part.getQuads(null, dir, rand));
            }
            allQuads.addAll(part.getQuads(null, null, rand));
        }
        if (side != null) {
            return allQuads.stream().filter(q -> q.getDirection() == side).toList();
        }
        return allQuads.isEmpty() ? List.of() : allQuads;
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
        Set<BakedModel> seenModels = Collections.newSetFromMap(new IdentityHashMap<>());
        
        // Сначала добавляем приоритетные части в нужном порядке
        for (String priorityPart : priorityParts) {
            BakedModel model = parts.get(priorityPart);
            if (model != null && seenModels.add(model)) {
                result.add(priorityPart);
            }
        }
        
        // Затем добавляем остальные части
        for (String partName : allPartNames) {
            BakedModel model = parts.get(partName);
            if (model != null && !result.contains(partName) && seenModels.add(model)) {
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

    // NeoForge: gleiche Signatur in IBakedModelExtension
    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack,
                                     boolean applyLeftHandTransform) {
        if (isCustomRenderer()) {
            return this;
        }
        getTransforms().getTransform(transformType).apply(applyLeftHandTransform, poseStack);
        return this;
    }

    public void clearCaches() {
        cachedParticleIcon = null;
    }
}
