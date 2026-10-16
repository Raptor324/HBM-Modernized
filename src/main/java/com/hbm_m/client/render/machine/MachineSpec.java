package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Иммутабельное описание рендера станка: набор частей (статические/анимированные/
 * динамические), хуки, резолверы модели и facing. Создаётся через
 * {@link MachineSpecBuilder}; рантайм-кеши VBO/инстансеров живут здесь же и
 * инвалидируются через {@link #clear()} (из RenderCacheManager).
 */
public final class MachineSpec<T extends BlockEntity> {

    /**
     * Описание одной части. {@code name} — уникальный ключ; {@code modelPartName} — имя части модели.
     *
     * <p>{@code animated} — признак АНИМИРОВАННОГО КОНТЕНТА (руки, шестерни, слайдеры):
     * такие части гаснут/скипаются по дистанции {@code modelUpdateDistance}.
     * Аниматор без анимации (легаси-офсеты запечки) — НЕ анимированная часть: она
     * обязана жить до статической отсечки, см. {@link MachineSpecBuilder#staticPart}.
     */
    record PartDef<T extends BlockEntity>(
            String name,
            String modelPartName,
            @Nullable PartAnimator<T> animator,      // null = трансформ не нужен
            @Nullable QuadResolver<T> dynamicQuads,  // null = брать часть модели по имени
            @Nullable Function<T, String> dynamicCacheKey,
            int boneId,                               // 0 = не bone-часть; 1..N = chain-группа
            boolean animated,                         // гейт по modelUpdateDistance
            String staticCacheKey,                    // предвычисленный "id/name" — без String-аллокаций в hot path
            @Nullable Function<T, Integer> lightOverride // null/-1 = свет мира; >=0 = форсированный packedLight (fullbright)
    ) {
        boolean dynamic() { return dynamicQuads != null; }
    }

    final String id;
    final Class<T> beClass;
    final net.minecraft.world.level.block.entity.BlockEntityType<T> type;
    final Function<T, BakedModel> modelResolver;
    final Function<T, Direction> facingResolver;
    final List<PartDef<T>> parts;
    final List<MachineRenderHook<T>> hooks;
    final int viewDistance; // -1 = дефолт по конфигу статики
    /** Стабильный ключ LightSampleCache для одного 8-corner сэмпла на машину за кадр. */
    final long lightSampleKey;
    @Nullable final MachineSpecBuilder.BlockTransform<T> blockTransform; // null = дефолтный setupBlockTransform

    // Конфиг multipart-модели (ConfiguredMultipartBakedModel), вливается в инстанс
    // в конце запекания моделей (MachineRenderRegistry.bindBakedModels).
    /** Части item-рендера; null = все части модели. */
    final @Nullable List<String> itemParts;
    /** Исключения из item-рендера; применяется после itemParts (когда та null). */
    final @Nullable List<String> itemExcept;
    /** Render types чанк-пасса (forge getRenderTypes); null = дефолт. */
    final @Nullable List<net.minecraft.client.renderer.RenderType> chunkRenderTypes;

    // Runtime: full cache key → GPU-держатель части. Кешируется между кадрами.
    private final Map<String, MachinePartRenderer> partRenderers = new ConcurrentHashMap<>();

    MachineSpec(String id, Class<T> beClass, net.minecraft.world.level.block.entity.BlockEntityType<T> type,
                Function<T, BakedModel> modelResolver,
                Function<T, Direction> facingResolver, List<PartDef<T>> parts,
                List<MachineRenderHook<T>> hooks, int viewDistance,
                @Nullable MachineSpecBuilder.BlockTransform<T> blockTransform,
                @Nullable List<String> itemParts, @Nullable List<String> itemExcept,
                @Nullable List<net.minecraft.client.renderer.RenderType> chunkRenderTypes) {
        this.id = id;
        this.beClass = beClass;
        this.type = type;
        this.modelResolver = modelResolver;
        this.facingResolver = facingResolver;
        this.parts = List.copyOf(parts);
        this.hooks = List.copyOf(hooks);
        this.viewDistance = viewDistance;
        this.lightSampleKey = (0x4D4143484C534B4FL) ^ (id.hashCode() * 0x9E3779B97F4A7C15L);
        this.blockTransform = blockTransform;
        this.itemParts = itemParts == null ? null : List.copyOf(itemParts);
        this.itemExcept = itemExcept == null ? null : List.copyOf(itemExcept);
        this.chunkRenderTypes = chunkRenderTypes == null ? null : List.copyOf(chunkRenderTypes);
    }

    net.minecraft.world.level.block.entity.BlockEntityType<T> type() { return type; }
    @Nullable List<String> itemParts() { return itemParts; }
    @Nullable List<String> itemExcept() { return itemExcept; }
    @Nullable List<net.minecraft.client.renderer.RenderType> chunkRenderTypes() { return chunkRenderTypes; }

    /**
     * Итоговый список частей item-рендера. Явный {@code itemParts} побеждает;
     * по умолчанию — НЕ-динамические части спеки (то, что BER рисует статикой
     * и анимацией), минус {@code itemExcept}. Динамические части (пер-BE
     * геометрия: уровни жидкости, условная рама) и вообще необъявленные
     * (мусорные группы OBJ) в item не попадают.
     */
    List<String> deriveItemParts() {
        if (itemParts != null) {
            return itemParts;
        }
        List<String> out = new ArrayList<>();
        for (PartDef<?> p : parts) {
            if (!p.dynamic() && !out.contains(p.modelPartName())) {
                out.add(p.modelPartName());
            }
        }
        if (itemExcept != null) {
            out.removeIf(itemExcept::contains);
        }
        return out;
    }

    @Nullable MachineSpecBuilder.BlockTransform<T> blockTransform() { return blockTransform; }

    String id() { return id; }
    List<PartDef<T>> parts() { return parts; }
    List<MachineRenderHook<T>> hooks() { return hooks; }
    Function<T, BakedModel> modelResolver() { return modelResolver; }
    Function<T, Direction> facingResolver() { return facingResolver; }
    int viewDistance() { return viewDistance; }

    /** Квады/модель части для этого BE (или null, если части нет в модели). */
    @Nullable BakedModel partModel(PartDef<T> part, BakedModel multipartModel) {
        if (part.dynamic()) return null;
        return (multipartModel instanceof com.hbm_m.client.model.AbstractMultipartBakedModel mp)
                ? mp.getPart(part.modelPartName()) : null;
    }

    @Nullable List<net.minecraft.client.renderer.block.model.BakedQuad> dynamicQuads(PartDef<T> part, T be) {
        if (!part.dynamic()) return null;
        try {
            return part.dynamicQuads().resolve(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] dynamic part '{}' resolver failed", id, part.name(), t);
            return null;
        }
    }

    @Nullable String dynamicCacheKeyValue(PartDef<T> part, T be) {
        if (!part.dynamic() || part.dynamicCacheKey() == null) return null;
        try {
            return part.dynamicCacheKey().apply(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] dynamic part '{}' cacheKey failed", id, part.name(), t);
            return null;
        }
    }

    String cacheKey(PartDef<T> part, @Nullable String dynamicKey) {
        return part.dynamic() ? part.staticCacheKey() + "/" + dynamicKey : part.staticCacheKey();
    }

    /** GPU-держатель части (лениво, на render thread). */
    MachinePartRenderer partRenderer(PartDef<T> part, @Nullable BakedModel partModel,
                                     @Nullable List<net.minecraft.client.renderer.block.model.BakedQuad> dynQuads,
                                     @Nullable String dynamicKey) {
        String key = cacheKey(part, dynamicKey);
        MachinePartRenderer existing = partRenderers.get(key);
        if (existing != null && existing.matches(part, key)) {
            existing.ensureBuilt(partModel, dynQuads);
            return existing;
        }
        MachinePartRenderer created = new MachinePartRenderer(key, part.name(), part.boneId(), part.dynamic());
        MachinePartRenderer raced = partRenderers.putIfAbsent(key, created);
        if (raced != null) {
            raced.ensureBuilt(partModel, dynQuads);
            return raced;
        }
        created.ensureBuilt(partModel, dynQuads);
        return created;
    }

    /**
     * Ленивый вариант {@link #partRenderer}: квад resolver ({@code dynamicQuads}) вызывается
     * ТОЛЬКО когда рендерер ещё не был построен. Иначе (VBO уже в кеше) гора временных
     * BakedQuad создавалась бы каждый кадр впустую — профайлер показывал ~75% времени кадра
     * в retextureAndFixUV/BakedQuad.&lt;init&gt; (танки с жидкостью) + штормmarkSpriteActive у Embeddium.
     */
    MachinePartRenderer partRendererLazy(PartDef<T> part, @Nullable BakedModel partModel,
                                         T be, @Nullable String dynamicKey) {
        String key = cacheKey(part, dynamicKey);
        MachinePartRenderer existing = partRenderers.get(key);
        if (existing != null && existing.matches(part, key)) {
            if (!existing.isAttempted()) {
                existing.ensureBuilt(partModel, dynamicQuads(part, be));
            }
            return existing;
        }
        MachinePartRenderer created = new MachinePartRenderer(key, part.name(), part.boneId(), part.dynamic());
        MachinePartRenderer raced = partRenderers.putIfAbsent(key, created);
        if (raced != null) {
            if (!raced.isAttempted()) {
                raced.ensureBuilt(partModel, dynamicQuads(part, be));
            }
            return raced;
        }
        created.ensureBuilt(partModel, dynamicQuads(part, be));
        return created;
    }

    void flush(Matrix4f projection) {
        for (MachinePartRenderer r : partRenderers.values()) {
            r.flush(projection);
        }
    }

    /** Фаза 2 (после MDI): затухающие инстансы прямых путей — см. InstancedRenderFrame.
     *  Окна рендереров сортируются по дальнему fading-инстансу (back-to-front глобально):
     *  fading идёт с depth-write, несортированный порядок окон depth-reject'ил бы
     *  дальние машины за ближними. */
    void flushFading(Matrix4f projection) {
        if (partRenderers.isEmpty()) {
            return;
        }
        List<MachinePartRenderer> fading = new ArrayList<>(partRenderers.size());
        for (MachinePartRenderer r : partRenderers.values()) {
            if (r.fadingSortKeyDistSq() >= 0f) {
                fading.add(r);
            }
        }
        fading.sort((a, b) -> Float.compare(b.fadingSortKeyDistSq(), a.fadingSortKeyDistSq()));
        for (MachinePartRenderer r : fading) {
            r.flushFading(projection);
        }
    }

    /** Инвалидация GPU-кешей этой спеки (reload/disconnect) — вызывается из RenderCacheManager. */
    void clear() {
        for (MachinePartRenderer r : partRenderers.values()) {
            r.clear();
        }
        partRenderers.clear();
    }

    List<MachinePartRenderer> partRenderersSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(partRenderers.values()));
    }
}
